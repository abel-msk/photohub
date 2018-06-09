package home.abel.photohub.tasks;

import java.util.*;

import home.abel.photohub.connector.prototype.ExceptionUnknownFormat;
import home.abel.photohub.utils.image.ExceptionIncorrectImgFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import home.abel.photohub.connector.prototype.PhotoObjectInt;
import home.abel.photohub.connector.prototype.SiteConnectorInt;
import home.abel.photohub.model.Node;
import home.abel.photohub.model.Site;
import home.abel.photohub.model.Schedule;
import home.abel.photohub.model.SiteRepository;
import home.abel.photohub.service.PhotoService;
import home.abel.photohub.service.SiteService;

public class ScanTask extends BaseTask {
	private static final long serialVersionUID = 1L;
	final Logger logger = LoggerFactory.getLogger(ScanTask.class);
	final Logger bgLog = LoggerFactory.getLogger("taskslog");


	private SiteConnectorInt connector;
	private PhotoService photoService;
	private SiteService siteSvc;

	public ScanTask(Site theSite, SiteService siteService, Schedule schedule, ScheduleProcessing scheduleSvc, PhotoService photoService ) throws Throwable {
		super(theSite,TaskNamesEnum.TNAME_SCAN,schedule,scheduleSvc, true);
		siteSvc = siteService;
		logger.trace("[Init] Site id is " + theSite.getId());
		bgLog.trace("[ScanTask.Init] Site id is " + theSite.getId());

		try {
			this.connector = siteSvc.getOrLoadConnector(theSite);
		}
		catch (Throwable e) {
			setStatus(TaskStatusEnum.ERR,e.getMessage());
			saveLog();
			//siteSvc.updateSite(theSite);
			logger.error("[Init] Site connection error.",e);
			bgLog.error("[ScanTask.Init] Site connection error.",e);
			throw e;
		}
		this.photoService = photoService;
		this.description = ScanTask.getStaticDescription();
		this.displayName = ScanTask.getStaticDisplayName();
	}

	/*-----------------------------------------------------------------------------------
			Self descriptions methods
	 -----------------------------------------------------------------------------------*/


	public static  String getStaticDisplayName() {
		return "Scan site";
	}
	public static  String getStaticDescription() {
		return "Scanning site for new objects and add to local db.";
	}
	public static  boolean isVisible() {
		return true;
	}
	public static Map<String,String> getParamsDescr() {
		return null;
	}


	/*-----------------------------------------------------------------------------------
       Task execution body
 	-----------------------------------------------------------------------------------*/

	@Override
	public void exec() throws Throwable {
		long totalSize = 0;
		Date startDate = new Date();

		try {
			doScann(connector, connector.getRootObjects(), null);

		}
		finally {
			checkDeleted(startDate);
			totalSize = siteSvc.updateSiteSize(getSite());
			logger.debug("[exec] Finished. Sites total size = " + totalSize );
			bgLog.trace("[ScanTask.exec] Finished. Sites total size = " + totalSize );
		}
	}
	
	//@Transactional
	//@Transactional (propagation=Propagation.REQUIRES_NEW)
	private void doScann(SiteConnectorInt connector, List<String> objKeyList, Node parentNode) throws Throwable {
			
		if ( objKeyList != null ) {
			for (String itemKey : objKeyList) {
				try {
					//   Проверяем существует ли такой объект в базе.  Ищем по его ID
					Node theNode = photoService.isPhotoExist(itemKey);
					PhotoObjectInt photoObj = connector.loadObject(itemKey);
					this.printMsg("Process object " + photoObj.getName() + "(" + photoObj.getId() + ")");

					//
					//  Process folder
					//
					if ((photoObj != null) && (photoObj.isFolder())) {
						if (theNode == null) {
							logger.info("[doScann] Add folder to db with id=" + photoObj.getId() + ", name=" + photoObj.getName());
							bgLog.trace("[ScanTask.doScann] Add folder to db with id=" + photoObj.getId() + ", name=" + photoObj.getName());

							theNode = photoService.addObjectFromSite(
									photoObj,
									parentNode != null ? parentNode.getId() : null,
									getSite().getId());
						}
						doScann(connector, photoObj.listSubObjects(), theNode);
					}
					//
					//  Process Object
					//
					else if (photoObj != null) {

						// TODO: Надо проверять существует ли фотка с таким  UUID
						//Node existObjNode = photoService.isPhotoExistByUUID(Item.getMeta().getUnicId());

						if (theNode == null) {
							logger.trace("[doScann] Add " + photoObj.getMimeType() + " object to db with id=" + photoObj.getId() + ", name=" + photoObj.getName());
							bgLog.trace("[ScanTask.doScann] Add " + photoObj.getMimeType() + " object to db with id=" + photoObj.getId() + ", name=" + photoObj.getName());

							theNode = photoService.addObjectFromSite(
									photoObj,
									parentNode != null ? parentNode.getId() : null,
									getSite().getId());
						}
						else {
							logger.trace("[doScann] Object Exist. Skipping. ID=" + photoObj.getId() + ", name=" + photoObj.getName());
							bgLog.trace("[ScanTask.doScann] Object Exist. Skipping. ID=" + photoObj.getId() + ", name=" + photoObj.getName());
						}
					}

					if (theNode != null) {
						photoService.setScanDate(theNode.getPhoto());
					}

				}
				catch (ExceptionUnknownFormat | ExceptionIncorrectImgFormat e) {
					logger.warn("[doScan] Skip object. " + e.getMessage());
					bgLog.warn("[ScanTask.doScann] Skip object. " + e.getMessage());
				}
				catch (RuntimeException e) {
					logger.warn("[doScan] Skip object. " + e.getMessage(), e);
					bgLog.warn("[ScanTask.doScann] Skip object. " + e.getMessage(), e);
				}
				catch (Exception ex) {
					logger.error("[doScan] Error " + ex.getMessage(), ex);
					bgLog.error("[ScanTask.doScann] Error " + ex.getMessage(), ex);
					throw new ExceptionTaskAbort(ex.getMessage(), ex);
				}
			}
		}
	}

	private void checkDeleted(Date fromDate) {

		//logger.trace("[checkDeleted] Find not scanned objects.");

		Iterable<Node> nodes = photoService.listdeletedObjects(fromDate,getSite());
		nodes.forEach(node -> {
			photoService.deleteObject(node,false,false);
			logger.trace("[checkDeleted] Found and delete not scanned object = " + node.getPhoto());
			bgLog.trace("[ScanTask.checkDeleted] Found and delete not scanned object = " + node.getPhoto());
		});

	}
}

package home.abel.photohub.tasks;

import java.util.*;

import home.abel.photohub.connector.prototype.ExceptionUnknownFormat;
import home.abel.photohub.model.*;
import home.abel.photohub.utils.image.ExceptionIncorrectImgFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import home.abel.photohub.connector.prototype.PhotoObjectInt;
import home.abel.photohub.connector.prototype.SiteConnectorInt;
import home.abel.photohub.service.PhotoService;
import home.abel.photohub.service.SiteService;

public class ScanTask extends BaseTask {
	private static final long serialVersionUID = 1L;
	final Logger logger = LoggerFactory.getLogger(ScanTask.class);


	private SiteConnectorInt connector;
	private PhotoService photoService;
	private SiteService siteSvc;

	public ScanTask(Site theSite, SiteService siteService, Schedule schedule, ScheduleProcessing scheduleSvc, PhotoService photoService ) throws Throwable {
		super(theSite,TaskNamesEnum.TNAME_SCAN,schedule,scheduleSvc, true);
		siteSvc = siteService;
		logger.trace("[Init] Site id is " + theSite.getId());

		try {
			this.connector = siteSvc.getOrLoadConnector(theSite);
		}
		catch (Throwable e) {
			setStatus(TaskStatusEnum.ERR,e.getMessage());
			saveLog();
			//siteSvc.updateSite(theSite);
			logger.error("[Init] Site connection error.",e);
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
			logger.trace("[exec] Finished. Sites total size = " + totalSize );
		}
	}
	
	//@Transactional
	//@Transactional (propagation=Propagation.REQUIRES_NEW)
	private void doScann(SiteConnectorInt connector, List<String> objKeyList, Node parentNode) throws Throwable {
			
		if ( objKeyList != null ) {
			for (String itemKey : objKeyList) {
				try {
					int objType = ModelConstants.OBJ_SINGLE;
					PhotoObjectInt photoObj = null;

					this.printMsg("Process object: " + itemKey);

					//   Проверяем существует ли такой объект в базе.  Ищем по его ID
					Node theNode = photoService.isPhotoExist(itemKey);

					if ( theNode != null) {
						objType = theNode.getPhoto().getType();
					}
					else {
						photoObj = connector.loadObject(itemKey);
						if ( photoObj.isFolder() ) {
							objType = ModelConstants.OBJ_FOLDER;
						}
					}

					//
					//  Process folder
					//
					if (objType == ModelConstants.OBJ_FOLDER) {

						//   Для фолдера грузим объект для сайта влюбом случае  т.к. внутри папки тоже надо сканировать
						if ( photoObj == null) {
							photoObj = connector.loadObject(itemKey);
						}

						//   Этого объекта нет в базе - сохраняем
						if (theNode == null) {
							logger.trace("[doScann] Add folder to db with id=" + photoObj.getId() + ", name=" + photoObj.getName());

							theNode = photoService.addObjectFromSite(
									photoObj,
									parentNode != null ? parentNode.getId() : null,
									getSite().getId());
						}

						//   Идем сканировать внутрь
						logger.trace("[doScann] Scab sub folder. ID=" + photoObj.getId() + ", name=" + photoObj.getName());

						doScann(connector, photoObj.listSubObjects(), theNode);
					}
					//
					//  Process Object
					//
					else {

						//   Объекта нет в бвзе, загружаем и сохраняем
						//
						if (theNode == null) {

							//   Загружаем
							photoObj = connector.loadObject(itemKey);

							//   Проверяем на наличие еакой фитки в другом объекте
							// TODO: Надо проверять существует ли фотка с таким  UUID
							//Node existObjNode = photoService.isPhotoExistByUUID(Item.getMeta().getUnicId());

							logger.trace("[doScann] Add " + photoObj.getMimeType() + " object to db with id=" + photoObj.getId() + ", name=" + photoObj.getName());

							//   Загружаем
							theNode = photoService.addObjectFromSite(
									photoObj,
									parentNode != null ? parentNode.getId() : null,
									getSite().getId());
						}
						else {
							logger.trace("[doScann] Object Exist. Skipping. ID=" + theNode.getPhoto().getOnSiteId() + ", name=" + theNode.getPhoto().getName());
						}
					}

					if (theNode != null) {
						photoService.setScanDate(theNode.getPhoto());
					}

				}
				catch (ExceptionUnknownFormat | ExceptionIncorrectImgFormat e) {
					logger.warn("[doScan] Skip object. " + e.getMessage());
				}
				catch (RuntimeException e) {
					logger.warn("[doScan] Skip object. " + e.getMessage(), e);
				}
				catch (Exception ex) {
					logger.error("[doScan] Error " + ex.getMessage(), ex);
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
		});

	}
}

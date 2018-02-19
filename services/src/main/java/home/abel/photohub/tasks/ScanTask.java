package home.abel.photohub.tasks;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	private SiteConnectorInt connector;
	private PhotoService photoService;
	private SiteService siteSvc;

	public ScanTask(Site theSite, SiteService siteService, Schedule schedule, ScheduleProcessing scheduleSvc, PhotoService photoService ) throws Throwable {
		super(theSite,TaskNamesEnum.TNAME_SCAN,schedule,scheduleSvc, true);
		siteSvc = siteService;
		logger.trace("[ScanTask.Init] Site id is " + theSite.getId());

		try {
			this.connector = siteSvc.getOrLoadConnector(theSite);
		}
		catch (Throwable e) {
			setStatus(TaskStatusEnum.ERR,e.getMessage());
			saveLog();
			//siteSvc.updateSite(theSite);
			logger.error("[ScanTask] Site connection error.",e);
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
		doScann(connector.getRootObjects(), null);	
	}
	
	@Transactional
	//@Transactional (propagation=Propagation.REQUIRES_NEW)
	public void doScann(List<PhotoObjectInt> objList, Node parentNode) throws Throwable {
			
		if ( objList != null ) {
			try {		
				for (PhotoObjectInt Item: objList) {

					//   Проверяем существует ли такой объект в базе.  Ищем по его ID
					Node theNode = photoService.isPhotoExist(Item.getId());
					
					//Photo dbPhotoObject = photoService.isPhotoExist(Item.getId());
					
					//   Если объект еще не существует - добавляем  в базу
					if ( (theNode == null) || Item.isFolder()) {
						
						// TODO: Надо проверять существует ли фотка с таким  UUID

						logger.trace("Add object to db with id="+Item.getId());
						this.printMsg("Process object " + Item.getName()+"("+Item.getId()+")");
						
						if (theNode == null) {
							theNode = photoService.addObjectFromSite(
									Item,
									parentNode != null?parentNode.getId():null,
									getSite().getId());
							}

						//  Если это фолдер то сканируем его содержимое
						if ( Item.isFolder() ) {
							doScann(Item.listSubObjects(), theNode);
						}
					} else {
						logger.trace("Skip, оbject with id=" + Item.getId() + " exist.");
					}
				}
			} catch (Exception ex) {
				//logger.error(ex.getMessage(),ex);
				throw new ExceptionTaskAbort(ex.getMessage(),ex);
			}
		logger.debug("[ScanTask.doScann] Finished success.");
		}
	}
}

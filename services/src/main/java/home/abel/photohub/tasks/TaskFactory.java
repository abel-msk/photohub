package home.abel.photohub.tasks;


import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import home.abel.photohub.model.Site;
import home.abel.photohub.model.SiteRepository;
import home.abel.photohub.service.PhotoService;
import home.abel.photohub.service.SiteService;



@Service
public class TaskFactory {
	final Logger logger = LoggerFactory.getLogger(TaskFactory.class);

	Map<TaskNamesEnum,String> userTaskNames = new HashMap<TaskNamesEnum,String>();
	Map<TaskNamesEnum,String> systemTaskNames = new HashMap<TaskNamesEnum,String>();
	
	@Autowired
	SiteService siteService;
	
	@Autowired
	PhotoService photoService;
	
	@Autowired
	SiteRepository siteRepo;
	
	@PersistenceContext 
	EntityManager em; 
	
	public TaskFactory() {
		//logger.trace("!!!!   TASK_FACTORY Class created.");
	}
	
	
	@PostConstruct
	public void Init() throws Exception {
		logger.trace("[TaskFactory.Init] Load available task names.");
			 
	    for(TaskNamesEnum enumValue : TaskNamesEnum.values()) {
	    	if ( enumValue.isScheduled()) {
		    	if(enumValue.isUserTask())  userTaskNames.put(enumValue , enumValue.getDescr());
		    	else  systemTaskNames.put(enumValue , enumValue.getDescr());
	    	}
	    }

	}
	 
	
	/**
	 * 	  Класс возвращает инициализированный инстанс класса FutureBaseTask 
	 *    для задачи указанной в параметре.  
	 *    Задача срздается для Сайта указанного в качестве вторго параметра
	 *    
	 * @param taskName
	 * @param siteId
	 * @return
	 * @throws Throwable
	 */
	public BaseTask createTask(String taskName, String siteId) throws Throwable {
		return createTask(TaskNamesEnum.valueOf(taskName),siteId);
	}
	public BaseTask createTask(String taskName, Site theSite) throws Throwable {
		return createTask(TaskNamesEnum.valueOf(taskName),theSite);
	}
	
	/**
	 * 
	 * 	  Класс возвращает инициализированный инстанс класса FutureBaseTask 
	 *    для задачи указанной в параметре.  
	 *    Задача срздается для Сайта указанного в качестве вторго параметра
	 *    
	 * @param taskName
	 * @param siteId
	 * @return
	 * @throws Throwable
	 */
	public BaseTask createTask(TaskNamesEnum taskName, String siteId) throws Throwable {
		Site theSite = null;
		if (siteId != null) {
			theSite = siteService.getSite(siteId);
		}
		return createTask(taskName,theSite);
	}
	
	/** 
	 *    Класс возвращает инициализированный инстанс класса FutureBaseTask 
	 *    для задачи указанной в параметре.  
	 *    Задача срздается для Сайта указанного в качестве вторго параметра
	 *    
	 * @param taskName
	 * @param site
	 * @return
	 * @throws Exception 
	 */
	public BaseTask createTask(TaskNamesEnum taskName, Site theSite) throws Throwable {
		BaseTask task = null;
		switch (taskName) {
			case TNAME_SCAN:
				task = new ScanTask(theSite, siteService, photoService);
        		break;
			case TNAME_CLEAN:	
				task = new CleanTask(theSite, siteService);
				break;
			case TNAME_REMOVE:	
				task = new RemoveSiteTask(theSite, siteService);
				break;
			case TNAME_EMPTY:	
				task = new EmptyTask(theSite, siteService);
				break;
			default: {
					throw new IllegalArgumentException("Cannot create task "+ taskName.toString());
				}
		}
		//task.setEntityManagerFactory(em.getEntityManagerFactory());
		return task;
	}
		
	
	/**
	 *    Возвращает список существующих задач
	 * @return
	 */
	public Map<TaskNamesEnum,String> getUserTaskNames() {		
		return userTaskNames;
	}

	public Map<TaskNamesEnum,String> getSystemTaskNames() {		
		return systemTaskNames;
	}
		
	/**
	 * 
	 *	Проверяет сушествует ли задача с таким именем
	 *
	 * @param taskName
	 * @return
	 */
	public boolean isTaskName(String taskName) {
		boolean isValud = true;
		try {
//			for ( TaskNamesEnum tn: TaskNamesEnum.values())
//			TaskNamesEnum.getByName(taskName);
			TaskNamesEnum.valueOf(taskName);
		} catch (IllegalArgumentException e) {
			isValud = false;
		}
		
		return isValud; 
	}
	
	
	
}

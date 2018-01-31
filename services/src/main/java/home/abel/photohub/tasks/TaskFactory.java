package home.abel.photohub.tasks;


import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import home.abel.photohub.model.Schedule;
import home.abel.photohub.model.TaskParam;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import home.abel.photohub.model.Site;
import home.abel.photohub.service.PhotoService;
import home.abel.photohub.service.SiteService;

import static home.abel.photohub.tasks.TaskNamesEnum.TNAME_EMPTY;
import static home.abel.photohub.tasks.TaskNamesEnum.TNAME_SCAN;


@Service
public class TaskFactory {
	final Logger logger = LoggerFactory.getLogger(TaskFactory.class);

	List<TaskDescription>classesDescrList = null;
	
	@Autowired
	SiteService siteService;
	
	@Autowired
	PhotoService photoService;

	@Autowired
	ScheduleProcessing scheduleSvc;
	
//	@Autowired
//	SiteRepository siteRepo;

	@Autowired
	Queue queue;

	@Autowired
	SiteService SiteSvc;

	@PersistenceContext 
	EntityManager em; 
	
	public TaskFactory() {
		//logger.trace("!!!!   TASK_FACTORY Class created.");
	}
	


	@PostConstruct
	/**
	 *   Формируем список описаний задач на основе статических методов определенных в классе задачи
	 */
	public void Init() throws Exception {

		classesDescrList = new ArrayList<>();

		TaskDescription td  = getTasksClassDescr(ScanTask.class.getName());
		td.setName(TNAME_SCAN.toString());
		logger.trace("Name: "+td.getName()+", description: "+td.getDescription());
		classesDescrList.add(td);


		td  = getTasksClassDescr(EmptyTask.class.getName());
		td.setName(TNAME_EMPTY.toString());
		logger.trace("Name: "+td.getName()+", description: "+td.getDescription());
		classesDescrList.add(td);

	}


	/*---------------------------------------------------------------------------------------------
	 *    Сбор информации по всем классам-задачам определенным в пакете
	 ---------------------------------------------------------------------------------------------*/
	/**
	 *
	 *    Загружает класс по указаному имени,
	 *    обращается к статическим методам что-бы взять описание класса, список параметров и тип класса visible/unvisible
	 *
	 * @param className  название класса
	 * @return Возвращает объект с заполнеными значениями описания
	 */
	public TaskDescription getTasksClassDescr(String className) {

		logger.trace("Load class " +className+ " description.");
		TaskDescription taskDescr = null;
		ClassLoader currentClassLoader = this.getClass().getClassLoader();
		try {
			Class<?> TaskImpClass = currentClassLoader.loadClass(className);

			taskDescr = new TaskDescription();

			if (BaseTask.class.isAssignableFrom(TaskImpClass)) {
				taskDescr.setName(className);

				try {
					Method m = TaskImpClass.getMethod("getStaticDisplayName", (Class<?>[])null);
					//logger.trace("Invoke method " +m.getName() + " of class " + className);
					taskDescr.setDisplayName((String) m.invoke(null));
				} catch (Exception e) {
					logger.warn("Cannot invoke static methods for class " + TaskImpClass.getName() + "method getDesc, Error:" + e.getMessage());
				}

				try {
					Method m = TaskImpClass.getMethod("getStaticDescription", (Class<?>[])null);
					//logger.trace("Invoke method class " +m.getName() + "description.");
					taskDescr.setDescription((String) m.invoke(null));
				} catch (Exception e) {
					logger.warn("Cannot invoke static methods for class " + TaskImpClass.getName() + "method getDesc, Error:" + e.getMessage());
				}
				try {
					Method m = TaskImpClass.getMethod("getParamsDescr", (Class<?>[])null);
					//logger.trace("Invoke method class " +m.getName() + "description.");
					taskDescr.setParams((Map<String, String>) m.invoke(null));
				} catch (Exception e) {
					logger.warn("Cannot invoke static methods for class " + TaskImpClass.getName() + "method getParamsDescr, Error:" + e.getMessage());
				}
				try {
					Method m = TaskImpClass.getMethod("isVisible", (Class<?>[])null);
					//logger.trace("Invoke method class " +m.getName() + "description.");
					taskDescr.setVisible((boolean) m.invoke(null));
				} catch (Exception e) {
					logger.warn("Cannot invoke static methods for class " + TaskImpClass.getName() + " method isVisible, Error:" + e.getMessage());
				}
			}
		}
		catch (Exception e1) {
			logger.warn("Cannot find class " + className + ", Error:" + e1.getMessage());
		}
		return taskDescr;
	}



	/*---------------------------------------------------------------------------------------------
	 *     Описание задач и параметров  подготовка списка пустых переметров
	 ---------------------------------------------------------------------------------------------*/

	public List<TaskDescription> getTasksDescr() {
		return classesDescrList;
	}

	/**
	 * Возвращает TaskDescription для задачи с именем введенным в качестве параметра.
	 * @param taskName
	 * @return
	 */
	public TaskDescription getTasksDescr(String taskName) {
		for (TaskDescription tTmpl : classesDescrList ) {
			if (tTmpl.getName().equals(taskName)) {
				return tTmpl;
			}
		}
		return  null;
	}

	/**
	 *   Возвращает  подготовленный список объектов TaskParam c именами, взятыми из списка дефолтных параметров
	 *   и пустыми значениям.
	 *
	 * @param taskName
	 * @return
	 */
	public List<TaskParam> getEmptyParamList(String taskName) {
		TaskDescription tds = getTasksDescr(taskName);
		List<TaskParam> emptyParamList = null;

		if ((tds != null) && (tds.getParams() != null)) {
			for(String pName : tds.getParams().keySet() ) {
				TaskParam tp = new TaskParam();
				tp.setName(pName);
				if (emptyParamList == null) {
					emptyParamList = new ArrayList<>();
				}
				emptyParamList.add(tp);
			}
		}
		return emptyParamList;
	}

	/*---------------------------------------------------------------------------------------------
	 *     СОЗДАНИЕ ЗАДАЧИ
	 ---------------------------------------------------------------------------------------------*/

	/**
	 * Создается задача без рассписания  с указаным именем и для указаного сайта
	 * @param taskName имя задаи
	 * @param siteId  сайт задачи
	 * @return
	 * @throws Throwable
	 */
	public BaseTask createTask( String taskName, String siteId ) throws Throwable {
		Schedule schedule = new Schedule();
		schedule.setEnable(false);
		Site theSite = null;
		if (siteId != null) {
			theSite = siteService.getSite(siteId);
			schedule.setSite(theSite);
		}
		return createTask(TaskNamesEnum.valueOf(taskName),theSite,schedule);
	}


	/**
	 *	Создается задача с указаным именем, для указаного сайта и с указаным расписанием
	 * 	в текстовом виде для задания рассписания на основе  CRON параметров
	 *
	 * @param taskName Название задачи
	 * @param siteId   ID сайта для которого запускается задача
	 * @param theSchedule Рассписание
	 * @return
	 * @throws Throwable
	 */
	public BaseTask createTask(
			String taskName,
			String siteId,
			Schedule theSchedule
	) throws Throwable {

		Site theSite = null;
		if (siteId != null) {
			theSite = siteService.getSite(siteId);
			theSchedule.setSite(theSite);
		}
		ScheduleProcessing.isValidCron(theSchedule);
		return createTask(TaskNamesEnum.valueOf(taskName),theSite,theSchedule);
	}

	/**
	 * Создается задача с указаным именем, для указаного сайта и с указаным расписанием
	 * в текстовом виде для задания рассписания на основе  CRON параметров
	 *
	 * @param taskName имя задачи
	 * @param siteId   сайт задачи
	 * @param seconds  на какой секунду запускать (чать CRON параметра)
	 * @param minute   минуты
	 * @param hour     часы
	 * @param dayOfMonth  день месяца
	 * @param month     месяй
	 * @param dayOfWeek  день недели
	 * @return
	 * @throws Throwable
	 */
	public BaseTask createTask(
			String taskName,
			String siteId,
			String seconds,
			String minute,
			String hour,
			String dayOfMonth,
			String month,
			String dayOfWeek
	) throws Throwable {

		Schedule schedule = ScheduleProcessing.validateCron(new Schedule(),  seconds, minute, hour, dayOfMonth,  month, dayOfWeek);
		schedule.setEnable(true);

		Site theSite = null;
		if (siteId != null) {
			theSite = siteService.getSite(siteId);
			schedule.setSite(theSite);
		}
		return createTask(TaskNamesEnum.valueOf(taskName),theSite,schedule);
	}

	/**
	 *    Класс возвращает инициализированный инстанс класса FutureBaseTask 
	 *    для задачи указанной в параметре.  
	 *    Задача срздается для Сайта указанного в качестве вторго параметра
	 *    
	 * @param taskName
	 * @param theSite
	 * @return
	 * @throws Exception 
	 */
	public BaseTask createTask(TaskNamesEnum taskName, Site theSite, Schedule theSchedule ) throws Throwable {
		theSchedule.setSite(theSite);

		logger.trace("[createTask] create Task=" + taskName + ", for Site=" + Queue.site2Id(theSite)+", with Schedule=" +theSchedule );

		if ( theSchedule.getParams() == null) {
			theSchedule.setParams(new ArrayList<>());
		}

		if (getTasksDescr(taskName.toString()) != null) {
			Map<String, String> paramsTemplate = getTasksDescr(taskName.toString()).getParams();
			if (paramsTemplate != null) {
				for (String tParamName : paramsTemplate.keySet()) {
					boolean found = false;
					for (TaskParam realTParam : theSchedule.getParams()) {
						if (tParamName.equalsIgnoreCase(realTParam.getName())) {
							found = true;
							break;
						}
					}
					if (!found) {
						theSchedule.addParam(new TaskParam(tParamName, "", "string"));
					}
				}
			}
		}


		BaseTask task = null;
		switch (taskName) {
			case TNAME_SCAN:
				task = new ScanTask(theSite, SiteSvc, theSchedule ,scheduleSvc, photoService);
        		break;
			case TNAME_CLEAN:	
				task = new CleanTask(theSite,SiteSvc, theSchedule, scheduleSvc);
				break;
			case TNAME_REMOVE:	
				task = new RemoveSiteTask(theSite,SiteSvc, theSchedule, scheduleSvc);
				break;
			case TNAME_EMPTY:
				task = new EmptyTask(theSite,theSchedule, scheduleSvc);
				break;
			default: {
					throw new IllegalArgumentException("Cannot create task "+ taskName.toString());
				}
		}



		return task;

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
			TaskNamesEnum.valueOf(taskName);
		} catch (IllegalArgumentException e) {
			isValud = false;
		}
		
		return isValud; 
	}
}

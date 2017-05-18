package home.abel.photohub.service;

import java.util.List;
import java.util.Map;

import home.abel.photohub.model.*;
import home.abel.photohub.tasks.BaseTask;
import home.abel.photohub.tasks.ExceptionTaskAbort;
import home.abel.photohub.tasks.TaskFactory;
import home.abel.photohub.tasks.TaskNamesEnum;
import home.abel.photohub.tasks.TaskStatusEnum;

import javax.annotation.PostConstruct;

//import javafx.collections.transformation.SortedList;

import home.abel.photohub.utils.ScheduleComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ScheduledFuture;
import java.util.regex.Pattern;

@Service
public class ScheduleService {
	final Logger logger = LoggerFactory.getLogger(ScheduleService.class);
	
	public static final String NULL_SITE = "system";

	
	@Autowired
	TaskQueueService queue;
	
	@Autowired
	SiteService siteService;
	
	@Autowired
	PhotoService photoService;
	
	@Autowired
	SiteRepository siteRepository;
	
	@Autowired
	TaskFactory taskFactory;
	
	@Autowired
	SiteRepository siteRepo;
	
	@Autowired
	TaskRepository taskRepository;
	
	@Autowired
	ScheduleRepository scheduleRepository;
	
	
	@Autowired
	ThreadPoolTaskScheduler threadPoolTaskScheduler;
	
	
	public ScheduleService() {
		
	}
	
	protected Map<String,Map<TaskNamesEnum,BaseTask>> sitesSchedQueueMap  = 
			new java.util.concurrent.ConcurrentHashMap<String,Map<TaskNamesEnum,BaseTask>>();
	
	
	@PostConstruct
	/**
	 * Сразу после старта, загружает все задачи с рассписанием в очередь запуска по рассписанию
	 * @throws Exception
	 */
	public void Init() throws Throwable {
		logger.trace("[SceduleService.Init] Arm saved scheduled jobs.");
		
		Iterable<Schedule> schList = scheduleRepository.findAll();
		for (Schedule sTask: schList ) {
			try {
				BaseTask task = taskFactory.createTask(TaskNamesEnum.valueOf(sTask.getTaskName()),sTask.getSite());
				putTask(task,sTask);
			} catch (Exception e) {
				logger.warn("[SceduleService.Init] Cannot run task eith name="+ sTask.getTaskName()
						+", for Site="+(sTask.getSite() == null?NULL_SITE:sTask.getSite().getId())+ ". Error="+e.getMessage());
			}

		}
	}
	

	
	/*************************************************************************************************
	 * 
	 * 
	 *      RETRIEVE SCHEDULE OR TASK
	 * 
	 * 
	 *************************************************************************************************/		
	
	
	/**
	 *	Возвращает рассписание для задачи сайта
	 *
	 * @param site
	 * @param taskName
	 * @return
	 */
	public Schedule getSchedule(Site site, TaskNamesEnum taskName) {
		return site.getSchedule(taskName.toString());
	}
	
	/**
	 * 	Возвращает рассписание задачи(по имени) для сайта.  
	 * @param theSiteId
	 * @param taskName
	 * @return
	 */
	public Schedule getSchedule(String theSiteId, TaskNamesEnum taskName) {
		List<Schedule> schList = scheduleRepository.findBySiteIdAndTaskName(theSiteId, taskName.toString());
		return schList.get(0);
	}
	
	/**
	 * Возвращает список рассписаний задач для сайта
	 * @param siteId
	 * @return
	 * @throws Exception
	 */

	public Iterable<Schedule> getSchedules(String siteId) throws Exception {

		Iterable<Schedule> result = null;
		if ( siteId != null ) {
			result = scheduleRepository.findAll(QSchedule.schedule.site.id.eq(siteId));
		}
		else {
			result = scheduleRepository.findAll(QSchedule.schedule.site.id.isNull());
		}
		
		return result;
	}



	/**
	 *
	 *    Возвращает список  возможных задачь для запуска по рассписанию
	 *    Если рассписание для задачи установленно добавляем его вместо пустого
	 *
	 * @param siteId
	 * @return
	 */

	public List<Schedule>getUserSchedList(String siteId) throws Exception {

		//Site theSite = siteRepo.findOne(siteId);
		Iterable<Schedule> trList = getSchedules(siteId);

		List<Schedule> ShedList = new home.abel.photohub.utils.SortedList<>(new ScheduleComparator());

		for( TaskNamesEnum taskNameEnum: TaskNamesEnum.values() ) {
			if (taskNameEnum.isScheduled()) {

				boolean found = false;
				for (Schedule theShed : trList) {
					if (taskNameEnum.toString().equalsIgnoreCase(theShed.getTaskName())) {
						ShedList.add(theShed);
						found = true;
						break;
					}
				}

				if (!found) {
					Schedule EmptySched = new Schedule();
					EmptySched.setTaskName(taskNameEnum.toString());
					ShedList.add(EmptySched);
				}
			}
		}

		return ShedList;
	}


	/**
	 *     Возвращает задачу из Очереди
	 * @param siteId
	 * @param taskName
	 * @return
	 * @throws Exception
	 */
	public BaseTask getScheduledTask(String siteId, String taskName) throws Exception {
		return getTaskFromQueue(siteId, TaskNamesEnum.valueOf(taskName));
	}	

		
	/**
	 *   Возвращает задачу из Очереди
	 * @param site
	 * @param taskName
	 * @return
	 * @throws Exception
	 */
	public BaseTask getScheduledTask(Site site, TaskNamesEnum taskName) throws Exception {
		if (site != null ) {
			return getTaskFromQueue(site.getId(), taskName);
		}
		return getTaskFromQueue(null, taskName);
	}
	/**
	 * 	Возвращает задачу из Очереди
	 * @param siteId
	 * @param taskName
	 * @return
	 * @throws Exception
	 */
	public BaseTask getScheduledTask(String siteId, TaskNamesEnum taskName) throws Exception {
		return getTaskFromQueue(siteId, taskName);
	}	
	

	/**
	 * Возвращает список доступстимых имен задач для запуска по порассписанию пользователем 
	 * @return
	 */
	public Map<TaskNamesEnum,String> getUserTaskNames() {
		return taskFactory.getUserTaskNames();
	}
	/**
	 * Возвращает список доступстимых имен задач для запуска по порассписанию системой 
	 * @return
	 */
	public Map<TaskNamesEnum,String> getSystemTaskNames() {
		return taskFactory.getSystemTaskNames();
	}
	

	/**
	 * 
	 *    Возвращает задаче из очереди рассписания.
	 * 
	 * 
	 * @param siteId
	 * @param name
	 * @return
	 * @throws Exception
	 */
	protected BaseTask getTaskFromQueue(String siteId, TaskNamesEnum name) throws Exception {
		Map<TaskNamesEnum,BaseTask>sitesTasks = sitesSchedQueueMap.get((siteId == null?NULL_SITE:siteId));
		if (sitesTasks != null) {
			BaseTask task =  sitesTasks.get(name);
			if (task != null) {
				return task;
			}
		}
		logger.warn("[ScheduledService.getTaskFromQueue] Task "+ name+", for Site id="+siteId+"  Not Found.");
		return null;
	}
	
	
	/*************************************************************************************************
	 * 
	 * 
	 *      CHECK SCHEDULE
	 * 
	 * 
	 *************************************************************************************************/		
	
	/**
	 *  
	 *	Определяет работает ли задача по рассписанию с имененм у указанного сайта.
	 * 
	 * @param name
	 * @param siteId
	 * @return
	 * @throws Exception 
	 */
	public boolean isSchedTaskRunning(String siteId, TaskNamesEnum name) throws Exception {
		BaseTask task = getTaskFromQueue(siteId,name);
		if (task == null) return false;
		if (task.getStatus()==TaskStatusEnum.RUN)  return true;
		return false;
	}

	public boolean isSchedTaskInQueue(String siteId,TaskNamesEnum name) throws Exception {
		if (getTaskFromQueue(siteId,name) == null ) return false;
		return true;
	}
	
	
	/*************************************************************************************************
	 * 
	 * 
	 *      SET SCHEDULE
	 * 
	 * 
	 *************************************************************************************************/	
	
	/**
	 * 
	 *	Сохраняеи в базе рассписание для задачи. Добавляет задачу в очередь задач.
	 *	Если задача с таким именем и для этого сайта уже существуе, заменяет ее.
	 *  
	 * @param siteId
	 * @throws Throwable
	 */
	@Transactional
	public BaseTask setShcedule(
			String siteId,
			TaskNamesEnum taskName, 
			String seconds,
			String minute,
			String hour,
			String dayOfMonth,
			String month,
			String dayOfWeek) throws Throwable {
	
		Schedule theSchedule = null;
		Site theSite = null;
		
		if (  ! taskName.isScheduled() ) {
			throw new ExceptionInvalidArgument("Task "+taskName+" cannot be scheduled.");
		}
		
		List<Schedule> schList = scheduleRepository.findBySiteIdAndTaskName(siteId, taskName.toString());
		
		if ((schList == null) || (schList.size() == 0)) {
			if ( siteId != null ) {
				theSite = siteService.getSite(siteId);
				if (theSite == null) {
					throw new ExceptionInvalidArgument("Cannot set schedule. Site with id="+siteId+" not found.");
				}
			}
			theSchedule = new Schedule();
			theSchedule.setTaskName(taskName.toString());
			if ( theSite != null) {
				theSite.addScedule(theSchedule);
				theSite = siteRepo.save(theSite);
			}
		}
		else {
			theSchedule = schList.get(0);
		}
		
		
		
		//   SECONDS
		if (seconds==null) {
			theSchedule.setSeconds("*");
		}
		else if ( Pattern.matches("^\\*|[*0-9,-/]+$",seconds)) {  
			theSchedule.setSeconds(seconds);
		}
		else {
			throw new ExceptionInvalidArgument("Cannot set schedule. Incorect seconds value=" +seconds );
		}
		
		//  MINUTE
		if (minute==null) {
			theSchedule.setMinute("*");
		}
		else if (Pattern.matches("^\\*|[*0-9,-/]+$",minute)) {
			theSchedule.setMinute(minute);
		}
		else {
			throw new ExceptionInvalidArgument("Cannot set schedule. Incorect minute value=" + minute );
		}		
	
		//	HOUR
		if (hour==null) {
			theSchedule.setHour("*");
		} 
		else if ( Pattern.matches("^\\*|[*0-9,-/]+$",hour)) {
			theSchedule.setHour(hour);
		}
		else {
			throw new ExceptionInvalidArgument("Cannot set schedule. Incorect hour value=" + hour );
		}

		//   DAY OF MONTH
		if (dayOfMonth==null) {
			theSchedule.setDayOfMonth("*");
		}
		else if (Pattern.matches("^\\*|[*0-9,-/]+$",dayOfMonth)) {
			theSchedule.setDayOfMonth(dayOfMonth);
		}
		else {
			throw new ExceptionInvalidArgument("Cannot set schedule. Incorect setDayOfMonth value=" + dayOfMonth );
		}
		
		//  MONTH
		if (month==null) {
			theSchedule.setMonth("*");
		} 
		else if (Pattern.matches("^\\*|[*0-9,-/]+$",month)) {
			theSchedule.setMonth(month);
		}
		else {
			throw new ExceptionInvalidArgument("Cannot set schedule. Incorect month value=" + month );
		}
		
		//   DAY OF WEEK
		if (dayOfWeek==null) {
			theSchedule.setDayOfWeek("*");
		} 
		else if (Pattern.matches("^\\*|[*0-9,-/]+$",dayOfWeek)) {
			theSchedule.setDayOfWeek(dayOfWeek);
		}
		else {
			throw new ExceptionInvalidArgument("Cannot set schedule. Incorect month value=" + dayOfWeek );
		}
		
		
//		if (theSite != null) {
//			theSite.addScedule(theSchedule);
//			siteRepository.save(theSite);	
//		}
//		else {
			scheduleRepository.save(theSchedule);	
//		}
		
		BaseTask queuedTask  = getTaskFromQueue(siteId,taskName);
		
		//   Такой задачи в очереди нет.  Тогда создаем новую.
		if (queuedTask == null) {
			queuedTask = (BaseTask)taskFactory.createTask(theSchedule.getTaskName(),theSite);
		}		
		
		//   Задача в очереди, удаляем из очереди.
		else {
			removeTask(queuedTask);
		}
		
		if (queuedTask == null )  {
			throw new ExceptionTaskAbort("[ScheuleService.setSchedule] Cannot create task");
		}
		BaseTask theTask = putTask(queuedTask, theSchedule);
		logger.debug("[ScheuleService.setSchedule] Change or create sheduled task="+theTask.getTag()+", for site="+
						(theTask.getSite()==null?NULL_SITE:theTask.getSite()));
		return theTask;
	}
	
	/**
	 * 
	 *  Запускает задачу на исполнение в соответствии с рассписанием
	 *  Рассписание задается в формате cron
	 * 
	 * *https://docs.spring.io/spring/docs/current/spring-framework-reference/html/scheduling.html
	 * 
	 * @param task
	 * @return
	 * @throws Exception
	 */
	protected BaseTask putTask(BaseTask task, Schedule theSchedule) throws Exception {
		CronTrigger cronTrigger = new CronTrigger(theSchedule.toString());	

		//cronTrigger.nextExecutionTime(triggerContext)
		
		ScheduledFuture<?> taskProcess = threadPoolTaskScheduler.schedule(task, (Trigger)cronTrigger);		
		task.setThisTaskProcess(taskProcess);
		logger.debug("[ScheduleService.putTask]  Put task=" + task.getTag()+" to schedule for site="+ (task.getSite()==null?"system":task.getSite()));
		
		Map<TaskNamesEnum,BaseTask>schedSitesTasks = sitesSchedQueueMap.get((task.getSite() == null?NULL_SITE:task.getSite().getId()));	
		
		if (schedSitesTasks == null) {	
			schedSitesTasks = new java.util.concurrent.ConcurrentHashMap<TaskNamesEnum,BaseTask>();
			sitesSchedQueueMap.put((task.getSite() == null?NULL_SITE:task.getSite().getId()), schedSitesTasks);   
			//schedSitesTasks.put(task.getSite().getId(),task);	
		}
		schedSitesTasks.put(task.getTag(), task);
		
		return task;
	}
	
	
	/*************************************************************************************************
	 * 
	 * 
	 *      REMOVE TASK
	 * 
	 * 
	 *************************************************************************************************/
	
	public BaseTask removeTask(String siteId, TaskNamesEnum taskName) throws Exception {	
		BaseTask theTask = getTaskFromQueue(siteId, taskName);
		if ( theTask== null ) { return null; }		
		return removeTask(theTask);
	}
	
	/**
	 * 
	 *    Удаляет задачу из рассписания и удаляет рассписание с сайта.
	 *  
	 * @return
	 * @throws Exception
	 */
	@Transactional	
	public BaseTask removeTask(BaseTask theTask) throws Exception {
		
		logger.trace("[ScheduledService.removeTask] Remove task="+theTask.getTag()+" with Site=" + theTask.getSite() );

		//   Remove site from queue
		BaseTask removedTask = removeTaskFromQueue(theTask);

		List<Schedule> schList = scheduleRepository.findBySiteIdAndTaskName(
				(theTask.getSite()==null?null:theTask.getSite().getId()),
				removedTask.getTag().toString());
		
		for (Schedule schedule : schList) {
			scheduleRepository.delete(schedule);
			Site theSite = schedule.getSite();
			if ( theSite != null) {
				theSite.removeSchedule(schedule);
				siteRepo.save(theSite);
			}
		}
		

		return removedTask;
	}	
	
	/**
	 * Удалить задачу из очереди задач с рассписанием и останавливаем задачу
	 * @param task
	 * @return
	 */
	protected BaseTask removeTaskFromQueue(BaseTask task) {
		BaseTask removedTask = null;
		Map<TaskNamesEnum,BaseTask>sitesTasks = sitesSchedQueueMap.get((task.getSite() == null?NULL_SITE:task.getSite().getId()));	
		if (sitesTasks != null) {
			removedTask= sitesTasks.remove(task.getTag());
		}
		if (removedTask == null) {
			removedTask = task;
		}
		removedTask.getThisTaskProcess().cancel(true);
		
		logger.debug("[TaskQueueService.removeScheduledTask] Task name="+removedTask.getTag()+", stopped and removed from schedule queue.");
		
		return task;
	}

	
	/*************************************************************************************************
	 * 
	 * 
	 *      LOG
	 * 
	 * 
	 *************************************************************************************************/
	public List<TaskRecord> getLogByTask(String siteId, TaskNamesEnum taskName) throws Exception {		
		return taskRepository.findBySiteBeanIdAndName(siteId, taskName.toString());
	}
	
	public List<TaskRecord> getLog(String siteId) throws Exception {		
		return taskRepository.findBySiteBeanId(siteId);
	}	
	
}

package home.abel.photohub.service;

import home.abel.photohub.model.*;
import home.abel.photohub.model.TaskRecordRepository;
import home.abel.photohub.tasks.BaseTask;
import home.abel.photohub.tasks.TaskFactory;
import home.abel.photohub.tasks.ScheduleProcessing;
import home.abel.photohub.tasks.Queue;
import home.abel.photohub.tasks.TaskStatusEnum;
import home.abel.photohub.tasks.TaskNamesEnum;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;


import com.querydsl.jpa.impl.JPAQuery;

//import com.mysema.query.jpa.JPASubQuery;
//import com.mysema.query.jpa.impl.JPAQuery;

/**
 *    This class contains all necessary methods for work with task QUEUE
 */

@Service
public class TaskQueueService {
	final Logger logger = LoggerFactory.getLogger(TaskQueueService.class);


	@Autowired
	Queue queue;


	@Autowired
	SiteRepository siteRepo;
	
	@Autowired
    TaskRecordRepository taskRepo;
	
	@Autowired
	TaskFactory taskFactory;


	@Autowired
	ScheduleRepository scheduleRepository;

	@Autowired
	ScheduleProcessing scheduleProcessing;

	@Autowired
	ThreadPoolTaskExecutor threadPoolTaskExecutor;

	@Autowired
	ThreadPoolTaskScheduler threadPoolTaskScheduler;

	@PersistenceContext
	private EntityManager entityManager;


	@PostConstruct
	/**
	 * Сразу после старта, загружает все задачи с рассписанием в очередь
	 * @throws Exception
	 */
	public void Init() throws Throwable {
		logger.trace("[SceduleService.Init] Arm saved scheduled jobs.");

		Iterable<Schedule> schList = scheduleRepository.findAll();
		for (Schedule scheduleObj: schList ) {
			BaseTask task = null;

			try {
				task = taskFactory.createTask(
						TaskNamesEnum.valueOf(scheduleObj.getTaskName()),
						scheduleObj.getSite(),
						scheduleObj);
				this.put(task);
			} catch (Exception e) {
				logger.warn("[SceduleService.Init] Cannot run task " +task+ ", Error=" + e.getMessage());
			}
		}
	}


	/*---------------------------------------------------------------------------------------------
	 *     Список задач
	 ---------------------------------------------------------------------------------------------*/

	/**
	 *   Возвращает список рассписаний для текущих задач
	 * @param siteId
	 * @return
	 */
	public List<BaseTask> getTaskList(String siteId) {

		List<BaseTask> tList = new ArrayList<>();

		if ( queue.getSitesTasks(siteId) != null ) {
			for (BaseTask task : queue.getSitesTasks(siteId).values()) {
				if (BaseTask.isVisible()) {
					tList.add(task);
				}
			}
		}


//		Collections.sort(tList, new Comparator<BaseTask>() {
//			@Override
//			public int compare(BaseTask i1, BaseTask i2) {
//				return (Long.parseLong(i2.getId()) > Long.parseLong(i1.getId())) ? -1 : 1;
//			}
//		});

		tList.sort((i1,i2)->(Long.parseLong(i2.getId()) > Long.parseLong(i1.getId())) ? -1 : 1);

		return tList;
	}

	/*---------------------------------------------------------------------------------------------
	 *     ЗАПУСК задачи
	 ---------------------------------------------------------------------------------------------*/
	/**
	 * Добавляем задачу в список активных задач,
	 * Запускаем задачу и задаем ей  TaskCallback  в качестве колбека.
	 * TaskCallback будет вызван при завершении задачи
	 *
	 * @param task
	 * @return
	 * @throws Exception
	 */
	public BaseTask put(BaseTask task) throws Exception {

//		if (isTaskRunning(task)){
//			throw new ExceptionTaskExist("The task "+ task + " already running.");
//		}

		if ( ! task.isScheduled() ) {
			task = runNow(task);
		}
		else {
			task = runScheduled(task);
		}

		queue.put(task);
		task.setQueue(queue);
		return task;
	}

	/**
	 *   Производит запуск задачи в фоновом режиме.
	 * @param task задача которую еужно запускать
	 * @return
	 */
	public BaseTask runNow(BaseTask task) throws Exception{
		//    Выполняем запуск задачи и добавляем в список "заряженых" задая для сайта.
		try {
			ListenableFuture<Void> taskProcess = (ListenableFuture<Void>) threadPoolTaskExecutor.submitListenable(task);
			//taskProcess.addCallback(new TaskCallback(task,queue));
			task.setThisTaskProcess(taskProcess);
		}
		catch (Exception e) {
			logger.warn("Task does not started. Task="+task+", Error="+e.getMessage());
			throw e;
		}
		return task;
	}

	/**
	 * Производит запус задачи по рассписанию.
	 * @param task  задача которую нужно запускать
	 * @return
	 */
	public BaseTask runScheduled(BaseTask task) throws Exception{
		try {
			CronTrigger cronTrigger = new CronTrigger(task.getSchedule().toCronString());
			ScheduledFuture<?> taskProcess = threadPoolTaskScheduler.schedule(task,cronTrigger);

			task.setThisTaskProcess(taskProcess);
		}
		catch (Exception e) {
			logger.warn("[runScheduled] Task "+task+" cannot be scheduled. Error="+e.getMessage());
			throw e;
		}
		return task;
	}


	/*---------------------------------------------------------------------------------------------
	 *     ПОИСК задачи в очереди
	 ---------------------------------------------------------------------------------------------*/
	public BaseTask  getTaskById(String taskId) {
		Schedule sh = scheduleRepository.findOne(taskId);

		//logger.trace("[getTaskById] look for task by id " + taskId + ". schedule " + (sh==null?"null":sh));
		if ( sh != null) {
			return queue.get(queue.site2Id(sh.getSite()), taskId);
		}
		return null;
	}

	/**
	 *  
	 *	Определяет запущена ли задача с имененм у указанного сайта.
	 *
	 * @param siteId
	 * @return
	 */
	public boolean isTaskRunning(String siteId, String taskId) {
		BaseTask task = queue.get(siteId, taskId);
		return (task!= null) && (task.getStatus() == TaskStatusEnum.RUN );
	}

	/*---------------------------------------------------------------------------------------------
	 *     ОСТАНОВКА И УДАЛЕНИЕ задачи из очереди
	 ---------------------------------------------------------------------------------------------*/
	/**
	 * Stop task executin and remove from DB and queue
	 * @param task Task object
	 * @return
	 */
	public BaseTask stopTask(BaseTask task, boolean abort) {
		BaseTask currentTask = queue.get(task);

		//  Останавливаем исполнение задачи если она в состоянии ожидания или работает но стоит флаг abort
		if ((currentTask.getStatus() == TaskStatusEnum.RUN) && (!abort)) {
			currentTask.getSchedule().setEnable(false);
		}
		currentTask.getThisTaskProcess().cancel(abort);

		//  TODO Save task record with cancel status

		queue.remove(currentTask);
		scheduleProcessing.removeSchedule(currentTask.getSchedule());

		//  Удалить задачу из базы
		//scheduleProcessing.removeSchedule(currentTask.getSchedule());
		logger.trace("[stopTask] Stopping task "  + currentTask);
		return task;
	}

	/**
	 * Remove all tasks for given site
	 * @param site Site db object
	 */
	public void stopTasksForSite(Site site) {
		Map<String,BaseTask> taskMap =queue.getSitesTasks(site.getId());
		for (String taskId: taskMap.keySet()) {
			logger.trace("[stopTasksForSite] Stor task " + taskMap.get(taskId));
			stopTask(taskMap.get(taskId),true);
		}
	}


	/*---------------------------------------------------------------------------------------------
	 *    Редактирование рассписания и параметров задачи.
	 ---------------------------------------------------------------------------------------------*/

	public BaseTask updateTaskSchedule(BaseTask task, Schedule schedule, List<TaskParam> inputParams, boolean abort) throws Throwable {

//		if ( schedule.getId() == null )
//			throw new ExceptionInvalidArgument("New schedule object not allowed Schedule"+schedule);

		CopyOnWriteArrayList<TaskParam>  inParams = null;
		if ( inputParams != null ) {
			inParams = new CopyOnWriteArrayList<>(inputParams);
		}


		BaseTask currentTask = getTaskById(task.getId());
		logger.trace("[updateTaskSchedule] Update task "+currentTask+" schedule to " + schedule);

		//Schedule oldSchedule = task.getSchedule();

		ScheduleProcessing.isValidCron(schedule);
		currentTask.getThisTaskProcess().cancel(abort);


		Schedule curSchedule = currentTask.getSchedule();
		curSchedule.setSeconds(schedule.getSeconds());
		curSchedule.setMinute(schedule.getMinute());
		curSchedule.setHour(schedule.getHour());
		curSchedule.setDayOfMonth(schedule.getDayOfMonth());
		curSchedule.setMonth(schedule.getMonth());
		curSchedule.setDayOfWeek(schedule.getDayOfWeek());

		if (inParams == null) {
			if (curSchedule.getParams() != null) {
				ArrayList<TaskParam>  tempParams = new ArrayList<>(schedule.getParams());
				for (TaskParam param :tempParams) {
					curSchedule.deleteParam(param.getName());
				}
			}
		}
		else {
			//   Находим параметр которых нет в inputParams и удаляем параметр из текущего списка
			//   если параметр присутствует то обновляем его значение
			if (curSchedule.getParams() != null) {
				for (TaskParam curParam : curSchedule.getParams()) {
					boolean found = false;
					for ( TaskParam inParam : inParams) {
						if ( curParam.getName().equals(inParam.getName())) {
							found = true;
							curParam.setValue(inParam.getValue());
						}
					}
					if ( ! found ) {
						curSchedule.deleteParam(curParam.getName());
					}
				}
			}

			//  Находим и добавляем новые параметры
			for (TaskParam inParam : inParams) {
				boolean found=false;
				for (TaskParam curParam : curSchedule.getParams()) {
					if (inParam.getName().equals(curParam.getName())) {
						found = true;
					}
				}
				if (! found ) {
					curSchedule.addParam(new TaskParam(inParam.getName(),inParam.getValue(),inParam.getType()));
				}
			}
		}


		curSchedule.setEnable(schedule.isEnable());
		currentTask.setSchedule(curSchedule);
		//logger.trace("[updateTaskSchedule] New tasks schedule =  "+currentTask.getSchedule());

		if ( curSchedule.isEnable() ) {
			logger.trace("[updateTaskSchedule] Run again schedule =  "+currentTask.getSchedule());
			runScheduled(currentTask);
		}
		return currentTask;
	}

	/*---------------------------------------------------------------------------------------------
	 *    Лог задач
	 ---------------------------------------------------------------------------------------------*/

	/**
	 * Возвращает логи выполнения задания
	 * @param taskId
	 * @param limit
	 * @param offset
	 * @return
	 */
	public List<TaskRecord> getTaskLog(String taskId, long limit, long offset) {

		// http://www.querydsl.com/static/querydsl/3.2.3/reference/html/ch02.html#jpa_integration
		JPAQuery<?> query = new JPAQuery<Void>(entityManager);

		List<TaskRecord> recordList = query.select(QTaskRecord.taskRecord).from(QTaskRecord.taskRecord)
				.where(QTaskRecord.taskRecord.scheduleId.eq(taskId))
				.orderBy(QTaskRecord.taskRecord.stopTime.desc())
				.offset(offset)
				.limit(limit)
				.fetch();

		return recordList;
	}

	public List<TaskRecord> getTaskLog(String taskId) {

		logger.trace("[getTaskLog] Request all TaskRecords for taskId=" + (taskId!=null?taskId:"null"));
		// http://www.querydsl.com/static/querydsl/3.2.3/reference/html/ch02.html#jpa_integration
		JPAQuery<?> query = new JPAQuery<Void>(entityManager);

		List<TaskRecord> recordList = query.select(QTaskRecord.taskRecord).from(QTaskRecord.taskRecord)
				.where(QTaskRecord.taskRecord.scheduleId.eq(taskId))
				.orderBy(QTaskRecord.taskRecord.stopTime.desc())
				.fetch();

		return recordList;
	}


	public TaskRecord getTaskLastLog(String taskId) {

		logger.trace("[getTaskLastLog] Request last tasks status taskId=" + (taskId!=null?taskId:"null"));
		// http://www.querydsl.com/static/querydsl/3.2.3/reference/html/ch02.html#jpa_integration
		JPAQuery<?> query = new JPAQuery<Void>(entityManager);

		List<TaskRecord> recordList = query.select(QTaskRecord.taskRecord).from(QTaskRecord.taskRecord)
				.where(QTaskRecord.taskRecord.scheduleId.eq(taskId))
				.orderBy(QTaskRecord.taskRecord.stopTime.desc())
				.offset(0)
				.limit(1)
				.fetch();

		if ( recordList != null ) {
			return recordList.get(0);
		}
		return null;
	}

}

package home.abel.photohub.service;

import com.querydsl.core.Tuple;
import com.querydsl.core.group.Group;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.sql.SQLExpressions;
import home.abel.photohub.model.QTaskRecord;
import home.abel.photohub.model.Site;
import home.abel.photohub.model.SiteRepository;
import home.abel.photohub.model.TaskRecord;
import home.abel.photohub.model.TaskRepository;
import home.abel.photohub.tasks.BaseTask;
import home.abel.photohub.tasks.ExceptionTaskExist;
import home.abel.photohub.tasks.TaskFactory;
import home.abel.photohub.tasks.TaskNamesEnum;
import home.abel.photohub.tasks.TaskStatusEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import com.querydsl.jpa.impl.JPAQuery;
import static com.querydsl.core.group.GroupBy.*;

//import com.mysema.query.jpa.JPASubQuery;
//import com.mysema.query.jpa.impl.JPAQuery;


@Service
public class TaskQueueService {
	final Logger logger = LoggerFactory.getLogger(PhotoAttrService.class);
	

	/**
	 * TaskCallback
	 * 
	 * Расширенный класс  ListenableFutureCallback, который будет вызван после после окончания 
	 * каждой задачи.
	 * В разсширеном классе, после вызова задача   данные о завершении заоачи записываются в базу.
	 * @author abel
	 */
	
	class TaskCallback implements ListenableFutureCallback<Void>  {
		final Logger logger = LoggerFactory.getLogger(TaskCallback.class);
		protected BaseTask baseTask;
		protected Map<String,Map<TaskNamesEnum,BaseTask>> queue;


		TaskCallback(BaseTask task, Map<String,Map<TaskNamesEnum,BaseTask>> queue) {
			this.queue = queue;
			this.baseTask = task;
			add();
		}	

		@Override
		public void onSuccess(Void result) {
			logger.debug("[TaskCallback.onSuccess] Task complete OK. Task="+baseTask.getTag());
			remove();
			onFinish();
		}

		@Override
		public void onFailure(Throwable ex) {
			logger.debug("[TaskCallback.onFailure] Task complete with error. Task="+baseTask.getTag());
			remove();
			onFinish();
		}
		
		private void remove() {
//			if (isAutoRemove()) {
				Map<TaskNamesEnum,BaseTask>sitesTasks = queue.get(baseTask.getSite().getId());		
				if (sitesTasks != null) {
					sitesTasks.remove(baseTask.getTag());
				}
//			}
		}
		
		private void add() {
			Map<TaskNamesEnum,BaseTask>sitesTasks = queue.get(baseTask.getSite().getId());	
			if (sitesTasks == null) {				
				sitesTasks = new java.util.concurrent.ConcurrentHashMap<TaskNamesEnum,BaseTask>();
				queue.put(baseTask.getSite().getId(),sitesTasks);	
			}
			sitesTasks.put(baseTask.getTag(), baseTask);
		}
		
		protected void onFinish() {
			logger.debug("Task "+baseTask.getTag()+", for site="+baseTask.getSite()+", finished.");
		}
	}

		
	protected Map<String,Map<TaskNamesEnum,BaseTask>> sitesQueueMap  = 
			new java.util.concurrent.ConcurrentHashMap<String,Map<TaskNamesEnum,BaseTask>>();
	

	@Autowired
	ThreadPoolTaskExecutor threadPoolTaskExecutor;

	
	@Autowired
	SiteRepository siteRepo;
	
	@Autowired
	TaskRepository taskRepo;
	
	@Autowired
	TaskFactory taskFactory;

	@Autowired
	private DataSource dataSource;
		
	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * 
	 *  Создает задачу для сайта и вставляет в очередь
	 * 
	 * @param name
	 * @param siteId
	 * @return
	 * @throws Throwable
	 */
	public BaseTask createTask(String name,String siteId) throws Throwable {
		return put(taskFactory.createTask(name, siteId));		
	}
	
	/**
	 * Добавляем задачу в список активных задач, 
	 * Запускаем задачу и задаем ей  TaskCallback  в качестве колбека.   
	 * TaskCallback будет вызван при завершении задачи
	 *   [TaskQueueService.put] 
	 * 
	 * @param task
	 * @return
	 * @throws Exception  Ошибка при запуске
	 */
	public BaseTask put(BaseTask task) throws Exception {
		return put(task.getSite().getId(), task.getTag(), task );
	}
	
	public BaseTask put(String siteId, TaskNamesEnum name, BaseTask task) throws Exception {
		
		//  Если в задаче объект Site не определен, пытаемся найти объект Site его по SiteId
		//  и сохранить объект в задаче.
		if ( task.getSite() == null) {
			Site theSite = siteRepo.findOne(siteId);
			if (theSite == null ) {
				throw new ExceptionInvalidArgument("Cannot start taks "+task.getTag()+". Site object with id "+siteId+" not foun.");
			}
			task.setSite(theSite);
		}
		
		//----------------------------------------------
		//  Находим список заряженных задач для SiteId иначе создаем новый
		if (isTaskRunning(name,siteId)) {
			throw new ExceptionTaskExist("The task "+ name + " for site " + siteId + " already running.");
		}
		
		//----------------------------------------------
		//    Выполняем запуск задачи и добавляем в список "заряженых" задая для сайта.		
		try {			
			ListenableFuture<Void> taskProcess = (ListenableFuture<Void>) threadPoolTaskExecutor.submitListenable(task);
			taskProcess.addCallback(new TaskCallback(task,sitesQueueMap));
			task.setThisTaskProcess(taskProcess);
			
//			ListenableFutureCallback<?> taskCB =  new TaskCallback(task,sitesQueueMap);
//			ListenableFuture<?> taskProcess =  threadPoolTaskExecutor.submitListenable(task);
//			taskProcess.addCallback(taskCB);
//			task.setThisTaskProcess(taskProcess);

		}
		catch (Exception e) {
			logger.warn("Task does not started. Task="+task+", Error="+e.getMessage());
			throw e;
		}
		return task;
	}

		
	/**
	 *  
	 *	Определяет запущена ли задача с имененм у указанного сайта.
	 * 
	 * @param name
	 * @param siteId
	 * @return
	 */
	public boolean isTaskRunning(TaskNamesEnum name, String siteId) {
		return isTaskInQueue(name,siteId,sitesQueueMap);
	}
	
	/**
	 *     Проверка нахождения задачи в очереди 
	 *     
	 * @param taskName
	 * @param siteId
	 * @param queue
	 * @return
	 */
	protected boolean isTaskInQueue(TaskNamesEnum taskName, String siteId, Map<String,Map<TaskNamesEnum,BaseTask>> queue) {
		//   Находим список заряженных задач для SiteId иначе создаем новый	
		Map<TaskNamesEnum,BaseTask>sitesTasks = queue.get(siteId);
		
		//   Возможно надо проверять не только наличие задачи в очереди но и ее состояние может она закончилась.
		//   Check if such task for site is running
		if ( sitesTasks != null ) {
			if ( sitesTasks.get(taskName) != null ) {
				return true;
			}
		}		
		return false;
	}

	/**
	 *  
	 *  Возвращает задачу из очереди. null если не найдено. 
	 * 
	 * @param siteId
	 * @param name
	 * @return
	 * @throws Exception
	 */
	public BaseTask get(String siteId, TaskNamesEnum name) throws Exception {
		Map<TaskNamesEnum, BaseTask> sitesTasks = sitesQueueMap.get(siteId);
		if (sitesTasks != null) {
			BaseTask task = sitesTasks.get(name);
			if (task != null) {
				return task;
			}
		}
		return null;
	}


	/*=============================================================================================
	 *
	 *     УДАЛЕНИЕ задачи из очереди
	 *
	 =============================================================================================*/
	
	/**
	 * Remove task from queue.
	 * @param name
	 * @return
	 */
	public BaseTask remove(String siteId, TaskNamesEnum name) {
		BaseTask task = null;
		Map<TaskNamesEnum,BaseTask>sitesTasks = sitesQueueMap.get(siteId);		
		if (sitesTasks != null ) {
			task = sitesTasks.remove(name);
		}
		return task;
	}
	
	/**
	 * Remove task from queue.
	 * @param task
	 * @return
	 */
	public BaseTask remove(BaseTask task) {
		Map<TaskNamesEnum,BaseTask>sitesTasks = sitesQueueMap.get(task.getSite().getId());		
		if (sitesTasks != null ) {
			task = sitesTasks.remove(task.getTag());
			if ((task != null) && (task.getStatus() == TaskStatusEnum.RUN)) { 
				task.getThisTaskProcess().cancel(true);
			}
		}
		return task;
	}

	/*=============================================================================================
	 *
	 *     ПОИСК Задачи в базе
	 *
	 =============================================================================================*/

	/**
	 * Возвращает задачу из базы по ID
	 * @param taskId
	 * @return
     */
	public TaskRecord getTaskRecordById(String taskId) {
		return taskRepo.findOne(taskId);
	}


	/**
	 *
	 * @param siteId
	 * @return
     */
	public List<TaskRecord> getUserTRList(String siteId) {

		Site theSite = siteRepo.findOne(siteId);
		List<TaskRecord> trList = getLastUserTR(siteId);

		for( TaskNamesEnum taskNameEnum: TaskNamesEnum.values() ) {
			if (taskNameEnum.isUserTask()) {
				boolean found = false;
				for (TaskRecord tRecord : trList) {
					if (taskNameEnum.toString().equalsIgnoreCase(tRecord.getName())) {
						found = true;
						break;
					}
				}

				if (!found) {
					TaskRecord ntr = new TaskRecord();
					ntr.setName(taskNameEnum.toString());
					ntr.setSiteBean(theSite);
					trList.add(ntr);
				}
			}
		}
		return trList;
	}


	/**
	 *    Возвращает список последних задач с разными именами для сайта
	 * @param siteId идентификатор сайта
	 * @return
     */
	public List<TaskRecord> getLastUserTR(String siteId) {
		//	   JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		//
		//	   String SQLStr =  "SELECT tr.*  FROM task_records tr" +
		//			   " INNER JOIN" +
		//			   "    (SELECT name, MAX(startTime) AS MaxDateTime FROM task_records WHERE site = ? GROUP BY name) maxTasks " +
		//			   " ON tr.name = maxTasks.name " +
		//			   "    AND tr.startTime = maxTasks.MaxDateTime " +
		//			   " WHERE tr.site = ?";
		//
		//	   List recordList = jdbcTemplate.query(SQLStr,
		//			   new BeanPropertyRowMapper(TaskRecord.class),
		//			   theSite.getId(),
		//			   theSite.getId()
		//	   );

		JPAQuery<?> query = new JPAQuery<Void>(entityManager);
		QTaskRecord qtr = QTaskRecord.taskRecord;
		QTaskRecord qtr2 = new QTaskRecord("neighbor");

		List<TaskRecord> recordsList = query.select(qtr).from(qtr)
				   .leftJoin(qtr2)
				   .on(qtr.name.eq(qtr2.name)
						   .and(qtr.startTime.lt(qtr2.startTime))
						   .and(qtr.siteBean.id.eq(siteId))
				   )
			   .where(qtr2.isNull(),qtr.siteBean.id.eq(siteId))
			   .fetch();

		ArrayList<TaskRecord> arrayTR = new ArrayList<>();
		for (TaskRecord tr: recordsList) {
		   if (TaskNamesEnum.valueOf(tr.getName()).isUserTask()) {
			   arrayTR.add(tr);
		   }
		}
		return arrayTR;
	}






	/**
	 *    Возвращает последную по вермени задачу tName для сайта siteId
	 * @param siteId идентификатор сайта
	 * @param tName  имя задачи
     * @return запись из базы кди найдено дибо null
     */
	public TaskRecord getLastUserTR(String siteId, String tName) {

		for ( TaskRecord tr : getLastUserTR(siteId) ) {
			if ( tr.getName().equalsIgnoreCase(tName)) {
				return tr;
			}
		}
		return null;
	}



	
	/**
	 *   Возвращает лог задачи для сайта
	 *   
	 * @param siteId
	 * @param taskName
	 * @return
	 */
	public List<TaskRecord> getTaskLog(String siteId, String taskName, long limit, long offset) {

		// http://www.querydsl.com/static/querydsl/3.2.3/reference/html/ch02.html#jpa_integration
		JPAQuery<?> query = new JPAQuery<Void>(entityManager);

		List<TaskRecord> recordList = query.select(QTaskRecord.taskRecord).from(QTaskRecord.taskRecord)
				.where(QTaskRecord.taskRecord.siteBean.id.eq(siteId)
						.and(QTaskRecord.taskRecord.name.eq(taskName))
						)
				.orderBy(QTaskRecord.taskRecord.stopTime.desc())
				.offset(offset)
				.limit(limit)
				.fetch();

		return recordList;
	}

}

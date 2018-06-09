package home.abel.photohub.tasks;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Future;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import home.abel.photohub.model.Schedule;
import home.abel.photohub.model.TaskParam;
import home.abel.photohub.service.ExceptionInvalidArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import home.abel.photohub.model.Site;
import home.abel.photohub.model.TaskRecord;

//@Transactional(readOnly=false)
//@Transactional(propagation=Propagation.SUPPORTS)
//@Transactional



public class BaseTask implements Runnable, Serializable {
	private static final long serialVersionUID = 1L;
	final Logger logger = LoggerFactory.getLogger(BaseTask.class);
	final Logger bgLog = LoggerFactory.getLogger("taskslog");


	@JsonIgnore
	private Future<?> thisTaskProcess;
	@JsonIgnore
	private ScheduleProcessing scheduleProcessing;
	@JsonIgnore
	private boolean saveToDB = true;
	@JsonIgnore
	private Queue queue = null;

	private Schedule schedule = null;
	private TaskRecord taskRecord;
	protected String displayName;
	protected String description;

	/*-----------------------------------------------------------------------------------
	    Конструктор используемый при десериализации из JSON формы
	 -----------------------------------------------------------------------------------*/
	@JsonCreator
	public  BaseTask(
			@JsonProperty("id") String id,
			@JsonProperty("schedule") Schedule schedule,
			@JsonProperty("taskRecord") TaskRecord taskRecord,
			@JsonProperty("siteId") String siteId
			) {
		saveToDB = false;
		this.schedule = schedule;
		if ((schedule != null) && (schedule.getId() ==null)) { schedule.setId(id); }
		if ((schedule != null) && (siteId !=null) && (schedule.getId() ==null) ) {
			throw new ExceptionInvalidArgument("SiteId parameter should be defined in schedule object property.");
		}
	}


	/*-----------------------------------------------------------------------------------
	    Инициализация инстанса объекта задачи
	 -----------------------------------------------------------------------------------*/
	public BaseTask(Site theSite, TaskNamesEnum theTag, Schedule theSchedule, ScheduleProcessing scheduleSvc, boolean saveToDB) {

		scheduleProcessing = scheduleSvc;
		this.saveToDB = saveToDB;

		schedule = theSchedule;
		if ( schedule == null) {
			schedule = new Schedule();
			schedule.setEnable(false);
		}
		schedule.setTaskName(theTag.toString());
		schedule.setSite(theSite);
		if ( isSaveToDB()) {
			schedule = scheduleProcessing.saveSchedule(schedule);
		}

		taskRecord = new TaskRecord();
		setStatus(TaskStatusEnum.IDLE,"Wait for execution");
		saveLog();
		logger.debug("Create new task. Task=" + this);
		bgLog.debug("Create new task. Task=" + this);
		this.description = BaseTask.getStaticDescription();
		this.displayName = BaseTask.getStaticDisplayName();
	}


	/*-----------------------------------------------------------------------------------
			Self descriptions methods
	 -----------------------------------------------------------------------------------*/
	public static  String getStaticDisplayName() {
		return "DUMMY";
	}
	public static  String getStaticDescription() {
		return "Description for empty base task";
	}

	public static boolean isVisible() {return true;};

	private static final Map<String,String> pMap;
	static {
		Map<String, String> tmpMap = new HashMap<>();
		tmpMap.put("PARAM1","Test param for empty task");
		pMap = Collections.unmodifiableMap(tmpMap);
	}
	public static Map<String,String> getParamsDescr() {
		return pMap;
	}



	/*-----------------------------------------------------------------------------------
			Gettsers and Setters
	 -----------------------------------------------------------------------------------*/
	public String getId() { return schedule.getId(); }

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@JsonIgnore
	public Site getSite() {
		return schedule.getSite();
	}
	@JsonIgnore
	public void setSite(Site site) { schedule.setSite(site); }

	public TaskRecord getTaskRecord() {
		return taskRecord;
	}
	public String getSiteId() {
		return (schedule.getSite()==null?null:schedule.getSite().getId());
	}

	private boolean isSaveToDB() {
		return saveToDB;
	}
	public void setSaveToDB(boolean saveToDB) {
		this.saveToDB = saveToDB;
	}

	public Future<?> getThisTaskProcess() {
		return thisTaskProcess;
	}
	public void setThisTaskProcess(Future<?> thisTaskProcess) {
		this.thisTaskProcess = thisTaskProcess;
	}


	public Queue getQueue() { return queue; }
	public void setQueue(Queue queue) { this.queue = queue; }


	public String toString() {
		return schedule.getTaskName()+"/"+getDisplayName()+"(task Id="+(schedule.getId()==null?"undef":schedule.getId()) +
				"|site="+(schedule.getSite()==null?"system":schedule.getSite())+
				"), Schedule="+ schedule;
	}

	 /*-----------------------------------------------------------------------------------
	     Запуск задачи
	 -----------------------------------------------------------------------------------*/
	/**
	 *    Вызывается внешним рассписанием
	 *    Создает запись в логе о начале исполнения
	 *    передает исполнение в метод exec()
	 *    после завершения создает запись об ошибке или успешном выполнеии в логе
	 */
	@Override
	public void run() {
		setStatus(TaskStatusEnum.RUN,"Start execution");
		saveLog();


		logger.debug("[BaseTask.run] Starting. Task="+this);
		bgLog.debug("[BaseTask.run] Starting. Task="+this);


		try {
			exec();
			setStatus(TaskStatusEnum.FIN,"Task "+this+". Execution success.");

		}
		catch (Throwable ex) {
			setStatus(TaskStatusEnum.ERR,ex.getMessage());
			String errStr = "Task "+this+". Execution aborted due to error="+ex.getMessage();
			logger.error(errStr,ex);
			bgLog.error(errStr,ex);
			throw new ExceptionTaskAbort(errStr,ex);
		}
		finally {
			//logger.debug("[BaseTask.run] Execution completed.");
			saveLog();
			logger.debug("[BaseTask.run] Execution completed. Save result record to DB. TaskRecord="+taskRecord);
			bgLog.debug("[BaseTask.run] Execution completed. Save result record to DB. TaskRecord="+taskRecord);

			if ( isSaveToDB()) {
				try {
					//this.scheduleProcessing.saveLog(logRecord);
					if ( ! schedule.isEnable()) {
						//   Remove task from DB
						schedule = scheduleProcessing.removeSchedule(schedule);
						//   Remove task from queue
						if( queue != null)  queue.remove(this);
					}
				} catch (Exception e) {
					logger.error("[BaseTask.run] Cannot update Site="+schedule.getSite()+" to DB.",e);
					bgLog.error("[BaseTask.run] Cannot update Site="+schedule.getSite()+" to DB.",e);
				}
			}
		}
	}

	/**
	 *    Вызывается в процессе запуса. Используется для перекрытия наследующими коассами.
	 */
	public void exec() throws Throwable {

	}

	/*-----------------------------------------------------------------------------------
			Работа с параметрами
	 -----------------------------------------------------------------------------------*/

	/**
	 *	Возвращает значение зараметр задачи по его инмени
	 * @return Список Объектов параметров задачи
	 */
	@JsonIgnore
	public List<TaskParam> getParams() {
		return this.schedule.getParams();
	}

	/**
	 * Просматривет список введенныхпараметро,
	 * если параметр с таким именем уже установлен то меняем его.
	 * Иначе добавляем новый параметр в список параметров.
	 *
	 * @param inParams List ob task param objects
	 */
	@JsonIgnore
	public void setParams(List<TaskParam> inParams) {
		if (inParams == null) return;
 		logger.trace("[setParams] Set new parameters array. Array=" + inParams);
		List<TaskParam> outParams = getSchedule().getParams();
		for ( TaskParam inParam : inParams ) {
			boolean found = false;
			for ( TaskParam outParam : outParams ) {
				if (inParam.getName().equals(outParam.getName()) ) {
					outParam.setValue(inParam.getValue());
					found=true;
					break;
				}
			}
			if (! found) {
				schedule.addParam(inParam);
			}
		}
		if ( isSaveToDB())  schedule = scheduleProcessing.updateSchedule(schedule);
	}


	/**
	 * 	 Если параметр с указанным именем уже существует то менят значение на новое
	 * 	 иначе добавляет новый парметр с указанным именем и указанным значением.
	 * 	 Сохраняет изменения в базу.
	 * 	 Параметр определяется по имени.
	 * @param param объект для параметра
	 * @return  возвращает объект для сохраненного в базу параметра
	 */
	@JsonIgnore
	public TaskParam setParam(TaskParam param) {
		List<TaskParam> params = schedule.getParams();
		boolean found = false;
		for ( TaskParam p: params ) {
			if (p.getName().equals(param.getName())) {
				p.setValue(param.getValue());
				found = true;
			}
		}
		if ( ! found ) {
			TaskParam newPObj = param;
			if (param.getId() != null ) {
				newPObj = new TaskParam();
				newPObj.setName(param.getName());
				newPObj.setValue(param.getValue());
			}
			schedule.addParam(newPObj);
		}
		if ( isSaveToDB())  schedule = scheduleProcessing.updateSchedule(schedule);
		return schedule.getParam(param.getName());
	}

	/**
	 * 	 Если параметр с указанным именем уже существует то менят значение на новое
	 * 	 иначе добавляет новый парметр с указанным именем и указанным значением.
	 * 	 Сохраняет изменения в базу.
	 * 	 Параметр определяется по имени.
	 * @param paramName Имя параметра
	 * @param paramValue Значения параметра
	 * @return  возвращает объект для сохраненного в базу параметра
	 */
	@JsonIgnore
	public TaskParam setParam(String paramName, String paramValue) {
		TaskParam param = new TaskParam();
		param.setName(paramName);
		param.setValue(paramValue);
		return setParam(param);
	}

	/*-----------------------------------------------------------------------------------
	     Работа с рассписанием
	 -----------------------------------------------------------------------------------*/

	/**
	 * Проверяет установленно рассписания для задачи
	 * @return true  усли рассписание установленно. False в противном случае
	 */
	@JsonIgnore
	public boolean isScheduled() {
		return  ((schedule == null) || (schedule.isEnable()));
	}

	/**
	 * 	Возвращает текущее рассписания
	 * @return бъект рассписания
	 */
	public Schedule getSchedule() {
		return schedule;
	}

	/**
	 * Вносит изменения в рассписание запуска задачи
	 * @param schedule объект рассписания
	 */
	public void setSchedule(Schedule schedule) {
		if (isSaveToDB()) {
			schedule = scheduleProcessing.updateSchedule(schedule);
			this.schedule = schedule;
		}
		else {
			logger.warn("[BaseTask.setSchedule] Set schedule  for task w/o save to db Flag. "+
					"Task=" + this );
		}
	}


	/*-----------------------------------------------------------------------------------
			Логирование и статусы
	 -----------------------------------------------------------------------------------*/
	@JsonIgnore
	public TaskStatusEnum getStatus() {
		return TaskStatusEnum.valueOf(taskRecord.getStatus());
	}
	@JsonIgnore
	protected void setStatus(TaskStatusEnum status, String message) {
		taskRecord.setMessage(message);
		taskRecord.setStatus(status.toString());
		taskRecord.setStopTime(new Date());

	}

	protected void saveLog() {
		taskRecord.setSiteBean(schedule.getSite());
		taskRecord.setName(schedule.getTaskName());
		taskRecord.setScheduleId(schedule.getId());

		if ( isSaveToDB()) {
			scheduleProcessing.saveLog(taskRecord);
		}
	}

	protected void printMsg(String msg) {
		taskRecord.setMessage(msg);
	}

}

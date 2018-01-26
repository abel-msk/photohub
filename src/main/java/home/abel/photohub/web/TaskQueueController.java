package home.abel.photohub.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import home.abel.photohub.model.Schedule;
import home.abel.photohub.model.TaskParam;
import home.abel.photohub.model.TaskRecord;
import home.abel.photohub.service.ExceptionInvalidArgument;
import home.abel.photohub.service.SiteService;
import home.abel.photohub.service.TaskQueueService;
import home.abel.photohub.tasks.*;
import home.abel.photohub.web.model.DefaultObjectResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static home.abel.photohub.model.QSchedule.schedule;

@CrossOrigin
@RestController
@RequestMapping("/api")
public class TaskQueueController {
	final Logger logger = LoggerFactory.getLogger(TaskQueueController.class);
	
	@Autowired 
	SiteService siteSvc;
	
	@Autowired 
	TaskQueueService taskQueue;

	@Autowired
	HeaderBuilderService headerBuild;
	
	@Autowired
	TaskFactory taskFactory;



	/*=============================================================================================
	 *
	 *   Return task names list registered uin the system and user can create.
	 *
	 =============================================================================================*/


	@RequestMapping(value = "/site/{id}/tasksdescr", method = RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody ResponseEntity<DefaultObjectResponse<List<TaskDescription>>> siteTaskRun(
			final HttpServletRequest HTTPrequest,
			final HttpServletResponse HTTPresponse,
			@PathVariable("id") String theSiteID
	) throws Throwable {
		List<TaskDescription> taskDescrList =  taskFactory.getTasksDescr();
		logger.trace("Task descr list: " + taskDescrList);
		DefaultObjectResponse<List<TaskDescription>> response = new DefaultObjectResponse<>("OK",0,taskFactory.getTasksDescr());

		return new ResponseEntity<>(response,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	}

	/*=============================================================================================
	 * 
	 *   Create and start task
	 *      
	 =============================================================================================*/	   
	@RequestMapping(value = "/site/{id}/task", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptSiteTaskRun(HttpServletRequest request) throws IOException, ServletException {
    	logger.debug("Request OPTION  for /site/{id}/task");
	    return new ResponseEntity<>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}
    	
    @RequestMapping(value = "/site/{id}/task", method = RequestMethod.PUT, produces=MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody ResponseEntity<DefaultObjectResponse<BaseTask>> siteTaskRun(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
    		@PathVariable("id") String theSiteID,
			//@RequestParam("schedule") String Schedule
			@RequestBody Schedule schedule
    		) throws Throwable {

		BaseTask task  = taskFactory.createTask(schedule.getTaskName(), theSiteID, schedule);
		task = taskQueue.put(task);
    	logger.debug("Create new task " + task );

    	DefaultObjectResponse<BaseTask> response = new DefaultObjectResponse<>("OK",0,task);
	    return new ResponseEntity<>(response,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
    }

	/*=============================================================================================
	 *
	 *   Update task schedule or task parameters
	 *
	 =============================================================================================*/
	@RequestMapping(value = "/site/{id}/task/{tid}/schedule", method = RequestMethod.PUT, produces=MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody ResponseEntity<DefaultObjectResponse<Schedule>> siteTaskUpdate(
			final HttpServletRequest HTTPrequest,
			final HttpServletResponse HTTPresponse,
			@PathVariable("id") String theSiteID,
			@PathVariable("tid") String theTaskId,
			@RequestBody Schedule schedule
	) throws Throwable {

		BaseTask task = taskQueue.getTaskById(theTaskId);
		//TODO:

		logger.debug("[siteTaskUpdate] New schedule="+schedule);

		for ( TaskParam param: schedule.getParams()) {
			logger.trace("[siteTaskUpdate] Params: " + param);
		}




		DefaultObjectResponse<Schedule> response;
		if ( task != null) {
			task = taskQueue.updateTaskSchedule(task, schedule, schedule.getParams(),true);
			response = new DefaultObjectResponse<>("OK",0,task.getSchedule());
			logger.debug("Update task " + task);
		}
		else {
			throw new ExceptionObjectNotFound("Task " +theTaskId+ " not found in the queue.");
		}
		return new ResponseEntity<>(response,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	}

//	@RequestMapping(value = "/site/{id}/task/{tid}/schedule/params/template", method = RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
//	@ResponseBody ResponseEntity<DefaultObjectResponse<List<TaskParam>>> siteTaskParamsTmpl(
//			final HttpServletRequest HTTPrequest,
//			final HttpServletResponse HTTPresponse,
//			@PathVariable("id") String theSiteID,
//			@PathVariable("tid") String theTaskId
//	) throws Throwable {;
//
//		BaseTask task = taskQueue.getTaskById(theTaskId);
//		DefaultObjectResponse<List<TaskParam>> response;
//		if (task != null) {
//			String taskName = task.getSchedule().getTaskName();
//			response = new DefaultObjectResponse<List<TaskParam>>("OK", 0, taskFactory.getEmptyParamList(taskName));
//		}
//		else
//			throw new ExceptionInvalidArgument("Thereis no task with ID="+theTaskId);
//
//		return new ResponseEntity<>(response,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
//	}


	/*=============================================================================================
	 *
	 *   List tasks
	 *
	 =============================================================================================*/
	@RequestMapping(value = "/site/{id}/tasks", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptTasks(HttpServletRequest request) throws IOException, ServletException {
		logger.debug("Request OPTION  for /site/tasknames");
		return new ResponseEntity<>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}
	/**
	 *  Возвращает список существующих задач
	 * @param HTTPrequest
	 * @param HTTPresponse
	 * @param theSiteID
	 * @return
	 * @throws Throwable
	 */
	@RequestMapping(value = "/site/{id}/tasks", method = RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody ResponseEntity<DefaultObjectResponse<List<BaseTask>>> getTasksList(
			final HttpServletRequest HTTPrequest,
			final HttpServletResponse HTTPresponse,
			@PathVariable("id") String theSiteID
	) throws Throwable  {
		List<BaseTask> taskMap= taskQueue.getTaskList(theSiteID);

		DefaultObjectResponse<List<BaseTask>> response = new DefaultObjectResponse<>("OK", 0, taskMap);

		logger.debug("Return tasks list "+ response.getObject());
		return new ResponseEntity<>(response,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	}

	/*=============================================================================================
	 *
	 *   Get Task
	 *
	 =============================================================================================*/
	@RequestMapping(value = "/site/{id}/task/{tid}", method = RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody ResponseEntity<DefaultObjectResponse<BaseTask>> getTasks(
			final HttpServletRequest HTTPrequest,
			final HttpServletResponse HTTPresponse,
			@PathVariable("id") String theSiteID,
			@PathVariable("tid") String theTaskId
	) throws Throwable  {
		DefaultObjectResponse<BaseTask> response = new DefaultObjectResponse<>("OK",0,
				taskQueue.getTaskById(theTaskId));
		logger.debug("Return  task "+ response.getObject());

		return new ResponseEntity<>(response,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	}

	/*=============================================================================================
	 *
	 *    Get Task schedule
	 *
	 =============================================================================================*/
	@RequestMapping(value = "/site/{id}/task/{tid}/schedule", method = RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody ResponseEntity<DefaultObjectResponse<Schedule>> updateTask(
			final HttpServletRequest HTTPrequest,
			final HttpServletResponse HTTPresponse,
			@PathVariable("id") String theSiteID,
			@PathVariable("tid") String taskId
	) throws Throwable {

		DefaultObjectResponse<Schedule> response = new DefaultObjectResponse<>("OK",0,
				taskQueue.getTaskById(taskId).getSchedule());
		logger.debug("Return  task "+ response.getObject());
		return new ResponseEntity<>(response,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	}

	/*=============================================================================================
	 *
	 *   Get task status
	 *
	 =============================================================================================*/
	@RequestMapping(value = "/site/{id}/task/{id}/taskrecord", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptTaskState(HttpServletRequest request) throws IOException, ServletException {
		logger.debug("Request OPTION  for /site/{id}/task/{id}");
		return new ResponseEntity<>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}

	@RequestMapping(value = "/site/{id}/task/{tid}/taskrecord", method = RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody ResponseEntity<DefaultObjectResponse<TaskRecord>> getTaskState(
			final HttpServletRequest HTTPrequest,
			final HttpServletResponse HTTPresponse,
			@PathVariable("id") String theSiteID,
			@PathVariable("tid") String taskId
			) throws Throwable {

		BaseTask task = taskQueue.getTaskById(taskId);
		DefaultObjectResponse<TaskRecord> response = null;
		if (task != null) {
			response = new DefaultObjectResponse<>("OK", 0, task.getTaskRecord());
		}
		else {
			response = new DefaultObjectResponse<>("OK", 0, taskQueue.getTaskLastLog(taskId));
		}

		logger.debug("Return  task status "+ response.getObject());
		return new ResponseEntity<>(response,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	}

	/*=============================================================================================
	 *
	 *   Stop task
	 *
	 =============================================================================================*/

	@RequestMapping(value = "/site/{id}/task/{tid}", method = RequestMethod.DELETE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody ResponseEntity<DefaultObjectResponse<BaseTask>> stopTask(
			final HttpServletRequest HTTPrequest,
			final HttpServletResponse HTTPresponse,
			@PathVariable("id") String theSiteID,
			@PathVariable("tid") String taskId
	) throws Throwable {

		BaseTask task = taskQueue.getTaskById(taskId);
		DefaultObjectResponse<BaseTask> response = null;

		if (task == null) {
			response = new DefaultObjectResponse<>("OK", 0, null);
		}
		else {
			response = new DefaultObjectResponse<>("OK", 0, taskQueue.stopTask(task,true));
		}

		logger.debug("Return  task status "+ response.getObject());
		return new ResponseEntity<>(response,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	}

	/*=============================================================================================
	 * 
	 *   Get task log
	 *      
	 =============================================================================================*/	   
	@RequestMapping(value = "/site/{id}/task/{tid}/log", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptSiteTaskLog(HttpServletRequest request) throws IOException, ServletException {
    	logger.debug("Request OPTION  for /site/{id}/tasklog/{name}");
	    return new ResponseEntity<>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}
	
    @RequestMapping(value = "/site/{id}/task/{tid}/log", method = RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody ResponseEntity<DefaultObjectResponse<List<TaskRecord>>> siteTaskLog(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
    		@PathVariable("id") String theSiteID,
    		@PathVariable("tis") String taskId,
			@RequestParam(value = "limit", defaultValue = "10",  required=false) long limit,
			@RequestParam(value = "offset", defaultValue = "0", required=false)  long offset
    		) throws Throwable {

		List<TaskRecord> trList = taskQueue.getTaskLog(taskId,limit,offset);
		logger.debug("Return  task log list. Size "+ trList.size());
	   	DefaultObjectResponse<List<TaskRecord>> response = new DefaultObjectResponse<>("OK",0,trList);
	    return new ResponseEntity<>(response,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
    }
	
}

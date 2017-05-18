package home.abel.photohub.web;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import home.abel.photohub.model.Site;
import home.abel.photohub.model.TaskRecord;
import home.abel.photohub.service.ScheduleService;
import home.abel.photohub.service.SiteService;
import home.abel.photohub.service.TaskQueueService;
import home.abel.photohub.tasks.ExceptionTaskExist;
import home.abel.photohub.tasks.TaskFactory;
import home.abel.photohub.tasks.TaskNamesEnum;
import home.abel.photohub.web.model.DefaultObjectResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
	ScheduleService scheduleService;
	
	@Autowired
	HeaderBuilderService headerBuild;
	
	@Autowired
	TaskFactory taskFactory;

	/*=============================================================================================
	 * 
	 *   List tasks 
	 *      
	 =============================================================================================*/
	@RequestMapping(value = "/site/{id}/tasks", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptTasks(HttpServletRequest request) throws IOException, ServletException {
    	logger.debug("Request OPTION  for /site/tasknames");
	    return new ResponseEntity<String>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}
	
	
	/**
	 * 
	 *   Возвращает список существующих задач
	 * 
	 * @param HTTPrequest
	 * @param HTTPresponse
	 * @return
	 * @throws Throwable
	 */
	@RequestMapping(value = "/site/{id}/tasks", method = RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<DefaultObjectResponse<List<TaskRecord>>> getTasks(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
			@PathVariable("id") String siteId
			) throws Throwable {
		
    	logger.debug("Request GET for /site/"+siteId+"/tasknames");
		DefaultObjectResponse<List<TaskRecord>> response = new DefaultObjectResponse<>("OK",0,taskQueue.getUserTRList(siteId));
		return new ResponseEntity<>(response,headerBuild.getHttpHeader(HTTPrequest),HttpStatus.OK);
	}
	


	/*=============================================================================================
	 * 
	 *   Start  task
	 *      
	 =============================================================================================*/	   
	@RequestMapping(value = "/site/{id}/task", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptSiteTaskRun(HttpServletRequest request) throws IOException, ServletException {
    	logger.debug("Request OPTION  for /site/{id}/task");
	    return new ResponseEntity<String>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}
    	
    @RequestMapping(value = "/site/{id}/task", method = RequestMethod.POST, produces=MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody ResponseEntity<DefaultObjectResponse<TaskRecord>> siteTaskRun(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
    		@PathVariable("id") String theSiteID,
			@RequestParam("taskname") String taskName
    		) throws Throwable {

    	logger.debug(">>> Request PUT for /site/"+theSiteID+"/task ["+taskName+"]  For start new task"  );
		TaskRecord theTaskRec = null;

		if (taskQueue.isTaskRunning(TaskNamesEnum.valueOf(taskName), theSiteID) ||
				scheduleService.isSchedTaskRunning(theSiteID,TaskNamesEnum.valueOf(taskName))
				) {
			throw new ExceptionTaskExist("The task "+ taskName + " for site " + theSiteID + " already running.");
		}
		else {
			theTaskRec = taskQueue.createTask(taskName, theSiteID).getRecord();
		}

    	DefaultObjectResponse<TaskRecord> response = new DefaultObjectResponse<TaskRecord>("OK",0,theTaskRec);
	    return new ResponseEntity<DefaultObjectResponse<TaskRecord>>(response,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
    }


	/*=============================================================================================
	 *
	 *   Get get task status
	 *
	 =============================================================================================*/
	@RequestMapping(value = "/site/{id}/task/{id}", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptTaskState(HttpServletRequest request) throws IOException, ServletException {
		logger.debug("Request OPTION  for /site/{id}/task/{id}");
		return new ResponseEntity<String>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}

	@RequestMapping(value = "/site/{id}/task/{id}", method = RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody ResponseEntity<DefaultObjectResponse<TaskRecord>> getTaskState(
			final HttpServletRequest HTTPrequest,
			final HttpServletResponse HTTPresponse,
			@PathVariable("id") String theSiteID,
			@PathVariable("id") String taskId
			) throws Throwable {

		logger.debug(">>> Request PUT for /site/"+theSiteID+"/task/"+taskId );

		TaskRecord theTaskRec = taskQueue.getTaskRecordById(taskId);
		if ( ! theTaskRec.getSiteBean().getId().equals(taskId) ) {
			logger.warn("[getTaskState] Requested taskID="+taskId+" belongs site="+theTaskRec.getSiteBean());
		}

		DefaultObjectResponse<TaskRecord> response = new DefaultObjectResponse<TaskRecord>("OK",0,theTaskRec);
		return new ResponseEntity<DefaultObjectResponse<TaskRecord>>(response,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	}


	/*=============================================================================================
	 * 
	 *   Get task log
	 *      
	 =============================================================================================*/	   
	@RequestMapping(value = "/site/{id}/tasklog/{name}", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptSiteTaskLog(HttpServletRequest request) throws IOException, ServletException {
    	logger.debug("Request OPTION  for /site/{id}/tasklog/{name}");
	    return new ResponseEntity<String>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}
	
    @RequestMapping(value = "/site/{id}/tasklog/{name}", method = RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody ResponseEntity<DefaultObjectResponse<List<TaskRecord>>> siteTaskLog(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
    		@PathVariable("id") String theSiteID,
    		@PathVariable("name") String taskName,
			@RequestParam(value = "limit", defaultValue = "10",  required=false) long limit,
			@RequestParam(value = "offset", defaultValue = "0", required=false)  long offset
    		) throws Throwable {
    	logger.debug(">>> Request PUT for /site/"+theSiteID+"/tasklog/"+taskName+" [limit="+limit+", offset="+offset+"]");

		List<TaskRecord> trList = taskQueue.getTaskLog(theSiteID,taskName,limit,offset);

	   	DefaultObjectResponse<List<TaskRecord>> response = new DefaultObjectResponse<List<TaskRecord>>("OK",0,trList);
	    return new ResponseEntity<DefaultObjectResponse<List<TaskRecord>>>(response,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
    }
	
}

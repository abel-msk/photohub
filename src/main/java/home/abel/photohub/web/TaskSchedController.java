package home.abel.photohub.web;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import home.abel.photohub.model.Schedule;
import home.abel.photohub.model.Site;
import home.abel.photohub.model.TaskRecord;
import home.abel.photohub.service.ScheduleService;
import home.abel.photohub.service.SiteService;
import home.abel.photohub.service.TaskQueueService;
import home.abel.photohub.tasks.BaseTask;
import home.abel.photohub.tasks.ExceptionTaskExist;
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
public class TaskSchedController {
	final Logger logger = LoggerFactory.getLogger(TaskSchedController.class);

	@Autowired 
	SiteService siteSvc;
	
	@Autowired 
	TaskQueueService taskQueue;

	@Autowired 
	ScheduleService scheduleService;
	
	@Autowired
	HeaderBuilderService headerBuild;
	
	/*=============================================================================================
	 * 
	 *   LIST SCHEDULE
	 *      
	 =============================================================================================*/

	
	@RequestMapping(value = "/site/{id}/schedules", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptsSchedule(HttpServletRequest request) throws IOException, ServletException {
    	logger.debug("Request OPTION  for /site/{id}/schedules");
	    return new ResponseEntity<String>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}
	
	@RequestMapping(value = "/site/{id}/schedules", method = RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody ResponseEntity<DefaultObjectResponse<List<Schedule>>> getSchedules(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
			@PathVariable("id") String theSiteID
			) throws Throwable {
		
		logger.debug(">>> Request GET for /site/"+theSiteID+"/schedules");
		if ( theSiteID.equalsIgnoreCase(ScheduleService.NULL_SITE )) {
			theSiteID = null;
		}
		
		List<Schedule> schList = scheduleService.getUserSchedList(theSiteID);
		DefaultObjectResponse<List<Schedule>> resp = new  DefaultObjectResponse<List<Schedule>>("OK",0,schList);
	    return new ResponseEntity<DefaultObjectResponse<List<Schedule>>>(resp,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	}  
	
	/*=============================================================================================
	 * 
	 *   GET SCHEDULE
	 *      
	 =============================================================================================*/	
	@RequestMapping(value = "/site/{id}/schedule/{name}", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptsScheduleByName(HttpServletRequest request) throws IOException, ServletException {
    	logger.debug("Request OPTION  for /site/{id}/schedule/{name}");
	    return new ResponseEntity<String>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}
	
	@RequestMapping(value = "/site/{id}/schedule/{name}", method = RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody ResponseEntity<DefaultObjectResponse<Schedule>> getSchedule(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
			@PathVariable("id") String theSiteID,
			@PathVariable("name") TaskNamesEnum taskName
			) throws Throwable {
		
		logger.debug(">>> Request GET for /site/"+theSiteID+"/schedule/"+taskName); 
		
		if ( theSiteID.equalsIgnoreCase(ScheduleService.NULL_SITE ) ) {
			theSiteID = null;
		}
		
		Schedule sch = scheduleService.getSchedule(theSiteID,taskName);
		DefaultObjectResponse<Schedule> resp = new  DefaultObjectResponse<Schedule>("OK",0,sch);
	    return new ResponseEntity<DefaultObjectResponse<Schedule>>(resp,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	}  
	
	/*=============================================================================================
	 * 
	 *   SET/UPDATE SCHEDULE
	 *      
	 =============================================================================================*/	
	@RequestMapping(value = "/site/{id}/schedule/{name}", method = RequestMethod.PUT, produces=MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody ResponseEntity<DefaultObjectResponse<Schedule>> setSchedule(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
			@PathVariable("id") String theSiteID,
			@PathVariable("name") TaskNamesEnum taskName,
			@RequestBody Schedule schObj
			) throws Throwable {
		
		logger.debug(">>> Request PUT for /site/"+theSiteID+"/schedule/"+taskName+ ", cron="+schObj);

		//Schedule sch = scheduleService.getSchedule(theSiteID,taskName);
	
		if ( theSiteID.equalsIgnoreCase(ScheduleService.NULL_SITE ) ) {
			theSiteID = null;
		}
		BaseTask task = scheduleService.setShcedule(theSiteID,taskName,
				schObj.isEnable(),
				schObj.getSeconds(),
				schObj.getMinute(),
				schObj.getHour(),
				schObj.getDayOfMonth(),
				schObj.getMonth(),
				schObj.getDayOfWeek()
				);

		Schedule sch = scheduleService.getSchedule(theSiteID,taskName);
		DefaultObjectResponse<Schedule> resp = new  DefaultObjectResponse<Schedule>("OK",0,sch);
	    return new ResponseEntity<DefaultObjectResponse<Schedule>>(resp,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	
	}  
	

	/*=============================================================================================
	 * 
	 *   SCHEDULE LOG
	 *      
	 =============================================================================================*/	

	@RequestMapping(value = "/site/{id}/schedule/{name}/log", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptsGetLog(HttpServletRequest request) throws IOException, ServletException {
    	logger.debug("Request OPTION  for /site/{id}/schedule/{name}/log");
	    return new ResponseEntity<String>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}
	
	@RequestMapping(value = "/site/{id}/schedule/{name}/log", method = RequestMethod.PUT, produces=MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody ResponseEntity<DefaultObjectResponse<List<TaskRecord>>> getLogBySite(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
			@PathVariable("id") String theSiteID,
			@PathVariable("name") TaskNamesEnum taskName
			) throws Throwable {
		
		logger.debug(">>> Request GET for /site/"+theSiteID+"/schedule/"+taskName+"/log");
		//Schedule sch = scheduleService.getSchedule(theSiteID,taskName);
	
		if ( theSiteID.equalsIgnoreCase(ScheduleService.NULL_SITE ) ) {
			theSiteID = null;
		}

		List<TaskRecord> trList  = scheduleService.getLogByTask(theSiteID,taskName);
		

		DefaultObjectResponse<List<TaskRecord>> resp = new  DefaultObjectResponse<List<TaskRecord>>("OK",0,trList);
	    return new ResponseEntity<DefaultObjectResponse<List<TaskRecord>>>(resp,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	
	} 
	
}

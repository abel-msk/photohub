package home.abel.photohub.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import home.abel.photohub.service.ConfVarEnum;
import home.abel.photohub.service.ConfigService;
import home.abel.photohub.service.PhotoService;
import home.abel.photohub.web.model.DefaultObjectResponse;
import home.abel.photohub.web.model.DefaultResponse;
import home.abel.photohub.web.model.ResponseConfParamFactory;
import home.abel.photohub.web.model.ResponseConfigParamObject;
import home.abel.photohub.web.model.ResponsePhotoObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ConfigController {
	final Logger logger = LoggerFactory.getLogger(ConfigController.class);
	
	@Autowired 
	ConfigService configSvc;
	
	@Autowired 
	private ResponseConfParamFactory respParamFactory;
	
	@Autowired
	HeaderBuilderService headerBuild;
	
	/*=============================================================================================
	 * 
	 *    Handle request for  OPTIONS
	 *      
	 =============================================================================================*/
	@RequestMapping(value = "/config", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptConfigList(HttpServletRequest request) throws IOException, ServletException {
    	logger.debug("Request OPTION  for /config");
	    return new ResponseEntity<String>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}
	@RequestMapping(value = "/config/{var}", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptConfigVariable(HttpServletRequest request) throws IOException, ServletException {
    	logger.debug("Request OPTION  for /config/{var}");
	    return new ResponseEntity<String>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}	
    
	/*=============================================================================================
	 * 
	 *    Handle request for  GET configuration values list
	 *      
	 =============================================================================================*/
	@RequestMapping(value = "/config", method = RequestMethod.GET, produces="application/json") 
	ResponseEntity<List<ResponseConfigParamObject>> getVarList(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse) throws Throwable
	{ 		
		logger.debug(">>> Request GET for /config");	
		ArrayList<ResponseConfigParamObject> respList = new ArrayList<ResponseConfigParamObject>();
		
		Map<ConfVarEnum,String> configList = configSvc.getPropertiesList();
		for (Map.Entry<ConfVarEnum, String> entry : configList.entrySet()) {
			ResponseConfigParamObject theObj = respParamFactory.getEmptyObject(entry.getKey());
		    theObj.setValue(entry.getValue());
		    respList.add(theObj);
		}

		logger.debug("<<< List ok");
		return new ResponseEntity<List<ResponseConfigParamObject>>(respList,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	}

    public ConfigController() {
    }

    /*=============================================================================================
         *
         *    Handle request for  PUT configuration values list
         *
         =============================================================================================*/
	@RequestMapping(value = "/config", method = RequestMethod.PUT, produces="application/json") 
	ResponseEntity<DefaultObjectResponse> setVarList(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
	        @RequestBody ResponseConfigParamObject[]  objectsList
	        ) throws Throwable
	{ 		
		logger.debug(">>> Request PUT for /config");	
		
		List<ResponseConfigParamObject> theList  =  Arrays.asList(objectsList);

		for (ResponseConfigParamObject theEntry:  theList) {
			if (theEntry.getAccess().equalsIgnoreCase("rw")) {
				logger.debug("Save list entry name="+ theEntry.getName() + ", value="+theEntry.getValue());	
				respParamFactory.setObject(theEntry);
			} else {
				logger.debug("Not saved list entry. Access=" + theEntry.getAccess() +"  name="+ theEntry.getName() + ", value="+theEntry.getValue());	
			}
		}

		DefaultObjectResponse response = new DefaultObjectResponse("List saved  successfully",0);

		logger.debug("<<< Save list ok");
		return new ResponseEntity<DefaultObjectResponse>(response,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);		
	}	

	/*=============================================================================================
	 * 
	 *    Handle request for  GET configuration value by name
	 *      
	 =============================================================================================*/	
	@RequestMapping(value = "/config/{var}", method = RequestMethod.GET, produces="application/json") 
	ResponseEntity<ResponseConfigParamObject> getVar(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
	        @PathVariable("var") String theVarName) throws Throwable
	{ 		
		logger.debug(">>> Request GET for /config/"+theVarName);	
		ResponseConfigParamObject respObj = respParamFactory.getObject(theVarName);
		logger.debug("<<< Get config parameter ok");
		return new ResponseEntity<ResponseConfigParamObject>(respObj,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	}
	
	/*=============================================================================================
	 * 
	 *    Handle request for  GET config variable value
	 *      
	 =============================================================================================*/	
	@RequestMapping(value = "/config/{var}", method = RequestMethod.PUT, produces="application/json") 
	ResponseEntity<DefaultResponse> setVar(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
	        @PathVariable("var") String theVarName,
	        ResponseConfigParamObject theValueObj
			) throws Throwable
	{ 		
		logger.debug(">>> Request PUT for /config/"+theVarName);	
		respParamFactory.setObject(theValueObj);
		
		DefaultResponse response = new DefaultResponse("Parameter saved.",0);
		logger.debug("<<< Config parameter saved");
		return new ResponseEntity<DefaultResponse>(response,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	}	
}

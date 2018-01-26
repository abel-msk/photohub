package home.abel.photohub.web;

import home.abel.photohub.connector.prototype.SiteCredentialInt;
import home.abel.photohub.connector.prototype.SitePropertyInt;
import home.abel.photohub.connector.prototype.SiteStatusEnum;
import home.abel.photohub.model.Node;
import home.abel.photohub.model.Schedule;
import home.abel.photohub.model.Site;
import home.abel.photohub.service.SiteService;
import home.abel.photohub.service.TaskQueueService;
import home.abel.photohub.tasks.BaseTask;
import home.abel.photohub.tasks.TaskFactory;
import home.abel.photohub.tasks.TaskNamesEnum;
import home.abel.photohub.web.model.AuthWaitSession;
import home.abel.photohub.web.model.DefaultObjectResponse;
import home.abel.photohub.web.model.DefaultResponse;
import home.abel.photohub.web.model.ResponseConfigParamObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api")
public class SiteController {
	final Logger logger = LoggerFactory.getLogger(SiteController.class);

	protected Map<String,AuthWaitSession> WaitingCallbackRedirectURLs  = 
			new java.util.concurrent.ConcurrentHashMap<String,AuthWaitSession>();
	
	@Autowired 
	SiteService siteSvc;
	@Autowired
	HeaderBuilderService headerBuild;

	@Autowired
	TaskQueueService taskQueue;

	@Autowired
	TaskFactory taskFactory;


	/*=============================================================================================
	 * 
	 *    Handle request for  GET  for get sites list  
	 *      
	 =============================================================================================*/
	@RequestMapping(value = "/site", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptConfilList(HttpServletRequest request) throws IOException, ServletException {
		logger.debug("Request OPTION  for /site");
		return new ResponseEntity<String>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}

	@RequestMapping(value = "/site", method = RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE) 
	ResponseEntity<DefaultObjectResponse<Iterable<Site>>> getSitesList(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse) throws Throwable
	{ 		
		logger.debug(">>> Request GET for /site");	
		Iterable<Site> theSitesList = siteSvc.getSitesList();

		return new ResponseEntity<DefaultObjectResponse<Iterable<Site>>>(
				new DefaultObjectResponse<Iterable<Site>>(theSitesList),
				headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	}
	/*=============================================================================================
	 * 
	 *    Handle request for GET  get site by its id
	 *      
	 =============================================================================================*/
	@RequestMapping(value = "/site/{id}", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptConfilVariable(HttpServletRequest request) throws IOException, ServletException {
		logger.debug("Request OPTION  for /site/{id}");
		return new ResponseEntity<String>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}

	@RequestMapping(value = "/site/{id}", method = RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE) 
	ResponseEntity<DefaultObjectResponse<Site>> getSite(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
	        @PathVariable("id") String objectID
			) throws Throwable
	{ 		
		logger.debug(">>> Request GET for /site/"+objectID);	
		Site theSite = siteSvc.getSite(objectID);
		return new ResponseEntity<DefaultObjectResponse<Site>>(
				new DefaultObjectResponse<Site>(theSite),
				headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	}
	/*=============================================================================================
	 * 
	 *    Handle request for PUT site object update, connect reconnect and authorize
	 *      
	 =============================================================================================*/
	@RequestMapping(value = "/site/{id}", method = RequestMethod.PUT, produces=MediaType.APPLICATION_JSON_VALUE) 
	ResponseEntity<DefaultObjectResponse<Site>> updateSite(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
	        @PathVariable("id") String objectID,
	        @RequestBody Site theSite
			) throws Throwable
	{ 		
		logger.debug(">>> Request PUT for /site/"+objectID);
		theSite.setId(objectID);
		theSite = siteSvc.updateSite(theSite);
		return new ResponseEntity<DefaultObjectResponse<Site>>(
				new DefaultObjectResponse<Site>(theSite),
				headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	}	
	
	/*=============================================================================================
	 * 
	 *    Handle request for GET return site connection crdential object
     *
	 =============================================================================================*/	
	@RequestMapping(value = "/site/{id}/connect", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptConnectSite(HttpServletRequest request) throws IOException, ServletException {
    	logger.debug("Request OPTION  for /site/{id}/connect");
	    return new ResponseEntity<String>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}
	/**
	 * Return Site Credential object for auth.
	 * 	     object {
	 *        userLoginFormUrl,
     *        accessToken,
     *        userMessage,
     *        properties[]
     *        state,
	 *     }
	 *     
	 * @param HTTPrequest
	 * @param HTTPresponse
	 * @param siteId
	 * @param callerUrl
	 * @return
	 * @throws Throwable
	 */
	@RequestMapping(value = "/site/{id}/connect", method = RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE) 
	ResponseEntity<DefaultObjectResponse<SiteCredentialInt>> connectSite(  
			final HttpServletRequest HTTPrequest, final HttpServletResponse HTTPresponse,
	        @PathVariable("id") String siteId,
	        @RequestParam(value = "caller", required=false) URL callerUrl
			) throws Throwable
	{ 		
		logger.debug(">>> Request GET for /site/"+siteId+"/connect [caller="+callerUrl+"]");
		
		String accessMeStr = HTTPrequest.getScheme()+"://"+HTTPrequest.getServerName() + 
		(HTTPrequest.getServerPort() == -1?"":(":"+Integer.toString(HTTPrequest.getServerPort())));
		String redirectorUrl= HTTPrequest.getRequestURI().substring(0,
				HTTPrequest.getRequestURI().lastIndexOf("connect")) + "redirector";
		
		URL accessMeUrl  = new URL(accessMeStr + redirectorUrl);  //authCode
		logger.debug("Prepare access me url = " + accessMeUrl + " for callback waitung" );			

		SiteCredentialInt cred = siteSvc.connectSite(siteId,accessMeUrl);
		
		//  Добавляем 
		if ( cred.getState() != SiteStatusEnum.CONNECT) {
			WaitingCallbackRedirectURLs.put(siteId,new AuthWaitSession(cred,callerUrl));
			logger.debug("Save final rirecet url="+callerUrl+", after callback will activated");
		}
		
		return new ResponseEntity<DefaultObjectResponse<SiteCredentialInt>>(
				new DefaultObjectResponse<SiteCredentialInt>(cred),
				headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	}	
	
	/*=============================================================================================
	 * 
	 *     Listeners for handle redirection after success on site authentication
	 *     Try to receive auth code for connector with OAuth2 method
     *
	 =============================================================================================*/
	@RequestMapping(value = "/site/{id}/redirector", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptAuthCode(HttpServletRequest request) throws IOException, ServletException {
    	logger.debug("Request OPTION  for /site/{id}/redirector");
	    return new ResponseEntity<String>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}
	
	@RequestMapping(value = "/site/{id}/redirector", method = RequestMethod.GET) 
	ResponseEntity<DefaultObjectResponse<String>> getAuthCode(final HttpServletRequest HTTPrequest,
			final HttpServletResponse HTTPresponse,
	        @PathVariable("id") String siteId,
	        @RequestParam(value = "code", required=false) String authCode
			) throws Throwable
	{ 		
		logger.debug(">>> Request GET for /site/"+siteId+"/redirector [code="+(authCode!=null?"OK":"null")+"]");	
		String redirectUrl = WaitingCallbackRedirectURLs.get(siteId).getReturnToPageUrl().toString();
		if (redirectUrl == null ) {
			throw new ExceptionObjectNotFound("Site " + siteId + " have not been connected.  Try connect first.");
		}	
		else {
			logger.debug("Retrieve redirectUrl="+redirectUrl+", from WaitingCallbackRedirectURLs for response redirect");
		}
		
		//
		// TODO:   Вместо получения параметра code, необходимо передавать все заголовки и параметры как AuthPropertyes
		//
		
		//   Get connector exchange credentiol, fillout id with received code and pass for authentication 
		SiteCredentialInt cred = WaitingCallbackRedirectURLs.get(siteId).getConnectorCred();
		if (cred == null ) {
			throw new ExceptionObjectNotFound("Got unexpected auth code for site " + siteId);
		}
		cred.setAccessToken(authCode);
		cred = siteSvc.authSite(siteId,cred);
		logger.debug("Got auth code for site id="+siteId+", type="+cred.getConnector().getSiteType()+". Auth passed with returned state = "+cred.getState());
		
		WaitingCallbackRedirectURLs.remove(siteId);
		logger.debug("Remove waiting callabck for site="+siteId+",  from WaitingCallbackRedirectURLs");
		
    	HttpHeaders headers= headerBuild.getHttpHeader(HTTPrequest);
    	headers.add("Location", redirectUrl);
		logger.debug("Set response Location header to "+redirectUrl);

	    return new ResponseEntity<DefaultObjectResponse<String>>(null,headers, HttpStatus.FOUND);
	}
	
	/*=============================================================================================
	 * 
	 *    Handle request for GET return site auth url
	 *      
	 =============================================================================================*/
	@RequestMapping(value = "/site/{id}/auth", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptAuthSite(HttpServletRequest request) throws IOException, ServletException {
    	logger.debug("Request OPTION  for /site/{id}/auth");
	    return new ResponseEntity<String>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}
	/**
	 *    Metod to pass auth token in auth object
	 * 
	 * 
	 * @param HTTPrequest
	 * @param HTTPresponse
	 * @param theSiteCred
	 * @return
	 * @throws Throwable
	 */
	@RequestMapping(value = "/site/{id}/auth", method = RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE) 
	ResponseEntity<DefaultObjectResponse<SiteCredentialInt>> authtSite(  final HttpServletRequest HTTPrequest, final HttpServletResponse HTTPresponse,
	        @PathVariable("id") String siteId,
	        @RequestBody SiteCredentialInt theSiteCred
			) throws Throwable
	{ 		
		logger.debug(">>> Request GET for /site/"+siteId+"/auth");	
		SiteCredentialInt cred = siteSvc.authSite(siteId, theSiteCred);	
		WaitingCallbackRedirectURLs.remove(siteId);

		return new ResponseEntity<DefaultObjectResponse<SiteCredentialInt>>(
				new DefaultObjectResponse<SiteCredentialInt>(cred),
				headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	}
	/*=============================================================================================
	 * 
	 *    Handle request for Disconnect site
	 *      
	 =============================================================================================*/
	@RequestMapping(value = "/site/{id}/disconnect", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptDiscSite(HttpServletRequest request) throws IOException, ServletException {
    	logger.debug("Request OPTION  for /site/{id}/disconnect");
	    return new ResponseEntity<String>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}
	@RequestMapping(value = "/site/{id}/disconnect", method = RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE) 
	ResponseEntity<DefaultObjectResponse<Site>> discSite(
			final HttpServletRequest HTTPrequest, final HttpServletResponse HTTPresponse,
	        @PathVariable("id") String siteId
			) throws Throwable
	{ 	
		logger.debug(">>> Request GET for /site/"+siteId+"/disconnect");	
		
		Site theSite = siteSvc.disconnectSite(siteId);
		
		logger.debug("Site disconnect. Connector state= "+theSite.getConnectorState().toString());
		return new ResponseEntity<DefaultObjectResponse<Site>>(
				new DefaultObjectResponse<Site>(theSite),
				headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	}
	
	
	/*=============================================================================================
	 * 
	 *    Handle request for DELETE site object update
	 *      
	 =============================================================================================*/
	@RequestMapping(value = "/site/{id}", method = RequestMethod.DELETE, produces=MediaType.APPLICATION_JSON_VALUE) 
	ResponseEntity<DefaultResponse> deleteSite(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
	        @PathVariable("id") String siteId
			) throws Throwable
	{ 		
//		logger.debug(">>> Request DELETE for /site/"+objectID);
//		siteSvc.removeSite(siteId);

		Site theSite = siteSvc.getSite(siteId);
		if ( theSite == null ) throw new ExceptionObjectNotFound("Site with ID="+siteId+" not found.");

		Schedule schedule = new Schedule();
		schedule.setTaskName(TaskNamesEnum.TNAME_REMOVE.toString());
		schedule.setEnable(false);
		schedule.setId(siteId);

		BaseTask task  = taskFactory.createTask(TaskNamesEnum.TNAME_REMOVE, theSite, schedule);
		task = taskQueue.put(task);


		DefaultResponse response = new DefaultResponse("Object deleted",0);
		return new ResponseEntity<DefaultResponse>(response,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	}	
	
	
	/*=============================================================================================
	 *
	 *    Handle request for Clean sites  content
	 *
	 =============================================================================================*/
	@RequestMapping(value = "/site/{id}/clean", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptCleanSite(HttpServletRequest request) throws IOException, ServletException {
    	logger.debug("Request OPTION  for /site/{id}/clean");
	    return new ResponseEntity<String>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}

	@RequestMapping(value = "/site/{id}/clean", method = RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<DefaultResponse> cleanSite(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
	        @PathVariable("id") String siteId
			) throws Throwable
	{

		Site theSite = siteSvc.getSite(siteId);
		if ( theSite == null ) throw new ExceptionObjectNotFound("Site with ID="+siteId+" not found.");

		Schedule schedule = new Schedule();
		schedule.setTaskName(TaskNamesEnum.TNAME_CLEAN.toString());
		schedule.setEnable(false);
		schedule.setId(siteId);

		BaseTask task  = taskFactory.createTask(TaskNamesEnum.TNAME_CLEAN, theSite, schedule);
		task = taskQueue.put(task);


		DefaultResponse response = new DefaultResponse("Site cleaned.",0);
		return new ResponseEntity<DefaultResponse>(response,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	}
	
	
	/*=============================================================================================
	 * 
	 *    Handle request for POST site object for add new site
	 *      
	 =============================================================================================*/
	@RequestMapping(value = "/site/add", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptAddSite(HttpServletRequest request) throws IOException, ServletException {
    	logger.debug("Request OPTION  for /site/add");
	    return new ResponseEntity<String>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}
	
	
	@RequestMapping(value = "/site/add", method = RequestMethod.POST, produces=MediaType.APPLICATION_JSON_VALUE) 
	ResponseEntity<DefaultObjectResponse<Site>> addSite(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
	        @RequestBody Site theSite
			) throws Throwable
	{ 		
		logger.debug(">>> Request POST for add new site."+
				" Name=" + theSite.getName() +
				", conectorType="+(theSite.getConnectorType()!=null?theSite.getConnectorType():"NULL")+ 
				", root="+(theSite.getRoot()!=null?theSite.getRoot():"NULL")
				);	
		//theSite.setId(null);
		theSite = siteSvc.createSite(theSite.getName(),
				theSite.getConnectorType(),
				theSite.getRoot(),
				siteSvc.createPropertyMap(theSite));
		
		
		logger.debug("Site add response.cret.state = "+theSite.getConnectorState().toString());
		return new ResponseEntity<DefaultObjectResponse<Site>>(
				new DefaultObjectResponse<Site>(theSite),
				headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	}	
	

	

	/*=============================================================================================
	 * 
	 *    Handle request for GET available site types
	 *      
	 =============================================================================================*/
	@RequestMapping(value = "/site/types", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptSiteTypes(HttpServletRequest request) throws IOException, ServletException {
    	logger.debug("Request OPTION  for /site/types");
	    return new ResponseEntity<String>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}
	
	@RequestMapping(value = "/site/types", method = RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE) 
	ResponseEntity<DefaultObjectResponse<List<String>>> getSiteTypes(HttpServletRequest request) throws Throwable {
		logger.debug(">>> Request "+ request.getMethod()+" "
				+request.getPathInfo()+" "+
				(request.getQueryString()==null?"":"["+request.getQueryString()+"]")
				);
		DefaultObjectResponse<List<String>> response = new DefaultObjectResponse<List<String>>("OK",0,siteSvc.getSiteTypes());
		
		return new ResponseEntity<DefaultObjectResponse<List<String>>>(response,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}
	
}

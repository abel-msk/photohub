package home.abel.photohub.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import home.abel.photohub.model.Node;
import home.abel.photohub.model.Site;
import home.abel.photohub.model.User;
import home.abel.photohub.service.ConfigService;
import home.abel.photohub.service.UserService;
import home.abel.photohub.web.model.DefaultResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserController {
	final Logger logger = LoggerFactory.getLogger(UserController.class);

	@Autowired 
	ConfigService configSvc;

	@Autowired
	UserService userService;
	
	@Autowired
	HeaderBuilderService headerBuild;
	

	
	/*=============================================================================================
	 * 
	 *    Handle request for  OPTIONS
	 *      
	 =============================================================================================*/
	@RequestMapping(value = "/user", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptConfilList(HttpServletRequest request) throws IOException, ServletException {
    	logger.debug("Request OPTION  for /user");
	    return new ResponseEntity<String>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}
	@RequestMapping(value = "/user/{id}", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptConfilVariable(HttpServletRequest request) throws IOException, ServletException {
    	logger.debug("Request OPTION  for /user/{id}");
	    return new ResponseEntity<String>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}
    
	
	
	/*=============================================================================================
	 * 
	 *    Handle request for  GET  for get users list
	 *      
	 =============================================================================================*/
	@RequestMapping(value = "/user", method = RequestMethod.GET, produces="application/json") 
	ResponseEntity<Iterable<User>> getUsersList(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse) throws Throwable
	{ 		
		logger.debug(">>> Request GET for /user");	
		Iterable<User> theUsersList = userService.getUsersList();

		logger.debug("<<< List ok");
		return new ResponseEntity<Iterable<User>>(theUsersList,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	}
	
	/*=============================================================================================
	 * 
	 *    Handle request for  GET  for get user by id
	 *      
	 =============================================================================================*/
	@RequestMapping(value = "/user/{id}", method = RequestMethod.GET, produces="application/json")
	ResponseEntity<User> getUser(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
	        @PathVariable("id") Long userId
	        ) throws Throwable
	{
		logger.debug(">>> Request GET for /user/"+ userId);
		User theUser = userService.getUser(userId);

		logger.debug("<<< List ok");
		return new ResponseEntity<User>(theUser,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	}

	
	
	/*=============================================================================================
	 * 
	 *    Handle request for  PUT  (update) user object
	 *      
	 =============================================================================================*/
	@RequestMapping(value = "/user/{id}", method = RequestMethod.PUT, produces="application/json") 
	ResponseEntity<User> updateUser(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
	        @PathVariable("id") Long userId,
	        @RequestBody User theUser
	        ) throws Throwable
	{ 		
		logger.debug(">>> Request PUT for /user/"+ userId);	
		theUser = userService.updateUser(theUser);

		logger.debug("<<< Updatet ok");
		return new ResponseEntity<User>(theUser,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	}
	
	
	
	/*=============================================================================================
	 * 
	 *    Handle request for POST new User object for add new user
	 *      
	 =============================================================================================*/
	@RequestMapping(value = "/user/add", method = RequestMethod.POST, produces="application/json") 
	ResponseEntity<User> addUser(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
	        @RequestBody User theUser
			) throws Throwable
	{
		logger.debug(">>> Request POST for add new user = " + theUser.getUsername());	
		theUser.setId(null);
		theUser = userService.addUser(theUser);
		logger.debug("<<< User add ok");
		return new ResponseEntity<User>(theUser,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	}
	
	/*=============================================================================================
	 * 
	 *    Handle request for DELETE user object 
	 *      
	 =============================================================================================*/
	@RequestMapping(value = "/user/{id}", method = RequestMethod.DELETE, produces="application/json")
	ResponseEntity<DefaultResponse> deleteUser(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
	        @PathVariable("id") Long userId
			) throws Throwable
	{
		logger.debug(">>> Request POST for delete userId = " + userId);
		userService.deleteUser(userId);
		DefaultResponse response = new DefaultResponse("User deleted",0);
		logger.debug("<<< User deleted ok ");
		return new ResponseEntity<DefaultResponse>(response,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	}

}


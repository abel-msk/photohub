package home.abel.photohub.web;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import home.abel.photohub.model.Node;
import home.abel.photohub.model.User;
import home.abel.photohub.model.UserRepository;
import home.abel.photohub.service.TokenService;
import home.abel.photohub.service.auth.UserAuthentication;
import home.abel.photohub.web.model.DefaultResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api")

public class LoginController {
	final Logger logger = LoggerFactory.getLogger(LoginController.class);

	@Autowired 
	TokenAuthenticationService tokenAuthenticationService;
	@Autowired
	AuthenticationManager authenticationManager;
	@Autowired
	private UserRepository userRepo;
	@Autowired
	TokenService tokenHandler;
	@Autowired
	HeaderBuilderService headerBuild;
	
	/*=============================================================================================
	 * 
	 *   Check Login request
	 *      
	 =============================================================================================*/
	@RequestMapping(value = "/login/check", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptCheckLogin(HttpServletRequest request) throws IOException, ServletException {
    	logger.debug("Request OPTION  for /login/check");
	    return new ResponseEntity<String>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}
	 
	@RequestMapping(value = "/login/check", method = RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE) 
	ResponseEntity<DefaultResponse> checkLogin(
	        final HttpServletRequest HTTPRequest,
	        final HttpServletResponse HTTPresponse) throws Throwable
	{ 		
		logger.debug(">>> Request GET fot /login/check");	
		DefaultResponse response = new DefaultResponse("OK",0);
		
		Authentication auth = tokenAuthenticationService.getAuthentication(HTTPRequest);
		if ( auth == null ) {
			response.setRc(52);
			response.setMessage("User not authenticated.");
		}
		logger.debug("Return status " + response);
		return new ResponseEntity<DefaultResponse>(response,headerBuild.getHttpHeader(HTTPRequest), HttpStatus.OK);
		//return ResponseEntity.ok().body(response);
	}
	
	
	/*=============================================================================================
	 * 
	 *   Login user
	 *      
	 =============================================================================================*/
	@RequestMapping(value = "/login/login", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptLoigin(HttpServletRequest HttpRequest) throws IOException, ServletException {
    	logger.debug("Request OPTION  for /login/login");
	    return new ResponseEntity<String>(null,headerBuild.getHttpHeader(HttpRequest), HttpStatus.OK);
	}
	@RequestMapping(value = "/login/login", method = RequestMethod.POST, produces=MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<DefaultResponse> jsonpLogin(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
	        @RequestParam(value = "username")  String userName,
	        @RequestParam(value = "password")  String userPasswd,
	        @RequestParam(value = "rememberMe", required=false) String remembetMe
			) throws Throwable
	{ 		

		logger.debug(">>> Login [ user = " + userName +" rememberMe = "+ (remembetMe==null?false:remembetMe) +" ]");	
		DefaultResponse response = new DefaultResponse("OK",0);
		
		String tokenString = login(HTTPresponse,userName,userPasswd,remembetMe);
		if (tokenString == null) {
			response.setRc(52);
			response.setMessage("User not authenticated.");
		}		
		return new ResponseEntity<DefaultResponse>(response,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	}
	
	
	/**
	 * 
	 *    Perform User Authentication. if authenticated generate token and save it in coockies.
	 *    Save user objcet in SecurityContextHolder
	 *    
	 * @param HTTPresponse 
	 * @param userName
	 * @param userPasswd
	 * @param remembetMe
	 * @return   generated token
	 * @throws Throwable
	 */
	public static final String AUTH_HEADER_NAME = "x-auth-token";
	private static final long ONE_DAY_MILLIS = 1000 * 60 * 60 * 24;
	private static final int ONE_DAY_SECS = 60 * 60 * 24;
	private static final int EXP_DAYS = 10;
	protected  String login(HttpServletResponse HTTPresponse, String userName, String userPasswd, String remembetMe ) throws Throwable
	{
		String tokenString = null;
		
		// Just check password
		final UsernamePasswordAuthenticationToken loginToken =  new UsernamePasswordAuthenticationToken(userName, userPasswd);	
		Authentication authentication = authenticationManager.authenticate(loginToken);
		
		if (authentication.isAuthenticated()) {
			
			//    Get user details and set expiration date
			User authenticatedUser =  userRepo.findByUsername(userName);
			authenticatedUser.setExpires(System.currentTimeMillis() + (ONE_DAY_MILLIS * EXP_DAYS));
			
			//    Prepare and create token
			UserAuthentication userAuthentication = new UserAuthentication(authenticatedUser);
			tokenString = tokenHandler.createTokenForUser(userAuthentication.getDetails());
			tokenString = tokenHandler.createTokenForUser(userAuthentication.getDetails());


			logger.debug("Create auth token = " + 
					tokenString.subSequence(0, 5) + 
					"*****" + 
					tokenString.subSequence(tokenString.length()-5, tokenString.length()) 
					);
			

			//   Insert cookie with token to response header
			
	        Cookie cookie = new Cookie(AUTH_HEADER_NAME, tokenString);
	        logger.trace("Send token as coockie " + cookie.getValue());
	        cookie.setPath("/");
	        cookie.setSecure(false);
	        cookie.setMaxAge(ONE_DAY_SECS * EXP_DAYS);
	        HTTPresponse.addCookie(cookie);
			
			// Add the authentication to the Security context
			SecurityContextHolder.getContext().setAuthentication(userAuthentication);
			logger.debug("User="+authenticatedUser.getUsername()+ " login successfuly. Response with token.");
		}
		
		return tokenString;
	}

}

package home.abel.photohub.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import home.abel.photohub.service.ExceptionDBContent;
import home.abel.photohub.service.ExceptionFileIO;
import home.abel.photohub.service.ExceptionInternalError;
import home.abel.photohub.service.ExceptionInvalidArgument;
import home.abel.photohub.service.ExceptionPhotoProcess;
import home.abel.photohub.service.ExceptionTokenAuth;
import home.abel.photohub.web.model.DefaultResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.AbstractJsonpResponseBodyAdvice;

@ControllerAdvice(basePackages = {"home.abel.photohub"} )
public class GlobalControllerAdvice extends AbstractJsonpResponseBodyAdvice {
	final Logger logger = LoggerFactory.getLogger(GlobalControllerAdvice.class);
	
    public GlobalControllerAdvice() {
        super("callback");
    }
	
	@Autowired
	HeaderBuilderService headerBuild;
	
	
    @ExceptionHandler(value=Exception.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    ResponseEntity<DefaultResponse> exceptionHandler(
    		final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
    		Exception e) {

    	
		DefaultResponse response = new DefaultResponse(e.getLocalizedMessage(),9999);
    	logger.error(" ControllerAdvice proces Error : " + e.getLocalizedMessage(),e);
    	
    	if (e instanceof ExceptionAccessDeny ) {
    		response.setRc(1);
    	} else  if (e instanceof ExceptionInvalidRequest ) {
    		response.setRc(2);
		} else  if (e instanceof ExceptionObjectNotFound ) {
			response.setRc(3);  	
		} else  if (e instanceof ExceptionDBContent ) {
			response.setRc(4); 
		} else  if (e instanceof ExceptionFileIO ) {
			response.setRc(5);
		} else  if (e instanceof ExceptionInternalError ) {
			response.setRc(6);
		} else  if (e instanceof ExceptionInvalidArgument ) {
			response.setRc(7);
		} else  if (e instanceof ExceptionPhotoProcess ) {
			response.setRc(8);
		} else  if (e instanceof ExceptionTokenAuth ) {
			response.setRc(9);
		} 
	    	
		logger.debug("<<< Response status : BAD_REQUEST. "+response);	
		return new ResponseEntity<DefaultResponse>(response,headerBuild.getHttpHeaderSilent(HTTPrequest),HttpStatus.BAD_REQUEST);
    }
    
//	/**
//	 * Create and return default header
//	 * @return 
//	 */
//	public static HttpHeaders getHttpHeader() {
//		HttpHeaders responseHeaders = new HttpHeaders();
//		responseHeaders.set("Access-Control-Allow-Origin", "*");
//		responseHeaders.add("Access-Control-Allow-Methods", "GET, OPTIONS, POST, DELETE, PUT, PATCH");
//		responseHeaders.add("Access-Control-Allow-Headers", "Content-Type");
//		responseHeaders.add("Access-Control-Max-Age", "86400");
//		//responseHeaders.add("Cache-Control","max-age=0, must-revalidate");
//		return responseHeaders;
//	}
	
} 
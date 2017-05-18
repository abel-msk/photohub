package home.abel.photohub.webconfig.standalone;

import home.abel.photohub.service.ConfigService;
import home.abel.photohub.web.model.DefaultResponse;

import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
/**
 * Redirect request for root to the index.html
 * @author abel
 *
 */
@Controller
public class RootPage {
	final Logger logger = LoggerFactory.getLogger(RootPage.class);

	@Autowired
	Environment env;
	
	//getServletContext().getRequestDispatcher(resName).forward(request, response);
	/**
	 * Redirect from / to index.html
	 * @return
	 * @throws URISyntaxException
	 */
//	@RequestMapping(value = "/", method = RequestMethod.GET)
//	public ResponseEntity<Object> redirectToExternalUrl() throws URISyntaxException {
//	    URI indexPage = new URI("index.html");
//	    HttpHeaders httpHeaders = new HttpHeaders();
//	    httpHeaders.setLocation(indexPage);
//	    return new ResponseEntity<Object>(httpHeaders, HttpStatus.MOVED_PERMANENTLY);//HttpStatus.MOVED_PERMANENTLY//HttpStatus.SEE_OTHER
//	}
//	
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public ModelAndView rootRequest() {
		
		logger.debug(">>> Get root request");
	
        ModelAndView mav = new ModelAndView();
        mav.setViewName("index");
        
        String str = "Hello World!";
        mav.addObject("message", str);
        logger.debug(">>> Send view index");
        return mav;
	}
	
	
//    @RequestMapping(value = "/")
//    public String index(Model model){
//
//        // implements business logic    
//
//        return "index";
//    }  
	
	
	/**
	 * Reply property 'apiURLPath' for root URL path  api requests
	 * If property does not set it try construct path from property server.address and server.port
	 * Default value server.address = 'localhost' and  server.port = '8080'  and prefix '/api'
	 * @return
	 */
	@RequestMapping(value = "/apiURLPath", method = RequestMethod.GET)
	@ResponseBody ResponseEntity<DefaultResponse>getApiPathEnviroment() {
		String apiPath = env.getProperty("apiURLPath");
		if (apiPath == null) {
			apiPath = 
					"http://" + env.getProperty("server.address","localhost") +
					":" + env.getProperty("server.port","8080") + 
					"/api";
		}
		logger.debug("Request for getApiPath : " + apiPath);
	    return new ResponseEntity<DefaultResponse>(
	    		new DefaultResponse(apiPath,0) , HttpStatus.OK);
	}
		
}

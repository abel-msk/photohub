package home.abel.photohub.webconfig.server;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration.Dynamic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.DispatcherServlet;

public class AppInit implements WebApplicationInitializer {

	private static final String DISPATCHER_SERVLET_NAME = "dispatcher";  
	final Logger logger = LoggerFactory.getLogger(AppInit.class);
	
	
	//	http://docs.spring.io/autorepo/docs/spring/4.1.x/javadoc-api/org/springframework/web/WebApplicationInitializer.html
    private static final Class<?>[] CONFIG_CLASSES = new Class<?>[]{SpringWebConfig.class};

	@Override  
	public void onStartup(ServletContext servletContext) throws ServletException {  
		logger.info("+++ Starting photohub application in server mode");
		
    	System.setProperty("spring.config.name", "photohub-server");   
       	System.setProperty("InstallationType", "server");
       	System.setProperty("photohub.store.conf","true");
       	
       	
	    // Create the 'root' Spring application context
		AnnotationConfigWebApplicationContext rootContext = new AnnotationConfigWebApplicationContext(); 
		rootContext.getEnvironment().setActiveProfiles("server","mysql");
		rootContext.register(CONFIG_CLASSES); 

       	
		logger.info("+++ Register SpringMVC config in RootContext");

				
	    // Manage the lifecycle of the root application context
		servletContext.addListener(new ContextLoaderListener(rootContext));
		logger.info("+++ addListener RootContext");
				
	    // Create the dispatcher servlet's Spring application context
	    AnnotationConfigWebApplicationContext dispatcherContext = new AnnotationConfigWebApplicationContext();
	    //dispatcherContext.register(DispatcherConfig.class);
	    logger.info("+++ Create dispatherContext");	
	    
		// Register and map Main Servlet
		Dynamic servlet = servletContext.addServlet(DISPATCHER_SERVLET_NAME, new DispatcherServlet(dispatcherContext));  
		/*  Old fashon
		// Register and map Main Servlet
		Dynamic servlet = servletContext.addServlet(DISPATCHER_SERVLET_NAME, new DispatcherServlet(rootContext));  
		*/
		servlet.addMapping("/");  
		servlet.setInitParameter("dispatchOptionsRequest", "true");
		servlet.setLoadOnStartup(1);  
		
		//  Tune up multipart file upload params                      MAX_FILE_UPLOAD_SIZE   MAX_REQUEST_SIZE  FILE_SIZE_TRESHHOLD
		servlet.setMultipartConfig(new MultipartConfigElement("/tmp", 1024*1024*5, 1024*1024*5*5, 1024*1024));
		System.setProperty("file.encoding", "UTF-8");
		
		//  Register Core filter with response headers
		//FilterRegistration RespFilter = servletContext.addFilter("CharacterEncodingFilter", new CoreFilter());
		//RespFilter.addMappingForUrlPatterns(null, true, "/*"); 

		//  Register CharsetEncodingFilter
		FilterRegistration charEncodingfilterReg = servletContext.addFilter("CharacterEncodingFilter", CharacterEncodingFilter.class);
		charEncodingfilterReg.setInitParameter("encoding", "UTF-8");
		charEncodingfilterReg.setInitParameter("forceEncoding", "true");
		charEncodingfilterReg.addMappingForUrlPatterns(null, false, "/*");
		
		//  Register springSecurityFilterChain filter
		FilterRegistration securityFilterReg =
	            servletContext.addFilter("springSecurityFilterChain", new DelegatingFilterProxy());
		securityFilterReg.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*"); 
		//securityFilterReg.setInitParameters(arg0)
		
		logger.info("+++ addSecurity filter");
	}  

	/*
	private void registerHiddenHttpMethodFilter(ServletContext servletContext) {  
	    FilterRegistration.Dynamic fr = servletContext.addFilter("hiddenHttpMethodFilter", HiddenHttpMethodFilter.class);  
	    fr.addMappingForServletNames(  
	            EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD),false,DISPATCHER_SERVLET_NAME);  
	}  
	//@Parameter(defaultValue = "test.email", readonly = true)		
	private void registerSecurity(ServletContext servletContext) {
	}
	*/
}

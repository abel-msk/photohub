package home.abel.photohub.webconfig.standalone;


import javax.servlet.MultipartConfigElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.support.SpringBootServletInitializer;
//import org.springframework.boot.context.embedded.MultipartConfigFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
//import com.mangofactory.swagger.plugin.EnableSwagger;

/*****************************************************************************
 * 
 *    Spring configuration and app initializer  for embeded tomcat
 *    Start with spring-boot framework as standalone application
 * 
 * @author abel
 ****************************************************************************/

@Configuration
//@EnableWebMvc
@ComponentScan(basePackages = {"home.abel.photohub.web","home.abel.photohub.webconfig.standalone"})

@Import({
	home.abel.photohub.webauth.StatelessAuthenticationSecurityConfig.class,
	home.abel.photohub.service.SpringConfig.class,
	home.abel.photohub.model.SpringDataConfig.class,
	home.abel.photohub.webconfig.standalone.MvcConfiguration.class})


//@EnableAutoConfiguration
@EnableSpringDataWebSupport
@EnableTransactionManagement

//@EnableAsync
//@EnableSwagger
@EnableSwagger2
@SpringBootApplication
public class AppInit extends SpringBootServletInitializer {
	final Logger logger = LoggerFactory.getLogger(AppInit.class);
	
	//  Create /swagger-ui.html
	//  Request from   http://localhost:8081/swagger-ui.html
	@Bean
	public Docket api() {                
	    return new Docket(DocumentationType.SWAGGER_2)          
	      .select()
	      .apis(RequestHandlerSelectors.basePackage("home.abel.photohub.web"))
	      //.paths(PathSelectors.ant("/api/*"))
	      .build()
	      .apiInfo(apiInfo());
	}
	 
	private ApiInfo apiInfo() {
		
        return new ApiInfoBuilder()
                .title("Photohub-REST-API")
                .description("Spring REST API. Generated by with Swagger")
                .termsOfServiceUrl("http://www-03.ibm.com/software/sla/sladb.nsf/sla/bm?Open")
                //.contact("Niklas Heidloff")
                .license("Apache License Version 2.0")
                .licenseUrl("https://github.com/IBM-Bluemix/news-aggregator/blob/master/LICENSE")
                .version("1.0")
                .build();
	}
	
	
	/*----------------------------------------------------------------- 
	 *    Configure Tomacat container Properties
	 *----------------------------------------------------------------- 
	 */
    @Bean
    public EmbeddedServletContainerFactory servletContainer() {
        TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory();
        //factory.setPort(8081);
        //factory.setSessionTimeout(10, TimeUnit.MINUTES);
        //factory.addErrorPages(new ErrorPage(HttpStatus.404, "/notfound.html");
		logger.trace("Initialize TomcatEmbeddedServletContainerFactory");
        return factory;
    }
	 
	/*----------------------------------------------------------------- 
	 *    Configure Security
	 ----------------------------------------------------------------- */	
//	@Bean
//	public SecurityContextService securityContextService() {
//		SecurityContextService scs = new SecurityContextSingleUser();
//		return scs;
//	}

	/*----------------------------------------------------------------- 
	 *    Create Dispatcher servlet
	 ----------------------------------------------------------------- */  
    @Bean
    public DispatcherServlet dispatcherServlet() {
    	DispatcherServlet ds =  new DispatcherServlet();
    	ds.setDispatchOptionsRequest(true);
    	logger.trace("Initialize dispatcherServlet");
    	return ds;
    }   
    
   
	/*----------------------------------------------------------------- 
	 *    Registering Dispatcher 
	 ----------------------------------------------------------------- */   
    @Bean
    public ServletRegistrationBean dispatcherServletRegistration() {
        ServletRegistrationBean registration = new ServletRegistrationBean(dispatcherServlet());
        registration.setName(DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME);
        registration.addUrlMappings("/*");
        registration.setOrder(1);
        
    	//MultipartConfigFactory factory = new MultipartConfigFactory();
        //factory.setMaxFileSize("128KB");
        //factory.setMaxRequestSize("128KB");        
        //registration.setMultipartConfig(factory.createMultipartConfig());
        registration.setMultipartConfig(multipartConfigElement());
        //multipartConfigElement()

		logger.trace("Initialize dispatcherServletRegistration");

		return registration;
    }  
    
	/*----------------------------------------------------------------- 
	 *    Registering Filters 
	 ----------------------------------------------------------------- */    
    @Bean
    public FilterRegistrationBean contextFilterRegistrationBean() {
	    FilterRegistrationBean registrationBean = new FilterRegistrationBean();
	    CharacterEncodingFilter encFilter = new CharacterEncodingFilter();
	    encFilter.setEncoding("UTF-8");   
	    registrationBean.setFilter(encFilter);
	    registrationBean.setOrder(1);
	    return registrationBean;
    }
	
	/*----------------------------------------------------------------- 
	 *    Create Multipart parameters parsing config 
	 ----------------------------------------------------------------- */  
    @Bean
    MultipartConfigElement multipartConfigElement() {
    	MultipartConfigFactory factory = new MultipartConfigFactory();
        //factory.setMaxFileSize("1024KB");
        //factory.setMaxRequestSize("1024KB");
        return factory.createMultipartConfig();
    }


	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurerAdapter() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				//registry.addMapping("/api/**");
				registry.addMapping("/**");
			}
		};
	}

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(AppInit.class);
    }

//	@Override
//	public void addCorsMappings(CorsRegistry registry) {
//		registry.addMapping("/**");
//	}



    /**==================================================================================
     *    Start Application
     * @param args
     * @throws Exception
     * ==================================================================================
     */
    public static void main(String[] args) throws Exception {
    	System.setProperty("spring.config.name", "photohub-standalone");   
       	System.setProperty("installationType", "standalone"); //photohub.run.type
       	System.setProperty("photohub.store.conf","true");
       	
       	System.setProperty("hsqldb.reconfig_logging", "false"); 
    	ConfigurableApplicationContext ctx = SpringApplication.run(AppInit.class, args);
    	ctx.getEnvironment().setActiveProfiles("standalone");
    	//ctx.refresh();
    }

}

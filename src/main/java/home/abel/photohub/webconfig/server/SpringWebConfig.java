package home.abel.photohub.webconfig.server;

//import home.abel.photohub.web.model.ResponsePhotoObjectFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@EnableWebMvc
@EnableSpringDataWebSupport
@EnableTransactionManagement

@EnableAsync

@Import({
	home.abel.photohub.webauth.StatelessAuthenticationSecurityConfig.class,
	home.abel.photohub.service.SpringConfig.class,
	home.abel.photohub.webconfig.server.SpringDataConfig.class
	})

@PropertySource({"classpath:photohub.properties","photohub-server.properties"})
@ComponentScan(basePackages = "home.abel.photohub.web")
//@Profile("server")
public class SpringWebConfig extends WebMvcConfigurerAdapter  {
	
//	@Bean
//	public ResponsePhotoObjectFactory responsePhotoObjectFactory(){
//		ResponsePhotoObjectFactory rof = new ResponsePhotoObjectFactory();
//		return rof;
//	}
	
    @Bean
    public MultipartResolver multipartResolver() {
    	return new StandardServletMultipartResolver();
    }
    
    /*
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
            .addResourceHandler("/resources/**")
            .addResourceLocations("/resources/")
            .setCachePeriod(31556926);
    }
*/
    
	/*
    @Override  
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {  
        configurer.favorPathExtension(true)  
            .useJaf(false)  
            .ignoreAcceptHeader(true)  
            .mediaType("html", MediaType.TEXT_HTML)  
            .mediaType("json", MediaType.APPLICATION_JSON)  
            .defaultContentType(MediaType.TEXT_HTML);  
    } 
    
    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }
    */
}


package home.abel.photohub.service;

import home.abel.photohub.connector.ConnectorsFactory;
import home.abel.photohub.tasks.TaskFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ComponentScan(basePackages={"home.abel.photohub.service","home.abel.photohub.utils"})
@EnableTransactionManagement
public class SpringConfig {
	@Autowired
	Environment env;

	@Bean
	public ConnectorsFactory connectorsFactory() throws Throwable {
		ConnectorsFactory factory = new ConnectorsFactory();
		factory.addConnectorClass("home.abel.photohub.connector.local.LocalSiteConnector");
		factory.addConnectorClass("home.abel.photohub.connector.google.GoogleSiteConnector");

	    return factory;
	}
	
	@Bean(destroyMethod="shutdown")
	public ThreadPoolTaskExecutor threadPoolTaskExecutor()  {
		ThreadPoolTaskExecutor threadPool = new ThreadPoolTaskExecutor();
		threadPool.setWaitForTasksToCompleteOnShutdown(true);
		threadPool.setCorePoolSize(2);
		threadPool.initialize();
	    return threadPool;
	}	   
	
	@Bean(destroyMethod="shutdown")
	public ThreadPoolTaskScheduler threadPoolTaskScheduler()  {
		ThreadPoolTaskScheduler threadPool = new ThreadPoolTaskScheduler();
		threadPool.setPoolSize(5);
		threadPool.setThreadNamePrefix("SchedQueue-");
		threadPool.setWaitForTasksToCompleteOnShutdown(false);
		threadPool.initialize();
	    return threadPool;
	}	   
	
	@Bean
	public TaskFactory  taskFactory()  {
	    return new TaskFactory();
	}	
	   
}


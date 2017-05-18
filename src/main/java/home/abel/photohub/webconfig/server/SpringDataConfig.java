package home.abel.photohub.webconfig.server;


import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.apache.commons.dbcp.BasicDataSource;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.logging.SessionLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaDialect;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@Configuration
@ComponentScan(
		excludeFilters = @Filter(type=FilterType.REGEX, pattern={"home.abel.photohub.model.standalone.*","home.abel.photohub.model.server.*"}),
		basePackages = "home.abel.photohub.model"
		)

@EnableJpaRepositories("home.abel.photohub.model")
@PropertySource("classpath:db-connector.properties")
@EnableTransactionManagement

public class SpringDataConfig {
	final Logger logger = LoggerFactory.getLogger(SpringDataConfig.class);

    @Autowired
    Environment env;
	
	@Bean(destroyMethod="close")
	public DataSource dataSource() {	
				
		BasicDataSource DS = new BasicDataSource();
		String dbURL = env.getProperty("db.url","");
		String dbName = env.getProperty("db.name","");
		String dbOptions = env.getProperty("db.params","");
		String dbUser = env.getProperty("db.username","");
		String dbPw = env.getProperty("db.password","");	
		String dbDriverName = env.getProperty("db.driver");
		
		logger.info("Create DB Connection: Driver="+ dbDriverName +
					", DB="+dbURL + dbName + dbOptions + ", Username=" +dbUser);
		
		
		DS.setUrl(dbURL + dbName + dbOptions);
		DS.setDriverClassName(dbDriverName);		
		//DS.setDriverClassName("com.mysql.jdbc.Driver");
		DS.setUsername(dbUser);
		DS.setPassword(dbPw);		
		return (DataSource) DS;
	}
	
	@Bean
	EclipseLinkJpaVendorAdapter vendorAdapter() {

		String DBName = env.getProperty("db.vendor.dbname","H2");	
		String DBPlatform = env.getProperty("db.vendor.platform","org.eclipse.persistence.platform.database.H2Platform");
		logger.info("Use vendor dapter: DB name="+ DBName + ", DB platform=" + DBPlatform);
			
		EclipseLinkJpaVendorAdapter va = new EclipseLinkJpaVendorAdapter();
		//va.setShowSql(false);
		va.setShowSql(true);
		va.setDatabasePlatform(DBPlatform);
		//va.setDatabasePlatform("org.eclipse.persistence.platform.database.MySQLPlatform");
		//va.setDatabasePlatform("org.eclipse.persistence.platform.database.OraclePlatform");
		va.setGenerateDdl(false);
		va.setDatabase(Database.valueOf(DBName));
		//va.setDatabase(Database.valueOf("ORACLE"));
		return va;
	}	
	
	@Bean
	public JpaDialect jpaDialect() {
			return new org.springframework.orm.jpa.vendor.EclipseLinkJpaDialect();
	}	
	
	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
		LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
						
        /***   Prepare factory propertyes   ***/
		Map<String, String> jpaProperties = new HashMap<String, String>();
		//  Can use -javaagent:eclipselink.jar
		//jpaProperties.put("eclipselink.weaving", "false");
		jpaProperties.put("eclipselink.weaving", "static");
		jpaProperties.put(PersistenceUnitProperties.LOGGING_LEVEL,SessionLog.WARNING_LABEL);
		
		boolean isDDLGeneration = env.getProperty("db.generate",boolean.class,true);	
		if (isDDLGeneration) {
			jpaProperties.put(PersistenceUnitProperties.DDL_GENERATION, PersistenceUnitProperties.CREATE_OR_EXTEND);
			jpaProperties.put(PersistenceUnitProperties.DDL_GENERATION_MODE, PersistenceUnitProperties.DDL_DATABASE_GENERATION);
		}
		
		/***  Configure propertyfactory ***/
		factory.setJpaPropertyMap(jpaProperties);        
        factory.setJpaVendorAdapter(vendorAdapter());
        factory.setJpaDialect(jpaDialect());
        factory.setPersistenceUnitName("photohub2-data");
        //factory.setPersistenceXmlLocation("META-INF/persistence.xml");
        //factory.setPersistenceXmlLocation("classpath:persistence.xml");       
        factory.setPersistenceXmlLocation("classpath:META-INF/persistence.xml");
		factory.setPackagesToScan("home.abel.photohub.data");
		factory.setDataSource(dataSource());
		return factory;
	}
		
	@Bean
	public PlatformTransactionManager transactionManager() {
		//JpaTransactionManager txManager =  new DataSourceTransactionManager(dataSource());
		JpaTransactionManager txManager = new JpaTransactionManager();
		txManager.setEntityManagerFactory(entityManagerFactory().getObject());
		txManager.setJpaDialect(jpaDialect());		
		return txManager;
	}	
	
	

	
	
	
}

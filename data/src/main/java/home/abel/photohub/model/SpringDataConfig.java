package home.abel.photohub.model;


import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import org.flywaydb.core.Flyway;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.apache.commons.dbcp.BasicDataSource;
import org.eclipse.persistence.config.LoggerType;
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
//@PropertySource("classpath:db-connector.properties")
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
		String dbDriverName = env.getProperty("db.driver","org.hsqldb.jdbc.JDBCDriver");
		
		logger.info("Create DB Connection: Driver="+ dbDriverName +
					", DB="+dbURL + dbName + dbOptions + ", Username=" +dbUser);
		
		
		DS.setUrl(dbURL + dbName + dbOptions);
		DS.setDriverClassName(dbDriverName);		
		//DS.setDriverClassName("com.mysql.jdbc.Driver");
		DS.setUsername(dbUser);
		DS.setPassword(dbPw);		
		return (DataSource) DS;
	}



    @Bean(name="flyway", initMethod = "migrate")
    public  org.flywaydb.core.Flyway initFlyway() throws Exception {
        	Flyway flyway= new Flyway();
			//dataSource().getConnection().setAutoCommit(false);
			//flyway.   // baselineOnMigrate to true
			flyway.setBaselineOnMigrate(true);
			flyway.setDataSource(dataSource());
			flyway.setLocations("classpath:db/migration/");

        return flyway;
    }




    @Bean
	EclipseLinkJpaVendorAdapter vendorAdapter() {

		String DBName = env.getProperty("db.vendor.dbname","HSQL");	
		String DBPlatform = env.getProperty("db.vendor.platform","org.eclipse.persistence.platform.database.HSQLPlatform");
		logger.info("Use vendor adapter: DB name="+ DBName + ", DB platform=" + DBPlatform);
			
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
    @DependsOn("flyway")
	public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
		LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
						
        /***   Prepare factory propertyes   ***/
		Map<String, String> jpaProperties = new HashMap<String, String>();
		//  Can use -javaagent:eclipselink.jar
		//jpaProperties.put("eclipselink.weaving", "false");
		jpaProperties.put("eclipselink.weaving", "static");
		
		jpaProperties.put(PersistenceUnitProperties.LOGGING_LOGGER,LoggerType.JavaLogger);
		//eclipselink.logging.logger
		
		java.lang.Boolean isSQLDebug = env.getProperty("db.sql.debug",java.lang.Boolean.class,java.lang.Boolean.FALSE);	
		if ( isSQLDebug ) {
			logger.info("Use SQL debug  mode");			
			jpaProperties.put(PersistenceUnitProperties.LOGGING_PARAMETERS, "True");
			jpaProperties.put(PersistenceUnitProperties.CATEGORY_LOGGING_LEVEL_ +  SessionLog.SQL, SessionLog.FINEST_LABEL);
		}
		
		java.lang.Boolean isJPADebug = env.getProperty("db.jpa.debug",java.lang.Boolean.class,java.lang.Boolean.FALSE);	
		if ( isJPADebug ) {
			logger.info("Use JPA debug all mode");
			jpaProperties.put(PersistenceUnitProperties.CATEGORY_LOGGING_LEVEL_ +  SessionLog.TRANSACTION, SessionLog.ALL_LABEL);
			jpaProperties.put(PersistenceUnitProperties.CATEGORY_LOGGING_LEVEL_ +  SessionLog.EVENT, SessionLog.ALL_LABEL);
			jpaProperties.put(PersistenceUnitProperties.CATEGORY_LOGGING_LEVEL_ +  SessionLog.CONNECTION, SessionLog.ALL_LABEL);
			jpaProperties.put(PersistenceUnitProperties.CATEGORY_LOGGING_LEVEL_ +  SessionLog.QUERY, SessionLog.ALL_LABEL);
			jpaProperties.put(PersistenceUnitProperties.CATEGORY_LOGGING_LEVEL_ +  SessionLog.CACHE, SessionLog.ALL_LABEL);
			jpaProperties.put(PersistenceUnitProperties.CATEGORY_LOGGING_LEVEL_ +  SessionLog.PROPAGATION, SessionLog.ALL_LABEL);
			jpaProperties.put(PersistenceUnitProperties.CATEGORY_LOGGING_LEVEL_ +  SessionLog.SEQUENCING, SessionLog.ALL_LABEL);
			jpaProperties.put(PersistenceUnitProperties.CATEGORY_LOGGING_LEVEL_ +  SessionLog.METAMODEL, SessionLog.ALL_LABEL);
			//jpaProperties.put(PersistenceUnitProperties.CATEGORY_LOGGING_LEVEL_ +  SessionLog.WEAVER, SessionLog.ALL_LABEL);
			//jpaProperties.put(PersistenceUnitProperties.CATEGORY_LOGGING_LEVEL_ +  SessionLog.PROPERTIES, SessionLog.ALL_LABEL);
			jpaProperties.put(PersistenceUnitProperties.CATEGORY_LOGGING_LEVEL_ +  SessionLog.SERVER, SessionLog.ALL_LABEL);
			//jpaProperties.put(PersistenceUnitProperties.LOGGING_LEVEL,SessionLog.FINEST_LABEL);
		
		}
		java.lang.Boolean isDDLGeneration = env.getProperty("db.generate",java.lang.Boolean.class,java.lang.Boolean.TRUE);	
		if (isDDLGeneration) {
			String DDLGenerationMode = env.getProperty("db.generate.mode",java.lang.String.class,"create");	
			logger.trace("DB Generation, mode "+DDLGenerationMode);
			if (DDLGenerationMode.toLowerCase() == "drop" ) {
				jpaProperties.put(PersistenceUnitProperties.DDL_GENERATION, 
						PersistenceUnitProperties.DROP_AND_CREATE);
			} else if (DDLGenerationMode.toLowerCase() == "none") {
				jpaProperties.put(PersistenceUnitProperties.DDL_GENERATION,
						PersistenceUnitProperties.NONE);
			} else {
				jpaProperties.put(PersistenceUnitProperties.DDL_GENERATION, 
						PersistenceUnitProperties.CREATE_OR_EXTEND);
			}
			String DDLGenerationFile = env.getProperty("db.generate.file",java.lang.String.class,null);	
			if (DDLGenerationFile != null ) {
				logger.trace("Copy DB generation sql to file " + DDLGenerationFile);
				jpaProperties.put(PersistenceUnitProperties.DDL_GENERATION_MODE, 
						PersistenceUnitProperties.DDL_BOTH_GENERATION);
				jpaProperties.put(PersistenceUnitProperties.CREATE_JDBC_DDL_FILE, 
						DDLGenerationFile);
			} else {
				jpaProperties.put(PersistenceUnitProperties.DDL_GENERATION_MODE, 
						PersistenceUnitProperties.DDL_DATABASE_GENERATION);
			}
		}
		/***  Configure propertyfactory ***/
		factory.setJpaPropertyMap(jpaProperties);        
        factory.setJpaVendorAdapter(vendorAdapter());
        factory.setJpaDialect(jpaDialect());
        factory.setPersistenceUnitName("photohub2-data");
        //factory.setPersistenceXmlLocation("META-INF/persistence.xml");
        //factory.setPersistenceXmlLocation("classpath:persistence.xml");       
        //factory.setPersistenceXmlLocation("classpath:META-INF/persistence.xml");
		factory.setPersistenceXmlLocation("classpath:persistence.xml");

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

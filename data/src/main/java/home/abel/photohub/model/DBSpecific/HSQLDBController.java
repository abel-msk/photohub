package home.abel.photohub.model.DBSpecific;

import home.abel.photohub.model.SpringDataConfig;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.sql.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;

@Component
public class HSQLDBController {
	final Logger logger = LoggerFactory.getLogger(HSQLDBController.class);

    @Autowired
    Environment env;
	
    @Autowired
    javax.sql.DataSource dataSource;
    
    @Autowired
    private ApplicationContext applicationContext;
    
	@PostConstruct
	public void init() {
	
		
		
	}


	/**
	 *    Shutdown HSQL DATABASE
	 */
	@PreDestroy	
	public void close() {
		String DBName = env.getProperty("db.vendor.dbname");		
		if ( DBName.compareTo("HSQL") == 0) {			
	        Connection conn = null;
	        try {
	            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
	            conn = DataSourceUtils.getConnection(jdbcTemplate.getDataSource());
	            conn.setAutoCommit(true);
	            jdbcTemplate.execute("SHUTDOWN"); 
	            logger.info("HSQL Database shutdown gracefuly.");
	            
	        } catch(Exception ex) {
	        	logger.error(ex.getMessage(),ex);
	        } finally {
	            try {
	                if(conn != null)
	                    conn.close();
	            } catch(Exception ex) {
	            	logger.error(ex.getMessage(),ex);
	            }
	        }						
		}
	}
}

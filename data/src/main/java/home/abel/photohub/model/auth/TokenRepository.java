package home.abel.photohub.model.auth;

import java.util.Date;

import home.abel.photohub.model.AuthToken;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Component
public class TokenRepository {
	final Logger logger = LoggerFactory.getLogger(TokenRepository.class);
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private DataSource dataSource;
	
	public TokenRepository() {
		
	}
	
	/**
	 *   Check if Bean initialized correctly
	 */
	@PostConstruct
	public void Init()  throws Exception {
        Assert.notNull(dataSource, "dataSource must be specified");     
		logger.debug("setdataSource called. Create jdbcTemplate");
		this.jdbcTemplate = new JdbcTemplate(dataSource);  
	}
		
	/**
	 * Find  token  in token store 
	 * @param token
	 * @return user name for found token
	 * @throws UsernameNotFoundException 
	 */
	public String getUserByToken(String token)  {
		String username = null;
		
        Assert.notNull(dataSource, "dataSource must be specified");
        
        
        logger.debug("Look for username by token : " + token);
        
		if ( ! StringUtils.isEmpty(token)) {
			try {
				username = jdbcTemplate.queryForObject(
					"select username from auth_token WHERE token = ?", String.class, token);
				
				logger.debug("Got user name: " + username + " for token:" + token );
			} catch (Exception e ) {
				logger.error("Access token repository error : " + e.getLocalizedMessage() );
				return null;
			}
		}	
		return username;
	}	
}

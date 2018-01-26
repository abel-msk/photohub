package home.abel.photohub.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import home.abel.photohub.model.Config;
import home.abel.photohub.model.QConfig;
import home.abel.photohub.model.User;
import home.abel.photohub.model.UserRole;
import home.abel.photohub.service.auth.ExceptionAccessDeny;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * 	The service for access and edit app configuration parameters
 * @author abel
 *
 */
@Service
public class ConfigService {

	/**
	 * This Enum class list all configuration parameters need to run application
	 * 
	 * @author abel
	 *
	 */
	final Logger logger = LoggerFactory.getLogger(ConfigService.class);

	public static final String LOCAL_THUMB_URL = "LOCAL_THUMB_URL";
	public static final String LOCAL_PHOTO_URL = "LOCAL_PHOTO_URL";
	public static final String URL_SELF_PREFIX = "self";
	
	private boolean useDB = true;
	
	@Autowired
	private home.abel.photohub.model.ConfigRepository confRepo;
	
    @Autowired
    Environment env;
    
	@Autowired
	UserService userService;

	@PersistenceContext
	private EntityManager em;

	private Properties confCache;	
	
	
	/**------------------------------------------------------------------------------------------
	 * 
	 * The service for access and edit app configuration parameters
	 * 
	 * After all bean ready,  check for parameters in DB.
	 * if db empty,   try to reload parame from property to db
	 * 
	 * @author abel
	 * @throws Exception 
	 *
	 */
	@PostConstruct
	public void Init() throws Exception {
		//LOCAL_THUMB_URL
		if ( System.getProperty(ConfVarEnum.USE_DB.getName(),"true") == "false") {
			useDB = false;	
			logger.warn("Property photohub.store.conf is FALSE.");
		}
		
		Config conf = confRepo.findOne(QConfig.config.name.eq(ConfVarEnum.LOCAL_THUMB_PATH.getName()));
		
		if ((conf == null) || (conf.getValue() == null)) {
			logger.warn("DB is empty. Load config values from properties, if any.");
		}
		
		//--------
		//   Check most required properties
		
		if (getValue(ConfVarEnum.LOCAL_PHOTO_PATH) == null)  {
			String errMsg = "Init property " + ConfVarEnum.LOCAL_PHOTO_PATH.getName() +" Required";
			logger.error("Cannot start application. " + errMsg);
			//throw new Exception(errMsg);
		}
		
		if (getValue(ConfVarEnum.LOCAL_THUMB_PATH) == null) {
			String errMsg = "Init property " + ConfVarEnum.LOCAL_PHOTO_PATH.getName() +" Required";
			logger.error("Cannot start application. " + errMsg);
			//throw new Exception(errMsg);
		}
		
		String installType = System.getProperty("installationType", "standalone");
		if ((installType != null) && (installType.equalsIgnoreCase("standalone"))) {
			
			//   For standalone installation, prepare default admin user
			//   if it has in the properties list and not stored in db yet
			if (getValue(ConfVarEnum.DEFAULT_USER) != null) {
				User defUserObj = null;
				String username =  getValue(ConfVarEnum.DEFAULT_USER);
				try {
					defUserObj = userService.loadUserByUsername(username);
				} catch (UsernameNotFoundException e) {
			    	logger.debug("No default user in DB. Create one.");
					String pw = getValue(ConfVarEnum.DEFAULT_PASS);
					if (pw == null) pw = "admin";
					defUserObj = userService.addUser(username,pw);
			    	logger.info("Create default admin user: " + defUserObj.getUsername() + ", with password: " + pw);
			    	defUserObj.grantRole(UserRole.ADMIN);
			    	//defUserObj.setExpires(System.currentTimeMillis() + (TokenService.EXPIRES_DAY * 10));
			    	userService.update(defUserObj);
				}
			}	
		}
	}
		

	public ConfigService() {
		confCache = new Properties();
	}
	
	/**------------------------------------------------------------------------------------------
	 * Get list of all configurations variables
	 * @return
	 */
	public Map<ConfVarEnum,String> getPropertiesList() {
		Map<ConfVarEnum,String>  PropMap = new HashMap<ConfVarEnum,String>();
		
		for(ConfVarEnum enumItem: ConfVarEnum.values()) {
			if (enumItem.getAccess() != "none")
				PropMap.put(enumItem, getValue(enumItem));
		}
		
		return PropMap;
	}
	
	
	/**------------------------------------------------------------------------------------------
	 * Get list of all configurations variables
	 * @return
	 */
	public Map<ConfVarEnum,String> getPropEnumsList() {
		Map<ConfVarEnum,String>  PropMap = new HashMap<ConfVarEnum,String>();
		
		for(ConfVarEnum enumItem: ConfVarEnum.values()) {
			if (enumItem.getAccess() != "none")
				PropMap.put(enumItem, getValue(enumItem));
		}
		return PropMap;
	}	
	
	
	//------------------------------------------------------------------------------------------
	/**
	 * Find configuration parameter by its DB variable name, end return its value.
	 * Name can bee one of from ConfVarEnum.
	 * @param strTheName
	 * @return
	 */
	public String getDBValue(String strTheName) {
		return getValue(ConfVarEnum.getByDBName(strTheName));
	}
	/**
	 * Find configuration parameter by its property variable name, end return its value.
	 * Name can bee one of from ConfVarEnum.
	 * @param strTheName
	 * @return
	 */
	public String getPropValue(String strTheName) {
		return getValue(ConfVarEnum.getByName(strTheName));
	}	
	
	/**
	 * Return config value as boolean.
	 * if value has string type "TRUE" or "true", it returns true as boolean. 
	 * For other cases it return false as boolean
	 * 
	 * @param theVarName
	 * @return true for string "true", false for other cases
	 */
	public boolean getBoolValue(ConfVarEnum theVarName) {
		String theVal = getValue(theVarName);
		if ((theVal != null) && (theVal.equalsIgnoreCase("true"))) return true;
		return false;
	}
	
	/** 
	 * Find configuration parameter by its property variable name, end return its value.
	 * Name can bee one of from ConfVarEnum.
	 * @param strTheName
	 * @param defaultValue this value will return if the parameter strTheName will not found
	 * @return the value on parameter strTheName or default value.
	 */
	public String getPropValue(String strTheName,String defaultValue) {
		String propertyValue = null;
		try {
			propertyValue = getValue(ConfVarEnum.getByName(strTheName));
		}
		catch (java.lang.IllegalArgumentException e) {
			logger.warn("Internal error. Get property "+strTheName+". :" + e.getMessage() ,e);
			return env.getProperty(strTheName, defaultValue);
		}
		return propertyValue;
	}	
	
	/**
	 * Return config value by its name. Name can bee one of from ConfVarEnum
	 * @param theEnum
	 * @return
	 */
	public String getValue(ConfVarEnum theEnum) {
		return getValue(theEnum,null);
	}
	
	/**
	 * Return config value by its name. Name can bee one of from ConfVarEnum
	 * @param theEnum
	 * @return
	 */
	public String getValue(ConfVarEnum theEnum, String defaultValue) {
		String confValue;
		if (theEnum == null) return null;
		
		//   Check in cache. otherwise try to load from ...
		if ((confValue = confCache.getProperty(theEnum.getName(),null)) == null) {
			logger.debug("Search value for config parameter " +  theEnum.getName());
			
			Config confItemObj = null;
			if(theEnum.isStoreInDB()) {
				confItemObj = confRepo.findOne(QConfig.config.name.eq(theEnum.getName()));
				confValue = confItemObj != null ? confItemObj.getValue() : null;
				if (confValue != null) logger.debug("Load value from DB "+theEnum.getName()+"="+confValue);	
			}
			//  Does not loaded from db, try load from properties
			if (confValue == null) {
				confValue = env.getProperty(theEnum.getName());
				if (confValue != null) logger.debug("Load value from properytes "+theEnum.getName()+"="+confValue);	
			}
			
			if (confValue != null) {
				//  if vaulue found and not from DB so store it in db
				if ((confItemObj == null) && (theEnum.isStoreInDB())) {
					confItemObj = new Config();
					confItemObj.setName(theEnum.getName());
					confItemObj.setValue(confValue);
					logger.debug("Save config object w name="+theEnum.getName()+", value="+confValue+"  to DB");	
					confRepo.save(confItemObj);
				}
				//   Finaly save value in cache
				logger.debug("Save config param name="+theEnum.getName()+", value="+confValue+"  to cache");	
				confCache.setProperty(theEnum.getName(),confValue);
			}
		}
		return (confValue != null) ? confValue : defaultValue ;
	}
	
	//------------------------------------------------------------------------------------------
	/**
	 * Save configuration parameter in DB, by its DB variable name.
	 * Name can bee one of from ConfVarEnum.
	 * 
	 * @param theDBVarName  can bee one of from ConfVarEnum.getDBVarName().
	 * @param theValue      the value to save.
	 * @throws ExceptionAccessDeny
	 */
	public void setValue(String theDBVarName, String theValue) throws ExceptionAccessDeny {
		setValue(ConfVarEnum.getByDBName(theDBVarName),theValue);
	}
	/**
	 *	Save changed config parameter to cache.
	 *  if system property photohub.store.conf set to true it also save to database.
	 *  
	 * @param theEnum can bee one of from ConfVarEnum
	 * @param theValue
	 * @throws ExceptionAccessDeny If the variable has not write permission.
	 */
	@Transactional
	public void setValue(ConfVarEnum theEnum, String theValue) throws ExceptionAccessDeny {
		if (theEnum == null) return;
		//if (theEnum.getAccess() == "rw") {
			confCache.setProperty(theEnum.getName(),theValue);
	
			if ( useDB ) {
				Config conf = new Config();
				conf.setName(theEnum.getName());

				
				//Config conf = confRepo.findOne(QConfig.config.name.eq(theEnum.getName()));
				//if (conf == null) {
				//	conf = new Config();
				//	conf.setName(theEnum.getName());
				//}
				conf.setValue(theValue);
				logger.trace("Save '"+ theEnum.getName() +"' property '" + theValue  +"', to DB. ");			
				confRepo.save(conf);
			}
		//}
		//else {
		//	throw new ExceptionAccessDeny("Try to change read only config value " +
		//			theEnum.getPropertyName() + "[" + theEnum.getDBVarName() + "]");
		//}
	}	
	/**
	 * Shortcut for INSTALLATION_TYPE property value 
	 * @return true if INSTALLATION_TYPE is server
	 */
	public boolean isInstalledAsServer() {
		return getValue(ConfVarEnum.INSTALLATION_TYPE,"").equalsIgnoreCase("server");
	}
	
	
	public boolean isSelfImageWeb() {
		/*
		//logger.debug("Use self as web server = " + getValue(ConfVarEnum.USE_IMAGE_WEB,""));
		String useImageWeb = getValue(ConfVarEnum.USE_IMAGE_WEB,"");
		logger.debug("useImageWeb return = "+useImageWeb);
		//return getValue(ConfVarEnum.USE_IMAGE_WEB,"").equalsIgnoreCase("true");
		if ( useImageWeb.equalsIgnoreCase("true") ) {
			logger.debug("TRUE is equal ");
		}
		else {
			logger.debug("TRUE is NOT equal ");
		}
		*/
		return getValue(ConfVarEnum.USE_IMAGE_WEB,"").equalsIgnoreCase("true");
	}
	
	
}

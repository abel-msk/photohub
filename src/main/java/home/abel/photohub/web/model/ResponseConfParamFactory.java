package home.abel.photohub.web.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import home.abel.photohub.service.ConfVarEnum;
import home.abel.photohub.service.ConfigService;
import home.abel.photohub.service.auth.ExceptionAccessDeny;
import home.abel.photohub.web.ExceptionObjectNotFound;

/**
 * Convert configuration parameters to/from object for JSON representation and configuration parameter
 *
 * @author abel
 *
 */
@Service
public class ResponseConfParamFactory {
	final Logger logger = LoggerFactory.getLogger(ResponseConfParamFactory.class);

	@Autowired
	private ConfigService configServ;
	
	/**
	 * Create object for config parameter, set values from config variables.
	 * Also other description attributes from {@link ConfVarEnum} ConfVarEnum enum.
	 * The objID is the configuration parameter name as it present in DB table
	 * 
	 * @param objID
	 * @return
	 * @throws ExceptionObjectNotFound
	 */
	public ResponseConfigParamObject getObject(String name) throws ExceptionObjectNotFound {
		ConfVarEnum ParamNameAsEnum  = ConfVarEnum.getByName(name);
		return getObject(ParamNameAsEnum);
	}
	
	/**
	 * Create object for config parameter, set values from config variables.
	 * Set  all attributes for object from ConfVarEnum
	 * 
	 * @param theEnumObj
	 * @return
	 * @throws ExceptionObjectNotFound
	 */
	public ResponseConfigParamObject getObject(ConfVarEnum theEnumObj) throws ExceptionObjectNotFound {
		
		ResponseConfigParamObject respObj = getEmptyObject(theEnumObj);
		respObj.setValue(configServ.getValue(theEnumObj));
		return respObj;	
	}

	
	/**
	 * Create object for configuration parameter http response.
	 * Set  all attributes for object from ConfVarEnum
	 * @param theEnumObj
	 * @return
	 * @throws ExceptionObjectNotFound
	 */
	public ResponseConfigParamObject getEmptyObject(ConfVarEnum theEnumObj) throws ExceptionObjectNotFound {
		
		if (theEnumObj.getAccess().equalsIgnoreCase("none")) {
			throw new ExceptionObjectNotFound("Config parameter " + theEnumObj.getName() + " not found or not allowed for retrieve.");
		}
		ResponseConfigParamObject respObj = new  ResponseConfigParamObject();
		respObj.setType(theEnumObj.getType());
		respObj.setAccess(theEnumObj.getAccess() );
		respObj.setName(theEnumObj.getName());
		respObj.setSort(theEnumObj.getSort());
		return respObj;
	}
	
	
	/**
	 * Get values from object and save as configuration variable.
	 * @param theObject
	 * @throws ExceptionObjectNotFound
	 * @throws ExceptionAccessDeny
	 */
	public void setObject(ResponseConfigParamObject theObject) throws ExceptionObjectNotFound, ExceptionAccessDeny {
		
		setObject(theObject.getName(), theObject.getValue());
	}
	
	/**
	 * Save  and value as  configuration variable objId.
	 * 
	 * @param objId - configuration parameter name as it present in DB table
	 * @param objValue - the value to save
	 * @throws ExceptionAccessDeny
	 * @throws ExceptionObjectNotFound
	 */
	public void setObject(String objId,String objValue) throws ExceptionAccessDeny, ExceptionObjectNotFound {
		ConfVarEnum ParamNameAsEnum  = ConfVarEnum.getByName(objId);
		if (! ParamNameAsEnum.getAccess().equalsIgnoreCase("rw")) {
			throw new ExceptionObjectNotFound("Config parameter " + objId + " is readonly.");
		}
		configServ.setValue(ParamNameAsEnum, objValue);	
	}

}

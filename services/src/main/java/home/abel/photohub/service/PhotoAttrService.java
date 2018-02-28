package home.abel.photohub.service;


import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import home.abel.photohub.model.ModelConstants;
import home.abel.photohub.model.Node;
import home.abel.photohub.model.Photo;
import home.abel.photohub.model.QNode;
import home.abel.photohub.service.auth.ExceptionAccessDeny;
import home.abel.photohub.utils.InternetDateFormat;

@Service
public class PhotoAttrService {
	final Logger logger = LoggerFactory.getLogger(PhotoAttrService.class);
	
	@Autowired
	private ConfigService confService;
	
	@Autowired 
	private ThumbService thumbService;

	@Autowired 
	private SiteService siteService;	
	
	@Autowired 
	private PhotoService photoService;	
	
	@Autowired
	private home.abel.photohub.model.NodeRepository nodeRepo;
	
	@Autowired
	private home.abel.photohub.model.PhotoRepository photoRepo;

	/*========================================================================
	 * 
	 *    GET ATTRIBUTES methods
	 * 
	 ========================================================================*/
	/**
	 * Return objects attribute value 
	 * @param objectId
	 * @param theAttrName
	 * @return
	 * @throws ExceptionInvalidArgument 
	 */
	public String getAttr(String objectId, String theAttrName) throws ExceptionInvalidArgument {
		Node theNode = nodeRepo.findOne(objectId);
		String  strAttr=getAttr(theNode,theAttrName);	
		logger.debug("get attribute "+ theAttrName + ", value=" + strAttr);
		return strAttr;	
	}
	/**
	 * Return objects attribute value
	 * @param theNode
	 * @param theAttrName
	 * @return
	 * @throws ExceptionInvalidArgument 
	 */
	public String getAttr(Node theNode, String theAttrName) throws ExceptionInvalidArgument  {
		PhotoAttrEnum theAttr =  PhotoAttrEnum.fromName(theAttrName);
		return getAttr(theNode,theAttr);
	}

	/**
	 * 	 *  Return  objects attribute  value by its name
	 * @param theNode
	 * @param thePhotoAttrEnum
	 * @return objects attribute  value
	 * @throws ExceptionInvalidArgument
	 */
	public String getAttr(Node theNode, PhotoAttrEnum thePhotoAttrEnum) throws ExceptionInvalidArgument  {
		InternetDateFormat dateFormat = new InternetDateFormat();
		String result = "";
		
		Photo thePhoto = theNode.getPhoto();
		
		//logger.trace("Get attribute for " + thePhotoAttrEnum);
		
		switch (thePhotoAttrEnum) {
			case 	PHOTO_NAME:
				result = thePhoto.getName();
				break;
			case 	PHOTO_URL:	
				result = getPhotoUrl(theNode,false);
				break;
			case 	PHOTO_REALURL:	
				result = getPhotoUrl(theNode,true);
				break;				
			case 	PHOTO_THUMB:
				result = thumbService.getThumbUrl(thePhoto.getId());
				break;
			case 	PHOTO_DESCR:
				result = thePhoto.getDescr();
				break;
			case 	PHOTO_TYPE:
				result = new Integer(thePhoto.getType()).toString();
				break;
			case 	PHOTO_PATH:
				result = getPhotoUrl(theNode,true);
				break;
			case 	SITE_ID:
				if ( thePhoto.getSiteBean() != null ) {
					result = thePhoto.getSiteBean().getId();
				}
				else result = null;
				break;
			case    CREATE_DATE:
			    dateFormat.setFractionalSecondsDigits(2);
			    result="";
			    if (thePhoto.getCreateTime() != null) {
			    	result = dateFormat.format(thePhoto.getCreateTime());	
			    }
				break;
			case    MOD_DATE:
			    dateFormat.setFractionalSecondsDigits(2);
			    result="";
			    if (thePhoto.getUpdateTime() != null) {
			    	result = dateFormat.format(thePhoto.getUpdateTime());	
			    }
				break;
			case    DIGIT_DATE:	
			    dateFormat.setFractionalSecondsDigits(2);
			    result="";
			    if (thePhoto.getDigitTime() != null) {
			    	result = dateFormat.format(thePhoto.getDigitTime());	
			    }
				break;
			
			case GPS_LAT:   
				if (thePhoto.getGpsLat() != 0)  result =  new Double(thePhoto.getGpsLat()).toString();
				break;
			case GPS_LON:     
				if (thePhoto.getGpsLon() != 0) result =  new Double(thePhoto.getGpsLon()).toString();
				break;
			case GPS_DIR:    
				if (thePhoto.getGpsDir() != 0) result =  new Double(thePhoto.getGpsDir()).toString();
				break;
				
			case GPS_ALT:     
				result =  thePhoto.getGpsAlt();
				break;				
			case APERURE: 
				result = thePhoto.getAperture();
				break;
			case EXP_MODE:
				//result = thePhoto.getExpMode();
				break;
			case EXP_TIME:  
				result = thePhoto.getExpTime();
				break;
			case FOCAL: 
				result = thePhoto.getFocalLen();
				break;
			case FOCUS: 
				result = thePhoto.getFocusDist();
				break;
			case ISO_SPEED:  
				result = thePhoto.getIsoSpeed();
				break;
			case CAM_MAKE:  
				result = thePhoto.getCamMake();
				break;
			case CAM_MODEL:   			
				result = thePhoto.getCamModel();
				break;
				
			default:
				throw new ExceptionInvalidArgument("Unknown attribute name: " + thePhotoAttrEnum.toString());
		}

		return result;
	}
	/*========================================================================
	 * 
	 *    LIST ATTRIBUTES methods
	 * 
	 ========================================================================*/	
	//	Node theNode = nodeRepo.findOne(objectId);	
	
	public  Map<PhotoAttrEnum,String>  getAttrList(String NodeId) throws ExceptionInvalidArgument  {
		Node theNode = nodeRepo.findOne(NodeId);	
		return getAttrList(theNode);
	}
	
	/**
	 * Return list of all attributes as  Map <attrName, attrValue>
	 * @param theNode
	 * @return - the Map object where key is a attribute name and value its attribute value
	 * @throws ExceptionInvalidArgument 
	 */
	public  Map<PhotoAttrEnum,String>  getAttrList(Node theNode)  throws ExceptionInvalidArgument  {		
		Map<PhotoAttrEnum,String> theMap = new TreeMap<PhotoAttrEnum,String>();
	
		if (theNode == null) {
			logger.warn("ObjectId parameter, required.");
			//throw new ServiceException("Node object parammeter parameter, required.");
		}
		else {	
			for (PhotoAttrEnum attrEnum : PhotoAttrEnum.values()) {				
					String attrValue = getAttr(theNode,attrEnum);
					logger.trace("Retrieve attribute. attr name=" +  attrEnum.getAttrName() + ", attr value=" + attrValue);				
					theMap.put(attrEnum,  attrValue);
			}
		}
	return 	theMap;
	}
	
	
	/*========================================================================
	 * 
	 *    SET ATTRIBUTES methods
	 * 
	 ========================================================================*/
	/**
	 * Set attribute to new object
	 * 
	 * @param objectId    ode object ID for attribute change
	 * @param theAttrName the attribute json name 
	 * @param value       the value need to set
	 * @return
	 * @throws Exception
	 */
	public String  setAttr(String objectId, String theAttrName, String value) throws Exception {
		Node theNode = nodeRepo.findOne(objectId);
		PhotoAttrEnum theAttr = PhotoAttrEnum.fromName(theAttrName);
		//PhotoAttrEnum.valueOf(name)
		return setAttr(theNode,theAttr,value);
	}
	/**
	 * Set attribute to new object
	 * 
	 * @param objectId  node object ID for attribute change 
	 * @param theAttr   the PhotoAttrEnum object
	 * @param value     the value need to set
	 * @return          new save value
	 * @throws Exception
	 */
	public String  setAttr(String objectId, PhotoAttrEnum theAttr, String value) throws Exception {
		Node theNode = nodeRepo.findOne(objectId);
		//PhotoAttrEnum.valueOf(name)
		return setAttr(theNode,theAttr,value);
	}	
	
	/**
	 * Set attribute to new object
	 * 
	 * @param theNode      node object for attribute change
	 * @param theAttr  the json attribute name
	 * @param value        the value need to set
	 * @return             new save value
	 * @throws Exception
	 */
	public String setAttr(Node theNode,PhotoAttrEnum theAttr, String value) throws Exception {
		boolean needSave = true;
		Photo thePhoto = theNode.getPhoto();
		InternetDateFormat dateFormat = new InternetDateFormat();
				
		switch (theAttr) {
			case 	PHOTO_NAME:
				thePhoto.setName(value);
				break;
			case 	PHOTO_DESCR:
				thePhoto.setDescr(value);
				break;
			case    PHOTO_TYPE:
				Integer theNewType = new java.lang.Integer(value);
				
				//  Folder cannot be converted from/to other types
				if ((thePhoto.getType() == ModelConstants.OBJ_FOLDER) ||
						(theNewType == ModelConstants.OBJ_SERIES)){
					throw new ExceptionPhotoProcess("Folder canot be converted from/to other types.");
				}
				
				//  For convert from collection to single photo check if collection is empty				
				if ((theNewType	==  ModelConstants.OBJ_SINGLE )  &&
						(thePhoto.getType()  == ModelConstants.OBJ_SERIES)){
					if ( nodeRepo.count(QNode.node.parent.eq(theNode.getId())) > 0 )  {
						throw new ExceptionPhotoProcess("Cannot convert to single photo not empty collection.");
					}
				}
				thePhoto.setType(theNewType);
				break;
			case    PHOTO_THUMB:
				thumbService.setThumb(value, thePhoto);
				needSave = false;
				break;
			case   MOD_DATE:
				thePhoto.setUpdateTime(dateFormat.parse(value));
				break;				
			case   CREATE_DATE:
				thePhoto.setCreateTime(dateFormat.parse(value));
				break;
			case GPS_LAT:   
				if (value != null) thePhoto.setGpsLat(new Double(value));
				break;
			case GPS_LON:     
				if (value != null) thePhoto.setGpsLon(new Double(value));
				break;	
				
			default: 
				throw new ExceptionAccessDeny("Try to change 'read-only' attribute.");
		}

		if (needSave) {
			photoRepo.save(thePhoto);
		}
		
		//   Finally return  return new save value
		try {
			return getAttr(theNode,theAttr);
		} catch (Exception e) {
			//TODO: Create empty attr object
			logger.error("INTERNAL ERROR (PhotoAttrService/setAttr) get unknown attr name after successful save.");
		}
		return null;
	}

	/*========================================================================
	 * 
	 *    Process some not obvious Attr construction for return
	 * 
	 ========================================================================*/	
	/**
	 *   Return url for original photo object.
	 *   In case when config parameter isSelfImageWeb = true or the object has no public(real) url
	 *   return link to self to image controller.  Usually image/{nodeId}.
	 *   When request for folder object, return null  so far it has no image source.
	 *   In other case return public url to image source.
	 * 
	 * @param theNode
	 * @return
	 */
	private String getPhotoUrl(Node theNode, boolean requiredRealPath) {
		String result = null;

		if ( theNode.getPhoto().getType() != ModelConstants.OBJ_FOLDER ) {
			if ((confService.isSelfImageWeb()) || (theNode.getPhoto().getRealUrl() == null)) {
				result = confService.getValue(ConfVarEnum.LOCAL_PHOTO_URL);
				result = result.endsWith("/") ? result : result + "/";
				result = result + theNode.getId();
			}
			else {
				result = theNode.getPhoto().getRealUrl().toString();
			}
		} 
		else {
			logger.debug("Request for photo url, return=NULL as far this is folder object");
		}
		return result;
	}

	
}

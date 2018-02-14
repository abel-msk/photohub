package home.abel.photohub.web.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import home.abel.photohub.model.ModelConstants;
import home.abel.photohub.model.Node;
import home.abel.photohub.model.NodeRepository;
import home.abel.photohub.service.ConfVarEnum;
import home.abel.photohub.service.ConfigService;
import home.abel.photohub.service.PhotoAttrEnum;
import home.abel.photohub.service.PhotoAttrService;
import home.abel.photohub.service.SiteService;
import home.abel.photohub.service.ThumbService;
import home.abel.photohub.web.ExceptionObjectNotFound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ResponsePhotoObjectFactory {
	final Logger logger = LoggerFactory.getLogger(ResponsePhotoObjectFactory.class);

	@Autowired
	private	Environment env;
	
	@Autowired 
	private ConfigService configSvc;
	
	@Autowired 
	private ThumbService thumbService;
	
	@Autowired 
	private SiteService siteService;	
	
	@Autowired
	private PhotoAttrService attrService;
	
	@Autowired
	private NodeRepository nodeRepo;
	
	
	public  ResponsePhotoObjectFactory() {
		
	}
	/**
	 * Return short attributes list as attributes of ResponsePhotoObject object
	 * @param curNode
	 * @return
	 */
	public ResponsePhotoObject getPhotoObject(Node curNode) {

		ResponsePhotoObject rpo = new ResponsePhotoObject();
		if (curNode == null) return null;
		
		rpo.setId(curNode.getId());
		rpo.setParentId((curNode.getParent()));
		try {
			rpo.setType(prepareAttrValue(PhotoAttrEnum.PHOTO_TYPE,
					attrService.getAttr(curNode,PhotoAttrEnum.PHOTO_TYPE)));
			rpo.setName(prepareAttrValue(PhotoAttrEnum.PHOTO_NAME,
					attrService.getAttr(curNode,PhotoAttrEnum.PHOTO_NAME)));
			rpo.setDescr(prepareAttrValue(PhotoAttrEnum.PHOTO_DESCR,
					attrService.getAttr(curNode,PhotoAttrEnum.PHOTO_DESCR)));
			rpo.setSiteId(prepareAttrValue(PhotoAttrEnum.SITE_ID,
					attrService.getAttr(curNode,PhotoAttrEnum.SITE_ID)));			
			rpo.setCreateDate(prepareAttrValue(PhotoAttrEnum.CREATE_DATE,
					attrService.getAttr(curNode,PhotoAttrEnum.CREATE_DATE)));
			rpo.setModDate(prepareAttrValue(PhotoAttrEnum.MOD_DATE,
					attrService.getAttr(curNode,PhotoAttrEnum.MOD_DATE)));
			rpo.setThumbUrl(prepareAttrValue(PhotoAttrEnum.PHOTO_THUMB,
					attrService.getAttr(curNode,PhotoAttrEnum.PHOTO_THUMB)));
			
			//  Create Photo url
			if (curNode.getPhoto().getType() != ModelConstants.OBJ_FOLDER) {
				rpo.setPhotoUrl(prepareAttrValue(PhotoAttrEnum.PHOTO_URL,
						attrService.getAttr(curNode,PhotoAttrEnum.PHOTO_URL)));	
			}
			
		}
		catch (Exception e) {
			logger.error("Internal service error : " + e.getMessage());
		}
		return rpo;	
	}
	
	
	
	
	/**
	 * Create ResponsePhotoAttr object fulled up with property and values
	 * @param objectId
	 * @param theAttrName - get properties from
	 * @return
	 * @throws Exception 
	 */
	public ResponsePhotoAttr getAttrObj(String objectId, String theAttrName) throws Exception {
		String theAttrValue = null;
		return new ResponsePhotoAttr(theAttrName,theAttrValue);
	}
	
	/**
	 * Return full list of attributes for photo object located at node by its id
	 * Return attributes as list of ResponsePhotoAttr objects
	 * @param theNodeId  the node ID
	 * @return
	 * @throws Exception
	 */
	public List<ResponsePhotoAttr> getAttrsList(String theNodeId) throws Exception{
		Node theNode = nodeRepo.findOne(theNodeId);
		if ( theNode == null) throw new ExceptionObjectNotFound("Node id="+ theNodeId + " not found.");
		return getAttrsList(theNode);
	}
	/**
	 * Return full list of attributes for photo object located at node 
	 * Return attributes as list of ResponsePhotoAttr objects
	 * @param theNode
	 * @return
	 */
	public List<ResponsePhotoAttr> getAttrsList(Node theNode) throws Exception{
		ArrayList<ResponsePhotoAttr> theList = new ArrayList<ResponsePhotoAttr>();
		
		Map<PhotoAttrEnum, String> theAttrsMap = attrService.getAttrList(theNode);			
		for (PhotoAttrEnum itemKey: theAttrsMap.keySet()) {
			ResponsePhotoAttr AttrObj = new ResponsePhotoAttr(itemKey,
					prepareAttrValue(itemKey,theAttrsMap.get(itemKey)));				
			theList.add(AttrObj);
		}
		return  theList;
	}	
	
	
	public ResponsePhotoAttr getAttr(String theNodeId, String theAttrName) throws Exception {
		return new ResponsePhotoAttr(PhotoAttrEnum.valueOf(theAttrName),attrService.getAttr(theNodeId, theAttrName));			
	}	
	
	
	public ResponsePhotoAttr getAttr(Node theNode, String theAttrName) throws Exception {
		return new ResponsePhotoAttr(PhotoAttrEnum.valueOf(theAttrName),attrService.getAttr(theNode, theAttrName));			
	}
	
	public ResponsePhotoAttr getAttr(Node theNode, PhotoAttrEnum theAttr) throws Exception {
		return new ResponsePhotoAttr(theAttr,attrService.getAttr(theNode, theAttr));			
	}	
	
	/**
	 * Make necessarily substitution we need return for web request.
	 *  
	 * @param theValue
	 * @return
	 */
	private String prepareAttrValue(PhotoAttrEnum attrEnum, String theValue) {
		logger.trace("Prepare  attribute="+attrEnum.getAttrName()+", value="+theValue);		
		return theValue;
	}
	
}

package home.abel.photohub.web;

import home.abel.photohub.model.ModelConstants;
import home.abel.photohub.model.Node;
import home.abel.photohub.model.Photo;
import home.abel.photohub.model.QPhoto;
import home.abel.photohub.model.Site;
import home.abel.photohub.service.ExceptionInvalidArgument;
import home.abel.photohub.service.ExceptionPhotoProcess;
import home.abel.photohub.service.PhotoAttrService;
import home.abel.photohub.service.PhotoListFilter;
import home.abel.photohub.service.PhotoService;
import home.abel.photohub.service.SiteService;
import home.abel.photohub.service.ThumbService;
import home.abel.photohub.utils.FileUtils;
import home.abel.photohub.utils.InternetDateFormat;
import home.abel.photohub.web.model.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.QPageRequest;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


@CrossOrigin
@RestController
@RequestMapping("/api")
//@Controller
public class PhotoController {
		
	final Logger logger = LoggerFactory.getLogger(PhotoController.class);
	@Autowired 
	PhotoService photoService; 
	@Autowired 
	SiteService siteService; 

	@Autowired 
	ThumbService thumbService; 

	@Autowired
	PhotoAttrService photoAttrService;
	
	@Autowired 
	ResponsePhotoObjectFactory photoObjFactory;
	
	@Autowired
	HeaderBuilderService headerBuild;
			

	
	/*=============================================================================================
	 * 
	 *    LIST:  Request for paged list of photo objects
	 *      
	 =============================================================================================*/
	@RequestMapping(value = "/list", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptGetPhotos(HttpServletRequest request) throws IOException, ServletException {
    	logger.debug("Request OPTION  for /object");
	    return new ResponseEntity<String>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}
	@RequestMapping(value = "/list", method = RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE) 
	ResponseEntity<DefaultPageResponse<List<Photo>>> getPhotos(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
			@RequestParam(value ="minDate", required = false) String minDateStr,
			@RequestParam(value ="maxDate", required = false) String maxDateStr,
			@RequestParam(value ="sitesList", required = false) String siteIdList,
			@RequestParam(value ="limit", required = false, defaultValue = "70") int limit,
			@RequestParam(value ="offset", required = false, defaultValue = "0") long offset
			) {
		
		logger.debug(">>> Request GET for /list, options=["+
				" minDate=" + (minDateStr==null?"null":minDateStr) +
				" maxDate=" + (maxDateStr==null?"null":maxDateStr) +
				" siteIdList=" + (siteIdList==null?"null":siteIdList) +
				" limit="+limit+
				" offset=" + offset+
				" ]");
						
		PhotoListFilter  filter = new PhotoListFilter();
		List<String> sitesList = new ArrayList<String>();

		filter.setMinDate(minDateStr);
		filter.setMaxDate(maxDateStr);
		
		if ((siteIdList != null) && (siteIdList.length() > 0)) {
			for (String siteId : siteIdList.split(",")) {
				sitesList.add(siteId);
			}
			filter.setSites(sitesList);	
		}
		
		DefaultPageResponse<List<Photo>> resp = new DefaultPageResponse<List<Photo>>("OK",0,
				photoService.listPhotos(filter,offset, limit));
		resp.setLimit(resp.getObject().size());
		resp.setOffset(offset);

		return new ResponseEntity<DefaultPageResponse<List<Photo>>>(resp,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	}
	
	
	
	/*=============================================================================================
	 * 
	 *    PHOTO:  Request photo object
	 *      
	 =============================================================================================*/	
	@RequestMapping(value = "/photo/{id}", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptGetPhoto(HttpServletRequest request) throws IOException, ServletException {
    	logger.debug("Request OPTION  for /photo/id");
	    return new ResponseEntity<String>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}
	
	@RequestMapping(value = "/photo/{id}", method = RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE) 
	ResponseEntity<DefaultObjectResponse<Photo>> getPhoto(
	        final HttpServletRequest request,
	        final HttpServletResponse response,
	        @PathVariable("id") String objectId ) throws IOException, ServletException {
    	logger.debug("Request GET for /photo/"+ objectId);
    	
    	DefaultObjectResponse<Photo> resp = new DefaultObjectResponse<Photo>("OK",0,photoService.getPhotoById(objectId)); 
    	
	    return new ResponseEntity<DefaultObjectResponse<Photo>>(resp,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}
	
	
	/*=============================================================================================
	 *
	 *  New Upload.
	 *  
	 *  Handle request for  ADD PHOTO object to SITE'S ROOT  ( upload )
	 * 
	 =============================================================================================*/
	@RequestMapping(value = "/site/{id}/upload", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptSiteUpload(HttpServletRequest request) throws IOException, ServletException {
    	logger.debug("Request OPTION  for /site/{id}/upload");
	    return new ResponseEntity<String>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}
	
	
	@RequestMapping(value = "/site/{id}/upload", method = RequestMethod.POST, 
			headers = "content-type=multipart/form-data",
			produces="application/json")
	@ResponseBody
	public ResponseEntity<List<ResponseFileUpload>> uploadToSite(
	        //final HttpServletRequest HTTPrequest,
	        MultipartHttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
			@PathVariable("id") String siteIdStr) throws Exception { 
				
		logger.debug(">>> Request /site/"+siteIdStr+"/upload, content-type=multipart/form-data");	
		Node theNewNode = null;
		//Site theSite = siteService.getSite(siteIdStr);		
		List<ResponseFileUpload> respList =  new ArrayList<ResponseFileUpload>();
		

        Iterator<String> itr = HTTPrequest.getFileNames();
        while (itr.hasNext()) {
            String uploadedFile = itr.next();
    		logger.debug("Select file for upload.");	

            MultipartFile file = HTTPrequest.getFile(uploadedFile);

			File tmpFile = genTempFile(file.getOriginalFilename());
            file.transferTo(tmpFile);
            
    		try {
    			//InputStream is = file.getInputStream();
    			logger.debug("Select filename = " + file.getOriginalFilename() + ", siteIdStr="+siteIdStr);
    			theNewNode = photoService.addPhoto(tmpFile, file.getOriginalFilename(), "", null, siteIdStr);
    			
    		} catch (Exception e) {
				throw new ExceptionInvalidRequest("Cannot add image file : " +
						file.getOriginalFilename() + ", error : " +
						e.getLocalizedMessage(),e);
    		}
    		finally {
    			tmpFile.delete();
    		}
    		respList.add(new ResponseFileUpload("File processed successfully",
    				file.getOriginalFilename(),
    				theNewNode==null?"0":theNewNode.getId(),
    				0));
        }
		
		return new ResponseEntity<List<ResponseFileUpload>>(
				respList,
				headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
		
	}


	
	protected File genTempFile(String originalFileName) throws IOException{
		String newTempFileName = null;
		String newTempFileExt = null;
		final int index = originalFileName.lastIndexOf('.');
		if (index >= 0) {
			newTempFileName  = originalFileName.substring(1,index-1);
			newTempFileExt = originalFileName.substring(index);
		}
		else {
			newTempFileName = originalFileName;
			//  ПРоверить расширение по контент типу file.contentType
			newTempFileExt = "";
		}

		File tmpFile =  File.createTempFile(newTempFileName,newTempFileExt);
		return tmpFile;
	}


	
	/*=============================================================================================
	 *
	 *    DEPRICATED Handle request for  ADD PHOTO object to FOLDER  ( upload )
	 * 
	 *    OLD WAY TO UPLOAD
	 *    
	 =============================================================================================*/
//	/**
//	 * Upload photo place it to coresponding folder on local site.
//	 * Create thumbnail, create  Node  and photoObject in db.
//	 * Return ObjectInfo   for newly created object
//	 *
//	 * Also replay to option request.
//	 *
//	 * @throws Exception
//	 */
//	@RequestMapping(value = "/object/{id}/upload", method = RequestMethod.OPTIONS)
//	ResponseEntity<String> acceptUpload(HttpServletRequest request) throws IOException, ServletException {
//    	logger.debug("Request OPTION  for /object/{id}/upload");
//	    return new ResponseEntity<String>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
//	}
//
//	/**
//	 * Upload image file and place it as Object in 'parentId' folder
//	 * @param inputParentId
//	 * @param theName
//	 * @param theDescr
//	 * @param theSiteId
//	 * @param multiPart
//	 * @return
//	 * @throws Exception
//	 */
//	@RequestMapping(value = "/object/{id:[\\d]+}/upload", method = RequestMethod.POST,
//				headers = "content-type=multipart/form-data",
//				produces="application/json")
//	@ResponseBody
//	public ResponseEntity<ResponseFileUpload> uploadMultipart(
//	        final HttpServletRequest HTTPrequest,
//	        final HttpServletResponse HTTPresponse,
//			@PathVariable("id") String inputParentId,
//			@RequestParam(value = "name", required = false)  String theName,
//			@RequestParam(value = "descr", required = false)  String theDescr,
//			@RequestParam(value = "siteId", required = false)  String theSiteId,
//	        @RequestParam("file") final MultipartFile multiPart) throws Exception {
//
//		logger.debug(">>> Request /upload, content-type=multipart/form-data, form-data=[parentId = " +
//				inputParentId + "siteId =" + theSiteId + ", file = " + multiPart.getOriginalFilename() + "]");
//		Node theNewNode = null;
//		Node theParentNode = photoService.getNodeById(inputParentId);
//
//		if ((theParentNode == null) && (theSiteId == null))
//			throw new ExceptionInvalidRequest("Requests param error.  Parent object id and  site is cannot be null at the same time");
//
//		if (theSiteId == null) theSiteId = theParentNode.getPhoto().getSiteBean().getId();
//		String imageFileOrigName  = FilenameUtils.getName(multiPart.getOriginalFilename());
//		if ( theName == null ) theName = imageFileOrigName;
//
//		File tmpFile = genTempFile(multiPart.getOriginalFilename());
//		multiPart.transferTo(tmpFile);
//		try {
//			theNewNode = photoService.addPhoto(tmpFile, theName, theDescr, inputParentId, theSiteId);
//		} catch (Exception e) {
//			throw new ExceptionInvalidRequest("Cannot add image file : " + multiPart.getOriginalFilename() + ", error : " + e.getLocalizedMessage(),e);
//		}
//		finally {
//			if ( tmpFile != null ) {
//				tmpFile.delete();
//			}
//		}
//
//		ResponseFileUpload response = new ResponseFileUpload("File processed successfully",multiPart.getOriginalFilename(),theNewNode.getId(),0);
//		response.setObject(photoObjFactory.getPhotoObject(theNewNode));
//		logger.debug("<<<  Upload OK");
//	    HttpHeaders headers = headerBuild.getHttpHeader(HTTPrequest);
//	    return new ResponseEntity<ResponseFileUpload>(response,headers, HttpStatus.OK);
//	}
	
//	/**
//	 * Create new node object as child of inputParentId node (as group or object)
//	 * @param inputParentId parent node id
//	 * @param multiPart
//	 * @return
//	 * @throws Exception
//	 */
//	@RequestMapping(value = "/object/{id:[\\d]+}", method = RequestMethod.POST, produces="application/json")
//	public ResponseEntity<ResponseFileUpload> setObject(
//	        final HttpServletRequest HTTPrequest,
//	        final HttpServletResponse HTTPresponse,
//			@PathVariable("id") final String inputParentId,
//			@RequestParam(value = "name", required = false)  String theName,
//			@RequestParam(value = "descr", required = false) final String theDescr,
//			@RequestParam(value = "site", required = false)  String theSiteId,
//	        @RequestParam("file") final MultipartFile multiPart) throws Exception {
//
//		logger.debug(">>> Request /object/"+inputParentId+", content-type=multipart/form-data, form-data=[parentId = " + inputParentId +
//				", name = " + theName +
//				", descr = " + theDescr +
//				", file = " + multiPart.getOriginalFilename() + "]");
//
//		Node theNewNode = null;
//		Node theParentNode = photoService.getNodeById(inputParentId);
//
//		if ((theParentNode == null) && (theSiteId == null))
//			throw new ExceptionInvalidRequest("Requests param error.  Parent object id and  site is cannot be null at the same time");
//
//		if (theSiteId == null) theSiteId = theParentNode.getPhoto().getSiteBean().getId();
//		String imageFileOrigName  = FilenameUtils.getName(multiPart.getOriginalFilename());
//		if (theName == null) theName = imageFileOrigName;
//
//		File tmpFile = genTempFile(multiPart.getOriginalFilename());
//		multiPart.transferTo(tmpFile);
//
//		try {
//			theNewNode = photoService.addPhoto(tmpFile, theName, theDescr, inputParentId, theSiteId);
//		} catch (Exception e) {
//			throw new ExceptionInvalidRequest("Cannot add image file : " + multiPart.getOriginalFilename() + ", error : " + e.getLocalizedMessage(),e);
//		}
//		finally {
//			if ( tmpFile != null ) {
//				tmpFile.delete();
//			}
//		}
//
//		ResponseFileUpload response = new ResponseFileUpload("File processed successfully",multiPart.getOriginalFilename(),theNewNode.getId(),0);
//		response.setObject(photoObjFactory.getPhotoObject(theNewNode));
//		logger.debug("<<<  Upload OK");
//	    HttpHeaders headers = headerBuild.getHttpHeader(HTTPrequest);
//	    return new ResponseEntity<ResponseFileUpload>(response,headers, HttpStatus.OK);
//	}
	
	/*=============================================================================================
	 *
	 *    Handle request for  CREATE folder
	 * 
	 =============================================================================================*/	
//	@RequestMapping(value = "/object/{parentId}/create", method = RequestMethod.OPTIONS)
//	ResponseEntity<String> acceptCreateFolder(HttpServletRequest request) throws IOException, ServletException {
//    	logger.debug("Request OPTION  for /object/{parentId}/create");
//	    return new ResponseEntity<String>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
//	}
//
//	@RequestMapping(value = "/object/{parentId}/create", method = RequestMethod.POST,
//			produces="application/json")
//	@ResponseBody
//	public ResponseEntity<DefaultObjectResponse<ResponsePhotoObject>> createFolder(
//	        final HttpServletRequest HTTPrequest,
//	        final HttpServletResponse HTTPresponse,
//	        @PathVariable("parentId") String parentId,
//	        @RequestParam("name") String objName,
//			@RequestParam(value = "site", required = false)  String theSiteId,
//			@RequestParam(value ="descr", required = false, defaultValue = "") String objDescription) throws Exception {
//
//		logger.debug(">>> Request /object/"+parentId+"/create,  options=[" +
//				"parentId = " + parentId +
//				", name = " + objName +
//				", descr = " + objDescription +
//				"]");
//
//		Node theParentNode = photoService.getNodeById(parentId);
//
//		if ((theParentNode == null) && (theSiteId == null))
//			throw new ExceptionInvalidRequest("Requests param error.  Parent object id and  site is cannot be null at the same time");
//
//		if (theSiteId == null) theSiteId = theParentNode.getPhoto().getSiteBean().getId();
//
//		Node theNode = photoService.addFolder(objName, objDescription, parentId, theSiteId);
//		DefaultObjectResponse<ResponsePhotoObject> response = new DefaultObjectResponse<ResponsePhotoObject>("Folder created successfully",0);
//		if (theNode != null) {
//			response.setObject(photoObjFactory.getPhotoObject(theNode));
//		}
//		logger.debug("<<<  Created OK");
//		return new ResponseEntity<DefaultObjectResponse<ResponsePhotoObject>>(response,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
//	}
		
	/*=============================================================================================
	 *
	 *  Handle request for  DELETE photo object  
	 * 
	 =============================================================================================*/

	@RequestMapping(value = "/objects", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptDeletBatch(HttpServletRequest request) throws IOException, ServletException {
		return new ResponseEntity<String>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}


	/**
	 * 		Multiple objects delete
	 * @param HTTPrequest
	 * @param HTTPresponse
	 * @param forseDelete Recursively delete if object if object is folder
	 * @param objList  list photo ids need to delete
	 * @return  List ob processed objects with their id and processing result status
	 * @throws Throwable
	 */
	@RequestMapping(value = "/objects", method = RequestMethod.DELETE, produces="application/json")
	ResponseEntity<DefaultObjectResponse<List<BatchResult>>> deleteObjectsBatch(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
			@RequestParam(value ="recursive", required = false, defaultValue = "false") boolean forseDelete,
			@RequestBody List<String> objList
			) throws Throwable
	{
		List<BatchResult> processObj = new ArrayList<>();

		for (String objId: objList) {
			BatchResult result = new BatchResult();
			result.setId(objId);
			processObj.add(result);

			//   Get all nodes for this photo and delete them all.
			Photo thePhoto = photoService.getPhotoById(objId);
			for (Node objNode  : thePhoto.getNodes() ) {
				try {
					photoService.deleteObject(objNode, forseDelete, true);
					result.setStatus(BatchResult.STATUS_OK);
				} catch (Exception e) {
					result.setStatus(1);
					result.setMessage("Object delete error: " + e.getMessage());
					logger.warn("[batchDeleteObjects] Cannot delete object id=" + thePhoto, e);
				}
			}
		}

		DefaultObjectResponse<List<BatchResult>> response = new DefaultObjectResponse<>(
				"Process completed.", 0, processObj
		);

		return new ResponseEntity<>(response,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	}


	@RequestMapping(value = "/object/{id}", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptDelet(HttpServletRequest request) throws IOException, ServletException {
		return new ResponseEntity<String>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}

	/**
	 * Delete object by ID
	 * @param objectId  object node id for requested object
	 * @param forseDelete
	 * @param withFile
	 * @return
	 * @throws Throwable
	 */
	@RequestMapping(value = "/object/{id}", method = RequestMethod.DELETE, produces="application/json")
	ResponseEntity<DefaultResponse> deleteObject(
			final HttpServletRequest HTTPrequest,
			final HttpServletResponse HTTPresponse,
			@PathVariable("id") String objectId,
			@RequestParam(value ="recursive", required = false, defaultValue = "false") boolean forseDelete,
			@RequestParam(value ="withFile", required = false, defaultValue = "false") boolean withFile
	) throws Throwable
	{
		photoService.deleteObject(objectId,forseDelete);
		DefaultResponse response = new DefaultResponse("Object deleted",0);
		return new ResponseEntity<DefaultResponse>(response,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
	}



	/*=============================================================================================
	 *
	 *     Handle request for  UPLOAD new thumb
	 * 
	 =============================================================================================*/
//	@RequestMapping(value = "/object/{id:[\\d]+}/thumb", method = RequestMethod.POST,
//			headers = "content-type=multipart/form-data",
//			produces="application/json")
//	@ResponseBody
//	public ResponseEntity<ResponseFileUpload> uploadThumbnail(
//	        final HttpServletRequest HTTPrequest,
//	        final HttpServletResponse HTTPresponse,
//			@PathVariable("id") String objectId,
//	        @RequestParam("file") final MultipartFile multiPart) throws Exception {
//		ResponseFileUpload response = null;
//
//		logger.debug(">>> Request UPLOAD  for /object/"+objectId+"/thumb, content-type=multipart/form-data, file = " + multiPart.getOriginalFilename() + "]");
//		if ( ! multiPart.isEmpty()) {
//			//  Check are object exist
//			Node theNode = photoService.getNodeById(objectId);
//			File tempImageFile = File.createTempFile(
//					FilenameUtils.removeExtension(multiPart.getOriginalFilename()) +  Long.toString(System.nanoTime()),
//					"." + FilenameUtils.getExtension(multiPart.getOriginalFilename()));
//			try {
//				// Save thumb object to temp file
//				multiPart.transferTo(tempImageFile);
//				logger.debug("Done file upload to : " + tempImageFile.getAbsolutePath());
//				thumbService.setThumb(tempImageFile.getAbsolutePath(), theNode.getPhoto());
//			} catch (Exception e) {
//				logger.error("Cannot set new thumbnail from image file : " + tempImageFile + ",  error : " + e.getLocalizedMessage());
//				throw new Exception(e);
//			}
//			finally {
//				FileUtils.fileDelete(tempImageFile,true);
//			}
//			response = new ResponseFileUpload("File processed successfully",multiPart.getOriginalFilename(),0);
//			response.setObject(photoObjFactory.getPhotoObject(theNode));
//		} else {
//			response = new ResponseFileUpload("Nothing to process",multiPart.getOriginalFilename(),0);
//		}
//		return new ResponseEntity<ResponseFileUpload>(response,headerBuild.getHttpHeader(HTTPrequest), HttpStatus.OK);
//	}
	
	
	/*=============================================================================================
	*
	*   Handle request Object Attributes 
	* 
	=============================================================================================*/
	
	@RequestMapping(value = "/object/{objectId:[\\d]+}/attr/{name}", method = RequestMethod.OPTIONS)
	ResponseEntity<String> acceptObjectAttributes(HttpServletRequest request) throws IOException, ServletException {
    	logger.debug("Request OPTION  for /object/{id}/attr/**");
	    return new ResponseEntity<String>(null,headerBuild.getHttpHeader(request), HttpStatus.OK);
	}	
	/**
	 * 
	 * @param objectId
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/object/{objectId:\\d+}/attr", method = RequestMethod.GET, 
			produces="application/json")
	@ResponseBody
	public ResponseEntity<List<ResponsePhotoAttr>> getObjectAttrList(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
	        @PathVariable("objectId") String objectId) throws Exception {
		logger.debug(">>> Request GET for /object/" + objectId + "/attr");	
		
		List<ResponsePhotoAttr> theAttrList = photoObjFactory.getAttrsList(objectId);
		
		String attrlistStr = "";
		for(ResponsePhotoAttr theAttrObj : theAttrList) {
			attrlistStr = attrlistStr + theAttrObj.getName() + "=" + theAttrObj.getValue() +"; ";
		}
		logger.debug("<<< Response: " + attrlistStr);
		return new ResponseEntity<List<ResponsePhotoAttr>>(theAttrList,headerBuild.getHttpHeader(HTTPrequest),HttpStatus.OK); 
	}
	/**
	 * Get photo object attribute  by Name
	 * 
	 * @param objectId
	 * @param theAttrName
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/object/{objectId:\\d+}/attr/{name}", method = RequestMethod.GET, 
			produces="application/json")
	@ResponseBody
	public ResponseEntity<ResponsePhotoAttr> getObjectAttr(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
	        @PathVariable("objectId") String objectId,
	        @PathVariable("name") String theAttrName) throws Exception {
		logger.debug(">>> Request GET for /object/" + objectId + "/attr/" + theAttrName);	
		
		ResponsePhotoAttr theAttrObj = photoObjFactory.getAttrObj(objectId,theAttrName);
		
		logger.debug("<<< Response: " + theAttrName + "=" + theAttrObj.getValue());		
		return new ResponseEntity<ResponsePhotoAttr>(theAttrObj,headerBuild.getHttpHeader(HTTPrequest),HttpStatus.OK); 
	}	
	
	/**
	 *    Set object attribute value by sending full attribute object
	 *    {
	 *    		"name":"PHOTO_NAME",
	 *    		"value":"villa 15-v2.5.3.hgjh",
	 *    		"type":"line","access":"rw",
	 *    		"namespace":"photo",
	 *    		"displayName":"Name",
	 *    		"priority":100
	 *    } 
	 *    Used variable 'value'.
	 *    
	 * @param objectId   - the object id
	 * @param theAttrName - Attribute name
	 * @param theArrtObj -  Attribute object.
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/object/{objectId}/attr/{name}", method = RequestMethod.PUT, 
			produces="application/json")
	@ResponseBody
	public ResponseEntity<ResponsePhotoAttr> setObjectAttr(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
	        @PathVariable("objectId") String objectId,
	        @PathVariable("name") String theAttrName,
	        @RequestBody ResponsePhotoAttr theArrtObj) throws Exception {
		logger.debug(">>> Request PUT for /object/" + objectId + "/attr/" + theAttrName);	
		logger.debug( "Save attribute " + theArrtObj.getName() +" value : " + theArrtObj.getValue());
		photoAttrService.setAttr(objectId,theArrtObj.getName(),theArrtObj.getValue());
		ResponsePhotoAttr response =  photoObjFactory.getAttr(objectId,theAttrName);
		logger.debug(">>> Response: " + response.getName() + ",  value=" + response.getValue());	
		return new ResponseEntity<ResponsePhotoAttr>(response,headerBuild.getHttpHeader(HTTPrequest),HttpStatus.OK); 
	}	
	
}

package home.abel.photohub.web;

import java.io.File;
import java.util.List;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import home.abel.photohub.connector.prototype.PhotoObjectInt;
import home.abel.photohub.connector.prototype.SiteConnectorInt;
import home.abel.photohub.model.Media;
import home.abel.photohub.model.ModelConstants;
import home.abel.photohub.model.Node;
import home.abel.photohub.model.Photo;
import home.abel.photohub.service.ConfVarEnum;
import home.abel.photohub.service.ConfigService;
import home.abel.photohub.service.PhotoAttrEnum;
import home.abel.photohub.service.PhotoAttrService;
import home.abel.photohub.service.PhotoService;
import home.abel.photohub.service.SiteService;
import home.abel.photohub.service.ThumbService;
import home.abel.photohub.web.model.DefaultObjectResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 *  Server access request for photo image and thumbnails
 *  For this service working, it should be configured with USE_IMAGE_WEB variable set  true
 *  In this way all local requests from 127.0.0.1 or the host name redirects to real file on installed computer.
 *  For other request we return the image as http reply.
 * @author abel
 *
 */
@CrossOrigin
@Controller
@RequestMapping("/api")
public class ImageController {
	final Logger logger = LoggerFactory.getLogger(ImageController.class);

	
	@Autowired 
	SiteService siteService; 
	
	@Autowired 
	ConfigService confService; 

	@Autowired 
	ThumbService thumbService; 
	
	@Autowired 
	PhotoAttrService attrService;
	
	@Autowired 
	PhotoService photoService;
	
	@Autowired
	HeaderBuilderService headerBuild;
	
	private ArrayList<String> localAddressesList;
	
	/**
	 * Collect and save all local host names and addresses
	 * @throws Exception
	 */
	@PostConstruct
	public void Init() throws Exception {
		InetAddress ip; 
		localAddressesList = new ArrayList<String>();
		localAddressesList.add("127.0.0.1");
		localAddressesList.add("localhost");
		try {
			ip = InetAddress.getLocalHost();
			localAddressesList.add(ip.getHostAddress());
			localAddressesList.add(ip.getHostName());
			logger.debug("Local host " + ip.getHostAddress() + "/" + ip.getHostName());
		} catch (UnknownHostException e) {
			logger.warn(e.getMessage());
		}
	}
	
//	/*=============================================================================================
//
//
//		VIDEO FILE SERVE
//
//
//	 =============================================================================================*/
//	@RequestMapping(value = "/video/{id}", method = RequestMethod.GET)
//	@ResponseBody
//	public ResponseEntity<InputStreamResource> streamVideo(
//			final HttpServletRequest HTTPrequest,
//			final HttpServletResponse HTTPresponse,
//			@PathVariable("id") String PhotoId) throws Exception {
//
//		logger.debug(">>> GET Request for /video/"+PhotoId);
//
//		ResponseEntity<InputStreamResource> resp = null;
//		Photo thePhoto = photoService.getPhotoById(PhotoId);
//
//		//TODO: Check if source has local copy
//
//		//  Необходимо загрузить конектор чтобы освежить авторизацию с сайтом-источником
//		//SiteConnectorInt connector = siteService.getOrLoadConnector(thePhoto.getSiteBean());
//
//		Media mediaObject = null;
//		for(Media media: thePhoto.getMediaObjects()) {
//			if (media.getType() == Media.MEDIA_VIDEO) {
//				logger.debug("Found media object."
//						+" Type=" + media.getType()
//						+", mimeType="+media.getMimeType()
//				);
//				mediaObject = media;
//				break;
//			}
//		}
//
//		InputStream is = null;
//		if ( mediaObject.getAccessType() == Media.ACCESS_LOCAL) {
//			is = new FileInputStream(mediaObject.getPath());
//		}
//		else {
//			URL url = new URL(mediaObject.getPath());
//			is = url.openStream();
//		}
//
//		HttpHeaders headers = headerBuild.getHttpHeader(HTTPrequest);
//		if ((mediaObject.getSize() > 0) &&  (mediaObject.getType() != Media.MEDIA_VIDEO )) {
//			headers.setContentLength(mediaObject.getSize());
//		}
//
//		String strMimeType = mediaObject.getMimeType();
//		int mimeDelimiterPos = strMimeType.indexOf("/");
//
//		MediaType thisObjMimeType= new MediaType(
//				strMimeType.substring(0,mimeDelimiterPos),
//				strMimeType.substring(mimeDelimiterPos+1)
//		);
//
//		headers.setContentType(thisObjMimeType);
//
//		logger.debug("[ImageController.downloadImage] Replay with image. "
//				+" Mime type="+thisObjMimeType
//				+", size="+mediaObject.getSize()
//				+", access type="+mediaObject.getAccessType()
//				+", path="+mediaObject.getPath());
//
//		resp =  new ResponseEntity<InputStreamResource>(
//				new InputStreamResource(is),
//				headers,
//				HttpStatus.OK);
//
//
//		logger.debug("<<< Request processed.");
//		return resp;
//	}
//


	/*=============================================================================================


		PHOTO FILE SERVE


	 =============================================================================================*/
	
	
	/**
	 * 
	 * Process http request rot photo file by their NodeID.
	 * Return image as stream only for LOCAL_DITE
	 * 
	 * @param HTTPrequest
	 * @param HTTPresponse
	 * @param PhotoId
	 * @return
	 * @throws Exception
	 */

	//TODO:  handle size request

	@RequestMapping(value = "/image/{PhotoId}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<InputStreamResource> downloadImage(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
			@RequestParam(value ="type", required = false, defaultValue = "12" ) Integer mediaType,
			@PathVariable("PhotoId") String PhotoId) throws Exception {
		
		logger.debug(">>> GET Request for /image/"+PhotoId+" [type="+mediaType+"]");

		ResponseEntity<InputStreamResource> resp = null;		
		Photo thePhoto = photoService.getPhotoById(PhotoId);
	
		//TODO: Check if source has local copy
		
		//  Необходимо загрузить конектор чтобы освежить авторизацию с сайтом-источником
		//SiteConnectorInt connector = siteService.getOrLoadConnector(thePhoto.getSiteBean());

		Media mediaObject = null;
		for(Media media: thePhoto.getMediaObjects()) {
			if (media.getType() == mediaType) {
				mediaObject = media;
				break;
			}
		}
		
		InputStream is = null;
		if ( mediaObject.getAccessType() == Media.ACCESS_LOCAL) {
			is = new FileInputStream(mediaObject.getPath());
		}
		else {
			URL url = new URL(mediaObject.getPath());
			is = url.openStream();
		}

	    HttpHeaders headers = headerBuild.getHttpHeader(HTTPrequest);
	    if (mediaObject.getSize() > 0) {
	    	headers.setContentLength(mediaObject.getSize());
	    }

	    String strMimeType = mediaObject.getMimeType();
		int mimeDelimiterPos = strMimeType.indexOf("/");

		MediaType thisObjMimeType= new MediaType(
				strMimeType.substring(0,mimeDelimiterPos),
				strMimeType.substring(mimeDelimiterPos+1)
		);

		headers.setContentType(thisObjMimeType);

		logger.debug("[ImageController.downloadImage] Replay with image. "
				+" Mime type="+thisObjMimeType
				+", size="+mediaObject.getSize()
				+", access type="+mediaObject.getAccessType()
				+", path="+mediaObject.getPath());
		
		resp =  new ResponseEntity<InputStreamResource>(
				new InputStreamResource(is),
				headers,
				HttpStatus.OK);


		logger.debug("<<< Request processed.");
		return resp;			
	}
	
	/*=============================================================================================


		THUMBNAIL PROCESSING
		

 	=============================================================================================*/
	
	/**
	 * 
	 * @param HTTPrequest
	 * @param HTTPresponse
	 * @param PhotoId
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/thumb/{thumbFolder}/{thumbId}.{ext}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<InputStreamResource> downloadThumb(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
	        @PathVariable("thumbFolder") String thumbFolder,
			@PathVariable("thumbId") String thumbId) throws Exception {
		
		logger.debug(">>> GET Request for /thumb/"+thumbId);
		ResponseEntity<InputStreamResource> resp = null;
		String realPath = confService.getValue(ConfVarEnum.LOCAL_THUMB_PATH,"");
		String fileExt = confService.getValue(ConfVarEnum.LOCAL_THUMB_FMT,"png");

		realPath = realPath + "/" 
				+ thumbFolder+"/" 
				+ thumbId + "." 
				+ fileExt;
		
		File imageFile = new File(realPath);

	    HttpHeaders headers = headerBuild.getHttpHeader(HTTPrequest);
		headers.setContentLength(imageFile.length());
		headers.setContentType(new MediaType("image",fileExt));
		
		logger.debug("Send file: " + imageFile.getName());

		resp =  new ResponseEntity<InputStreamResource>(
				new InputStreamResource(new FileInputStream(imageFile)),
				headers,
				HttpStatus.OK);
		/*
		resp = ResponseEntity.ok()
	            .contentLength(imageFile.length())
	            .contentType(new MediaType("image",fileExt))
	            //.contentType(MediaType.IMAGE_PNG)
	            .body(new InputStreamResource(new FileInputStream(imageFile)));
	    */		
		return resp;
	}	
	
}

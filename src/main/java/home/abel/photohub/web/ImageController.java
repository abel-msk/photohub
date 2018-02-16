package home.abel.photohub.web;

import java.io.File;
import java.util.Enumeration;
import java.util.List;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gdata.data.media.mediarss.MediaKeywords;
import home.abel.photohub.connector.prototype.EnumMediaType;
import home.abel.photohub.connector.prototype.PhotoMediaObjectInt;
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
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpRange;


/**
 *  Server access request for photo image and thumbnails
 *  For this service working, it should be configured with USE_IMAGE_WEB variable set  true
 *  In this way all local requests from 127.0.0.1 or the host name redirects to real file on installed computer.
 *  For other request we return the image as http reply.
 * @author abel
 *
 */

//  Byte range request-responce example:
//  https://stackoverflow.com/questions/28427339/how-to-implement-http-byte-range-requests-in-spring-mvc


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

	/*=============================================================================================


		PHOTO FILE SERVE


	 =============================================================================================*/

	private final static String[] transmittedHeaders= {"Range","Connection"};

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
	@RequestMapping(value = "/image/{PhotoId}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<AbstractResource> downloadImage(
	        final HttpServletRequest HTTPrequest,
	        final HttpServletResponse HTTPresponse,
			@RequestParam(value ="type", required = false, defaultValue = "12" ) Integer mediaType,
			@PathVariable("PhotoId") String PhotoId
			//@RequestHeader(value="Range",required = false, defaultValue = "none") String rahgeHederValue
	) throws Exception {
		
		logger.debug("GET Request for /image/"+PhotoId+" [type="+mediaType+"]");

		AbstractResource resp = null;
		Photo thePhoto = photoService.getPhotoById(PhotoId);
		StringBuilder siteReqHeaders = new StringBuilder();

		//  Необходимо загрузить конектор чтобы освежить авторизацию с сайтом-источником
		SiteConnectorInt connector = siteService.getOrLoadConnector(thePhoto.getSiteBean());

		Media mediaObject = null;
		for(Media media: thePhoto.getMediaObjects()) {
			if (media.getType() == mediaType) {
				mediaObject = media;
				break;
			}
		}

		if (mediaObject == null) {
			throw new  ExceptionInvalidRequest("Media not found for Object=" + PhotoId);
		}


		//
		//   Extract input request headers^ and retransmit  they to remote site request generator
		//
		for (String hdrName : transmittedHeaders) {
			//String hdrVal = HTTPrequest.getHeader(hdrName);
			Enumeration<String>  hdrVals =  HTTPrequest.getHeaders(hdrName);
			if ( hdrVals != null) {
				while (hdrVals.hasMoreElements()) {
					siteReqHeaders.append(hdrName).append(": ").append(hdrVals.nextElement()).append("\n");
				}
			}
		}

		logger.trace(" Input request hraders:" + (siteReqHeaders.length()>0?siteReqHeaders.toString():"Empty"));

		//
		//   Retransmit request
		//
		AbstractResource responseResource  = connector.loadMediaByPath(mediaObject.getPath(),
				(siteReqHeaders.length()>0?siteReqHeaders.toString():null)
		);

		//
		//   Extract the received respons header lines when open stream from site
		//
		HttpHeaders hdrList = new HttpHeaders();
		if  ( responseResource.getDescription() != null ) {

			String descrStr = responseResource.getDescription();
			Pattern regex = Pattern.compile("\\[(.*?)\\]",Pattern.DOTALL);
			Matcher regexMatcher = regex.matcher(descrStr);
			if (regexMatcher.find()) {
				try {
					descrStr = regexMatcher.group(1);
				}
				catch (Exception e) {
					logger.warn("[downloadImage] Cannot find headers in stream description: "+ e.getMessage());
					descrStr = null;
				}
			}
			else {
				logger.warn("[downloadImage] Cannot find headers in stream description."+descrStr);
				descrStr = null;
			}

			if ( descrStr != null) {
				String[] siteRespHeaders = descrStr.split("\\r?\\n");
				for (String hdr : siteRespHeaders) {
					//logger.trace("[downloadImage] parse header string = " + hdr);
					if (hdr.contains(":")) {
						hdrList.add(hdr.substring(0, hdr.indexOf(":")),
								hdr.substring(hdr.indexOf(":") + 1).trim()
						);
					}
				}
			}
		}

		//
		//   Prepare own response headers
		//
		if (hdrList.getContentType() == null) {
			hdrList.setContentType(MediaType.parseMediaType(mediaObject.getMimeType()));
		}
		if (hdrList.getContentLength() <= 0) {
			hdrList.setContentLength(mediaObject.getSize());
		}

		logger.debug("[downloadImage] Replay with image. "
				+" Mime type="+hdrList.getContentType()  + "("+mediaObject.getMimeType()+ ")"
				+", size="+hdrList.getContentLength() +"("+mediaObject.getSize()+ ")"
				+", access type="+mediaObject.getAccessType()
				+", path="+mediaObject.getPath());

		//logger.trace("[ImageController.downloadImage] Replay headers " + hdrList );

		return ResponseEntity.ok()
				.headers(hdrList)
				//.contentLength(mediaObject.getSize())
				//.contentType(MediaType.parseMediaType(mediaObject.getMimeType()))
				.body(responseResource);



//
//		return ResponseEntity.ok()
//				.contentLength(theMedia.getSize())
//				.contentType(MediaType.parseMediaType(theMedia.getMimeType()))
//				.body(resp);

	}
	
	/*=============================================================================================


		THUMBNAIL PROCESSING
		

 	=============================================================================================*/

	/**
	 *
	 * @param HTTPrequest
	 * @param HTTPresponse
	 * @param thumbFolder
	 * @param thumbId
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







//
//
//  !!!!  			@RequestHeader("Range") String rahgeHederValue
//
//	@RequestMapping(value = "/image/{PhotoId}", method = RequestMethod.GET)
//	@ResponseBody
//	public ResponseEntity<UrlResource> downloadImage(
//			final HttpServletRequest HTTPrequest,
//			final HttpServletResponse HTTPresponse,
//			@RequestParam(value ="type", required = false, defaultValue = "12" ) Integer mediaType,
//			@PathVariable("PhotoId") String PhotoId,
//			@RequestHeader("Range") String rahgeHederValue
//	) throws Exception {

//		logger.debug(">>> GET Request for /image/"+PhotoId+" [type="+mediaType+"]");
//
//		ResponseEntity<InputStreamResource> resp = null;
//		Photo thePhoto = photoService.getPhotoById(PhotoId);
//
//
//		//  Необходимо загрузить конектор чтобы освежить авторизацию с сайтом-источником
//		//SiteConnectorInt connector = siteService.getOrLoadConnector(thePhoto.getSiteBean());
//
//		Media mediaObject = null;
//		for(Media media: thePhoto.getMediaObjects()) {
//			if (media.getType() == mediaType) {
//				mediaObject = media;
//				break;
//			}
//		}
//
//		String thePath = "";
//		if (mediaObject.getAccessType() == Media.ACCESS_LOCAL) {
//			thePath = "file:///";
//		}
//		thePath += mediaObject.getPath();
//		UrlResource responseResource = new UrlResource(thePath);


//
//		List<ResourceRegion> regionsList = null;
//		if (rahgeHederValue != null && (rahgeHederValue.length() > 0) ) {
//			regionsList =  HttpRange.toResourceRegions(
//					HttpRange.parseRanges(rahgeHederValue),
//					responseResource);
//		}


//		InputStream is = null;
//		try {
//			if (mediaObject.getAccessType() == Media.ACCESS_LOCAL) {
//				is = new FileInputStream(mediaObject.getPath());
//			} else {
//				URL url = new URL(mediaObject.getPath());
//				is = url.openStream();
//			}
//		}
//		catch (Exception e) {
//			throw new ExceptionObjectNotFound("Cannot load object from site. "+ e.getMessage());
//		}


//	    HttpHeaders headers = headerBuild.getHttpHeader(HTTPrequest);
//	    if (mediaObject.getSize() > 0) {
//	    	headers.setContentLength(mediaObject.getSize());
//	    }
//
//	    String strMimeType = mediaObject.getMimeType();
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
//				+" Mime type="+mediaObject.getMimeType()
//				+", size="+mediaObject.getSize()
//				+", access type="+mediaObject.getAccessType()
//				+", path="+mediaObject.getPath());

//		resp =  new ResponseEntity<InputStreamResource>(
//				new InputStreamResource(is),
//				headers,
//				HttpStatus.OK);
//
//
//		logger.debug("<<< Request processed.");
//		return resp;

//		return ResponseEntity.ok()
//				.contentLength(mediaObject.getSize())
//				.contentType(MediaType.parseMediaType(mediaObject.getMimeType()))
//				.body(new InputStreamResource(is));

//		return ResponseEntity.ok()
//				.contentLength(mediaObject.getSize())
//				.contentType(MediaType.parseMediaType(mediaObject.getMimeType()))
//				.body(regionsList.get(0));
//
//		return ResponseEntity.ok()
//				.contentLength(mediaObject.getSize())
//				.contentType(MediaType.parseMediaType(mediaObject.getMimeType()))
//				.body(responseResource);
//
//	}


}

package home.abel.photohub.service;
import home.abel.photohub.connector.prototype.PhotoMediaObjectInt;
import home.abel.photohub.connector.prototype.PhotoObjectInt;
import home.abel.photohub.model.Media;
import home.abel.photohub.model.ModelConstants;
import home.abel.photohub.model.Photo;
import home.abel.photohub.service.ConfigService;
import home.abel.photohub.utils.FileUtils;
import home.abel.photohub.utils.image.ImageScaler;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.AbstractResource;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;

@Service
public class ThumbService {
	
	final Logger logger = LoggerFactory.getLogger(ThumbService.class);
			
	@Autowired 
	private SiteService siteService; 	

	@Autowired 
	private ConfigService confService;	
	
	@Autowired
	Environment config;
	
	private String thumbExt = null;
	private Dimension thumbDimension = null;
	//private String defaultFolderIcon = null;
	//private ImageScaler imageScaler = null;
	
	public  ThumbService() {
		thumbDimension = new Dimension();
		thumbDimension.height = 250;
		thumbDimension.width = 250;
		//imageScaler = new ImageScalerImpl();
	}
	
	/*=============================================================================================
	 * 
	 *    Set service parameters
	 *      
	 =============================================================================================*/
	/**
	 * Get all properties from from external property file
	 */
	@PostConstruct
	public void Init() {
		//defaultFolderIcon  = config.getProperty(ConfVarEnum.DEFAULT_FLD_THUMB.getPropertyName(),"--");
		//defaultFolderIcon  = confService.getValue(ConfVarEnum.DEFAULT_FLD_THUMB);

		String propName = confService.getValue(ConfVarEnum.LOCAL_THUMB_FMT);
		logger.debug("Got property name " + propName);
		setThumbDefaultFormat(confService.getValue(ConfVarEnum.LOCAL_THUMB_FMT));
	}	
		
	/**
	 * Set dimension of thumbs
	 * @param dim
	 */
	public void setThumbDimension(Dimension dim) {
		thumbDimension = dim;
	}
	public Dimension getThumbDimension() {
		return this.thumbDimension;
	}
	/**
	 * Set default thumbnail format and extension
	 * @param ext
	 */
	public void setThumbDefaultFormat(String ext) {
		thumbExt = ext;
		if (thumbExt == null) { thumbExt = "png"; }
		if (thumbExt.substring(0,1) != ".") {
			thumbExt = "." + thumbExt;
		}
	}
	
	/*=============================================================================================
	 * 
	 *    Create thumbnail
	 *      
	 =============================================================================================*/
	
	public String setThumb(String sourceFilePath, Photo thePhoto) throws Exception {
		
		//String photoFullPath = null;
		String newThumbPath = getThumbPath(thePhoto.getId());

		if (sourceFilePath != null) {
			//  TODO:  check -- is resize required
			logger.debug("Createing thumb:  sourceFilePath="+sourceFilePath+", newThumbPath=" + newThumbPath + ", size=" + thumbDimension.width + "x" + thumbDimension.height);
			ImageScaler.doScale(sourceFilePath,newThumbPath,thumbDimension);
			logger.debug("Thumbnail created in path: " + newThumbPath);
		}
		//   Object is folder
		else if ((thePhoto.getType() == ModelConstants.OBJ_FOLDER)  ||
				(thePhoto.getType() == ModelConstants.OBJ_SERIES)){
			String defaultFolderIcon = confService.getValue(ConfVarEnum.DEFAULT_FLD_THUMB);
			logger.debug("Default thumbnail '" + defaultFolderIcon + "' copyed to: " + newThumbPath);	
			
			FileUtils.copyFile(defaultFolderIcon, newThumbPath);	
		} 
		//   Object is Image but with out the image
		else {
			throw new ExceptionPhotoProcess("Cannot access original photo object - sourceFilePath ns null.");
		}
		
		return newThumbPath;
	}
	
	public void setDefaultThumb(Photo thePhoto) throws ExceptionFileIO {
		String newThumbPath = getThumbPath(thePhoto.getId());
		String defaultFolderIcon = confService.getValue(ConfVarEnum.DEFAULT_FLD_THUMB);
		logger.debug("Default thumbnail '" + defaultFolderIcon + "' copyed to: " + newThumbPath);			
		FileUtils.copyFile(defaultFolderIcon, newThumbPath);
	}
	
	/**
	 * 	Upload thumbnail from site and save locally.  If there is no thumbnail on site, use default.
	 * 
	 * @param sitesPhotoObject
	 * @param thePhoto
	 * @throws IOException
	 */
	public void uploadThumbnail(PhotoObjectInt sitesPhotoObject, Photo thePhoto) throws IOException,ExceptionPhotoProcess {
		//   Save Thumbnail
		if (sitesPhotoObject.hasThumbnailSource()) {
			
			PhotoMediaObjectInt mObject =  sitesPhotoObject.getThumbnail(thumbDimension);
			
			//   Save thumbnail to local file
			String newThumbPath = getThumbPath(thePhoto.getId());
			File outputFile = new File(newThumbPath);
			try {
				FileUtils.saveFile(mObject.getContentStream().getInputStream(), outputFile);
			}
			catch (Exception e) {
				throw new IOException(e);
			}
			
			//   Save thumbnail info to DB
			Media dbMObject = new Media();
			dbMObject.setType(Media.MEDIA_THUMB);
			dbMObject.setAccessType(Media.ACCESS_LOCAL);
			dbMObject.setHeight(mObject.getHeight());
			dbMObject.setWidth(mObject.getWidth());
			dbMObject.setSize(outputFile.length());
			dbMObject.setMimeType(mObject.getMimeType());
			dbMObject.setPath(newThumbPath);
			thePhoto.addMediaObject(dbMObject);
		}
		else {
			setDefaultThumb(thePhoto);
		}
	}	
	
	
	/*=============================================================================================
	 * 
	 *    Copy thumbnail
	 *      
	 =============================================================================================*/
	public void copyThumb(Photo photoFrom, Photo photoTo) throws Exception {
		String fromThumpPath = getThumbPath(photoFrom.getId());
		String toThumbPath = getThumbPath(photoTo.getId());
		if (FileUtils.isAccessable(fromThumpPath)) {
			if (FileUtils.isAccessable(toThumbPath)) {
				FileUtils.fileDelete(toThumbPath,false);
			}
			FileUtils.copyFile(fromThumpPath, toThumbPath);
		}
		else {
			throw new ExceptionFileIO("Source thumbnail not found. File=" + fromThumpPath + ", Photo Id=" + photoFrom.getId() );
		}
	}
	
	/*=============================================================================================
	 * 
	 *    Service function
	 *      
	 =============================================================================================*/
	/*
	private String getPhotoPath(Photo thePhoto) throws ExceptionDBContent {
		if (thePhoto.getSiteBean().getType() == ModelConstants.SITE_LOCAL) {
			return FilenameUtils.normalize(siteService.getSiteRoot(thePhoto) + File.separator + thePhoto.getPath());
		}
		return null;
	}
	*/
	public String getThumbPath(String photoId) {
		return FilenameUtils.normalize(
				confService.getValue(ConfVarEnum.LOCAL_THUMB_PATH) +
				File.separator +  genSubPath(photoId) + File.separator + photoId+ thumbExt);
	}

//	public String getThumbUrl(String photoId) {
//		String val = confService.getValue(ConfVarEnum.LOCAL_THUMB_URL,"");
//		val = val.endsWith("/") ? val : val + "/";
//		val =  val + genSubPath(photoId) + "/" + photoId+ thumbExt;
//		return val;
//	}
	
	/*=============================================================================================
	 * 
	 *    Generate full path for store
	 *      
	 =============================================================================================*/
	
	/**
	 * 
	 * @param fileName
	 * @return
	 */
	public static String genSubPath (String fileName) {
		String subDir = null;
		
		if (fileName.length() == 1 ) {
			subDir = "0" + fileName;
		} else if (fileName.length() == 2) {
			subDir = fileName;
		} else {
			subDir = fileName.substring(fileName.length() - 2, fileName.length());
		}
		return subDir;
	}	
}

package home.abel.photohub.connector.local;

import java.awt.Dimension;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import home.abel.photohub.connector.prototype.*;
import home.abel.photohub.utils.image.ImageData;
import home.abel.photohub.utils.image.Metadata;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import home.abel.photohub.connector.BasePhotoMetadata;
import home.abel.photohub.connector.BasePhotoObj;

public class LocalPhotoObject extends BasePhotoObj {
	final Logger logger = LoggerFactory.getLogger(LocalPhotoObject.class);
	//private static String[] VALID_EXT_ARRAY = {"gif","tiff","jpg","jpeg","png"};

	protected File photoObjectsFile = null;
	protected BasePhotoMetadata metaObj = null; 
	protected boolean isFolder = false;
	protected ImageData imageData = null;

	public LocalPhotoObject(SiteConnectorInt conn, File objectFile) throws Exception  {
		super(conn);
		photoObjectsFile = objectFile;
		isFolder = photoObjectsFile.isDirectory();

		if (! isFolder ) {
			String fileExt = FilenameUtils.getExtension(photoObjectsFile.getName());
			setMimeType(ImageData.getMimeTypeByExt(fileExt));
			logger.debug("[LocalPhotoObject.init] Set mime type = " + getMimeType());

			if (ImageData.isValidImage(photoObjectsFile)) {
				setType("image");
				imageData = new ImageData(new FileInputStream(photoObjectsFile));

				//
				//     Check for UUID in the metadata   and generate new ine if absent
				//
				if ( imageData.getMetadata() != null ) {
					if (imageData.getMetadata().getUnicId() == null) {
						imageData.getMetadata().setUnicId(Metadata.generateUUID());
						if (photoObjectsFile.canWrite()) {
							imageData.saveJPEG(new FileOutputStream(photoObjectsFile));
						} else {
							logger.error("Cannot change metadata for read only file " + photoObjectsFile.getAbsolutePath());
						}
					}
				}
				setWidth(imageData.getWidth());
				setHeight(imageData.getHeight());

			}
			else if (getMimeType().startsWith("video")) {
				setType("video");
			}
			else {
				String fn=photoObjectsFile.getAbsolutePath();
				photoObjectsFile = null;
				throw new ExceptionUnknownFormat(" Unknown media format for file "+fn );
			}
			setSize(photoObjectsFile.length());
		}
		this.setId(photoObjectsFile.getAbsolutePath());
	}
	
	public LocalPhotoObject(SiteConnectorInt conn, String pathToFile) throws Exception {
		this(conn,new File(pathToFile));
	}

	
	/**
	 * Return object's source file
	 * 
	 * @return
	 */
	public File getFile() {
		return this.photoObjectsFile;
	}
	
	
	/**
	 *   Return object ID. ( Absolute  path to source file )
	 */
	@Override
	public String getId() {
		if (hasPhotoSource()) 
			return this.photoObjectsFile.getAbsolutePath();
		else return null;
	}
	
	/**
	 * (non-Javadoc)
	 * @see home.abel.photohub.connector.BasePhotoObj#getName()
	 */
	@Override
	public String getName() {
		if ((super.getName() == null) && (this.photoObjectsFile != null)) {
			super.setName(this.photoObjectsFile.getName());
		}
		return super.getName();
	}
	
	/**
	 * (non-Javadoc)
	 * @see home.abel.photohub.connector.BasePhotoObj#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) {
		super.setName(name);
		if ( getType() == null ) {
			setType(FilenameUtils.getExtension(name));
		}
	}
		
	
	/**
	 * Load metadata object if not loaded and store ExIf images tag
	 * 
	 * (non-Javadoc)
	 * @see home.abel.photohub.connector.BasePhotoObj#getMeta()
	 */
	@Override
	public PhotoMetadataInt getMeta() throws Exception {

		if ( imageData != null) {
			return imageData.getMetadata();
		}
		else return null;
	}
	
	
	@Override
	public void setMeta(PhotoMetadataInt newMetaData) throws Exception {
		if (isFolder()) throw new Exception("Object is a directory.");

		if ((!getMimeType().endsWith("tiff")) && (!getMimeType().endsWith("jpeg"))) {
			throw new Exception("Object has not metadata");
		}

		Metadata originalMD = imageData.getMetadata();

		originalMD.setCameraMake(newMetaData.getCameraMake());
		originalMD.setCameraModel(newMetaData.getCameraModel());
		originalMD.setAperture(newMetaData.getAperture());
		originalMD.setExposureTime(newMetaData.getExposureTime());
		originalMD.setFocalLength(newMetaData.getFocalLength());
		originalMD.setIso(newMetaData.getIso());
		originalMD.setLatitude(newMetaData.getLatitude());
		originalMD.setLongitude(newMetaData.getLongitude());
		originalMD.setAltitude(newMetaData.getAltitude());
		originalMD.setDateCreated(newMetaData.getDateCreated());
		originalMD.setDateOriginal(newMetaData.getDateOriginal());
		originalMD.setDateUpdate(newMetaData.getDateUpdate());
		originalMD.setUnicId(newMetaData.getUnicId());
		originalMD.setFlash(newMetaData.getFlash());
		originalMD.setTzOffset(newMetaData.getTzOffset());
		originalMD.setOrientation(newMetaData.getOrientation());
		originalMD.setSoftware(newMetaData.getSoftware());
		originalMD.setResolution(newMetaData.getResolution());
		originalMD.setShutterSpeed(newMetaData.getShutterSpeed());
		originalMD.setBrightness(newMetaData.getBrightness());
		originalMD.setUserComment(newMetaData.getUserComment());

		imageData.setMetadata(originalMD);
		if (photoObjectsFile.canWrite()) {
			imageData.saveJPEG(new FileOutputStream(photoObjectsFile));
		}
		else {
			logger.error("Cannot change metadata for read only file " + photoObjectsFile.getAbsolutePath());
		}
		//imageData = new ImageData(new FileInputStream(this.photoObjectsFile));

	}


	private File getUniqueFileName(String directory, String extension) {
	    return new File(directory, 
	    		new StringBuilder().append(new Date().getTime()).append(UUID.randomUUID())
	    		.append(".")
	    		.append(extension).toString());
	}
	
	
	/**
	 *   Return true if object describe folder
	 */
	@Override
	public boolean isFolder() {
		return isFolder;
	}
	
	/**
	 *	For the folder type object it return list of contains non hidden real files and folders, 
	 *  converted in to PhotoObjectInt
	 *  @see home.abel.photohub.connector.BasePhotoObj#listSubObjects()
	 */
	@Override
	public List<String> listSubObjects() {
		List<String> list = null;
		
		if (isFolder() && hasPhotoSource()) {
			list = new ArrayList<>();
			File[] filesAr = this.photoObjectsFile.listFiles();
			if (filesAr != null) {
				for (File curFile : filesAr) {
					if (curFile.canRead() && (! curFile.isHidden()) &&
							( ImageData.isValidImage(curFile) ||  curFile.isDirectory())
							) {
						if (ImageData.isValidImage(curFile) || (curFile.isDirectory())) {
							//LocalPhotoObject item = new LocalPhotoObject(this.getConnector(), curFile.getAbsolutePath());
							list.add(curFile.getAbsolutePath());
						}
						else {
							logger.warn("[LocalPhotoObject.listSubObjects] invalid image file found "+curFile.getAbsolutePath()+". Ignored.");
						}
					}
				}
			}
		}
		return list;
	}

	/**
	 *  This object has accessible source
	 */
	@Override
	public boolean hasPhotoSource() {
		return photoObjectsFile != null;
	}

	
	/**
	 * Return MediaObject for photo
	 * @throws IOException - file not found
	 */
	@Override
	public PhotoMediaObjectInt getMedia(EnumMediaType type) throws IOException {
		LocalMediaObject mediaFile = null;
		if ( photoObjectsFile != null) {
			mediaFile = new LocalMediaObject(this.getConnector(), imageData);
			mediaFile.setHeight(this.height);
			mediaFile.setWidth(this.width);
			mediaFile.setSize(this.size);
			mediaFile.setMimeType("image/jpeg");  //TODO; check for TIFF or GIF file
			mediaFile.setType(EnumMediaType.IMAGE_FILE);
			mediaFile.setPath(photoObjectsFile.getAbsolutePath());

			if (getType().equalsIgnoreCase("image")) {
				mediaFile.setType(EnumMediaType.IMAGE_FILE);
			}
			else if (getType().equalsIgnoreCase("video")) {
				mediaFile.setType(EnumMediaType.VIDEO_FILE);
			}
			else {
				mediaFile.setType(EnumMediaType.UNKNOWN);
			}

		}
		else {
			throw new IOException("Photo file path not defined.");
		}
		return mediaFile;
	}
	
	/**
	 *   His object have thumbnail (Mаy be generated)
	 */
	@Override
	public boolean hasThumbnailSource() {
		return (! isFolder) && (hasPhotoSource());
	}

	/**
	 * Return MediaObject for thumbnail
	 */
	@Override
	public PhotoMediaObjectInt  getThumbnail(Dimension dim) throws IOException {

		//TODO: create thumb for video

		LocalMediaObject mediaFile = new LocalMediaObject(
				this.getConnector(),
				imageData
				);

		mediaFile.setType(EnumMediaType.THUMB_FILE);
		mediaFile.setPath(photoObjectsFile.getAbsolutePath());
		mediaFile.setMimeType("image/png");
		
		//logger.trace("Process thubnail image. Original image  width=" +getWidth()+", height="+getHeight());

				
		if ( getHeight() > getWidth() ) {
			mediaFile.setHeight(dim.height);	
			float aspect = (float)getWidth()/(float)getHeight();
			mediaFile.setWidth((int)(dim.width*aspect));
			logger.trace("Process thubnail. Normalize by height. Scale to width=" +mediaFile.getWidth()+", height="+mediaFile.getHeight());
		}
		else {
			mediaFile.setWidth(dim.width) ;
			float aspect = (float)getHeight()/(float)getWidth();
			mediaFile.setHeight((int)(dim.height*aspect));
			logger.trace("Process thubnail. Normalize by width. Scale to width=" +mediaFile.getWidth()+", height="+mediaFile.getHeight());

		}

		return mediaFile;
	}



	
	public void delete() throws Exception{
		if ( photoObjectsFile.delete()) {
			 photoObjectsFile = null;
		}
		else throw new RuntimeException("Cannot delete file '"+getId() +"'");
	}

	
	
}

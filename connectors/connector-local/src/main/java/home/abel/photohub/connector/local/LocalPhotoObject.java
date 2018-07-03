package home.abel.photohub.connector.local;

import java.awt.*;
import java.io.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.List;

import home.abel.photohub.connector.prototype.*;
import home.abel.photohub.utils.image.ExceptionImgProcess;
import home.abel.photohub.utils.image.ImageData;
import home.abel.photohub.utils.image.Metadata;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
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
	public final static String defaultExt = "jpeg";



	public LocalPhotoObject(SiteConnectorInt conn, File objectFile) throws Exception  {
		super(conn);
		photoObjectsFile = objectFile;
		isFolder = photoObjectsFile.isDirectory();


		if (! isFolder ) {
			String fileExt = FilenameUtils.getExtension(photoObjectsFile.getName());

			if (ImageData.isValidImage(photoObjectsFile) &&
					(! fileExt.toUpperCase().startsWith("TIF")))
			{
				imageData = new ImageData(new FileInputStream(photoObjectsFile));

				setMimeType(ImageData.getMimeTypeByExt(fileExt));
				setType("image");

				//
				//     Check for UUID in the metadata   and generate new one if absent
				//
				if ( imageData.getMetadata() != null ) {

					//
					//     Check is image has unicID.   if not, generate one and update image
					//
					if (imageData.getMetadata().getUnicId() == null) {
						if (photoObjectsFile.canWrite()) {
							Metadata md = imageData.getMetadata();
							md.setUnicId(Metadata.generateUUID());
							logger.trace("[init] object update UnicId =  "+md.getUnicId());
							File tmpFile = null;

							try {
								tmpFile = File.createTempFile("image", "." + fileExt);
								try {
									imageData.saveJPEG(new FileOutputStream(tmpFile));
								}
								catch (ExifRewriter.ExifOverflowException e ) {
									md.setOutputSet(md.copyOutputSet());
									imageData.setMetadata(md);
									imageData.saveJPEG(new FileOutputStream(tmpFile));
								}
								logger.debug("[init] Add uni ID to image file");
								ImageData.copyFileUsingChannel(tmpFile, photoObjectsFile);
							}
							catch (IOException e) {
								imageData.setReadOnly(true);
								logger.warn("Open read only :" + photoObjectsFile.getAbsolutePath() +", Error:"+e.getMessage());
								//throw  new ExceptionImgProcess("Cannot update metadata. ");
							}
							finally {
								if (tmpFile != null) tmpFile.delete();
							}
						} else {
							logger.warn("Cannot update metadata for read only file " + photoObjectsFile.getAbsolutePath());
						}
					}
				}
				setWidth(imageData.getWidth());
				setHeight(imageData.getHeight());
				setSize(photoObjectsFile.length());

			}
//			else if (getMimeType().startsWith("video")) {
//				setType("video");
//				throw new ExceptionUnknownFormat(" Unknown media format for file "+fn );
//			}
			else {
				String fn=photoObjectsFile.getAbsolutePath();
				photoObjectsFile = null;
				throw new ExceptionUnknownFormat(" Unknown/Unsupported media format for file "+fn );
			}
			logger.trace("[init] load object mimetype=" + getMimeType()+ ", id "+objectFile.getAbsolutePath());
		}
		else {
			logger.trace("[init] load object folder, id "+objectFile.getAbsolutePath());
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
			PhotoMetadataInt md = imageData.getMetadata();
			if (md != null) {
				md.setDateUpdate(new Date(photoObjectsFile.lastModified()));
			}
			else {
				md = new BasePhotoMetadata();
				md.setDateUpdate(new Date(photoObjectsFile.lastModified()));

				//TODO: Посмотреть что  еще мы можем добавить в метаданные
			}
			return md;
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
		originalMD.setxResolution(newMetaData.getxResolution());
		originalMD.setyResolution(newMetaData.getyResolution());
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
							if (FilenameUtils.getExtension(curFile.getName()).toUpperCase().startsWith("TIF")) {
								//
								//   Substitute TIFF with JPEG
								//
								//   Check if there is same name jpeg image for this tiff file
								//   if so  create jpeg image
								//   Проверяем есть ли файл с таким именени но  JPEG  в этой директории
								//
								try {
									String filePath = curFile.getAbsolutePath();
									File defFile =  new File(FilenameUtils.getPath(filePath)
											+ FilenameUtils.getBaseName(filePath)
											+ "." + defaultExt);
									if (! defFile.exists()) {
										convertToJPEG(curFile, defFile);
										list.add(defFile.getAbsolutePath());
									}
								} catch (IOException | ImageWriteException ex) {
									logger.warn("[listSubObjects] Cannot convert TIFF to JPEG, cannot write result. Skip file. Reason : "+ ex.getMessage());
								}
							}
							//
							//   For ather valid image file just add name to list
							else {
								list.add(curFile.getAbsolutePath());
							}
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
	 *    Convert image to JPEG format and place in same directory as source
	 * @param inFile
	 * @param outFile
	 * @return
	 * @throws IOException
	 * @throws ImageWriteException
	 */
	protected File convertToJPEG(File inFile, File outFile) throws IOException, ImageWriteException {
		ImageData tmpImage = new ImageData(new FileInputStream(inFile));

		try {
			tmpImage.saveJPEG(new FileOutputStream(outFile));
		}
		catch (ExifRewriter.ExifOverflowException e ) {
			Metadata md = tmpImage.getMetadata();
			md.setOutputSet(md.copyOutputSet());
			logger.debug("[convertToJPEG] Save JPEG with metadata overwrite.");
			imageData.saveJPEG(new FileOutputStream(outFile));
		}
		return outFile;
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
			logger.trace("[getThumbnail] Process thubnail. Normalize by height. Scale to width=" +mediaFile.getWidth()+", height="+mediaFile.getHeight());
		}
		else {
			mediaFile.setWidth(dim.width) ;
			float aspect = (float)getHeight()/(float)getWidth();
			mediaFile.setHeight((int)(dim.height*aspect));
			logger.trace("[getThumbnail] Process thubnail. Normalize by width. Scale to width=" +mediaFile.getWidth()+", height="+mediaFile.getHeight());
		}

		return mediaFile;
	}



	
	public void delete() throws Exception{
		if ( photoObjectsFile.delete()) {
			logger.debug("[delete]  File "+photoObjectsFile.getAbsolutePath()+" deleted.");
			photoObjectsFile = null;
		}
		else throw new RuntimeException("Cannot delete file '"+getId() +"'");
	}


	public PhotoObjectInt rotate90(rotateEnum direction) throws Exception {
		File tmpFile = null;
		ImageData newImage = null;

		if (isFolder) {
			return null;
		}

		Timestamp ts1 = new Timestamp(System.currentTimeMillis());

		logger.trace("");

		if (direction == rotateEnum.CLOCKWISE  ) {
			newImage = imageData.rotate(true);
		}
		else {
			newImage = imageData.rotate(false);
		}
		//
		//   Update change time in metadata
		//
		if ( newImage.getMetadata() != null ) {
			newImage.getMetadata().setDateUpdate(new Date());
		}

		Timestamp ts2 = new Timestamp(System.currentTimeMillis());
		logger.debug("[rotate90] Rotate duration " + (new Float(ts2.getTime() -  ts1.getTime()) / 1000) );

		try {

			tmpFile =  new File(photoObjectsFile.getParentFile() + "/" + System.currentTimeMillis());
			org.apache.commons.io.FileUtils.moveFile(photoObjectsFile, tmpFile);
			photoObjectsFile.createNewFile();
			newImage.saveJPEG(new FileOutputStream(photoObjectsFile));
		} catch (Throwable th) {
			logger.error("[rotate90] rotate error:"+th.getMessage(),th);
			if  (tmpFile.exists()) {
				photoObjectsFile.delete();
				org.apache.commons.io.FileUtils.moveFile(tmpFile, photoObjectsFile);
			}
			throw th;
		}
		finally {
			tmpFile.delete();
			Timestamp ts3 = new Timestamp(System.currentTimeMillis());
			logger.debug("[rotate90] Create new source file duration " + ( new Float(ts3.getTime() -  ts2.getTime()) / 1000) );
		}

		PhotoObjectInt newObj = new LocalPhotoObject(getConnector(),photoObjectsFile);
		logger.debug("[rotate90] Create new rotated image object " + newObj);
		return newObj;
	}

}

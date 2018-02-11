package home.abel.photohub.connector.local;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.common.RationalNumber;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
//import org.apache.sanselan.common.ImageMetadata;
//import org.apache.sanselan.common.RationalNumberUtilities;
//import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
//import org.apache.sanselan.formats.tiff.TiffImageMetadata;
//import org.apache.sanselan.formats.tiff.write.TiffOutputDirectory;
//import org.apache.sanselan.formats.tiff.write.TiffOutputSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import home.abel.photohub.connector.BasePhotoMetadata;
import home.abel.photohub.connector.BasePhotoObj;
import home.abel.photohub.connector.prototype.ExifMetadataTags;
import home.abel.photohub.connector.prototype.EnumMediaType;
import home.abel.photohub.connector.prototype.PhotoMediaObjectInt;
import home.abel.photohub.connector.prototype.PhotoMetadataInt;
import home.abel.photohub.connector.prototype.PhotoObjectInt;
import home.abel.photohub.connector.prototype.SiteConnectorInt;

public class LocalPhotoObject extends BasePhotoObj {
	final Logger logger = LoggerFactory.getLogger(LocalPhotoObject.class);
	private static String[] VALID_EXT_ARRAY = {"gif","tiff","jpg","jpeg","png"};

	protected File photoObjectsFile = null;
	protected BasePhotoMetadata metaObj = null; 
	protected boolean isFolder = false;
	//protected BufferedImage memImage = null;
	
	public LocalPhotoObject(SiteConnectorInt conn, File objectFile) throws IOException  {
		super(conn);
		photoObjectsFile = objectFile;
		isFolder = photoObjectsFile.isDirectory();
		if (! isFolder ) { 
			setSize(photoObjectsFile.length());
			
			// normalize extension type
			String fileExt = FilenameUtils.getExtension(objectFile.getName()).toLowerCase();
			if (fileExt != null ) {
				if (fileExt.startsWith("tif")) {
					setType("image");
					setMimeType("image/tiff");
				}
				if (fileExt.startsWith("tiff")) {
					setType("image");
					setMimeType("image/tiff");
				} else if (fileExt.startsWith("jpg")) {
					setType("image");
					setMimeType("image/jpeg");
				} else if (fileExt.startsWith("jpeg")) {
					setType("image");
					setMimeType("image/jpeg");
				} else if (fileExt.startsWith("png")) {
					setType("image");
					setMimeType("image/png");
				} else if (fileExt.startsWith("avi")) {
					setType("video");
					setMimeType("video/mp4");
				} else if (fileExt.startsWith("mp4")) {
					setType("video");
					setMimeType("video/mp4");
				} else {
					setMimeType("unknown");
					setType("unknown");
				}
			}
			
			BufferedImage memImage = ImageIO.read(photoObjectsFile);
			setWidth(getWidth());
			setHeight(getHeight());
			
			//logger.trace("Load image.  width=" +getWidth()+", height="+getHeight());
		}
		this.setId(objectFile.getAbsolutePath());
	}
	
	public LocalPhotoObject(SiteConnectorInt conn, String pathToFile) throws IOException {
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
	
	/*
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
	
	/*
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
		if ( photoObjectsFile == null ) return null;
		
		if (( metaObj == null) && (! isFolder())) {
			try {
				ImageMetadataExtractorLocal  extractor= new ImageMetadataExtractorLocal(photoObjectsFile);
				metaObj = extractor.loadMetadata();
			}
			catch (Exception e) {
				logger.warn("Cannot extract metadata. " + e.getMessage() ,e);
			}
		}
		return metaObj;
	}
	
	
	@Override
	public void setMeta(PhotoMetadataInt newMetaData) throws Exception {
		if (isFolder()) throw new Exception("Object is a directory.");
		
		File tempFile = getUniqueFileName(
				photoObjectsFile.getParentFile().getAbsolutePath(),
				FilenameUtils.getExtension(photoObjectsFile.getName()));
		
        OutputStream os = null;
        boolean canThrow = false;
        
        try {
            TiffOutputSet outputSet = null;

            // note that metadata might be null if no metadata is found.
            final ImageMetadata metadata = Imaging.getMetadata(photoObjectsFile);
            final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
            if (null != jpegMetadata) {
                // note that exif might be null if no Exif metadata is found.
                final TiffImageMetadata exif = jpegMetadata.getExif();

                if (null != exif) {
                    // TiffImageMetadata class is immutable (read-only).
                    // TiffOutputSet class represents the Exif data to write.
                    //
                    // Usually, we want to update existing Exif metadata by
                    // changing
                    // the values of a few fields, or adding a field.
                    // In these cases, it is easiest to use getOutputSet() to
                    // start with a "copy" of the fields read from the image.
                    outputSet = exif.getOutputSet();
                }
            }

            // if file does not contain any exif metadata, we create an empty
            // set of exif metadata. Otherwise, we keep all of the other
            // existing tags.
            if (null == outputSet) {
                outputSet = new TiffOutputSet();
            }

            // Example of how to add a field/tag to the output set.
            //
            // Note that you should first remove the field/tag if it already
            // exists in this directory, or you may end up with duplicate
            // tags. See above.
            //
            // Certain fields/tags are expected in certain Exif directories;
            // Others can occur in more than one directory (and often have a
            // different meaning in different directories).
            //
            // TagInfo constants often contain a description of what
            // directories are associated with a given tag.
            //
            final TiffOutputDirectory exifDirectory = outputSet
                    .getOrCreateExifDirectory();
            final TiffOutputDirectory BaselineDirectory = outputSet.getOrCreateRootDirectory();
                    
            // make sure to remove old value if present (this method will
            // not fail if the tag does not exist).
            

            //----------------------------------------------------------------------
            //   Обрабатываем ТЕГИ  из Exif дирктории
            //----------------------------------------------------------------------
            boolean NeedUpdate = false;
            
			//ImageDescription
            
            //
            Double longitude = null;
            Double latitude = null;

            //  Loop throught input tags
            ExifMetadataTags[] values = ExifMetadataTags.values();
    		for (int i = 0; i < values.length; i++) {
    			ExifMetadataTags tagEnum = values[i];
    			
    			if ( newMetaData.getMetaTag(tagEnum) != null) {
    					logger.debug("Store exif tag " + tagEnum.toString());
    					
    					switch (tagEnum) {
        				case CAMERA_MAKE:
        					final String cameraMake = newMetaData.getCameraMake();
        					BaselineDirectory.removeField(TiffTagConstants.TIFF_TAG_MAKE);
        					BaselineDirectory.add(TiffTagConstants.TIFF_TAG_MAKE, cameraMake );
        					NeedUpdate = true;
        					break;
        				case CAMERA_MODEL:
        					final String cameraModel = newMetaData.getCameraModel();
        					BaselineDirectory.removeField(TiffTagConstants.TIFF_TAG_MODEL);
        					BaselineDirectory.add(TiffTagConstants.TIFF_TAG_MODEL,cameraModel);
        					NeedUpdate = true;	        					
        					break;    
    					case DATE_CREATED:
    						//By standart required date in format YYYY:MM:DD HH:MM:SS
    						SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss"); ////YYYY:MM:DD HH:MM:SS
    						//dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
    						final String datetime = dateFormatGmt.format(newMetaData.getCreationTime());
    						logger.debug("Process date = " + datetime);
    					
    						exifDirectory.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
    						exifDirectory.add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL,datetime);    								
    						NeedUpdate = true;
    						break;
    					case APERTURE:
    						if ( getAsRational(newMetaData.getAperture()) != null )  {
	    		                exifDirectory.removeField(ExifTagConstants.EXIF_TAG_APERTURE_VALUE);
	    		                exifDirectory.add(ExifTagConstants.EXIF_TAG_APERTURE_VALUE, 
	    		                		getAsRational(newMetaData.getAperture())
	    		                		);
	    		                NeedUpdate = true;
    						}
    						break;
//    					case DISTANCE:
//    						if ( getAsRational(newMetaData.getDistance()) != null )  {
//	    		                exifDirectory.removeField(ExifTagConstants.EXIF_TAG_SUBJECT_DISTANCE);
//	    		                exifDirectory.add(ExifTagConstants.EXIF_TAG_SUBJECT_DISTANCE, 
//	    		                		getAsRational(newMetaData.getDistance())
//	    		                		);
//	    		                NeedUpdate = true;
//    						}
//    						break;
    					case EXPOSURE_TIME:
    						if ( getAsRational(newMetaData.getExposureTime()) != null )  {
	    						//  Convert to real rational
	    		                exifDirectory.removeField(ExifTagConstants.EXIF_TAG_EXPOSURE_TIME);
	    		                exifDirectory.add(ExifTagConstants.EXIF_TAG_SUBJECT_DISTANCE, 
	    		                		getAsRational(newMetaData.getExposureTime())
	    		                		);
	    		                NeedUpdate = true;  
    						}
    						break;
    					case FOCAL_LENGTH:
    						if ( getAsRational(newMetaData.getFocal()) != null )  {
	    		                exifDirectory.removeField(ExifTagConstants.EXIF_TAG_FOCAL_LENGTH);
	    		                exifDirectory.add(ExifTagConstants.EXIF_TAG_FOCAL_LENGTH, 
	    		                		getAsRational(newMetaData.getFocal())
	    		                		);
	    		                NeedUpdate = true;
    						}
    						break;
    					case FLASH:
    						//ExifTagConstants.FLASH_VALUE_FIRED:
            				//ExifTagConstants.FLASH_VALUE_NO_FLASH)
    		                exifDirectory.removeField(ExifTagConstants.EXIF_TAG_FLASH);
    		                exifDirectory.add(ExifTagConstants.EXIF_TAG_FLASH, 
    		                		newMetaData.getFlash().shortValue());
    		                NeedUpdate = true;
    						break;
    					case ISO_EQUIVALENT:
    		                exifDirectory.removeField(ExifTagConstants.EXIF_TAG_ISO);
    		                exifDirectory.add(ExifTagConstants.EXIF_TAG_ISO, 
    		                		(short) Short.parseShort(newMetaData.getIso())
    		                		);
    		                NeedUpdate = true;
    						break;
    					case UNIQUE_ID:
    					
    						final String uid = newMetaData.getUnicId().substring(0, 32);
    						
    						//final String uid = newMetaData.getUnicId();
    						//final String uid = t2;

    						//logger.debug("Store UNIC_ID value " + newMetaData.getUnicId());
    		                exifDirectory.removeField(ExifTagConstants.EXIF_TAG_IMAGE_UNIQUE_ID);
    		                exifDirectory.add(ExifTagConstants.EXIF_TAG_IMAGE_UNIQUE_ID,uid);
    		                NeedUpdate = true;
    						break;
    						
    					case GPS_LATITUDE:
    						latitude = newMetaData.getLatitude();
    						break;
    					case GPS_LONGITUDE:
    						longitude = newMetaData.getLongitude();
    						break;
    					default:	
    				}
    			}
    		} /// end for

    		if (( latitude != null) && (longitude != null)) {
    			outputSet.setGPSInDegrees(longitude, latitude);
    			NeedUpdate = true;
    		}
    					
            //----------------------------------------------------------------------
            //   Save updated tags.
            //----------------------------------------------------------------------
            if ( NeedUpdate) {
	            os = new FileOutputStream(tempFile);
	            os = new BufferedOutputStream(os);
	            
	            new ExifRewriter().updateExifMetadataLossless(photoObjectsFile, os, outputSet);
	            
	            //FileUtils.moveFile(tempFile, photoObjectsFile);
	            tempFile.renameTo(photoObjectsFile);
	            //Files.move(source, source.resolveSibling("newname"));
            }
            canThrow = true;
        } finally {
        	tempFile.delete();
            IOUtils.closeQuietly(os);
        }
	
	}
	
	
	
	
	
	private RationalNumber getAsRational(String numberStr) throws Exception{
				
		RationalNumber res = null;
	
		logger.debug("Parse rational for "+ numberStr+"");
		if ( numberStr.matches("\\d*/\\d*")) {
			int numerator =  Integer.parseInt(numberStr.substring(0,numberStr.indexOf('/')));
			int divisor = Integer.parseInt(numberStr.substring(numberStr.indexOf('/')+1));
			res = new RationalNumber(numerator,divisor);
		}
		//else if ( numberStr.contains(".1234567890") ) {
		else if ( numberStr.matches("\\d*\\.\\d*") ) {
			if (Double.valueOf(numberStr) == 0) return null;
			res = RationalNumber.valueOf(Double.valueOf(numberStr));
		}
		else if ( numberStr.matches("\\d*") ) {
			if (Double.valueOf(numberStr) == 0) return null;
			res = RationalNumber.valueOf(Double.valueOf(numberStr));
		}
		else {
			throw new Exception("Incorrect rational format");
		}
		
		return res;
		
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
	public List<PhotoObjectInt> listSubObjects() {
		List<PhotoObjectInt> list = null;
		
		if (isFolder() && hasPhotoSource()) {
			list = new ArrayList<PhotoObjectInt>();
			File[] filesAr = this.photoObjectsFile.listFiles();
			if (filesAr != null) {
				for (File curFile : filesAr) {
					if (curFile.canRead() && (! curFile.isHidden()) &&
							( isValidImage(curFile) ||  curFile.isDirectory())
							) {
						try {
							LocalPhotoObject item = new LocalPhotoObject(this.getConnector(),curFile.getAbsolutePath());
							list.add(item);		
						} catch (IOException ioe) {
							logger.warn("Wrong image format. Skiping. File " + photoObjectsFile.getAbsolutePath());
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

//	/**
//	 * @deprecated
//	 *   Return Input File Stream from source file object. 
//	 */
//	@Override
//	public InputStream getPhotoSource() throws IOException {
//		FileInputStream fis= null;
//		if (hasPhotoSource()) fis = new FileInputStream(photoObjectsFile);
//		else {
//			logger.warn("Access to source when source file not defined");
//		}
//		return fis;
//	}
	
	/**
	 * Return MediaObject for photo
	 * @throws IOException - file not found
	 */
	@Override
	public PhotoMediaObjectInt getMedia(EnumMediaType type) throws IOException {
		LocalMediaObject mediaFile = null;
		if ( photoObjectsFile != null) {
			mediaFile = new LocalMediaObject(this.getConnector(),photoObjectsFile,"PHOTO");
			mediaFile.setHeight(this.height);
			mediaFile.setWidth(this.width);
			mediaFile.setSize(this.size);
			mediaFile.setMimeType(getMimeType());

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

//	/**
//	 * @deprecated
//	 * Return input stream to thumbnail object scaled from source file
//	 * @throws IOException 
//	 */
//	@Override
//	public InputStream getThumbnailSource(Dimension dim) throws IOException {
//		InputStream is = null;
//		if ((! isFolder()) && hasPhotoSource()) {
//			ImageScaler scaller = new ImageScaler();
//			is = scaller.doScale(photoObjectsFile, dim);
//		}
//		else if (photoObjectsFile == null){
//			logger.warn("Access to thumbnail when source file not defined");
//		}
//		return is;
//	}
	/**
	 * Return MediaObject for thumbnail
	 */
	@Override
	public PhotoMediaObjectInt  getThumbnail(Dimension dim) throws IOException {
		LocalMediaObject mediaFile = new LocalMediaObject(
				this.getConnector(),
				photoObjectsFile,
				"THUMB"
				);
		
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
		
		mediaFile.setMimeType("image/png");
		mediaFile.setType(EnumMediaType.THUMB_FILE);
		return mediaFile;
	}
	
	public void delete() throws Exception{
		if ( photoObjectsFile.delete()) {
			 photoObjectsFile = null;
		}
		else throw new RuntimeException("Cannot delete file '"+getId() +"'");
	}
	
	
	/********************************************************************************
	 * 
	 *   Images file filter.
	 *   Return the true if file has extension as present in  extsArray.
	 *   
	 * @param theFile - the tested file
	 * @return
	 */
	public static boolean isValidImage(File theFile) {
		for (String ext: VALID_EXT_ARRAY) {
			if (theFile.getName().toUpperCase().endsWith(ext.toUpperCase())) {
				return true;
			}
		}
		return false;
	}
	
	
	
	
	
}

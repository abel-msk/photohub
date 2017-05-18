package home.abel.photohub.utils.image;


import home.abel.photohub.connector.BasePhotoMetadata;
import home.abel.photohub.connector.prototype.ExifMetadataTags;
import home.abel.photohub.connector.prototype.PhotoMetadataInt;
import org.apache.commons.imaging.Imaging;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;


public class ImageMetadataProcessor {

	
	final Logger logger = LoggerFactory.getLogger(ImageMetadataProcessor.class);
	private static String[] VALID_EXT_ARRAY = {"gif","tiff","jpg","jpeg","png"};
	private static String[] VALID_META_EXT_AR = {"gif","tiff","jpg","jpeg"};
	
	File photoObjectsFile = null;
	
	public ImageMetadataProcessor(File photoObjectsFile) {
		this.photoObjectsFile = photoObjectsFile;
	}
	
	
	public File getImgFile() {
		return photoObjectsFile;
	}
	
	
	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	public PhotoMetadataInt getMeta() throws Exception {
		if ( photoObjectsFile == null ) return null;
		BasePhotoMetadata metaObj = null;
		
		if (isValidImage(photoObjectsFile)) {
			try {
				ImageMetadataExtractor extractor= new ImageMetadataExtractor(photoObjectsFile);
				metaObj = extractor.loadMetadata();
			}
			catch (Exception e) {
				logger.warn("Cannot extract metadata. " + e.getMessage() ,e);
				metaObj = new BasePhotoMetadata();
			}
		}
		return metaObj;
	}
	
	
	/********************************************************************************
	 * 
	 */
	
	
	
	/**
	 * 
	 * @param newMetaData
	 * @return  New file witd saved metadata, or null if nothing to save
	 * @throws Exception
	 */
	public File setMeta(PhotoMetadataInt newMetaData) throws Exception {
  			
//		File tempFile = getUniqueFileName(
//				//photoObjectsFile.getParentFile().getAbsolutePath(),
//				System.getProperty("java.io.tmpdir"),
//				FilenameUtils.getExtension(photoObjectsFile.getName()));
		
		//File tempFile = photoObjectsFile;
		File tempFile = null;
		
		
        OutputStream os = null;
        boolean canThrow = false;
        
        try {
            TiffOutputSet outputSet = null;

            // note that metadata might be null if no metadata is found.
            final org.apache.commons.imaging.common.ImageMetadata metadata = Imaging.getMetadata(photoObjectsFile);
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
    		                logger.debug("Process UUID = " + uid);
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
            	try {
	       		  	tempFile = 
	       		  			File.createTempFile((new StringBuilder().append(new Date().getTime()).append(UUID.randomUUID())).toString(),
	       		  								FilenameUtils.getExtension(photoObjectsFile.getName()));
	       		  	
		            os = new FileOutputStream(tempFile);
		            os = new BufferedOutputStream(os);
		            
		            new ExifRewriter().updateExifMetadataLossless(photoObjectsFile, os, outputSet);
		            
		            //FileUtils.moveFile(tempFile, photoObjectsFile);
		            //tempFile.renameTo(photoObjectsFile);
		            //Files.move(source, source.resolveSibling("newname"));
            	}
            	catch (Exception e) {
            		logger.debug("[setMeta] Cannot write metadata. File="+ tempFile.getAbsolutePath());
            		tempFile.delete();
            		tempFile = null;
            		throw e;
            	}
            }
            canThrow = true;        	
        } finally {
        	//tempFile.delete();
            IOUtils.closeQuietly(os);
        }
        
        return tempFile;
	}
	
	
	/********************************************************************************
	 * 
	 */
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
	
	/********************************************************************************
	 * 
	 * @param directory
	 * @param extension
	 * @return
	 */
	private File getUniqueFileName(String directory, String extension) {
	    return new File(directory, 
	    		new StringBuilder().append(new Date().getTime()).append(UUID.randomUUID())
	    		.append(".")
	    		.append(extension).toString());
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
		
	public static boolean isImgHasMeta(File theFile) {
		for (String ext: VALID_META_EXT_AR) {
			if (theFile.getName().toUpperCase().endsWith(ext.toUpperCase())) {
				return true;
			}
		}
		return false;
	}
	
}

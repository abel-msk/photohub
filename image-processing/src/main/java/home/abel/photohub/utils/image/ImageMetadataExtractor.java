package home.abel.photohub.utils.image;


import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDescriptor;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import home.abel.photohub.connector.BasePhotoMetadata;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import java.util.Collection;

public class ImageMetadataExtractor  {
	//private static final long serialVersionUID = 4774888237028696522L;	
	final Logger logger = LoggerFactory.getLogger(ImageMetadataExtractor.class);

	private Metadata metadata;
	private boolean isLoaded = false;
	private GpsDirectory gpsDir = null;
	private ExifSubIFDDirectory subIFDD = null;
	private ExifSubIFDDescriptor subIFDDDescr = null;
	private ExifIFD0Directory IFD0 = null;
	private File inputFile = null;
	

	
	/**
	 * Scan image for metadata
	 * @param inputFile  Input image file
	 * @throws Exception
	 */
	public ImageMetadataExtractor(File inputFile) throws Exception {
		this.inputFile = inputFile;
		String fNameExt = FilenameUtils.getExtension(inputFile.getName());

		if  ((! fNameExt.equalsIgnoreCase("jpg")) && 
				(! fNameExt.equalsIgnoreCase("jpeg")) &&
				(! fNameExt.toUpperCase().startsWith("TIF"))
				) {
			throw new ImageProcessingException("Unsuported image format for extract metadata. File=" + inputFile.getName());
		}

		try {
			logger.info("Read image from file " + inputFile.getAbsolutePath());
			this.metadata = ImageMetadataReader.readMetadata(inputFile);
			//this.fileSize = new Long(inputFile.length());
			this.isLoaded = true;
		} 
		catch (com.drew.imaging.ImageProcessingException ipe) {
			logger.error("Read image error: " + ipe.getMessage());		
			throw new ImageProcessingException(ipe);
		}
		catch (Exception e) {
			logger.error("Read image error: "+ e.getMessage(),e);
			throw new ImageProcessingException(e);
		}
		
		
		try {


//            Directory directory
//                    = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
//            this.subIFDDDescr
//                    = new ExifSubIFDDescriptor(directory);


            this.subIFDD = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            this.subIFDDDescr  = new ExifSubIFDDescriptor(subIFDD);
            this.IFD0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);

            Collection<GpsDirectory> gpsDirectories = metadata.getDirectoriesOfType(GpsDirectory.class);

            for (GpsDirectory gpsDirectory : gpsDirectories) {
                // Try to read out the location, making sure it's non-zero
                GeoLocation geoLocation = gpsDirectory.getGeoLocation();
                if (geoLocation != null && !geoLocation.isZero()) {
                    this.gpsDir = gpsDirectory;
                    break;
                }
            }

			//this.gpsDir  = metadata.getDirectoriesOfType(GpsDirectory.class);

		}
		catch (NullPointerException npe) {
			logger.error("Read image metadata error: " + npe.getLocalizedMessage());	
			throw new ImageProcessingException(npe);
		}
	}

	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	public BasePhotoMetadata loadMetadata() throws Exception {
        BasePhotoMetadata matadataObject = new BasePhotoMetadata();
		return loadMetadata(matadataObject);
	}
	
	/**
	 * 
	 * @param theMetadata
	 * @return
	 * @throws Exception
	 */
	public BasePhotoMetadata loadMetadata(BasePhotoMetadata theMetadata) throws Exception{
		if ( ! isLoaded )  throw new Exception("Data not loaded.");
		
		if (gpsDir!= null ) {
			GeoLocation Loc = gpsDir.getGeoLocation();
			//if ( Loc != null ) {
				theMetadata.setLatitude(Loc.getLatitude());
				theMetadata.setLongitude(Loc.getLongitude());
                theMetadata.setAltitude(gpsDir.getString(GpsDirectory.TAG_ALTITUDE_REF));
			//}
			//else logger.debug("Image  :" +inputFile.getName()+ "  metaDir GpsDirectory local error.");
		}
		else {
			logger.debug("Image :" +inputFile.getName()+ "  metaDir GpsDirectory is empty");
		}
		if ( subIFDD != null ) {
			//theMetadata.setDigitTime(subIFDD.getDate(ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED));
			theMetadata.setCreationTime(subIFDD.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL));
			
			String tt = subIFDD.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);// (ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
			logger.debug("get exif tag creation date as string : " +tt );
		}
		else {
			logger.debug("Image :" +inputFile.getName()+ "  metaDir ExifSubIFDDirectory is empty");
		}


		if (subIFDDDescr != null) {
			if  (subIFDD.containsTag(ExifSubIFDDirectory.TAG_APERTURE)) {
//				theMetadata.setAperture(
//						String.valueOf(subIFDD.getRational(ExifSubIFDDirectory.TAG_APERTURE).getNumerator()) +"/" +
//						String.valueOf(subIFDD.getRational(ExifSubIFDDirectory.TAG_APERTURE).getDenominator())
//						);
				
				theMetadata.setAperture(String.valueOf(subIFDD.getDouble(ExifSubIFDDirectory.TAG_APERTURE)));
				//theMetadata.setAperture(subIFDDDescr.getDescription(ExifSubIFDDirectory.TAG_APERTURE));
			}
			
			//theMetadata.setExpMode(String.valueOf(subIFDD.getInt(ExifSubIFDDirectory.TAG_EXPOSURE_MODE)));
			
			if  (subIFDD.containsTag(ExifSubIFDDirectory.TAG_EXPOSURE_TIME)) {
				
				theMetadata.setExposureTime(
					String.valueOf(subIFDD.getRational(ExifSubIFDDirectory.TAG_EXPOSURE_TIME).getNumerator()) +"/" +
					String.valueOf(subIFDD.getRational(ExifSubIFDDirectory.TAG_EXPOSURE_TIME).getDenominator())
					);
					
				//theMetadata.setExposureTime(subIFDDDescr.getDescription(ExifSubIFDDirectory.TAG_EXPOSURE_TIME));
				//theMetadata.setExposureTime(String.valueOf(subIFDD.getDouble(ExifSubIFDDirectory.TAG_EXPOSURE_TIME)));
			}
			
			if  (subIFDD.containsTag(ExifSubIFDDirectory.TAG_FOCAL_LENGTH)) {
				//theMetadata.setFocal(subIFDDDescr.getDescription(ExifSubIFDDirectory.TAG_FOCAL_LENGTH));
				theMetadata.setFocal(String.valueOf(subIFDD.getDouble(ExifSubIFDDirectory.TAG_FOCAL_LENGTH)));
			}
			//theMetadata.setFocus(subIFDDDescr.getDescription(ExifSubIFDDirectory.TAG_F)); 
			
			// ???
			if  (subIFDD.containsTag(ExifSubIFDDirectory.TAG_SUBJECT_DISTANCE)) {
				//theMetadata.setDistance(subIFDDDescr.getDescription(ExifSubIFDDirectory.TAG_SUBJECT_DISTANCE)); 
				theMetadata.setDistance(String.valueOf(subIFDD.getLong(ExifSubIFDDirectory.TAG_SUBJECT_DISTANCE))); 
			}
			
			if  (subIFDD.containsTag(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT)) {
				//theMetadata.setIso(subIFDDDescr.getDescription(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT));
				theMetadata.setIso(String.valueOf(subIFDD.getInt(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT)));
			}
			
			if  (subIFDD.containsTag(ExifSubIFDDirectory.TAG_IMAGE_UNIQUE_ID)) {
				theMetadata.setUnicId(subIFDD.getString(ExifSubIFDDirectory.TAG_IMAGE_UNIQUE_ID,"UTF-8"));
			}
			//thePhoto.setDpi(String dpi) 

			if  (subIFDD.containsTag(ExifSubIFDDirectory.TAG_FLASH)) {
				theMetadata.setFlash(new Integer(subIFDD.getInt(ExifSubIFDDirectory.TAG_FLASH)));
			}
			
		}
		else {
			logger.debug("Image :" +inputFile.getName()+ "  metaDir ExifSubIFDDirectory is empty");
		}
		if (IFD0 != null ) {
			theMetadata.setCameraMake(IFD0.getString(ExifIFD0Directory.TAG_MAKE)); 
			theMetadata.setCameraModel(IFD0.getString(ExifIFD0Directory.TAG_MODEL));
		}
		else {
			logger.debug("Image :" +inputFile.getName()+ "  metaDir ExifIFD0Directory is empty");
		}
		
//		theMetadata.setDimH(int dimH) 
//		theMetadata.setDimW(int dimW) 
//		if (fileSize != null) {
//			theMetadata.setSize(fileSize.toString());
//		}
		
		return theMetadata;
		
	}

}



//URL imageURL;
//imageURL = new URL(sourcePath);
//logger.info("Load image from URL  " + imageURL.toString());		
//BufferedImage image = ImageIO.read(imageURL);
//
////  Save Image to the temp File
//tempFile = File.createTempFile("neiImage","gif");
//ImageIO.write(image, "JPEG", tempFile);		
//this.fileSize = new Long(tempFile.length());
//
//// Open Temp file and extract matadata
//BufferedInputStream bufferedTempFileStream = new BufferedInputStream (new FileInputStream(tempFile));
//this.metadata = ImageMetadataReader.readMetadata(bufferedTempFileStream, true);
//
//// Close stream and remove file
//bufferedTempFileStream.close();
//tempFile.delete();





package home.abel.photohub.utils.image;

import com.fasterxml.uuid.Generators;
import home.abel.photohub.connector.prototype.PhotoMetadataInt;
import org.apache.commons.imaging.common.RationalNumber;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffEpTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *   This class covers apace image io Tiff tags and implements PhotoMetadataInt for use with site connetor
 */
public class Metadata implements PhotoMetadataInt  {
    final static Logger logger = LoggerFactory.getLogger(Metadata.class);
    private  SimpleDateFormat exifFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

    private boolean changed = false;
    private TiffImageMetadata metadata = null;

    private String unicId = null;
    private Date dateOriginal = null;
    private Date dateCreated = null;
    private Date dateUpdate = null;
    private int tzOffset = 0;
    private String cameraMake = null;
    private String cameraModel = null;
    private int orientation = 0;
    private String software = null;
    private double resolution = 0;
    private int iso = 0;
    private double shutterSpeed = 0;
    private double aperture = 0;
    private double brightness = 0;
    private String gpsLatRef = null;
    private double latitude = 0;
    private String gpsLongRef = null;
    private double longitude = 0;
    private int flash = 0;
    private double exposureTime = 0;
    private double focalLength = 0;
    private double altitude = 0;
    private String userComment = null;



    private static final Map<Integer,String> orientationConsts;
    static {
        orientationConsts = new HashMap<>();
        orientationConsts.put(TiffTagConstants.ORIENTATION_VALUE_HORIZONTAL_NORMAL, "Horizontal normal");
        orientationConsts.put(TiffTagConstants.ORIENTATION_VALUE_MIRROR_HORIZONTAL, "Horizontal mirror");
        orientationConsts.put(TiffTagConstants.ORIENTATION_VALUE_ROTATE_180, "Rotate 180");
        orientationConsts.put(TiffTagConstants.ORIENTATION_VALUE_MIRROR_VERTICAL, "Vertical mirror");
        orientationConsts.put(TiffTagConstants.ORIENTATION_VALUE_MIRROR_HORIZONTAL_AND_ROTATE_270_CW, "Horizontal mirror rotate 270cw");
        orientationConsts.put(TiffTagConstants.ORIENTATION_VALUE_ROTATE_90_CW, "Rotate 90cw");
        orientationConsts.put(TiffTagConstants.ORIENTATION_VALUE_MIRROR_HORIZONTAL_AND_ROTATE_90_CW, "Horizontal mirror Rotate 90cw");
        orientationConsts.put(TiffTagConstants.ORIENTATION_VALUE_ROTATE_270_CW, "Rotate 90cw");
    }

    private static final Map<Integer,String> flashConsts;
    static {
        flashConsts = new HashMap<>();
        flashConsts.put(ExifTagConstants.FLASH_VALUE_FIRED, "Flash fired.");
        flashConsts.put(ExifTagConstants.FLASH_VALUE_FIRED_RETURN_NOT_DETECTED, "Flash fired.");
        flashConsts.put(ExifTagConstants.FLASH_VALUE_FIRED_RETURN_DETECTED, "Flash fired.");
        flashConsts.put(ExifTagConstants.FLASH_VALUE_ON_DID_NOT_FIRE, "Flash dud not fired.");
        flashConsts.put(ExifTagConstants.FLASH_VALUE_ON, "Flash on.");
        flashConsts.put(ExifTagConstants.FLASH_VALUE_ON_RETURN_NOT_DETECTED, "Flash on.");
        flashConsts.put(ExifTagConstants.FLASH_VALUE_ON_RETURN_DETECTED, "Flash on.");
        flashConsts.put(ExifTagConstants.FLASH_VALUE_OFF, "Flash off.");
        flashConsts.put(ExifTagConstants.FLASH_VALUE_OFF_DID_NOT_FIRE_RETURN_NOT_DETECTED, "Flash off. Did not fired.");
        flashConsts.put(ExifTagConstants.FLASH_VALUE_AUTO_DID_NOT_FIRE, "Flash auto. Did not fired.");
        flashConsts.put(ExifTagConstants.FLASH_VALUE_AUTO_FIRED, "Flash auto. Fired.");
        flashConsts.put(ExifTagConstants.FLASH_VALUE_AUTO_FIRED_RETURN_NOT_DETECTED, "Flash auto. Fired.");
        flashConsts.put(ExifTagConstants.FLASH_VALUE_AUTO_FIRED_RETURN_DETECTED, "Flash auto. Fired.");
        flashConsts.put(ExifTagConstants.FLASH_VALUE_NO_FLASH_FUNCTION, "No flash function.");
        flashConsts.put(ExifTagConstants.FLASH_VALUE_OFF_NO_FLASH_FUNCTION, "No flash function.");
        flashConsts.put(ExifTagConstants.FLASH_VALUE_FIRED_RED_EYE_REDUCTION, "Flash fired. Red eye reduction.");
        flashConsts.put(ExifTagConstants.FLASH_VALUE_FIRED_RED_EYE_REDUCTION_RETURN_NOT_DETECTED, "Flash fired. Red eye reduction.");
        flashConsts.put(ExifTagConstants.FLASH_VALUE_FIRED_RED_EYE_REDUCTION_RETURN_DETECTED, "Flash fired. Red eye reduction.");
        flashConsts.put(ExifTagConstants.FLASH_VALUE_OFF_RED_EYE_REDUCTION, "Flash off. Red eye reduction.");
        flashConsts.put(ExifTagConstants.FLASH_VALUE_AUTO_DID_NOT_FIRE_RED_EYE_REDUCTION, "Flash auto. Did not fired.");
        flashConsts.put(ExifTagConstants.FLASH_VALUE_AUTO_FIRED_RED_EYE_REDUCTION, "Flash auto. Fired. Red eye reduction.");
        flashConsts.put(ExifTagConstants.FLASH_VALUE_AUTO_FIRED_RED_EYE_REDUCTION_RETURN_NOT_DETECTED, "Flash auto. Fired. Red eye reduction.");
        flashConsts.put(ExifTagConstants.FLASH_VALUE_AUTO_FIRED_RED_EYE_REDUCTION_RETURN_DETECTED, "Flash auto. Fired. Red eye reduction.");
    }


    //   Tags description
    //   https://commons.apache.org/proper/commons-imaging/apidocs/org/apache/commons/imaging/formats/tiff/constants/ExifTagConstants.html
    //   https://commons.apache.org/proper/commons-imaging/apidocs/org/apache/commons/imaging/formats/tiff/constants/package-summary.html

    //getFocus
    //setDistance

    // TagInfoShort	EXIF_TAG_SATURATION_1
    // static TagInfoAscii	EXIF_TAG_SERIAL_NUMBER
    //static TagInfoGpsText	EXIF_TAG_USER_COMMENT

    //    0xa002	ExifImageWidth	int16u:	ExifIFD	(called PixelXDimension by the EXIF spec.)
    //    0xa003	ExifImageHeight	int16u:	ExifIFD	(called PixelYDimension by the EXIF spec.)


    public Metadata(TiffImageMetadata meta) {
        metadata = meta;
        exifFormat.setTimeZone(TimeZone.getTimeZone("GMT"));


        try {
            unicId =  metadata.findField(ExifTagConstants.EXIF_TAG_IMAGE_UNIQUE_ID).getStringValue();
        } catch (Exception e) {
            logger.warn("cannot read property " + ExifTagConstants.EXIF_TAG_IMAGE_UNIQUE_ID.name);
        }
        try {
            dateOriginal = exifFormat.parse(metadata.findField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL).getStringValue());
        } catch (Exception e) {
            logger.warn("cannot read property " + ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL.name);
        }
        try {
            dateCreated = exifFormat.parse(metadata.findField(ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED).getStringValue());
        } catch (Exception e) {
            logger.warn("cannot read property " + ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED.name);
        }
        try {
            dateUpdate = exifFormat.parse(metadata.findField(TiffTagConstants.TIFF_TAG_DATE_TIME).getStringValue());
        } catch (Exception e) {
            logger.warn("cannot read property " + TiffTagConstants.TIFF_TAG_DATE_TIME.name);
        }
        try {
            tzOffset = metadata.findField(TiffEpTagConstants.EXIF_TAG_TIME_ZONE_OFFSET).getIntValue();
        } catch (Exception e) {
            logger.warn("cannot read property " + TiffEpTagConstants.EXIF_TAG_TIME_ZONE_OFFSET.name);
        }
        try {
            cameraMake= metadata.findField(TiffTagConstants.TIFF_TAG_MAKE).getStringValue();
        } catch (Exception e) {
            logger.warn("cannot read property " + TiffTagConstants.TIFF_TAG_MAKE.name);
        }
        try {
            cameraModel = metadata.findField(TiffTagConstants.TIFF_TAG_MODEL).getStringValue();
        } catch (Exception e) {
            logger.warn("cannot read property " + TiffTagConstants.TIFF_TAG_MODEL.name);
        }
        try {
            orientation = metadata.findField(TiffTagConstants.TIFF_TAG_ORIENTATION).getIntValue();
        } catch (Exception e) {
            logger.warn("cannot read property " + TiffTagConstants.TIFF_TAG_ORIENTATION.name);
        }
        try {
            software = metadata.findField(TiffTagConstants.TIFF_TAG_SOFTWARE).getStringValue();
        } catch (Exception e) {
            logger.warn("cannot read property " + TiffTagConstants.TIFF_TAG_SOFTWARE.name);
        }
        try {
            userComment = metadata.findField(ExifTagConstants.EXIF_TAG_USER_COMMENT).getStringValue();
        } catch (Exception e) {
            logger.warn("cannot read property " + ExifTagConstants.EXIF_TAG_USER_COMMENT.name);
        }
        try {
            resolution = metadata.findField(TiffTagConstants.TIFF_TAG_XRESOLUTION).getDoubleValue();
        } catch (Exception e) {
            logger.warn("cannot read property " + TiffTagConstants.TIFF_TAG_XRESOLUTION.name);
        }
        try {
            iso = metadata.findField(ExifTagConstants.EXIF_TAG_ISO).getIntValue();
        } catch (Exception e) {
            logger.warn("cannot read property " + ExifTagConstants.EXIF_TAG_ISO.name);
        }
        try {
            flash = metadata.findField(ExifTagConstants.EXIF_TAG_FLASH).getIntValue();
        } catch (Exception e) {
            logger.warn("cannot read property " + ExifTagConstants.EXIF_TAG_FLASH.name);
        }
        try {
            shutterSpeed =  metadata.findField(ExifTagConstants.EXIF_TAG_SHUTTER_SPEED_VALUE).getDoubleValue();
        } catch (Exception e) {
            logger.warn("cannot read property " + ExifTagConstants.EXIF_TAG_SHUTTER_SPEED_VALUE.name);
        }
        try {
            aperture = metadata.findField(ExifTagConstants.EXIF_TAG_APERTURE_VALUE).getDoubleValue();
        } catch (Exception e) {
            logger.warn("cannot read property " + ExifTagConstants.EXIF_TAG_APERTURE_VALUE.name);
        }
        try {
            brightness = metadata.findField(ExifTagConstants.EXIF_TAG_BRIGHTNESS_VALUE).getDoubleValue();
        } catch (Exception e) {
            logger.warn("cannot read property " + ExifTagConstants.EXIF_TAG_BRIGHTNESS_VALUE.name);
        }

        try {
            exposureTime = metadata.findField(ExifTagConstants.EXIF_TAG_EXPOSURE_TIME).getDoubleValue();
        } catch (Exception e) {
            logger.warn("cannot read property " + ExifTagConstants.EXIF_TAG_EXPOSURE_TIME.name);
        }

        try {
            focalLength = metadata.findField(ExifTagConstants.EXIF_TAG_FOCAL_LENGTH).getDoubleValue();
        } catch (Exception e) {
            logger.warn("cannot read property " + ExifTagConstants.EXIF_TAG_FOCAL_LENGTH.name);
        }


        try {
            gpsLatRef = metadata.findField(GpsTagConstants.GPS_TAG_GPS_LATITUDE_REF).getStringValue();
        } catch (Exception e) {
            logger.warn("cannot read property " + GpsTagConstants.GPS_TAG_GPS_LATITUDE_REF.name);
        }
        try {
            latitude = metadata.findField(GpsTagConstants.GPS_TAG_GPS_LATITUDE).getDoubleValue();
        } catch (Exception e) {
            logger.warn("cannot read property " + GpsTagConstants.GPS_TAG_GPS_LATITUDE.name);
        }
        try {
            gpsLongRef =   metadata.findField(GpsTagConstants.GPS_TAG_GPS_LONGITUDE_REF).getStringValue();
        } catch (Exception e) {
            logger.warn("cannot read property " + GpsTagConstants.GPS_TAG_GPS_LONGITUDE_REF.name);
        }
        try {
            longitude  =   metadata.findField(GpsTagConstants.GPS_TAG_GPS_LONGITUDE).getDoubleValue();
        } catch (Exception e) {
            logger.warn("cannot read property " + GpsTagConstants.GPS_TAG_GPS_LONGITUDE.name);
        }

    }

    /**
     * Return map of possible values for ExifTagConstants.EXIF_TAG_FLASH  exif tag with human readable descriptions
     * @return
     */
    public static Map<Integer,String> getFlashDescr() {
        return flashConsts;
    }

    /**
     *  Return map of possible values for TiffTagConstants.TIFF_TAG_ORIENTATION exif tag with human readable descriptions
     * @return
     */
    public static Map<Integer,String> getOrientationDescr() {
        return orientationConsts;
    }


    /**
     * Return updated exif directory in output format (ready for insert to the tiff/jpeg image)
     * @return
     */
    public  TiffOutputSet saveOutputSet() {
        TiffOutputSet out = null;
        try {
            if (metadata != null) {
                out = metadata.getOutputSet();
            } else {
                out = new TiffOutputSet();
            }

            TiffOutputDirectory rootDirectory = out.getOrCreateRootDirectory();
            TiffOutputDirectory exifDirectory = out.getOrCreateExifDirectory();
            TiffOutputDirectory gpsDirectory = out.getOrCreateGPSDirectory();

            if (getUnicId() != null) {
                try {
                    //logger.debug("[Metadata.saveOutputSet] write uuid "+ getUnicId());
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_IMAGE_UNIQUE_ID);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_IMAGE_UNIQUE_ID, getUnicId());
                } catch (Exception e) {
                    logger.warn("cannot update value for tag " + ExifTagConstants.EXIF_TAG_IMAGE_UNIQUE_ID.name, e);
                }
            }

            if (getDateOriginal() != null) {
                try {
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, exifFormat.format(getDateOriginal()));
                } catch (Exception e) {
                    logger.warn("cannot update value for tag " + ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL.name, e);
                }
            }

            if (getDateCreated() != null) {
                try {
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED, exifFormat.format(getDateCreated()));
                } catch (Exception e) {
                    logger.warn("cannot update value for tag " + ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED.name, e);
                }
            }

            if (getDateUpdate() != null) {
                try {
                    rootDirectory.removeField(TiffTagConstants.TIFF_TAG_DATE_TIME);
                    rootDirectory.add(TiffTagConstants.TIFF_TAG_DATE_TIME, exifFormat.format(getDateUpdate()));
                } catch (Exception e) {
                    logger.warn("cannot update value for tag " + TiffTagConstants.TIFF_TAG_DATE_TIME.name, e);
                }
            }

            if (getTzOffset() != 0 ) {
                try {
                    exifDirectory.removeField(TiffEpTagConstants.EXIF_TAG_TIME_ZONE_OFFSET);
                    exifDirectory.add(TiffEpTagConstants.EXIF_TAG_TIME_ZONE_OFFSET, new Integer(getTzOffset()).shortValue());
                } catch (Exception e) {
                    logger.warn("cannot update value for tag " + TiffEpTagConstants.EXIF_TAG_TIME_ZONE_OFFSET.name, e);
                }
            }

            if (getCameraMake() != null) {
                try {
                    rootDirectory.removeField(TiffTagConstants.TIFF_TAG_MAKE);
                    rootDirectory.add(TiffTagConstants.TIFF_TAG_MAKE, getCameraMake());
                } catch (Exception e) {
                    logger.warn("cannot update value for tag " + TiffTagConstants.TIFF_TAG_MAKE.name, e);
                }
            }

            if ( getCameraMake() != null) {
                try {
                    rootDirectory.removeField(TiffTagConstants.TIFF_TAG_MODEL);
                    rootDirectory.add(TiffTagConstants.TIFF_TAG_MODEL, getCameraMake());
                } catch (Exception e) {
                    logger.warn("cannot update value for tag " + TiffTagConstants.TIFF_TAG_MODEL.name, e);
                }
            }

            if (getOrientation() != 0 ) {
                try {
                    rootDirectory.removeField(TiffTagConstants.TIFF_TAG_ORIENTATION);
                    rootDirectory.add(TiffTagConstants.TIFF_TAG_ORIENTATION, new Integer(getOrientation()).shortValue());
                } catch (Exception e) {
                    logger.warn("cannot update value for tag " + TiffTagConstants.TIFF_TAG_ORIENTATION.name, e);
                }
            }

            if (getSoftware() != null ) {
                try {
                    rootDirectory.removeField(TiffTagConstants.TIFF_TAG_SOFTWARE);
                    rootDirectory.add(TiffTagConstants.TIFF_TAG_SOFTWARE, getSoftware());
                } catch (Exception e) {
                    logger.warn("cannot update value for tag " + TiffTagConstants.TIFF_TAG_SOFTWARE.name, e);
                }
            }

            if (  getUserComment() != null ) {
                try {
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_USER_COMMENT);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_USER_COMMENT, getUserComment());
                } catch (Exception e) {
                    logger.warn("cannot update value for tag " + ExifTagConstants.EXIF_TAG_USER_COMMENT.name, e);
                }
            }

            if  ( getResolution() != 0) {
                try {
                    rootDirectory.removeField(TiffTagConstants.TIFF_TAG_XRESOLUTION);
                    rootDirectory.add(TiffTagConstants.TIFF_TAG_XRESOLUTION, RationalNumber.valueOf(getResolution()));
                } catch (Exception e) {
                    logger.warn("cannot update value for tag " + TiffTagConstants.TIFF_TAG_XRESOLUTION.name, e);
                }
            }

            if ( getIso() != 0) {
                try {
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_ISO);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_ISO, new Integer(getIso()).shortValue());
                } catch (Exception e) {
                    logger.warn("cannot update value for tag " + ExifTagConstants.EXIF_TAG_ISO.name, e);
                }
            }

            if ( getFlash() != 0 ) {
                try {
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_FLASH);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_FLASH, new Integer(getFlash()).shortValue());
                } catch (Exception e) {
                    logger.warn("cannot update value for tag " + ExifTagConstants.EXIF_TAG_FLASH.name, e);
                }
            }

            if ( getShutterSpeed() != 0) {
                try {
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_SHUTTER_SPEED_VALUE);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_SHUTTER_SPEED_VALUE, RationalNumber.valueOf(getShutterSpeed()));
                } catch (Exception e) {
                    logger.warn("cannot update value for tag " + ExifTagConstants.EXIF_TAG_ISO.name, e);
                }
            }

            if ( getAperture() != 0) {
                try {
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_APERTURE_VALUE);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_APERTURE_VALUE, RationalNumber.valueOf(getAperture()));
                } catch (Exception e) {
                    logger.warn("cannot update value for tag " + ExifTagConstants.EXIF_TAG_APERTURE_VALUE.name, e);
                }
            }

            if (getBrightness() != 0) {
                try {
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_BRIGHTNESS_VALUE);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_BRIGHTNESS_VALUE, RationalNumber.valueOf(getBrightness()));
                } catch (Exception e) {
                    logger.warn("cannot update value for tag " + ExifTagConstants.EXIF_TAG_BRIGHTNESS_VALUE.name, e);
                }
            }

            if ( getExposureTime() != 0) {
                try {
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_EXPOSURE_TIME);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_EXPOSURE_TIME, RationalNumber.valueOf(getExposureTime()));
                } catch (Exception e) {
                    logger.warn("cannot update value for tag " + ExifTagConstants.EXIF_TAG_EXPOSURE_TIME.name, e);
                }
            }

            if ( getFocalLength() != 0) {
                try {
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_FOCAL_LENGTH);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_FOCAL_LENGTH, RationalNumber.valueOf(getFocalLength()));
                } catch (Exception e) {
                    logger.warn("cannot update value for tag " + ExifTagConstants.EXIF_TAG_FOCAL_LENGTH.name, e);
                }
            }

            if ( getAltitude() != 0) {
                try {
                    gpsDirectory.removeField(GpsTagConstants.GPS_TAG_GPS_ALTITUDE);
                    gpsDirectory.add(GpsTagConstants.GPS_TAG_GPS_ALTITUDE, RationalNumber.valueOf(getAltitude()));
                } catch (Exception e) {
                    logger.warn("cannot update value for tag " + GpsTagConstants.GPS_TAG_GPS_ALTITUDE.name, e);
                }
            }

            if (( getLongitude() != 0 ) && (getLatitude() != 0)) {
                try {
                    out.setGPSInDegrees(getLongitude(), getLatitude());
                } catch (Exception e) {
                    logger.warn("Cannot update value for GPS tags ", e);
                }
            }

//
//            try {
//                exifDirectory.removeField(GpsTagConstants.GPS_TAG_GPS_LATITUDE_REF);
//                exifDirectory.add(GpsTagConstants.GPS_TAG_GPS_LATITUDE_REF, getGpsLattRef());
//            } catch (Exception e) {
//                logger.warn("cannot update value for tag " + GpsTagConstants.GPS_TAG_GPS_LATITUDE_REF.name, e);
//            }
//
//            try {
//                exifDirectory.removeField(GpsTagConstants.GPS_TAG_GPS_LATITUDE);
//                exifDirectory.add(GpsTagConstants.GPS_TAG_GPS_LATITUDE, RationalNumber.valueOf(getGpsLatt()));
//            } catch (Exception e) {
//                logger.warn("cannot update value for tag " + GpsTagConstants.GPS_TAG_GPS_LATITUDE.name, e);
//            }
//
//            try {
//                exifDirectory.removeField(GpsTagConstants.GPS_TAG_GPS_LONGITUDE_REF);
//                exifDirectory.add(GpsTagConstants.GPS_TAG_GPS_LONGITUDE_REF, getGpsLattRef());
//            } catch (Exception e) {
//                logger.warn("cannot update value for tag " + GpsTagConstants.GPS_TAG_GPS_LONGITUDE_REF.name, e);
//            }
//
//            try {
//                exifDirectory.removeField(GpsTagConstants.GPS_TAG_GPS_LONGITUDE);
//                exifDirectory.add(GpsTagConstants.GPS_TAG_GPS_LONGITUDE, RationalNumber.valueOf(getGpsLatt()));
//            } catch (Exception e) {
//                logger.warn("cannot update value for tag " + GpsTagConstants.GPS_TAG_GPS_LONGITUDE.name, e);
//            }

        }
        catch (Exception e) {
            throw new ExceptionMetadataProcess("Cannot prepare metadata for update.",e);
        }
        logger.debug("[Metadata.saveOutputSet] Prepare metadata output set.");
        return out;
    }


    /**
     *   Generate and return UnicId string
     *
     * @return
     */
    public static String generateUUID() {
        UUID uuid = Generators.timeBasedGenerator().generate();
        //logger.debug("Generate UUID "+uuid );
        ByteArrayOutputStream os = new  ByteArrayOutputStream();

        for (Byte b : uuid.toString().getBytes()) {
            if ( b != '-') {
                os.write(b);
            }
        }
        try {
            os.close();
        } catch ( Exception e){
            logger.debug("[ImageData.generateUUID]  cannot write to memory ",e);
            return null;
        }
        return os.toString();

//
//
//        String theID  = uuid.toString().replace('-', '\0');
//        return theID;
    }

    public static String getFlashRef (int flashValue) {
        return flashConsts.get(flashValue);
    }


    public static String getOrientationRef(int Orientation) {
        return  orientationConsts.get(Orientation);
    }




    @Override
    public String getUnicId() {
        return unicId;
    }
    @Override
    public void setUnicId(String unicId) {
        this.unicId = unicId;
        changed = true;
    }
    @Override
    public Date getDateOriginal() {
        return dateOriginal;
    }
    @Override
    public void setDateOriginal(Date dateOriginal) {
        this.dateOriginal = dateOriginal;
        changed = true;
    }
    @Override
    public Date getDateCreated() {
        return dateCreated;
    }
    @Override
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
        changed = true;
    }
    @Override
    public Date getDateUpdate() {
        return dateUpdate;
    }
    @Override
    public void setDateUpdate(Date dateUpdate) {
        this.dateUpdate = dateUpdate;
        changed = true;
    }
    @Override
    public int getTzOffset() {
        return tzOffset;
    }
    @Override
    public void setTzOffset(int tzOffset) {
        this.tzOffset = tzOffset;
        changed = true;
    }
    @Override
    public String getCameraMake() {
        return cameraMake;
    }
    @Override
    public void setCameraMake(String camMake) {
        this.cameraMake = camMake;
    }
    @Override
    public String getCameraModel() {
        return cameraModel;
    }
    @Override
    public void setCameraModel(String camModel) {
        this.cameraModel = camModel;
        changed = true;
    }
    @Override
    public int getOrientation() {
        return orientation;
    }
    @Override
    public void setOrientation(int orientation) {
        this.orientation = orientation;
        changed = true;
    }

    @Override
    public String getSoftware() {
        return software;
    }
    @Override
    public void setSoftware(String software) {
        this.software = software;
        changed = true;
    }
    @Override
    public double getResolution() {
        return resolution;
    }
    @Override
    public void setResolution(double resolution) {
        this.resolution = resolution;
        changed = true;
    }
    @Override
    public int getIso() {
        return iso;
    }
    @Override
    public void setIso(int iso) {
        this.iso = iso;
        changed = true;
    }
    @Override
    public double getShutterSpeed() {
        return shutterSpeed;
    }
    @Override
    public void setShutterSpeed(double shutterSpeed) {
        this.shutterSpeed = shutterSpeed;
        changed = true;
    }
    @Override
    public double getAperture() {
        return aperture;
    }
    @Override
    public void setAperture(double aperture) {
        this.aperture = aperture;
        changed = true;
    }
    @Override
    public double getBrightness() {
        return brightness;
    }
    @Override
    public void setBrightness(double brightness) {
        this.brightness = brightness;
        changed = true;
    }

    public String getGpsLattRef() {
        return gpsLatRef;
    }

    public void setGpsLattRef(String gpsLattRef) {
        this.gpsLatRef = gpsLattRef;
        changed = true;
    }
    @Override
    public double getLatitude() {
        return latitude;
    }
    @Override
    public void setLatitude(double lattitude) {
        this.latitude = lattitude;
        changed = true;
    }

    public String getGpsLongRef() {
        return gpsLongRef;
    }

    public void setGpsLongRef(String gpsLongRef) {
        this.gpsLongRef = gpsLongRef;
        changed = true;
    }

    @Override
    public double getLongitude() {
        return longitude;
    }
    @Override
    public void setLongitude(double longitude) {
        this.longitude = longitude;
        changed = true;
    }


    public boolean isChanged() {
        return changed;
    }
    public void setChanged(boolean changed) {
        this.changed = changed;
    }


    @Override
    public int getFlash() {
        return flash;
    }
    @Override
    public void setFlash(int flash) {
        this.flash = flash;
        changed = true;
    }

    @Override
    public double getExposureTime() {
        return exposureTime;
    }
    @Override
    public void setExposureTime(double exposureTime) {
        this.exposureTime = exposureTime;
        changed = true;
    }
    @Override
    public double getFocalLength() {
        return focalLength;
    }
    @Override
    public void setFocalLength(double focalLength) {
        this.focalLength = focalLength;
        changed = true;
    }

    @Override
    public double getAltitude() {
        return altitude;
    }
    @Override
    public void setAltitude(double altitude) {
        this.altitude = altitude;
        changed = true;
    }

    @Override
    public String getUserComment() {
        return userComment;
    }
    @Override
    public void setUserComment(String userComment) {
        this.userComment = userComment;
    }
}

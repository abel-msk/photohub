package home.abel.photohub.utils.image;

import com.fasterxml.uuid.Generators;
import home.abel.photohub.connector.prototype.PhotoMetadataInt;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.common.RationalNumber;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.*;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputField;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;


/*
https://commons.apache.org/proper/commons-imaging/apidocs/org/apache/commons/imaging/formats/tiff/constants/TiffTagConstants.html


static TagInfoAscii	TIFF_TAG_IMAGE_DESCRIPTION
static TagInfoShortOrLong	TIFF_TAG_IMAGE_LENGTH
static TagInfoShortOrLong	TIFF_TAG_IMAGE_WIDTH

static TagInfoAscii	TIFF_TAG_DATE_TIME
static TagInfoAscii	TIFF_TAG_DOCUMENT_NAME
static TagInfoAscii	TIFF_TAG_DOCUMENT_NAME
static TagInfoAscii	TIFF_TAG_DATE_TIME


static TagInfoRationals	TIFF_TAG_WHITE_POINT






TIFF_TAG_XRESOLUTION
TIFF_TAG_YRESOLUTION


static TagInfoShort	TIFF_TAG_RESOLUTION_UNIT    ++
	static int	RESOLUTION_UNIT_VALUE_CM    ++
	static int	RESOLUTION_UNIT_VALUE_INCHES    ++
	static int	RESOLUTION_UNIT_VALUE_NONE    ++



https://commons.apache.org/proper/commons-imaging/apidocs/index.html


static TagInfoAscii	EXIF_TAG_SMOOTHNESS     ++

static TagInfoShort	EXIF_TAG_WHITE_BALANCE_1    ++
static TagInfoAscii	EXIF_TAG_WHITE_BALANCE_2    ++
static int	WHITE_BALANCE_1_VALUE_AUTO    ++
static int	WHITE_BALANCE_1_VALUE_MANUAL  ++

static TagInfoShort	EXIF_TAG_SHARPNESS_1  ++
static TagInfoAscii	EXIF_TAG_SHARPNESS_2  ++
static int	SHARPNESS_1_VALUE_HARD    ++
static int	SHARPNESS_1_VALUE_NORMAL  ++
static int	SHARPNESS_1_VALUE_SOFT   ++



static TagInfoRationals	EXIF_TAG_FNUMBER  ++


Transformaion
https://stackoverflow.com/questions/5905868/how-to-rotate-jpeg-images-based-on-the-orientation-metadata/26130136


 */


/**
 *   This class covers apace image io Tiff tags and implements PhotoMetadataInt for use with site connetor
 */
public class Metadata implements PhotoMetadataInt  {
    final static Logger logger = LoggerFactory.getLogger(Metadata.class);
    private  SimpleDateFormat exifFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

    private boolean changed = false;
    private TiffImageMetadata metadata = null;
    private TiffOutputSet outputSet = null;

    private String unicId = null;
    private Date dateOriginal = null;
    private Date dateCreated = null;
    private Date dateUpdate = null;
    private int tzOffset = 0;
    private String cameraMake = null;
    private String cameraModel = null;
    private int orientation = -1;
    private String software = null;
    private int iso = 0;
    private double shutterSpeed = 0;
    private double aperture = 0;
    private double brightness = 0;
    private String gpsLatRef = null;
    private double latitude = 0;
    private String gpsLongRef = null;
    private double longitude = 0;
    private int flash = -1;
    private double exposureTime = 0;
    private int exposureMode = -1;
    private int exposureProgram = -1;
    private double focalLength = 0;
    private double altitude = 0;
    private String userComment = null;
    private int lightSource = 0;
    private double fNumber = 0;
    private int sharpness = -1;
    private String sharpnessRef = null;
    private int whiteBalance = -1;
    private String whiteBalanceRef = null;
    private String smoothness = null;
    private int saturation = -1;
    private String saturationRef = null;
    private int resolutionUnit = -1;
    private double xResolution = -1;
    private double yResolution = -1;






    private static final Map<Integer,String> orientationConsts = new HashMap<>();
    static {
        orientationConsts.put(TiffTagConstants.ORIENTATION_VALUE_HORIZONTAL_NORMAL, "Horizontal normal");
        orientationConsts.put(TiffTagConstants.ORIENTATION_VALUE_MIRROR_HORIZONTAL, "Horizontal mirror");
        orientationConsts.put(TiffTagConstants.ORIENTATION_VALUE_ROTATE_180, "Rotate 180");
        orientationConsts.put(TiffTagConstants.ORIENTATION_VALUE_MIRROR_VERTICAL, "Vertical mirror");
        orientationConsts.put(TiffTagConstants.ORIENTATION_VALUE_MIRROR_HORIZONTAL_AND_ROTATE_270_CW, "Horizontal mirror rotate 270cw");
        orientationConsts.put(TiffTagConstants.ORIENTATION_VALUE_ROTATE_90_CW, "Rotate 90cw");
        orientationConsts.put(TiffTagConstants.ORIENTATION_VALUE_MIRROR_HORIZONTAL_AND_ROTATE_90_CW, "Horizontal mirror Rotate 90cw");
        orientationConsts.put(TiffTagConstants.ORIENTATION_VALUE_ROTATE_270_CW, "Rotate 90cw");
    }

    private static final Map<Integer,String> flashConsts = new HashMap<>();
    static {
        flashConsts.put(0, "No flash");
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


    private static final Map<Integer,String> expoConsts =  new HashMap<>();;
    static {
        expoConsts.put(ExifTagConstants.EXPOSURE_MODE_VALUE_AUTO, "Auto");
        expoConsts.put(ExifTagConstants.EXPOSURE_MODE_VALUE_AUTO_BRACKET, "Auto bracket");
        expoConsts.put(ExifTagConstants.EXPOSURE_MODE_VALUE_MANUAL, "Manual");
    }

    private static final Map<Integer,String> expoProgConsts =  new HashMap<>();;
    static {
        expoConsts.put(0, "Not Defined");
        expoConsts.put(ExifTagConstants.EXPOSURE_PROGRAM_VALUE_ACTION_HIGH_SPEED, "Action");
        expoConsts.put(ExifTagConstants.EXPOSURE_PROGRAM_VALUE_APERTURE_PRIORITY_AE, "Aperture-priority");
        expoConsts.put(ExifTagConstants.EXPOSURE_PROGRAM_VALUE_CREATIVE_SLOW_SPEED, "Creative");
        expoConsts.put(ExifTagConstants.EXPOSURE_PROGRAM_VALUE_LANDSCAPE, "Landscape");
        expoConsts.put(ExifTagConstants.EXPOSURE_PROGRAM_VALUE_MANUAL, "Manual");
        expoConsts.put(ExifTagConstants.EXPOSURE_PROGRAM_VALUE_PORTRAIT, "Portrait");
        expoConsts.put(ExifTagConstants.EXPOSURE_PROGRAM_VALUE_PROGRAM_AE,"Program");
        expoConsts.put(ExifTagConstants.EXPOSURE_PROGRAM_VALUE_APERTURE_PRIORITY_AE, "Auto bracket");
        expoConsts.put(ExifTagConstants.EXPOSURE_PROGRAM_VALUE_SHUTTER_SPEED_PRIORITY_AE, "Shutter speed priority");
    }

    private static final Map<Integer,String> lightSrcConsts =  new HashMap<>();
    static {
        lightSrcConsts.put(0,"Unknown");
        lightSrcConsts.put(ExifTagConstants.LIGHT_SOURCE_VALUE_CLOUDY,"Cloudy");
        lightSrcConsts.put(ExifTagConstants.LIGHT_SOURCE_VALUE_COOL_WHITE_FLUORESCENT,"Cool White Fluorescent");
        lightSrcConsts.put(ExifTagConstants.LIGHT_SOURCE_VALUE_D50,"D50");
        lightSrcConsts.put(ExifTagConstants.LIGHT_SOURCE_VALUE_D55,"D55");
        lightSrcConsts.put(ExifTagConstants.LIGHT_SOURCE_VALUE_D65,"D65");
        lightSrcConsts.put(ExifTagConstants.LIGHT_SOURCE_VALUE_D75,"D75");
        lightSrcConsts.put(ExifTagConstants.LIGHT_SOURCE_VALUE_DAY_WHITE_FLUORESCENT,"Day White Fluorescent");
        lightSrcConsts.put(ExifTagConstants.LIGHT_SOURCE_VALUE_DAYLIGHT,"Daylight");
        lightSrcConsts.put(ExifTagConstants.LIGHT_SOURCE_VALUE_DAYLIGHT_FLUORESCENT,"Daylight Fluorescent");
        lightSrcConsts.put(ExifTagConstants.LIGHT_SOURCE_VALUE_FINE_WEATHER,"Fine Weather");
        lightSrcConsts.put(ExifTagConstants.LIGHT_SOURCE_VALUE_FLASH,"Flash");
        lightSrcConsts.put(ExifTagConstants.LIGHT_SOURCE_VALUE_FLUORESCENT,"Fluorescent");
        lightSrcConsts.put(ExifTagConstants.LIGHT_SOURCE_VALUE_ISO_STUDIO_TUNGSTEN,"ISO Studio Tungsten");
        lightSrcConsts.put(ExifTagConstants.LIGHT_SOURCE_VALUE_OTHER,"Other");
        lightSrcConsts.put(ExifTagConstants.LIGHT_SOURCE_VALUE_SHADE,"Shade");
        lightSrcConsts.put(ExifTagConstants.LIGHT_SOURCE_VALUE_STANDARD_LIGHT_A,"Standard Light A");
        lightSrcConsts.put(ExifTagConstants.LIGHT_SOURCE_VALUE_STANDARD_LIGHT_B,"Standard Light B");
        lightSrcConsts.put(ExifTagConstants.LIGHT_SOURCE_VALUE_STANDARD_LIGHT_C,"Standard Light C");
        lightSrcConsts.put(ExifTagConstants.LIGHT_SOURCE_VALUE_TUNGSTEN,"Tungsten (Incandescent)");
        lightSrcConsts.put(ExifTagConstants.LIGHT_SOURCE_VALUE_WHITE_FLUORESCENT,"White Fluorescent");
    }

    private static final Map<Integer,String> sharpnessConsts =  new HashMap<>();
    static {
        sharpnessConsts.put(-1,"Unknown");
        sharpnessConsts.put(ExifTagConstants.SHARPNESS_1_VALUE_HARD,"Hard");
        sharpnessConsts.put(ExifTagConstants.SHARPNESS_1_VALUE_NORMAL,"Normal");
        sharpnessConsts.put(ExifTagConstants.SHARPNESS_1_VALUE_SOFT,"Soft");
    }

    private static final Map<Integer,String> whiteBalanceConsts =  new HashMap<>();
    static {
        whiteBalanceConsts.put(-1,"Unknown");
        whiteBalanceConsts.put(ExifTagConstants.WHITE_BALANCE_1_VALUE_AUTO,"Auto");
        whiteBalanceConsts.put(ExifTagConstants.WHITE_BALANCE_1_VALUE_MANUAL,"Manual");
    }


    private static final Map<Integer,String> resolutionUnitConsts =  new HashMap<>();
    static {
        resolutionUnitConsts.put(-1,"Unknown");
        resolutionUnitConsts.put(TiffTagConstants.RESOLUTION_UNIT_VALUE_CM,"Centimeters");
        resolutionUnitConsts.put(TiffTagConstants.RESOLUTION_UNIT_VALUE_INCHES,"Inches");
        resolutionUnitConsts.put(TiffTagConstants.RESOLUTION_UNIT_VALUE_NONE,"None");
    }

    private static final Map<Integer,String> saturationConsts =  new HashMap<>();
    static {
        saturationConsts.put(-1,"Unknown");
        saturationConsts.put(ExifTagConstants.SATURATION_1_VALUE_HIGH,"High");
        saturationConsts.put(ExifTagConstants.SATURATION_1_VALUE_LOW,"Low");
        saturationConsts.put(ExifTagConstants.SATURATION_1_VALUE_NORMAL,"Normal");
    }


    //   Tags description
    //   https://commons.apache.org/proper/commons-imaging/apidocs/org/apache/commons/imaging/formats/tiff/constants/ExifTagConstants.html
    //   https://commons.apache.org/proper/commons-imaging/apidocs/org/apache/commons/imaging/formats/tiff/constants/package-summary.html

    //getFocus
    //setDistance

    // TagInfoShort	EXIF_TAG_SATURATION_1
    // https://commons.apache.org/proper/commons-imaging/apidocs/org/apache/commons/imaging/formats/tiff/constants/ExifTagConstants.html
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
            logger.info("Cannot read property " + ExifTagConstants.EXIF_TAG_IMAGE_UNIQUE_ID.name);
        }
        try {
            dateOriginal = exifFormat.parse(metadata.findField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL).getStringValue());
        } catch (Exception e) {
            logger.info("Cannot read property " + ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL.name);
        }
        try {
            dateCreated = exifFormat.parse(metadata.findField(ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED).getStringValue());
        } catch (Exception e) {
            logger.info("Cannot read property " + ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED.name);
        }
        try {
            dateUpdate = exifFormat.parse(metadata.findField(TiffTagConstants.TIFF_TAG_DATE_TIME).getStringValue());
        } catch (Exception e) {
            logger.info("Cannot read property " + TiffTagConstants.TIFF_TAG_DATE_TIME.name);
        }
        try {
            tzOffset = metadata.findField(TiffEpTagConstants.EXIF_TAG_TIME_ZONE_OFFSET).getIntValue();
        } catch (Exception e) {
            logger.info("Cannot read property " + TiffEpTagConstants.EXIF_TAG_TIME_ZONE_OFFSET.name);
        }
        try {
            cameraMake= metadata.findField(TiffTagConstants.TIFF_TAG_MAKE).getStringValue();
        } catch (Exception e) {
            logger.info("Cannot read property " + TiffTagConstants.TIFF_TAG_MAKE.name);
        }
        try {
            cameraModel = metadata.findField(TiffTagConstants.TIFF_TAG_MODEL).getStringValue();
        } catch (Exception e) {
            logger.info("Cannot read property " + TiffTagConstants.TIFF_TAG_MODEL.name);
        }
        try {
            orientation = metadata.findField(TiffTagConstants.TIFF_TAG_ORIENTATION).getIntValue();
        } catch (Exception e) {
            logger.info("Cannot read property " + TiffTagConstants.TIFF_TAG_ORIENTATION.name);
        }
        try {
            software = metadata.findField(TiffTagConstants.TIFF_TAG_SOFTWARE).getStringValue();
        } catch (Exception e) {
            logger.info("Cannot read property " + TiffTagConstants.TIFF_TAG_SOFTWARE.name);
        }
        try {
            userComment = metadata.findField(ExifTagConstants.EXIF_TAG_USER_COMMENT).getStringValue();
        } catch (Exception e) {
            logger.info("Cannot read property " + ExifTagConstants.EXIF_TAG_USER_COMMENT.name);
        }
        try {
            xResolution = metadata.findField(TiffTagConstants.TIFF_TAG_XRESOLUTION).getDoubleValue();
        } catch (Exception e) {
            logger.info("Cannot read property " + TiffTagConstants.TIFF_TAG_XRESOLUTION.name);
        }
        try {
             yResolution = metadata.findField(TiffTagConstants.TIFF_TAG_XRESOLUTION).getDoubleValue();
        } catch (Exception e) {
            logger.info("Cannot read property " + TiffTagConstants.TIFF_TAG_YRESOLUTION.name);
        }

        try {
            iso = metadata.findField(ExifTagConstants.EXIF_TAG_ISO).getIntValue();
        } catch (Exception e) {
            logger.info("Cannot read property " + ExifTagConstants.EXIF_TAG_ISO.name);
        }
        try {
            flash = metadata.findField(ExifTagConstants.EXIF_TAG_FLASH).getIntValue();
        } catch (Exception e) {
            logger.info("Cannot read property " + ExifTagConstants.EXIF_TAG_FLASH.name);
        }
        try {
            shutterSpeed =  metadata.findField(ExifTagConstants.EXIF_TAG_SHUTTER_SPEED_VALUE).getDoubleValue();
        } catch (Exception e) {
            logger.info("Cannot read property " + ExifTagConstants.EXIF_TAG_SHUTTER_SPEED_VALUE.name);
        }
        try {
            aperture = metadata.findField(ExifTagConstants.EXIF_TAG_APERTURE_VALUE).getDoubleValue();
        } catch (Exception e) {
            logger.info("Cannot read property " + ExifTagConstants.EXIF_TAG_APERTURE_VALUE.name);
        }
        try {
            brightness = metadata.findField(ExifTagConstants.EXIF_TAG_BRIGHTNESS_VALUE).getDoubleValue();
        } catch (Exception e) {
            logger.info("Cannot read property " + ExifTagConstants.EXIF_TAG_BRIGHTNESS_VALUE.name);
        }

        try {
            exposureTime = metadata.findField(ExifTagConstants.EXIF_TAG_EXPOSURE_TIME).getDoubleValue();
        } catch (Exception e) {
            logger.info("Cannot read property " + ExifTagConstants.EXIF_TAG_EXPOSURE_TIME.name);
        }

        try {
            exposureTime = metadata.findField(ExifTagConstants.EXIF_TAG_EXPOSURE_MODE).getIntValue();
        } catch (Exception e) {
            logger.info("Cannot read property " + ExifTagConstants.EXIF_TAG_EXPOSURE_MODE.name);
        }

        try {
            exposureProgram = metadata.findField(ExifTagConstants.EXIF_TAG_EXPOSURE_PROGRAM).getIntValue();
        } catch (Exception e) {
            logger.info("Cannot read property " + ExifTagConstants.EXIF_TAG_EXPOSURE_PROGRAM.name);
        }

        try {
            lightSource = metadata.findField(ExifTagConstants.EXIF_TAG_LIGHT_SOURCE).getIntValue();
        } catch (Exception e) {
            logger.info("Cannot read property " + ExifTagConstants.EXIF_TAG_LIGHT_SOURCE.name);
        }

        try {
            focalLength = metadata.findField(ExifTagConstants.EXIF_TAG_FOCAL_LENGTH).getDoubleValue();
        } catch (Exception e) {
            logger.info("Cannot read property " + ExifTagConstants.EXIF_TAG_FOCAL_LENGTH.name);
        }

        try {
            gpsLatRef = metadata.findField(GpsTagConstants.GPS_TAG_GPS_LATITUDE_REF).getStringValue();
        } catch (Exception e) {
            logger.info("Cannot read property " + GpsTagConstants.GPS_TAG_GPS_LATITUDE_REF.name);
        }
        try {
            latitude = metadata.findField(GpsTagConstants.GPS_TAG_GPS_LATITUDE).getDoubleValue();
        } catch (Exception e) {
            logger.info("Cannot read property " + GpsTagConstants.GPS_TAG_GPS_LATITUDE.name);
        }
        try {
            gpsLongRef =   metadata.findField(GpsTagConstants.GPS_TAG_GPS_LONGITUDE_REF).getStringValue();
        } catch (Exception e) {
            logger.info("Cannot read property " + GpsTagConstants.GPS_TAG_GPS_LONGITUDE_REF.name);
        }
        try {
            longitude  =   metadata.findField(GpsTagConstants.GPS_TAG_GPS_LONGITUDE).getDoubleValue();
        } catch (Exception e) {
            logger.info("Cannot read property " + GpsTagConstants.GPS_TAG_GPS_LONGITUDE.name);
        }

        try {
            resolutionUnit =  metadata.findField(TiffTagConstants.TIFF_TAG_RESOLUTION_UNIT).getIntValue();
        } catch (Exception e) {
            logger.info("Cannot read property " + TiffTagConstants.TIFF_TAG_RESOLUTION_UNIT.name);
        }

        try {
            smoothness = metadata.findField(ExifTagConstants.EXIF_TAG_SMOOTHNESS).getStringValue();
        } catch (Exception e) {
            logger.info("Cannot read property " + ExifTagConstants.EXIF_TAG_SMOOTHNESS.name);
        }

        try {
            whiteBalance =    metadata.findField(ExifTagConstants.EXIF_TAG_WHITE_BALANCE_1).getIntValue();
        } catch (Exception e) {
            logger.info("Cannot read property " + ExifTagConstants.EXIF_TAG_WHITE_BALANCE_1.name);
        }

        try {
            whiteBalanceRef =  metadata.findField(ExifTagConstants.EXIF_TAG_WHITE_BALANCE_2).getStringValue();
        } catch (Exception e) {
            logger.info("Cannot read property " + ExifTagConstants.EXIF_TAG_WHITE_BALANCE_2.name);
        }

        try {
            sharpness =  metadata.findField(ExifTagConstants.EXIF_TAG_SHARPNESS_1).getIntValue();
        } catch (Exception e) {
            logger.info("Cannot read property  " + ExifTagConstants.EXIF_TAG_SHARPNESS_1.name);
        }

        try {
            sharpnessRef =  metadata.findField(ExifTagConstants.EXIF_TAG_SHARPNESS_2).getStringValue();
        } catch (Exception e) {
            logger.info("Cannot read property  " + ExifTagConstants.EXIF_TAG_SHARPNESS_2.name);
        }

        try {
             fNumber =  metadata.findField(ExifTagConstants.EXIF_TAG_FNUMBER).getDoubleValue();
        } catch (Exception e) {
            logger.info("Cannot read property  " + ExifTagConstants.EXIF_TAG_FNUMBER.name);
        }

        try {
            saturation =  metadata.findField(ExifTagConstants.EXIF_TAG_SATURATION_1).getIntValue();
        } catch (Exception e) {
            logger.info("Cannot read property  " + ExifTagConstants.EXIF_TAG_SATURATION_1.name);
        }

        try {
            saturationRef =  metadata.findField(ExifTagConstants.EXIF_TAG_SATURATION_2).getStringValue();
        } catch (Exception e) {
            logger.info("Cannot read property  " + ExifTagConstants.EXIF_TAG_SATURATION_2.name);
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
        TiffOutputSet out;

        try {
            out = this.getOutputSet();

            TiffOutputDirectory rootDirectory = out.getOrCreateRootDirectory();
            TiffOutputDirectory exifDirectory = out.getOrCreateExifDirectory();
            TiffOutputDirectory gpsDirectory = out.getOrCreateGPSDirectory();

            if (getUnicId() != null) {
                try {
                    //logger.debug("[Metadata.saveOutputSet] write uuid "+ getUnicId());
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_IMAGE_UNIQUE_ID);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_IMAGE_UNIQUE_ID, getUnicId());
                } catch (Exception e) {
                    logger.info("Cannot update value for tag " + ExifTagConstants.EXIF_TAG_IMAGE_UNIQUE_ID.name, e);
                }
            }


            if (getDateOriginal() != null) {
                try {
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, exifFormat.format(getDateOriginal()));
                } catch (Exception e) {
                    logger.info("Cannot update value for tag " + ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL.name, e);
                }
            }

            if (getDateCreated() != null) {
                try {
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED, exifFormat.format(getDateCreated()));
                } catch (Exception e) {
                    logger.info("Cannot update value for tag " + ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED.name, e);
                }
            }

            if (getDateUpdate() != null) {
                try {
                    rootDirectory.removeField(TiffTagConstants.TIFF_TAG_DATE_TIME);
                    rootDirectory.add(TiffTagConstants.TIFF_TAG_DATE_TIME, exifFormat.format(getDateUpdate()));
                } catch (Exception e) {
                    logger.info("Cannot update value for tag " + TiffTagConstants.TIFF_TAG_DATE_TIME.name, e);
                }
            }

            if (getTzOffset() != 0 ) {
                try {
                    exifDirectory.removeField(TiffEpTagConstants.EXIF_TAG_TIME_ZONE_OFFSET);
                    exifDirectory.add(TiffEpTagConstants.EXIF_TAG_TIME_ZONE_OFFSET, new Integer(getTzOffset()).shortValue());
                } catch (Exception e) {
                    logger.info("Cannot update value for tag " + TiffEpTagConstants.EXIF_TAG_TIME_ZONE_OFFSET.name, e);
                }
            }

            if (getCameraMake() != null) {
                try {
                    rootDirectory.removeField(TiffTagConstants.TIFF_TAG_MAKE);
                    rootDirectory.add(TiffTagConstants.TIFF_TAG_MAKE, getCameraMake());
                } catch (Exception e) {
                    logger.info("Cannot update value for tag " + TiffTagConstants.TIFF_TAG_MAKE.name, e);
                }
            }

            if ( getCameraMake() != null) {
                try {
                    rootDirectory.removeField(TiffTagConstants.TIFF_TAG_MODEL);
                    rootDirectory.add(TiffTagConstants.TIFF_TAG_MODEL, getCameraMake());
                } catch (Exception e) {
                    logger.info("Cannot update value for tag " + TiffTagConstants.TIFF_TAG_MODEL.name, e);
                }
            }

            if (getOrientation() != 0 ) {
                try {
                    rootDirectory.removeField(TiffTagConstants.TIFF_TAG_ORIENTATION);
                    rootDirectory.add(TiffTagConstants.TIFF_TAG_ORIENTATION, new Integer(getOrientation()).shortValue());
                } catch (Exception e) {
                    logger.info("Cannot update value for tag " + TiffTagConstants.TIFF_TAG_ORIENTATION.name, e);
                }
            }

            if (getSoftware() != null ) {
                try {
                    rootDirectory.removeField(TiffTagConstants.TIFF_TAG_SOFTWARE);
                    rootDirectory.add(TiffTagConstants.TIFF_TAG_SOFTWARE, getSoftware());
                } catch (Exception e) {
                    logger.info("Cannot update value for tag " + TiffTagConstants.TIFF_TAG_SOFTWARE.name, e);
                }
            }

            if (  getUserComment() != null ) {
                try {
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_USER_COMMENT);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_USER_COMMENT, getUserComment());
                } catch (Exception e) {
                    logger.info("Cannot update value for tag " + ExifTagConstants.EXIF_TAG_USER_COMMENT.name, e);
                }
            }

            if  ( getxResolution() != 0) {
                try {
                    rootDirectory.removeField(TiffTagConstants.TIFF_TAG_XRESOLUTION);
                    rootDirectory.add(TiffTagConstants.TIFF_TAG_XRESOLUTION, RationalNumber.valueOf(getxResolution()));
                } catch (Exception e) {
                    logger.info("Cannot update value for tag " + TiffTagConstants.TIFF_TAG_XRESOLUTION.name, e);
                }
            }

            if  ( getyResolution() != 0) {
                try {
                    rootDirectory.removeField(TiffTagConstants.TIFF_TAG_YRESOLUTION);
                    rootDirectory.add(TiffTagConstants.TIFF_TAG_YRESOLUTION, RationalNumber.valueOf(getyResolution()));
                } catch (Exception e) {
                    logger.info("Cannot update value for tag " + TiffTagConstants.TIFF_TAG_YRESOLUTION.name, e);
                }
            }

            if ( getIso() != 0) {
                try {
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_ISO);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_ISO, new Integer(getIso()).shortValue());
                } catch (Exception e) {
                    logger.info("Cannot update value for tag " + ExifTagConstants.EXIF_TAG_ISO.name, e);
                }
            }

            if ( getFlash() >= 0 ) {
                try {
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_FLASH);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_FLASH, new Integer(getFlash()).shortValue());
                } catch (Exception e) {
                    logger.info("Cannot update value for tag " + ExifTagConstants.EXIF_TAG_FLASH.name, e);
                }
            }

            if ( getShutterSpeed() != 0) {
                try {
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_SHUTTER_SPEED_VALUE);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_SHUTTER_SPEED_VALUE, RationalNumber.valueOf(getShutterSpeed()));
                } catch (Exception e) {
                    logger.info("Cannot update value for tag " + ExifTagConstants.EXIF_TAG_ISO.name, e);
                }
            }

            if ( getAperture() != 0) {
                try {
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_APERTURE_VALUE);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_APERTURE_VALUE, RationalNumber.valueOf(getAperture()));
                } catch (Exception e) {
                    logger.info("Cannot update value for tag " + ExifTagConstants.EXIF_TAG_APERTURE_VALUE.name, e);
                }
            }

            if (getBrightness() != 0) {
                try {
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_BRIGHTNESS_VALUE);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_BRIGHTNESS_VALUE, RationalNumber.valueOf(getBrightness()));
                } catch (Exception e) {
                    logger.info("Cannot update value for tag " + ExifTagConstants.EXIF_TAG_BRIGHTNESS_VALUE.name, e);
                }
            }

            if ( getExposureTime() != 0) {
                try {
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_EXPOSURE_TIME);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_EXPOSURE_TIME, RationalNumber.valueOf(getExposureTime()));
                } catch (Exception e) {
                    logger.info("Cannot update value for tag " + ExifTagConstants.EXIF_TAG_EXPOSURE_TIME.name, e);
                }
            }

            if ( getExposureTime() != 0) {
                try {
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_EXPOSURE_MODE);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_EXPOSURE_MODE, new Integer(getExposureMode()).shortValue());
                } catch (Exception e) {
                    logger.info("Cannot update value for tag " + ExifTagConstants.EXIF_TAG_EXPOSURE_MODE.name, e);
                }
            }


            if ( getExposureTime() != 0) {
                try {
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_EXPOSURE_PROGRAM);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_EXPOSURE_PROGRAM, new Integer(getExposureProgram()).shortValue());
                } catch (Exception e) {
                    logger.info("Cannot update value for tag " + ExifTagConstants.EXIF_TAG_EXPOSURE_PROGRAM.name, e);
                }
            }


            if ( getExposureTime() != 0) {
                try {
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_LIGHT_SOURCE);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_LIGHT_SOURCE, new Integer(getLightSource()).shortValue());
                } catch (Exception e) {
                    logger.info("Cannot update value for tag " + ExifTagConstants.EXIF_TAG_LIGHT_SOURCE.name, e);
                }
            }


            if ( getFocalLength() != 0) {
                try {
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_FOCAL_LENGTH);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_FOCAL_LENGTH, RationalNumber.valueOf(getFocalLength()));
                } catch (Exception e) {
                    logger.info("Cannot update value for tag " + ExifTagConstants.EXIF_TAG_FOCAL_LENGTH.name, e);
                }
            }


            //  GPS ALT

            if ( getAltitude() != 0) {
                try {
                    gpsDirectory.removeField(GpsTagConstants.GPS_TAG_GPS_ALTITUDE);
                    gpsDirectory.add(GpsTagConstants.GPS_TAG_GPS_ALTITUDE, RationalNumber.valueOf(getAltitude()));
                } catch (Exception e) {
                    logger.info("Cannot update value for tag " + GpsTagConstants.GPS_TAG_GPS_ALTITUDE.name, e);
                }
            }

            //  GPS LONG+LAT

            if (( getLongitude() != 0 ) && (getLatitude() != 0)) {
                try {
                    out.setGPSInDegrees(getLongitude(), getLatitude());
                } catch (Exception e) {
                    logger.info("Cannot update value for GPS tags ", e);
                }
            }


            if ( getResolutionUnit() >= 0) {
                try {
                    rootDirectory.removeField(TiffTagConstants.TIFF_TAG_RESOLUTION_UNIT);
                    rootDirectory.add(TiffTagConstants.TIFF_TAG_RESOLUTION_UNIT, new Integer(getResolutionUnit()).shortValue());
                } catch (Exception e) {
                    logger.info("Cannot update value for tag " + TiffTagConstants.TIFF_TAG_RESOLUTION_UNIT.name, e);
                }
            }

            if ( getSmoothness() != null) {
                try {
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_SMOOTHNESS);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_SMOOTHNESS,getSmoothness());
                } catch (Exception e) {
                    logger.info("Cannot update value for tag " + ExifTagConstants.EXIF_TAG_SMOOTHNESS.name, e);
                }
            }

            if ( getWhiteBalance() >= 0) {
                try {
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_WHITE_BALANCE_1);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_WHITE_BALANCE_1,new Integer(getWhiteBalance()).shortValue());
                } catch (Exception e) {
                    logger.info("Cannot update value for tag " + ExifTagConstants.EXIF_TAG_WHITE_BALANCE_1.name, e);
                }
            }


            if ( getWhiteBalanceRef() != null) {
                try {
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_WHITE_BALANCE_2);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_WHITE_BALANCE_2,getWhiteBalanceRef());
                } catch (Exception e) {
                    logger.info("Cannot update value for tag " + ExifTagConstants.EXIF_TAG_WHITE_BALANCE_2.name, e);
                }
            }


            if ( getSharpness() >= 0) {
                try {
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_SHARPNESS_1);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_SHARPNESS_1,new Integer(getSharpness()).shortValue());
                } catch (Exception e) {
                    logger.info("Cannot update value for tag " + ExifTagConstants.EXIF_TAG_SHARPNESS_1.name, e);
                }
            }

            if ( getSaturation() >= 0) {
                try {
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_SATURATION_1);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_SATURATION_1,new Integer(getSaturation()).shortValue());
                } catch (Exception e) {
                    logger.info("Cannot update value for tag " + ExifTagConstants.EXIF_TAG_SATURATION_1.name, e);
                }
            }


            if ( getWhiteBalanceRef() != null) {
                try {
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_SHARPNESS_2);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_SHARPNESS_2,getSharpnessRef());
                } catch (Exception e) {
                    logger.info("Cannot update value for tag " + ExifTagConstants.EXIF_TAG_SHARPNESS_2.name, e);
                }
            }


            //  FNUMBER

            if ( getfNumber() != 0) {
                try {
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_FNUMBER);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_FNUMBER, RationalNumber.valueOf(getfNumber()));
                } catch (Exception e) {
                    logger.info("Cannot update value for tag " + ExifTagConstants.EXIF_TAG_FNUMBER.name, e);
                }
            }

//
//            try {
//                exifDirectory.removeField(GpsTagConstants.GPS_TAG_GPS_LATITUDE_REF);
//                exifDirectory.add(GpsTagConstants.GPS_TAG_GPS_LATITUDE_REF, getGpsLattRef());
//            } catch (Exception e) {
//                logger.info("Cannot update value for tag " + GpsTagConstants.GPS_TAG_GPS_LATITUDE_REF.name, e);
//            }
//
//            try {
//                exifDirectory.removeField(GpsTagConstants.GPS_TAG_GPS_LATITUDE);
//                exifDirectory.add(GpsTagConstants.GPS_TAG_GPS_LATITUDE, RationalNumber.valueOf(getGpsLatt()));
//            } catch (Exception e) {
//                logger.info("Cannot update value for tag " + GpsTagConstants.GPS_TAG_GPS_LATITUDE.name, e);
//            }
//
//            try {
//                exifDirectory.removeField(GpsTagConstants.GPS_TAG_GPS_LONGITUDE_REF);
//                exifDirectory.add(GpsTagConstants.GPS_TAG_GPS_LONGITUDE_REF, getGpsLattRef());
//            } catch (Exception e) {
//                logger.info("Cannot update value for tag " + GpsTagConstants.GPS_TAG_GPS_LONGITUDE_REF.name, e);
//            }
//
//            try {
//                exifDirectory.removeField(GpsTagConstants.GPS_TAG_GPS_LONGITUDE);
//                exifDirectory.add(GpsTagConstants.GPS_TAG_GPS_LONGITUDE, RationalNumber.valueOf(getGpsLatt()));
//            } catch (Exception e) {
//                logger.info("Cannot update value for tag " + GpsTagConstants.GPS_TAG_GPS_LONGITUDE.name, e);
//            }

        }
        catch (Exception e) {
            throw new ExceptionMetadataProcess("Cannot prepare metadata for update.",e);
        }
        logger.debug("[Metadata.saveOutputSet] Prepare metadata output set.");
        return out;
    }


    public void dump() throws ImageWriteException{

        //TiffImageMetadata meta = (md==null?metadata:md);
        TiffImageMetadata meta = metadata;

        logger.debug("\n\n=== Input metadata ===");

        List<TiffField> tfldList = meta.getAllFields();

        for (TiffField tfld : tfldList ) {
            if ( tfld.getTagInfo().directoryType != null) {
                logger.debug("Field =" + tfld.getTagName() + ", length=" + tfld.getCount()
                          + ", Dir type name = " + tfld.getTagInfo().directoryType.name
                          + ", type id = " + tfld.getTagInfo().directoryType.directoryType
                );
            }
            else {
                logger.debug("Field =" + tfld.getTagName() + ", length=" + tfld.getCount() + ", DIR=NULL");
            }
        }

        logger.debug("\n\n=== Output metadata ===");
        TiffOutputSet out = saveOutputSet();
        try {
            List<TiffOutputDirectory> dirList = out.getDirectories();
            for (TiffOutputDirectory dir : dirList) {
                logger.debug("Found directory. Name = " + dir.description() + ", item = " + dir.getItemDescription() + ", len = " + dir.getItemLength());
                List<TiffOutputField> outFields = dir.getFields();
                for (TiffOutputField fld : outFields) {
                    logger.debug("Found Filed. Name = " + fld.tagInfo.name + ", len = " + fld.count);
                }
            }
        }
        catch ( Exception e ) {
            logger.warn("[dump] Cannot read directory. " + e.getMessage());
        }
    }

    /**
     *
     * @return
     * @throws ImageWriteException
     */
    public TiffOutputSet copyOutputSet() throws ImageWriteException {

        TiffImageMetadata meta = metadata;
        List<TiffField> tfldList = meta.getAllFields();
        TiffOutputSet out = new TiffOutputSet();


        TiffOutputDirectory rootDirectory = out.getOrCreateRootDirectory();
        TiffOutputDirectory exifDirectory = out.getOrCreateExifDirectory();
        TiffOutputDirectory gpsDirectory = out.getOrCreateGPSDirectory();


        //EXIF_TAG_PHOTOSHOP_SETTINGS

        for (TiffField tfld : tfldList) {
            if (tfld.getTagInfo().directoryType != null) {

                switch (tfld.getTagInfo().directoryType.directoryType) {
                    case TiffDirectoryConstants.DIRECTORY_TYPE_ROOT:   //https://commons.apache.org/proper/commons-imaging/apidocs/index.html
                        copyValue(tfld, rootDirectory);
                        break;
                    case TiffDirectoryConstants.DIRECTORY_TYPE_GPS:    //https://commons.apache.org/proper/commons-imaging/apidocs/index.html
                        copyValue(tfld, gpsDirectory);
                        break;
                    case TiffDirectoryConstants.DIRECTORY_TYPE_EXIF:   //https://commons.apache.org/proper/commons-imaging/apidocs/index.html
                        copyValue(tfld, exifDirectory);
                        break;
                    default:
                        logger.warn("[copyOutputSet] Cannot locate directory : " + tfld.getTagName());
                }
            }
        }
        return out;
    }

    /**
     *
     * @param tfld
     * @param dir
     */
    public void copyValue(TiffField tfld,TiffOutputDirectory dir) {

        TiffOutputField tf = new TiffOutputField(tfld.getTagInfo(),
                tfld.getFieldType(),
                tfld.getBytesLength(),
                tfld.getByteArrayValue()
        );
        dir.removeField(tfld.getTagInfo());
        dir.add(tf);
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

    public static String getExposureModeRef(int exposureRef) {
        return  expoConsts.get(exposureRef);
    }

    public static String getExposureProgRef(int exposureProgRef) {
        return  expoProgConsts.get(exposureProgRef);
    }

    public static String getLightSource(int lightSource) {
        return  lightSrcConsts.get(lightSource);
    }


    public TiffOutputSet getOutputSet() throws ImageWriteException {

        TiffOutputSet out = this.outputSet;

        if ( out == null ) {
            if (metadata != null) {
                out = metadata.getOutputSet();
                logger.debug("[getOutputSet] generate output set from metadata.");
            } else {
                out = new TiffOutputSet();
                logger.debug("[getOutputSet] generate empty output set.");
            }
        }

        return out;
    }

    public void setOutputSet(TiffOutputSet outputSet) {
        this.outputSet = outputSet;
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


    public int getExposureMode() {
        return exposureMode;
    }

    public void setExposureMode(int exposureMode) {
        this.exposureMode = exposureMode;
    }


    public int getExposureProgram() {
        return exposureProgram;
    }

    public void setExposureProgram(int exposureProgram) {
        this.exposureProgram = exposureProgram;
    }


    public int getLightSource() {
        return lightSource;
    }

    public void setLightSource(int lightSource) {
        this.lightSource = lightSource;
    }


    public double getfNumber() {
        return fNumber;
    }

    public void setfNumber(double fNumber) {
        this.fNumber = fNumber;
    }


    public int getSharpness() {
        return sharpness;
    }

    public void setSharpness(int sharpness) {
        this.sharpness = sharpness;
    }

    public String getSharpnessRef() {
        return sharpnessRef;
    }

    public void setSharpnessRef(String sharpnessRef) {
        this.sharpnessRef = sharpnessRef;
    }

    public int getWhiteBalance() {
        return whiteBalance;
    }

    public void setWhiteBalance(int whiteBalance) {
        this.whiteBalance = whiteBalance;
    }

    public String getWhiteBalanceRef() {
        return whiteBalanceRef;
    }

    public void setWhiteBalanceRef(String whiteBalanceRef) {
        this.whiteBalanceRef = whiteBalanceRef;
    }

    public String getSmoothness() {
        return smoothness;
    }

    public void setSmoothness(String smoothness) {
        this.smoothness = smoothness;
    }

    public int getResolutionUnit() {
        return resolutionUnit;
    }

    public int getSaturation() {
        return saturation;
    }

    public void setSaturation(int saturation) {
        this.saturation = saturation;
    }

    public String getSaturationRef() {
        return saturationRef;
    }

    public void setSaturationRef(String saturationRef) {
        this.saturationRef = saturationRef;
    }

    public void setResolutionUnit(int resolutionUnit) {
        this.resolutionUnit = resolutionUnit;
    }

    @Override
    public double getxResolution() {
        return xResolution;
    }
    @Override
    public void setxResolution(double xResolution) {
        this.xResolution = xResolution;
    }
    @Override
    public double getyResolution() {
        return yResolution;
    }

    @Override
    public void setyResolution(double yResolution) {
        this.yResolution = yResolution;
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

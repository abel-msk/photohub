package home.abel.photohub.utils.image;


import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.common.RationalNumber;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffEpTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfo;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputField;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Metadata {
    final static Logger logger = LoggerFactory.getLogger(Metadata.class);
    private  SimpleDateFormat exifFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

    private boolean changed = false;
    private TiffImageMetadata metadata = null;
//
//    public  ArrayList<TagInfo> tagsList = new ArrayList();
//    public  Map<TagInfo,TiffField> tagsMap= new HashMap<>();


    private String unicId = null;
    private Date dateOriginal = null;
    private Date dateCreated = null;
    private Date dateUpdate = null;
    private int tzOffset = 0;
    private String camMake = null;
    private String camModel = null;
    private int orientation = 0;
    private String software = null;
    private double resolution = 0;
    private int iso = 0;
    private double shutterSpeed = 0;
    private double aperture = 0;
    private double brightness = 0;
    private String gpsLattRef = null;
    private double gpsLatt = 0;
    private String gpsLongRef = null;
    private double gpsLong = 0;


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
            camMake= metadata.findField(TiffTagConstants.TIFF_TAG_MAKE).getStringValue();
        } catch (Exception e) {
            logger.warn("cannot read property " + TiffTagConstants.TIFF_TAG_MAKE.name);
        }
        try {
            camModel = metadata.findField(TiffTagConstants.TIFF_TAG_MODEL).getStringValue();
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
            gpsLattRef = metadata.findField(GpsTagConstants.GPS_TAG_GPS_LATITUDE_REF).getStringValue();
        } catch (Exception e) {
            logger.warn("cannot read property " + GpsTagConstants.GPS_TAG_GPS_LATITUDE_REF.name);
        }
        try {
            gpsLatt = metadata.findField(GpsTagConstants.GPS_TAG_GPS_LATITUDE).getDoubleValue();
        } catch (Exception e) {
            logger.warn("cannot read property " + GpsTagConstants.GPS_TAG_GPS_LATITUDE.name);
        }
        try {
            gpsLongRef =   metadata.findField(GpsTagConstants.GPS_TAG_GPS_LONGITUDE_REF).getStringValue();
        } catch (Exception e) {
            logger.warn("cannot read property " + GpsTagConstants.GPS_TAG_GPS_LONGITUDE_REF.name);
        }
        try {
            gpsLong  =   metadata.findField(GpsTagConstants.GPS_TAG_GPS_LONGITUDE).getDoubleValue();
        } catch (Exception e) {
            logger.warn("cannot read property " + GpsTagConstants.GPS_TAG_GPS_LONGITUDE.name);
        }

    }


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


            if (getUnicId() != null) {
                try {
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

            try {
                exifDirectory.removeField(TiffEpTagConstants.EXIF_TAG_TIME_ZONE_OFFSET);
                exifDirectory.add(TiffEpTagConstants.EXIF_TAG_TIME_ZONE_OFFSET, new Integer(getTzOffset()).shortValue());
            } catch (Exception e) {
                logger.warn("cannot update value for tag " + TiffEpTagConstants.EXIF_TAG_TIME_ZONE_OFFSET.name, e);
            }

            if (getCamMake() != null) {
                try {
                    rootDirectory.removeField(TiffTagConstants.TIFF_TAG_MAKE);
                    rootDirectory.add(TiffTagConstants.TIFF_TAG_MAKE, getCamMake());
                } catch (Exception e) {
                    logger.warn("cannot update value for tag " + TiffTagConstants.TIFF_TAG_MAKE.name, e);
                }
            }

            if ( getCamModel() != null) {
                try {
                    rootDirectory.removeField(TiffTagConstants.TIFF_TAG_MODEL);
                    rootDirectory.add(TiffTagConstants.TIFF_TAG_MODEL, getCamModel());
                } catch (Exception e) {
                    logger.warn("cannot update value for tag " + TiffTagConstants.TIFF_TAG_MODEL.name, e);
                }
            }

            try {
                rootDirectory.removeField(TiffTagConstants.TIFF_TAG_ORIENTATION);
                rootDirectory.add(TiffTagConstants.TIFF_TAG_ORIENTATION, new Integer(getOrientation()).shortValue());
            } catch (Exception e) {
                logger.warn("cannot update value for tag " + TiffTagConstants.TIFF_TAG_ORIENTATION.name, e);
            }

            if (  getSoftware() != null ) {
                try {
                    rootDirectory.removeField(TiffTagConstants.TIFF_TAG_SOFTWARE);
                    rootDirectory.add(TiffTagConstants.TIFF_TAG_SOFTWARE, getSoftware());
                } catch (Exception e) {
                    logger.warn("cannot update value for tag " + TiffTagConstants.TIFF_TAG_SOFTWARE.name, e);
                }
            }

            try {
                rootDirectory.removeField(TiffTagConstants.TIFF_TAG_XRESOLUTION);
                rootDirectory.add(TiffTagConstants.TIFF_TAG_XRESOLUTION, RationalNumber.valueOf(getResolution()));
            } catch (Exception e) {
                logger.warn("cannot update value for tag " + TiffTagConstants.TIFF_TAG_XRESOLUTION.name, e);
            }

            try {
                exifDirectory.removeField(ExifTagConstants.EXIF_TAG_ISO);
                exifDirectory.add(ExifTagConstants.EXIF_TAG_ISO, new Integer(getIso()).shortValue());
            } catch (Exception e) {
                logger.warn("cannot update value for tag " + ExifTagConstants.EXIF_TAG_ISO.name, e);
            }

            try {
                exifDirectory.removeField(ExifTagConstants.EXIF_TAG_SHUTTER_SPEED_VALUE);
                exifDirectory.add(ExifTagConstants.EXIF_TAG_SHUTTER_SPEED_VALUE, RationalNumber.valueOf(getShutterSpeed()));
            } catch (Exception e) {
                logger.warn("cannot update value for tag " + ExifTagConstants.EXIF_TAG_ISO.name, e);
            }

            try {
                exifDirectory.removeField(ExifTagConstants.EXIF_TAG_APERTURE_VALUE);
                exifDirectory.add(ExifTagConstants.EXIF_TAG_APERTURE_VALUE, RationalNumber.valueOf(getAperture()));
            } catch (Exception e) {
                logger.warn("cannot update value for tag " + ExifTagConstants.EXIF_TAG_APERTURE_VALUE.name, e);
            }

            try {
                exifDirectory.removeField(ExifTagConstants.EXIF_TAG_BRIGHTNESS_VALUE);
                exifDirectory.add(ExifTagConstants.EXIF_TAG_BRIGHTNESS_VALUE, RationalNumber.valueOf(getBrightness()));
            } catch (Exception e) {
                logger.warn("cannot update value for tag " + ExifTagConstants.EXIF_TAG_BRIGHTNESS_VALUE.name, e);
            }

            try {
                out.setGPSInDegrees(getGpsLong(), getGpsLatt());
            } catch (Exception e) {
                logger.warn("Cannot update value for GPS tags ", e);
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
        logger.debug("[Metadata.saveOutputSet] Prepare output set.");
        return out;
    }


    public String getUnicId() {
        return unicId;
    }

    public void setUnicId(String unicId) {
        this.unicId = unicId;
    }

    public Date getDateOriginal() {
        return dateOriginal;
    }

    public void setDateOriginal(Date dateOriginal) {
        this.dateOriginal = dateOriginal;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getDateUpdate() {
        return dateUpdate;
    }

    public void setDateUpdate(Date dateUpdate) {
        this.dateUpdate = dateUpdate;
    }

    public int getTzOffset() {
        return tzOffset;
    }

    public void setTzOffset(int tzOffset) {
        this.tzOffset = tzOffset;
    }

    public String getCamMake() {
        return camMake;
    }

    public void setCamMake(String camMake) {
        this.camMake = camMake;
    }

    public String getCamModel() {
        return camModel;
    }

    public void setCamModel(String camModel) {
        this.camModel = camModel;
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    public String getSoftware() {
        return software;
    }

    public void setSoftware(String software) {
        this.software = software;
    }

    public double getResolution() {
        return resolution;
    }

    public void setResolution(double resolution) {
        this.resolution = resolution;
    }

    public int getIso() {
        return iso;
    }

    public void setIso(int iso) {
        this.iso = iso;
    }

    public double getShutterSpeed() {
        return shutterSpeed;
    }

    public void setShutterSpeed(double shutterSpeed) {
        this.shutterSpeed = shutterSpeed;
    }

    public double getAperture() {
        return aperture;
    }

    public void setAperture(double aperture) {
        this.aperture = aperture;
    }

    public double getBrightness() {
        return brightness;
    }

    public void setBrightness(double brightness) {
        this.brightness = brightness;
    }

    public String getGpsLattRef() {
        return gpsLattRef;
    }

    public void setGpsLattRef(String gpsLattRef) {
        this.gpsLattRef = gpsLattRef;
    }

    public double getGpsLatt() {
        return gpsLatt;
    }

    public void setGpsLatt(double gpsLatt) {
        this.gpsLatt = gpsLatt;
    }

    public String getGpsLongRef() {
        return gpsLongRef;
    }

    public void setGpsLongRef(String gpsLongRef) {
        this.gpsLongRef = gpsLongRef;
    }

    public double getGpsLong() {
        return gpsLong;
    }

    public void setGpsLong(double gpsLong) {
        this.gpsLong = gpsLong;
    }
}

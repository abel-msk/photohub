package home.abel.photohub.connector;

import home.abel.photohub.connector.prototype.PhotoMetadataInt;
import home.abel.photohub.connector.prototype.ExifMetadataTags;
//import home.abel.photohub.service.PhotoAttrService;



import java.text.ParseException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BasePhotoMetadata implements PhotoMetadataInt {
	final Logger logger = LoggerFactory.getLogger(BasePhotoMetadata.class);

	private String cameraMake = null;
	private String cameraModel = null;
	private String Aperture = null;
	private String Distance = null;
	private String ExposureTime = null;
	private String Focal = null;
	private String Iso = null;
	private String focus = null;
	private Double Latitude = null;
	private Double Longitude = null;
	private String Altitude =  null;
	private Date CreationTime = null;
	private String UnicId = null;
	private Integer flash = null;
	
	@Override
	public String getCameraMake() {
		return cameraMake;
	}

	public void setCameraMake(String cameraMake) {
		this.cameraMake = cameraMake;
	}
	
	@Override
	public String getCameraModel() {
		return cameraModel;
	}
	
	public void setCameraModel(String cameraModel) {
		this.cameraModel = cameraModel;
	}
	
	@Override
	public String getAperture() {
		return Aperture;
	}
	
	public void setAperture(String aperture) {
		Aperture = aperture;
	}
	
	@Override
	public String getDistance() {
		return Distance;
	}
	public void setDistance(String distance) {
		Distance = distance;
	}
	
	@Override
	public String getExposureTime() {
		return ExposureTime;
	}
	public void setExposureTime(String exposureTime) {
		ExposureTime = exposureTime;
	}
	
	@Override
	public String getFocal() {
		return Focal;
	}
	public void setFocal(String focal) {
		Focal = focal;
	}
	
	@Override
	public String getIso() {
		return Iso;
	}
	public void setIso(String iso) {
		Iso = iso;
	}
	
	@Override
	public Double getLatitude() {
		return Latitude;
	}
	public void setLatitude(Double latitude) {
		Latitude = latitude;
	}
	
	@Override
	public Double getLongitude() {
		return Longitude;
	}
	public void setLongitude(Double longitude) {
		Longitude = longitude;
	}
	
	@Override
	public Date getCreationTime() {
		return CreationTime;
	}
	@Override
	public void setCreationTime(Date creationTime) {
		CreationTime = creationTime;
	}
	
	@Override
	public String getUnicId() {
		return UnicId;
	}
	@Override
	public void setUnicId(String unicId) {
		UnicId = unicId;
	}

	@Override
	public String getAltitude() {
		// TODO Auto-generated method stub
		return Altitude;
	}

	@Override
	public void setAltitude(String alt) {
		this.Altitude = alt;
		
	}

	@Override
	public String getFocus() {
		return focus;
	}

	@Override
	public void setFocus(String focus) {
		this.focus = focus;
	}
	
	@Override
	public Integer getFlash() {
		return flash;
	}
	
	@Override
	public void setFlash(Integer flash) {
		this.flash = flash;
	}
	
	@Override
	public void setMetaTag(ExifMetadataTags tag, String value) {
//		try {
			switch (tag) {
			case CAMERA_MAKE:
				this.cameraMake = value;
				break;
			case CAMERA_MODEL:
				this.cameraModel = value;
				break;
			case DATE_CREATED:
				this.CreationTime = new Date(new Long(value));
				break;
			case APERTURE:
				this.Aperture = value;
				break;
			case EXPOSURE_TIME:
				this.ExposureTime = value;
				break;
			case FOCAL_LENGTH:
				this.Focal = value;
				break;
			case FLASH:
				this.flash = new Integer(value);
				break;
			case ISO_EQUIVALENT:
				this.Iso = value;
				break;
			case ALTITUDE:
				this.Altitude = value;
				break;
			case GPS_LATITUDE:
				this.Latitude = new Double(value);
				break;
			case GPS_LONGITUDE:
				this.Longitude = new Double(value);
				break;
			case UNIQUE_ID:
				this.UnicId = value;
				break;
			default:
			}
//		} catch (Exception e) {
//			logger.error("[setMetaTag] cannto assign tag="+(tag!=null?"NULL":tag)
//					+", value="+(value!=null?value:"NULL")
//					,e);
//		}
	}

	@Override
	public String getMetaTag(ExifMetadataTags tag) {
//		try {
			switch (tag) {
				case CAMERA_MAKE:
					return this.cameraMake;
				case CAMERA_MODEL:
					return this.cameraModel;
				case DATE_CREATED:
					return (this.CreationTime == null?null:String.valueOf(this.CreationTime.getTime()));
				case APERTURE:
					return this.Aperture;
				case EXPOSURE_TIME:
					return this.ExposureTime;
				case FOCAL_LENGTH:
					return this.Focal;
				case FLASH:
					return (this.flash==null?null:this.flash.toString());
				case ISO_EQUIVALENT:
					return this.Iso;
				case ALTITUDE:
					return this.Altitude;
				case GPS_LATITUDE:
					return (this.Latitude==null?null:this.Latitude.toString());
				case GPS_LONGITUDE:
					return (this.Longitude==null?null:this.Longitude.toString());
				case UNIQUE_ID:
					return this.UnicId;
				default:
					return "";	
			}
//		} catch (Exception e) {
//			logger.error("[getMetaTag] Cannot get tag="+(tag!=null?"NULL":tag)
//					,e);
//		}
//		return "";
	}
	
}

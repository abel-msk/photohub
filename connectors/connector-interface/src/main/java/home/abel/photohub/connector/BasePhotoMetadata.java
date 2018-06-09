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

	private String unicId = null;
	private Date dateOriginal = null;
	private Date dateCreated = null;
	private Date dateUpdate = null;
	private int tzOffset = 0;
	private String cameraMake = null;
	private String cameraModel = null;
	private int orientation = 0;
	private String software = null;
	private double xResolution = 0;
	private double yResolution = 0;
	private int iso = 0;
	private double shutterSpeed = 0;
	private double aperture = 0;
	private double brightness = 0;
	private String gpsLattRef = null;
	private double latitude = 0;
	private String gpsLongRef = null;
	private double longitude = 0;
	private int flash =0;
	private double exposureTime = 0;
	private double focalLength = 0;
	private double altitude = 0;
	private String userComment = null;



	@Override
	public String getUnicId() {
		return unicId;
	}

	@Override
	public void setUnicId(String unicId) {
		this.unicId = unicId;
	}

	@Override
	public Date getDateOriginal() {
		return dateOriginal;
	}

	@Override
	public void setDateOriginal(Date dateOriginal) {
		this.dateOriginal = dateOriginal;
	}

	@Override
	public Date getDateCreated() {
		return dateCreated;
	}

	@Override
	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}

	@Override
	public Date getDateUpdate() {
		return dateUpdate;
	}

	@Override
	public void setDateUpdate(Date dateUpdate) {
		this.dateUpdate = dateUpdate;
	}

	@Override
	public int getTzOffset() {
		return tzOffset;
	}

	@Override
	public void setTzOffset(int tzOffset) {
		this.tzOffset = tzOffset;
	}

	@Override
	public String getCameraMake() {
		return cameraMake;
	}

	@Override
	public void setCameraMake(String cameraMake) {
		this.cameraMake = cameraMake;
	}

	@Override
	public String getCameraModel() {
		return cameraModel;
	}

	@Override
	public void setCameraModel(String cameraModel) {
		this.cameraModel = cameraModel;
	}

	@Override
	public int getOrientation() {
		return orientation;
	}

	@Override
	public void setOrientation(int orientation) {
		this.orientation = orientation;
	}

	@Override
	public String getSoftware() {
		return software;
	}

	@Override
	public void setSoftware(String software) {
		this.software = software;
	}

	@Override
	public double getxResolution() {
		return xResolution;
	}

	@Override
	public void setxResolution(double xResolution) { this.xResolution = xResolution; }

	@Override
	public double getyResolution() {
		return yResolution;
	}

	@Override
	public void setyResolution(double yResolution) {
		this.yResolution = yResolution;
	}

	@Override
	public int getIso() {
		return iso;
	}

	@Override
	public void setIso(int iso) {
		this.iso = iso;
	}

	@Override
	public double getShutterSpeed() {
		return shutterSpeed;
	}

	@Override
	public void setShutterSpeed(double shutterSpeed) {
		this.shutterSpeed = shutterSpeed;
	}

	@Override
	public double getAperture() {
		return aperture;
	}

	@Override
	public void setAperture(double aperture) {
		this.aperture = aperture;
	}

	@Override
	public double getBrightness() {
		return brightness;
	}

	@Override
	public void setBrightness(double brightness) {
		this.brightness = brightness;
	}

//	public String getGpsLattRef() {
//		return gpsLattRef;
//	}
//
//	public void setGpsLattRef(String gpsLattRef) {
//		this.gpsLattRef = gpsLattRef;
//	}

	@Override
	public double getLatitude() {
		return latitude;
	}

	@Override
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

//	public String getGpsLongRef() {
//		return gpsLongRef;
//	}
//
//	public void setGpsLongRef(String gpsLongRef) {
//		this.gpsLongRef = gpsLongRef;
//	}

	@Override
	public double getLongitude() {
		return longitude;
	}

	@Override
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	@Override
	public int getFlash() {
		return flash;
	}

	@Override
	public void setFlash(int flash) {
		this.flash = flash;
	}

	@Override
	public double getExposureTime() {
		return exposureTime;
	}

	@Override
	public void setExposureTime(double exposureTime) {
		this.exposureTime = exposureTime;
	}

	@Override
	public double getFocalLength() {
		return focalLength;
	}

	@Override
	public void setFocalLength(double focalLength) {
		this.focalLength = focalLength;
	}

	@Override
	public double getAltitude() {
		return altitude;
	}

	@Override
	public void setAltitude(double altitude) {
		this.altitude = altitude;
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

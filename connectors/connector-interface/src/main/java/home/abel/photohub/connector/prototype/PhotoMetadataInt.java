package home.abel.photohub.connector.prototype;

import home.abel.photohub.connector.prototype.PhotoMetadataInt;

import java.util.Date;

public interface PhotoMetadataInt {
	
	
	public String getCameraMake();
	public void setCameraMake(String cameraMake);
	
	public String  getCameraModel();
	public void setCameraModel(String cameraModel);
	
	public double getAperture();
	public void setAperture(double aperture);
	
//	public String getDistance();
//	public void setDistance(String distance);
	
	public double getExposureTime();
	public void setExposureTime(double exposureTime);

	public double getFocalLength();
	public void setFocalLength(double focalLength);
	
	public int getIso();
	public void setIso(int iso);
	
//	public String getFocus();
//	public void setFocus(String focus);

	public double getLatitude();
	public void  setLatitude(double latitude);
	
	public double getLongitude();
	public void setLongitude(double longitude);
	
	public double getAltitude();
	public void setAltitude(double altitude);
	
	public Date getDateCreated();
	public void setDateCreated(Date time);

	public Date getDateOriginal();
	public void setDateOriginal(Date time);

	public Date getDateUpdate();
	public void setDateUpdate(Date dateUpdate);

	public String getUnicId();
	public void setUnicId(String uuid);

	public int getFlash();
	public void setFlash(int flash);

	public int getTzOffset();
	public void setTzOffset(int tzOffset);

	public int getOrientation();
	public void setOrientation(int orientation);

	public String getSoftware();
	public void setSoftware(String software);

//	public double getResolution();
//	public void setResolution(double resolution);

	public double getxResolution();
	public void setxResolution(double xRresolution);

	public double getyResolution();
	public void setyResolution(double xResolution);


	public double getShutterSpeed();
	public void setShutterSpeed(double shutterSpeed);


	public double getBrightness();
	public void setBrightness(double brightness);


	public String getUserComment();
	public void setUserComment(String userComment);

//	public void setMetaTag(ExifMetadataTags tag, String value);
//	public String getMetaTag(ExifMetadataTags tag);
}

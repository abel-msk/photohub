package home.abel.photohub.connector.prototype;

import home.abel.photohub.connector.prototype.PhotoMetadataInt;

import java.util.Date;

public interface PhotoMetadataInt {
	
	
	public String getCameraMake();
	public void setCameraMake(String cameraMake);
	
	public String  getCameraModel();
	public void setCameraModel(String cameraModel);
	
	public String getAperture();
	public void setAperture(String str);	
	
	public String getDistance();
	public void setDistance(String str);
	
	public String getExposureTime();
	public void setExposureTime(String str);

	public String getFocal();
	public void setFocal(String str);
	
	public String getIso();
	public void setIso(String str);
	
	public String getFocus();
	public void setFocus(String str);	

	
	public Double getLatitude();
	public void  setLatitude(Double lat);
	
	public Double getLongitude();
	public void setLongitude(Double lon);
	
	public String getAltitude();
	public void setAltitude(String alt);
	
	public Date getCreationTime();	
	public void setCreationTime(Date time);
	
	public String getUnicId();
	public void setUnicId(String uuid);
	
	
	public Integer getFlash();
	public void setFlash(Integer flash);
	
	
//	public void setCameraMake(String str);
//	public void setCameraModel(String str);
//	public void setAperture(String str);
//	public void setDistance(String str);
//	public void setExposureTime(String str);
//	public void setFocal(String str);
//	public void setIso(String str);
//	public void setCreationTime(Date date);
//	public void setGepoPos(Double lat, Double lon);
//	public void setImgUnicId(String id);

	public void setMetaTag(ExifMetadataTags tag, String value);
	public String getMetaTag(ExifMetadataTags tag);
}

package home.abel.photohub.connector.prototype;

import java.util.Date;

public enum ExifMetadataTags {
		GPS_LONGITUDE (Double.class),
		GPS_LATITUDE (Double.class),
		DATE_DIGITIZED (Date.class),
		ALTITUDE (Long.class),
		DATE_CREATED (Date.class),
		APERTURE(String.class),
		SHUTTER_SPEED(String.class),
		EXPOSURE_TIME(String.class),
		FOCAL_LENGTH(String.class),
		FLASH (Short.class),
		ISO_EQUIVALENT(Integer.class),
		CAMERA_MAKE(String.class),
		CAMERA_MODEL(String.class),
		UNIQUE_ID(String.class);
		
		private final Class<?> resultClass;
		
		ExifMetadataTags(java.lang.Class<?> resultClass) {
			this.resultClass = resultClass;
		}

		public Class<?> getResultClass() {
			return resultClass;
		}	
}
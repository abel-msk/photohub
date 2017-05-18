package home.abel.photohub.service;




public enum PhotoAttrEnum {
	//           json_attr_name | Presentation   | access rule  | namespace    | short descr      
	//                            type
	PHOTO_NAME  ("name",        "line",          "rw",          "photo",      "Photo Name"),
	PHOTO_URL   ("photoUrl",    "image",         "none",        "photo",      "The source photo url"),
	PHOTO_REALURL("photoRealUrl","image",       "none",        "photo",      "The source photo url wo subst"),
	PHOTO_THUMB ("thumbUrl",    "image",         "rw",          "photo",      "Url to thumbnail image"),
	PHOTO_DESCR ("descr",       "text",          "rw",          "photo",      "Description"),
	PHOTO_TYPE  ("type",        "line",          "none",        "photo",      "Store object type"),
	PHOTO_PATH  ("photoPath",   "line",          "ro",          "photo",      "Path to file on site"),
	CREATE_DATE ("createDate",  "date",          "rw",          "timestamps", "Time the photo was created"),
	DIGIT_DATE  ("digitTime",   "date",          "rw",          "timestamps", "Time the photo was digitilized or store"),
	MOD_DATE    ("modDate",     "date",          "rw",          "timestamps", "Last modofication time"),
	SITE_ID     ("siteId",      "line",          "none",        "site",       "SiteId"),
	GPS_LAT     ("gpsLat",      "line",          "rw",          "gps",        "Gpa Latitude"),
	GPS_LON     ("gpsLon",      "line",          "rw",          "gps",        "Gpa Longitude"),
	GPS_ALT     ("gpsAlt",      "line",          "ro",          "gps",        "Gps Altitude"),	
	GPS_DIR     ("gpsDir",      "line",          "ro",          "gps",        "Gps Directory"),
	APERURE     ("aperture",    "line",          "ro",          "SubIFD",     "Aperture"),
	EXP_MODE    ("expMode",     "line",          "ro",          "SubIFD",     "Exposure mode"),	
	EXP_TIME    ("expTime",     "line",          "ro",          "SubIFD",     "Exposure time"),	
	FOCAL       ("focalLen",    "line",          "ro",          "SubIFD",     "Focal length"),	
	FOCUS       ("focusDist",   "line",          "ro",          "SubIFD",     "Focus Distance"),	
	ISO_SPEED   ("isoSpeed",    "line",          "ro",          "SubIFD",     "ISO speed"),	
	CAM_MAKE    ("camMake",     "line",          "ro",          "IFD0",       "Camera Vendor"),	
	CAM_MODEL   ("camModel",    "line",          "ro",          "IFD0",       "Camera Model")
	;
	private final String attrName;
    private final String type;   
    private final String access; 
    private final String displayName;
    private final String namespace;
    /*private final String getter;*/
    
    PhotoAttrEnum(String attrName,  String type, String access, String namespace,   String dispName) {
    	this.displayName = dispName;
        this.type = type;
        this.access = access;
        this.namespace = namespace;
        this.attrName = attrName;
    }
    public String getType() { return type;}
    public String getAccess() { return access;}
    public String getDisplayName() { return displayName;}
    public String getNamespace() { return namespace;}
	public String getAttrName() {return attrName;}	 
	
	public static PhotoAttrEnum fromName(String jsonName) {
		if (jsonName != null) {
			for (PhotoAttrEnum theAttr : PhotoAttrEnum.values()) {
				if (jsonName.equalsIgnoreCase(theAttr.attrName)) {
					return theAttr;
				}
			}
		}
		return null;
	}
	
}

package home.abel.photohub.service;

//import home.abel.photohub.service.ConfVarEnum;

public enum ConfVarEnum {
		//                 | property name            |type  		|access | store_in_db	|sort_order
		LOCAL_THUMB_PATH   ("localThumbPath",         "local_path",	"rw", 	true,			20),
				//"The thumb store root catalog path on local computer."		
		LOCAL_THUMB_FMT    ("localThumbFormat",       "str",		"rw", 	true,			30),
				//"The default thumb image file format. use: gif or png"
		DEFAULT_FLD_THUMB  ("defualtFolderThumb",     "local_path",	"rw", 	true,			40),
				//"The thumbnail image for use when new folder created"
		INSTALLATION_TYPE  ("installationType",       "str",		"ro", 	false,			70),
				//"Current server mode. Use 'standalone' or 'server'"),
		USE_DB             ("conf.save","bool",			"none",	false,			100),
				//"Use accessed DB for store modifyed config parameters"
		DEFAULT_USER       ("defaultUserName",        "str",		"none",	true,			110),
				//"The default user name for store objects in db for single user mode (standalone)"
		DEFAULT_PASS       ("defaultPassword",        "str",		"none",	true,			110),
		        //"The default user name for store objects in db for single user mode (standalone)"
		;

		private final String name;   
	    private final String access; 	
	    private final String type;
	    private final int sort;
	    private final boolean storeInDB;
	    
	    ConfVarEnum(String name, String type, String access, boolean storeInDB, int sort) {
	    	this.name = name;
	    	this.access = access;
	    	this.type = type;
	    	this.storeInDB = storeInDB; 
	    	this.sort = sort;
	    }
	    
	    public String getName() {return this.name; }
	    public String getAccess() {return this.access; }
	    public String getType() {return this.type; }
	    public int getSort() {return this.sort; }
	    
	    public static ConfVarEnum getByDBName(String theDbVar) {
		    for(ConfVarEnum enumValue : ConfVarEnum.values()) {
		        if (enumValue.getName().equalsIgnoreCase(theDbVar)) {
		            return enumValue;
		        }
		    }
		    throw new IllegalArgumentException("There is no value with name '" + theDbVar + " in ConfVarEnum  db variable name"); 
	    }

	    public static ConfVarEnum getByName(String theName) {
		    for(ConfVarEnum enumValue : ConfVarEnum.values()) {
		        if (enumValue.getName().equalsIgnoreCase(theName)) {
		            return enumValue;
		        }
		    }
		    throw new IllegalArgumentException("There is no value with name '" + theName + " in ConfVarEnum property variable name"); 
	    }	    
	    
	    public boolean isStoreInDB() {
			return storeInDB;
		}
}

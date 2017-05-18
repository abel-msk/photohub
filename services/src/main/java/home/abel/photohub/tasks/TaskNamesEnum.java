package home.abel.photohub.tasks;


public enum TaskNamesEnum {
						             // isUser,isScheduled,isQueued
	TNAME_SCAN    ("TNAME_SCAN","SCAN",true ,true, false,"Scan on site for newly created objects and dowload it"),
	TNAME_CLEAN   ("TNAME_CLEAN","CLEAN",true ,false, false,"Remove stored objects for site." ),
	TNAME_REMOVE  ("TNAME_REMOVE","REMOVE",false ,false, false,"Remove site." ),
	TNAME_EMPTY   ("TNAME_EMPTY","EMPTY",false ,true,true,"Empty task for testing")
	;
	
	private final String name;
	private final String dispName;
	private final boolean userTask;
	private final boolean sheduled;
	private final boolean queued;
	private final String descr;
	
	TaskNamesEnum(String name,
				  String dispName,
				  boolean userTask,
				  boolean sheduled,
				  boolean queued,
				  String descr)
	{
		this.name = name;
		this.dispName = dispName;
		this.userTask = userTask;
		this.sheduled = sheduled;
		this.queued = queued;
		this.descr = descr;
	}
		
//	public String getName() {
//		return name;
//	}
	public boolean isUserTask() {
		return userTask;
	}

	public boolean isScheduled() {
		return sheduled;
	}
	public boolean isQueued() {
		return queued;
	}

	public String getDispName() {return dispName; }
	public String getDescr() {
		return descr;
	}

//	public static TaskNamesEnum getByName(String theName) {
//		for(TaskNamesEnum enumValue : TaskNamesEnum.values()) {
//			if (enumValue.getName().equalsIgnoreCase(theName)) {
//				return enumValue;
//			}
//		}
//		throw new IllegalArgumentException("There is no value with name '" + theName + " in ConfVarEnum property variable name"); 
//	}

}

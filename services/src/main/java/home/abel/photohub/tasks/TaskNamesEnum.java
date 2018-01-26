package home.abel.photohub.tasks;


public enum TaskNamesEnum {

		           // TASK_NAME   isUser,isScheduled,isQueued
//	TNAME_SCAN    ("TNAME_SCAN"  ,true  ,true, false),
//	TNAME_CLEAN   ("TNAME_CLEAN" ,true  ,false, false),
//	TNAME_REMOVE  ("TNAME_REMOVE",false ,false, false),
//	TNAME_EMPTY   ("TNAME_EMPTY" ,false ,true,true)
//	;

	TNAME_SCAN,
	TNAME_CLEAN,
	TNAME_REMOVE,
	TNAME_EMPTY,
	
//	private final String name;
//	private final boolean userTask;
//	private final boolean sheduled;
//	private final boolean queued;
//
//	TaskNamesEnum(String name,
//				  boolean userTask,
//				  boolean sheduled,
//				  boolean queued)
//	{
//		this.name = name;
//		this.userTask = userTask;
//		this.sheduled = sheduled;
//		this.queued = queued;
//	}
//
//	public String getName() {
//		return name;
//	}
//	public boolean isUserTask() {
//		return userTask;
//	}
//	public boolean isScheduled() {
//		return sheduled;
//	}
//	public boolean isQueued() {
//		return queued;
//	}

//	public static TaskNamesEnum getByName(String theName) {
//		for(TaskNamesEnum enumValue : TaskNamesEnum.values()) {
//			if (enumValue.getName().equalsIgnoreCase(theName)) {
//				return enumValue;
//			}
//		}
//		throw new IllegalArgumentException("There is no value with name '" + theName + " in ConfVarEnum property variable name"); 
//	}

}

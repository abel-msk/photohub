package home.abel.photohub.tasks;


import home.abel.photohub.model.Schedule;
import home.abel.photohub.model.Site;

import home.abel.photohub.model.TaskParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.method.P;

import java.util.*;

public class EmptyTask extends BaseTask  {

	private static final long serialVersionUID = 1L;
	final Logger logger = LoggerFactory.getLogger(CleanTask.class);

	public EmptyTask(Site theSite, Schedule schedule, ScheduleProcessing scheduleSvc) {

		super(theSite,TaskNamesEnum.TNAME_EMPTY,schedule,scheduleSvc, true);
		this.description = EmptyTask.getStaticDescription();
		this.displayName = EmptyTask.getStaticDisplayName();

	}

	/*-----------------------------------------------------------------------------------
			Self descriptions methods
	 -----------------------------------------------------------------------------------*/


	public static  String getStaticDisplayName() {
		return "Dummy";
	}
	public static  String getStaticDescription() {
		return "Empty Task";
	}

	public static  boolean isVisible() {
		return true;
	}

	private static final Map<String,String> pMap;
	static {
		Map<String, String> tmpMap = new HashMap<>();
		tmpMap.put("PARAM1","Test param for empty task");
		pMap = Collections.unmodifiableMap(tmpMap);
	}
	public static Map<String,String> getParamsDescr() {
		return pMap;
	}

	/*-----------------------------------------------------------------------------------
       Task execution body
 -----------------------------------------------------------------------------------*/

	@Override
	public void exec() throws Throwable {

		logger.trace("[EmptyTask.run] Invocation empty task at = " + new Date());
		List<TaskParam> params = getParams();
		if ( params != null ) {
			for (TaskParam param : params ) {
				logger.trace("[EmptyTask.exec] Param name "+param.getName()+" have " + param.getValue());
			}
		}
		else {
			logger.trace("Params list is empty");
		}

	}
	
}

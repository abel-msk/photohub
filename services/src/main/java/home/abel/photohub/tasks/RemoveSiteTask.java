package home.abel.photohub.tasks;

import home.abel.photohub.model.Schedule;
import home.abel.photohub.model.Site;
import home.abel.photohub.service.SiteService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveSiteTask extends BaseTask {

	private static final long serialVersionUID = 1L;
	final Logger logger = LoggerFactory.getLogger(CleanTask.class);
	private SiteService siteSvc;

	public RemoveSiteTask(Site theSite, SiteService siteSvc, Schedule schedule, ScheduleProcessing scheduleSvc) {
		super(theSite,TaskNamesEnum.TNAME_REMOVE,schedule,scheduleSvc,false);
		this.siteSvc = siteSvc;
	}
	@Override
	public void exec() throws Throwable {
		siteSvc.removeSite(getSchedule().getSite());
	}

	public static boolean isVisible() {return false;};
	public String getDisplayName() {
		return "Remove site";
	}

}

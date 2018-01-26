package home.abel.photohub.tasks;

import home.abel.photohub.model.Site;
import home.abel.photohub.model.Schedule;
import home.abel.photohub.model.SiteRepository;
import home.abel.photohub.service.SiteService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CleanTask extends BaseTask  {
	private static final long serialVersionUID = 1L;
	SiteService SiteSvc;
	final Logger logger = LoggerFactory.getLogger(CleanTask.class);


	public CleanTask(Site theSite, SiteService siteSvc, Schedule schedule, ScheduleProcessing scheduleSvc) {
		super(theSite,TaskNamesEnum.TNAME_CLEAN,schedule, scheduleSvc,true);
		this.SiteSvc = siteSvc;
	}




	@Override
	public void exec() throws Throwable {
		SiteSvc.cleanSite(getSite());
	}

	public static boolean isVisible() {return false;};
	public String getDisplayName() {
		return "Site clean";
	}
}

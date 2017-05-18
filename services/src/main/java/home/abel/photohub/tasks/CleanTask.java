package home.abel.photohub.tasks;

import home.abel.photohub.model.Site;
import home.abel.photohub.model.SiteRepository;
import home.abel.photohub.service.SiteService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CleanTask extends BaseTask {

	private static final long serialVersionUID = 1L;
	final Logger logger = LoggerFactory.getLogger(CleanTask.class);

	public CleanTask(Site theSite, SiteService siteSvc) {
		super(theSite,TaskNamesEnum.TNAME_CLEAN,siteSvc, true);
	}
	
	@Override
	public void exec() throws Throwable {
		getSiteSvc().cleanSite(getSite());	
	}

}

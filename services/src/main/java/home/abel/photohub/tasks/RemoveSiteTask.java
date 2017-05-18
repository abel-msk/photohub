package home.abel.photohub.tasks;

import home.abel.photohub.model.Site;
import home.abel.photohub.service.SiteService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveSiteTask extends BaseTask {

	private static final long serialVersionUID = 1L;
	final Logger logger = LoggerFactory.getLogger(CleanTask.class);

	public RemoveSiteTask(Site theSite, SiteService siteService) {
		super(theSite,TaskNamesEnum.TNAME_REMOVE,siteService,false);
	}
	@Override
	public void exec() throws Throwable {
		getSiteSvc().removeSite(getSite());	
	}
}

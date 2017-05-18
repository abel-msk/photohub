package home.abel.photohub.tasks;

import javax.persistence.EntityManager;

import home.abel.photohub.model.Site;
import home.abel.photohub.service.SiteService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class EmptyTask extends BaseTask{

	private static final long serialVersionUID = 1L;
	final Logger logger = LoggerFactory.getLogger(CleanTask.class);

	public EmptyTask(Site theSite, SiteService siteSvc) {
		super(theSite,TaskNamesEnum.TNAME_EMPTY,siteSvc, true);
	}
	
	@Override
	public void exec() throws Throwable {

		logger.trace("[EmptyTask.run] Invocation empty task at = " + new Date());
	}
	
}

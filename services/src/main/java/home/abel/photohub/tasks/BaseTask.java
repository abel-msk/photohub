package home.abel.photohub.tasks;

import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.Future;

import javax.mail.Session;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.concurrent.ListenableFuture;

import home.abel.photohub.model.Site;
import home.abel.photohub.model.TaskRecord;
import home.abel.photohub.service.SiteService;

//@Transactional(readOnly=false)
//@Transactional(propagation=Propagation.SUPPORTS)
//@Transactional
public class BaseTask implements Runnable, Serializable {
	private static final long serialVersionUID = 1L;
	final Logger logger = LoggerFactory.getLogger(BaseTask.class);
	
	private Site site;
	private TaskNamesEnum tag;
	private SiteService siteSvc;
	private Future<?> thisTaskProcess;
	protected TaskRecord record;
	private boolean saveToDB = true;

	
	public BaseTask(Site theSite, TaskNamesEnum theTag,SiteService siteSvc, boolean saveToDB) {
		this.siteSvc = siteSvc;
		this.saveToDB = saveToDB;
		site = theSite;
		tag = theTag;
		record = new TaskRecord();
		record.setStartTime(new Date());
		record.setName(tag.toString());
		setStatus(TaskStatusEnum.IDLE,"Wait for execution");

		//  Save to DB;
		if ( isSaveToDB()) {
			record = siteSvc.addTaskRecord(site, record);
		}
	}
	
	
//	private EntityManager em = null;
//	
//	public void setEntityManagerFactory(EntityManagerFactory emf) {
//		em = emf.createEntityManager();
//	}
	
	public TaskNamesEnum getTag() {
		return tag;
	}

	public void setTag(TaskNamesEnum tag) {
		this.tag = tag;
	}
	
	public Site getSite() {
		return site;
	}

	public void setSite(Site site) {
		this.site = site;
	}

	public TaskRecord getRecord() {
		return record;
	}

	public void setRecord(TaskRecord record) {
		this.record = record;
	}


	
	public SiteService getSiteSvc() {
		return siteSvc;
	}

	public void setSiteSvc(SiteService siteSvc) {
		this.siteSvc = siteSvc;
	}

	public TaskStatusEnum getStatus() {
		return TaskStatusEnum.valueOf(record.getStatus());
	}
	
	public Future<?> getThisTaskProcess() {
		return thisTaskProcess;
	}

	public void setThisTaskProcess(Future<?> thisTaskProcess) {
		this.thisTaskProcess = thisTaskProcess;
	}
	
	public void setStatus(TaskStatusEnum status, String message) {
		record.setMessage(message);
		record.setStatus(status.toString());
		record.setStopTime(new Date());
	}	
	
	public void printMsg(String msg) {
		record.setMessage(msg);
	}
	

	@Override
	public void run() {
		setStatus(TaskStatusEnum.RUN,"Start execution");
		
		if ( isSaveToDB()) {
			record = siteSvc.addTaskRecord(site,record);
		}

		logger.debug("[BaseTask.run] Try to execute task name="+ getTag()+", for site="+
				(getSite()==null?"system":getSite()) +
				", id = " + (record.getId()==null?"null":record.getId())
		);

		try {
			exec();
			setStatus(TaskStatusEnum.FIN,"Execution complete.");
		}
		catch (Throwable ex) {
			setStatus(TaskStatusEnum.ERR,ex.getMessage());
			throw new ExceptionTaskAbort("Abort execution task="+getTag());
		}
		finally {
			logger.debug("[BaseTask.run] Execution completed.");

			if ( isSaveToDB()) {
				try {
					//logger.trace("[BaseTask.run] Execution completed. Save result record to DB. Record="+record.getMessage());
					siteSvc.updateTaskRecord(record);
				} catch (Exception e) {
					logger.error("[BaseTask.run] Cannot update Site="+site+" to DB.",e);
				}
			}
		}
	}
	
	public void exec() throws Throwable {
		
	}
	
	public boolean isSaveToDB() {
		return saveToDB;
	}


	public void setSaveToDB(boolean saveToDB) {
		this.saveToDB = saveToDB;
	}
	
}

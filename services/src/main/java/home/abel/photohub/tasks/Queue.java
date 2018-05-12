package home.abel.photohub.tasks;
import home.abel.photohub.model.Site;
import home.abel.photohub.service.TaskQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
/**
 *
 *    In memory list for all running, waiting or scheduled for execution tasks
 *    Performs all nessesary  insertion and deletions
 *
 *
 */
public class Queue extends ConcurrentHashMap <String,ConcurrentHashMap<String,BaseTask>> {
    final Logger logger = LoggerFactory.getLogger(Queue.class);


    public static final String  NULL_SITE_ID = "system";


    public static String site2Id (Site site) {
        if (site == null) {
            return NULL_SITE_ID;
        }
        else return site.getId();
    }

    public BaseTask get(BaseTask task) {
        ConcurrentHashMap<String,BaseTask> subMap = this.get(site2Id(task.getSite()));
        if (subMap != null ) {
            return subMap.get(task.getId());
        }
        return null;
    }


    public BaseTask get (String siteId, String taskId) {
        ConcurrentHashMap<String,BaseTask> subMap = this.get(siteId);
        if (subMap != null ) {
            return subMap.get(taskId);
        }
        return null;
    }

    public BaseTask put (BaseTask task) {
        BaseTask newTask = null;
        ConcurrentHashMap<String,BaseTask> subMap = this.get(site2Id(task.getSite()));

        if ( subMap == null) {
            subMap = new ConcurrentHashMap<String,BaseTask>();
            put(site2Id(task.getSite()),subMap);
            logger.trace("Create Queue for site " + site2Id(task.getSite()));
        }

        if ( subMap.get(task.getId()) == null) {
            newTask = subMap.put(task.getId(), task);
            logger.trace("Task  "+task+" not found in queue "+site2Id(task.getSite())+".  Create new one.");

        }
        else {
            newTask = subMap.replace(task.getId(), task);
            logger.trace("Task "+task+" replaced in queue "+site2Id(task.getSite()));

        }
        return newTask;
    }

    public Map<String,BaseTask> getSitesTasks(String siteId) {
        return this.get(siteId);
    }


    public BaseTask remove(BaseTask task) {
        ConcurrentHashMap<String,BaseTask> subMap = this.get(site2Id(task.getSite()));
        BaseTask removedTask = null;
        if (subMap != null ) {
            logger.trace("[remove] Remove from queue task "+task);
            removedTask = subMap.remove(task.getId());
        }
        return removedTask;
    };

}

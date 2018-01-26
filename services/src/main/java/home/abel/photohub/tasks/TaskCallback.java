package home.abel.photohub.tasks;

import home.abel.photohub.service.TaskQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * TaskCallback
 *
 * Расширенный класс  ListenableFutureCallback, который будет вызван после после окончания
 * каждой задачи.
 * В разсширеном классе, после вызова задача   данные о завершении заоачи записываются в базу.
 * @author abel
 */

public class TaskCallback implements ListenableFutureCallback<Void> {
    final Logger logger = LoggerFactory.getLogger(TaskCallback.class);
    private BaseTask task;


    public TaskCallback(BaseTask task) {
        this.task = task;
    }

    @Override
    public void onSuccess(Void result) {
        logger.trace("[TaskCallback.onSuccess] Task complete OK. Task="+task);
        remove();
        onFinish();
    }

    @Override
    public void onFailure(Throwable ex) {
        logger.trace("[TaskCallback.onFailure] Task complete with error. Task="+task);
        remove();
        onFinish();
    }

    private void remove() {
       // executor.remove(task);

    }

    private void onFinish() {
        logger.trace("Task="+task+", finished.");

        //  Перезагрузить задачу на следующий период.
//        if (task.isScheduled()) {
//            try {
//                executor.runScheduled(task);
//            }
//            catch (Exception e) {
//                logger.warn("Cannot set schedule for task "+task);
//                task.setStatus(TaskStatusEnum.ERR,"restart schedule error: "+task);
//            }
//        }

    }
}
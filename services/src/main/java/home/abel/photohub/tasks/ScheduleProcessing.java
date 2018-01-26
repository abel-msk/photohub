package home.abel.photohub.tasks;

import home.abel.photohub.model.*;
import home.abel.photohub.model.TaskRecordRepository;
import home.abel.photohub.model.ScheduleRepository;
import home.abel.photohub.service.ExceptionInvalidArgument;
import home.abel.photohub.service.PhotoService;
import home.abel.photohub.service.SiteService;
import home.abel.photohub.service.TaskQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
/**
 *
 *  Service   interaction interface with DB for insertion and deletion  all tasks
 *
 */
public class ScheduleProcessing {

    final Logger logger = LoggerFactory.getLogger(ScheduleProcessing.class);

    public static final String NULL_SITE = "system";

    @Autowired
    TaskRecordRepository taskRecordRepository;

    @Autowired
    ScheduleRepository scheduleRepository;

    @Autowired
    SiteRepository siteRepositry;

    /**
     *  Сохраняет рассписание в базе если этого рассписаниея в базе еще нет.
     * @param schedule объект рассписания
     * @return объект рассписания
     */
    public Schedule saveSchedule(Schedule schedule) {
        //if ((schedule.getId() == null) || (scheduleRepository.findOne(schedule.getId()) == null)) {
            schedule = scheduleRepository.save(schedule);
        logger.trace("[createSechdule] Save schedule to db. Schedule="+schedule);

        //
        return schedule;
    }

    /**
     *  Сохраняет  запись о состоянии задачи в базу.
     * @param taskRecord объект статус задачи
     */
     public void saveLog(TaskRecord taskRecord) {
         //logger.trace("[saveLog] Save task record " + taskRecord + " to db.");
         if ( taskRecord.getSiteBean() != null) {
             Site theSite = taskRecord.getSiteBean();
             theSite.addTaskRecord(taskRecord);
             siteRepositry.save(theSite);
             //taskRecord = taskRecordRepository.save(taskRecord);
         }
         else {
             taskRecord = taskRecordRepository.save(taskRecord);
         }
         logger.trace("[saveLog] Task record saved to db.  TaskRecord=" + taskRecord);

     }

    /**
     *  Удаляет задачу с рассписанием виз базы
     * @param schedule объект рассписания
     * @return объект рассписания
     */
    public Schedule removeSchedule(Schedule schedule) {


        if ((schedule.getId() != null) && (scheduleRepository.findOne(schedule.getId()) != null)) {
            scheduleRepository.delete(schedule);
            logger.trace("[removeSchedule] Remove schedule from db. Schedule=" + schedule);


        }
        else {
            logger.debug("[removeSchedule] Request to remove  from db schedule  w/o Id. Schedule=" + schedule);
        }
        return schedule;
    }

    /**
     * Записывает изменения в объекте рассписание в базу
     * @param schedule объект рассписания
     * @return объект рассписания
     * @throws RuntimeException не правильные парметр cron рассписания
     */
    public Schedule updateSchedule(Schedule schedule) throws RuntimeException {

            this.validateCron(schedule,
                    schedule.getSeconds(),
                    schedule.getMinute(),
                    schedule.getHour(),
                    schedule.getDayOfMonth(),
                    schedule.getMonth(),
                    schedule.getDayOfWeek()
            );

        //logger.trace("[updateSchedule] chedule "+schedule);
        return scheduleRepository.save(schedule);
    }


    /**
     *
     *  Проверяем значения полей рассписания в стиле cron и сохранаем в и обновлем в объекте Schedule
     *
     * @param seconds значение поля секунл
     * @param minute значение поля минут
     * @param hour  значение поля часов
     * @param dayOfMonth значение поля день месяца
     * @param month  значение поля месяц
     * @param dayOfWeek значение поля день недели
     * @throws ExceptionInvalidArgument  - неправильный формат поля рассписания
     * @return  измененный оьбъект
     */


    public static Schedule validateCron(
            Schedule origSchedule,
            String seconds,
            String minute,
            String hour,
            String dayOfMonth,
            String month,
            String dayOfWeek) throws ExceptionInvalidArgument {


        Schedule theSchedule = new Schedule();

        //   SECONDS
        if (seconds==null) {
            theSchedule.setSeconds("1");
        }
        else if ( Pattern.matches("^\\*|[*0-9,-/]+$",seconds)) {
            theSchedule.setSeconds(seconds);
        }
        else {
            throw new ExceptionInvalidArgument("Cannot set schedule. Incorect seconds value=" +seconds );
        }

        //  MINUTE
        if (minute==null) {
            theSchedule.setMinute("1");
        }
        else if (Pattern.matches("^\\*|[*0-9,-/]+$",minute)) {
            theSchedule.setMinute(minute);
        }
        else {
            throw new ExceptionInvalidArgument("Cannot set schedule. Incorect minute value=" + minute );
        }

        //	HOUR
        if (hour==null) {
            theSchedule.setHour("*");
        }
        else if ( Pattern.matches("^\\*|[*0-9,-/]+$",hour)) {
            theSchedule.setHour(hour);
        }
        else {
            throw new ExceptionInvalidArgument("Cannot set schedule. Incorect hour value=" + hour );
        }

        //   DAY OF MONTH
        if (dayOfMonth==null) {
            theSchedule.setDayOfMonth("*");
        }
        else if (Pattern.matches("^\\*|[*0-9,-/]+$",dayOfMonth)) {
            theSchedule.setDayOfMonth(dayOfMonth);
        }
        else {
            throw new ExceptionInvalidArgument("Cannot set schedule. Incorect setDayOfMonth value=" + dayOfMonth );
        }

        //  MONTH
        if (month==null) {
            theSchedule.setMonth("*");
        }
        else if (Pattern.matches("^\\*|[*0-9,-/]+$",month)) {
            theSchedule.setMonth(month);
        }
        else {
            throw new ExceptionInvalidArgument("Cannot set schedule. Incorect month value=" + month );
        }

        //   DAY OF WEEK
        if (dayOfWeek==null) {
            theSchedule.setDayOfWeek("*");
        }
        else if (Pattern.matches("^\\*|[*0-9,-/]+$",dayOfWeek)) {
            theSchedule.setDayOfWeek(dayOfWeek);
        }
        else {
            throw new ExceptionInvalidArgument("Cannot set schedule. Incorect month value=" + dayOfWeek );
        }

        origSchedule.setSeconds(theSchedule.getSeconds());
        origSchedule.setMinute(theSchedule.getMinute());
        origSchedule.setHour(theSchedule.getHour());
        origSchedule.setDayOfMonth(theSchedule.getDayOfMonth());
        origSchedule.setMonth(theSchedule.getMonth());
        origSchedule.setDayOfWeek(theSchedule.getDayOfWeek());

        return origSchedule;
    }


    public static boolean isValidCron(  Schedule  schedule ) throws ExceptionInvalidArgument {

        //   SECONDS
        if (schedule.getSeconds()==null) {
            schedule.setSeconds("1");
        }
        else if ( ! Pattern.matches("^\\*|[*0-9,-/]+$",schedule.getSeconds())) {
            throw new ExceptionInvalidArgument("Incorrect cron expression. Seconds value=" +schedule.getSeconds() );
        }

        //  MINUTE
        if (schedule.getMinute()==null) {
            schedule.setMinute("1");
        }
        else if ( ! Pattern.matches("^\\*|[*0-9,-/]+$",schedule.getMinute())) {
            throw new ExceptionInvalidArgument("Incorrect cron expression. Minute value=" +schedule.getMinute() );
        }


        //	HOUR
        if (schedule.getHour()==null) {
            schedule.setHour("*");
        }
        else if ( ! Pattern.matches("^\\*|[*0-9,-/]+$",schedule.getHour())) {
            throw new ExceptionInvalidArgument("Cannot set schedule. Incorect hour value=" + schedule.getHour() );
        }

        //   DAY OF MONTH
        if (schedule.getDayOfMonth()==null) {
            schedule.setDayOfMonth("*");
        }
        else if ( ! Pattern.matches("^\\*|[*0-9,-/]+$",schedule.getDayOfMonth())) {
            throw new ExceptionInvalidArgument("Cannot set schedule. Incorect setDayOfMonth value=" + schedule.getDayOfMonth() );
        }

        //  MONTH
        if (schedule.getMonth()==null) {
            schedule.setMonth("*");
        }
        else if (! Pattern.matches("^\\*|[*0-9,-/]+$",schedule.getMonth())) {
            throw new ExceptionInvalidArgument("Cannot set schedule. Incorect month value=" + schedule.getMonth() );
        }

        //   DAY OF WEEK
        if (schedule.getDayOfWeek()==null) {
            schedule.setDayOfWeek("*");
        }
        else if ( ! Pattern.matches("^\\*|[*0-9,-/]+$",schedule.getDayOfWeek())) {
            throw new ExceptionInvalidArgument("Cannot set schedule. Incorect month value=" + schedule.getDayOfWeek() );
        }

        return true;
    }



}

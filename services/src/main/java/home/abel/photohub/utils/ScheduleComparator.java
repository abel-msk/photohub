package home.abel.photohub.utils;

import home.abel.photohub.model.Schedule;

import java.util.Comparator;

/**
 * Created by abel on 05.05.17.
 */
public class ScheduleComparator implements Comparator<Schedule> {

    @Override
    public int compare(Schedule obj1, Schedule obj2) {
        //  Поиск пробелов, для сортировки по фамилии
//        int k = obj1.getTaskName().substring(obj1.getTaskName().indexOf(" "))
//                .compareTo(obj2.getTaskName().substring(obj2.getTaskName().indexOf(" ")));
//        if(k == 0) {
            return obj1.getTaskName().compareTo(obj2.getTaskName());
//        }
//        else {
//            return k;
//        }
    }

}

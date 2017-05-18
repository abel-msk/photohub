package home.abel.photohub.utils;
import home.abel.photohub.model.TaskRecord;
import java.util.Comparator;

public class TaskListComparator implements Comparator<TaskRecord> {

    @Override
    public int compare(TaskRecord obj1, TaskRecord obj2) {

//        //  Поиск пробелов, для сортировки по фамилии
//        int k = obj1.getName().substring(obj1.getName().indexOf(" "))
//                .compareTo(obj2.getName().substring(obj2.getName().indexOf(" ")));
//        if(k == 0) {
            return obj1.getName().compareTo(obj2.getName());
//        }
//        else {
//            return k;
//        }
    }
}
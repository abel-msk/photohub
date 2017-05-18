package home.abel.photohub.model;

import java.util.List;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;
import home.abel.photohub.model.Schedule;

public interface ScheduleRepository extends CrudRepository<Schedule, String>, QueryDslPredicateExecutor<Schedule>  {
	 List<Schedule> findBySiteIdAndTaskName(String id, String taskName);
}

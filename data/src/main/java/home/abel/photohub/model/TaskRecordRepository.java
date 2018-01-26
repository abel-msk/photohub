package home.abel.photohub.model;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;

public interface TaskRecordRepository extends  CrudRepository<TaskRecord, String>,  QueryDslPredicateExecutor<TaskRecord>  {
		
	 List<TaskRecord> findBySiteBeanIdAndName(String id, String taskName);
	 List<TaskRecord> findBySiteBeanId(String id);
	 
}

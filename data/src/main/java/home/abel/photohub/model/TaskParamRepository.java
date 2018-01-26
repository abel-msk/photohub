package home.abel.photohub.model;

import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;

public interface TaskParamRepository extends CrudRepository<TaskParam, String>, QueryDslPredicateExecutor<TaskParam> {


}

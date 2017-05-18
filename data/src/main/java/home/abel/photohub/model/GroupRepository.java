package home.abel.photohub.model;

import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;

public interface GroupRepository extends  CrudRepository<Group, String>,
	QueryDslPredicateExecutor<Group> {
}

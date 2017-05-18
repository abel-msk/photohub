package home.abel.photohub.model;

import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;

public interface GroupAuthorityRepository extends CrudRepository<GroupAuthority, String>,
QueryDslPredicateExecutor<GroupAuthority> {

}

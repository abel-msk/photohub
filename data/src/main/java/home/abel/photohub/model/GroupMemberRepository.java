package home.abel.photohub.model;

import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;

public interface GroupMemberRepository extends CrudRepository<GroupMember, String>,
QueryDslPredicateExecutor<GroupMember> {

}

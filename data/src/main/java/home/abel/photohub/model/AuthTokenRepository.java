package home.abel.photohub.model;

import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;

public interface AuthTokenRepository extends CrudRepository<AuthToken, String>,
	QueryDslPredicateExecutor<AuthToken> {
}

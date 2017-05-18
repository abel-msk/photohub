package home.abel.photohub.model;

import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;

public interface ConfigRepository extends CrudRepository<Config, String>,
		QueryDslPredicateExecutor<Config> {

}

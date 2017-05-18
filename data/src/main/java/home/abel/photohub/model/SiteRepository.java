package home.abel.photohub.model;

import org.springframework.data.repository.CrudRepository;
import java.util.List;

import org.springframework.data.querydsl.QueryDslPredicateExecutor;

public interface SiteRepository extends CrudRepository<Site, String>, QueryDslPredicateExecutor<Site>  {
	 List<Site> findByConnectorTypeAndSiteUser(String type, String user);
}

package home.abel.photohub.model;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;


public interface PhotoRepository extends
		PagingAndSortingRepository<Photo, String>,
		QueryDslPredicateExecutor<Photo> {
	
	@Query("SELECT c FROM Photo c WHERE  c.type != 2  ORDER BY c.createTime ASC")
	public Page<Photo> ListAllByType(Pageable pageable);
	
}

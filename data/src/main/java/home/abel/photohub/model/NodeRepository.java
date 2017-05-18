package home.abel.photohub.model;

import java.util.ArrayList;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;


public interface NodeRepository extends PagingAndSortingRepository<Node, String>,
		QueryDslPredicateExecutor<Node> {
	
	@Query(value = "SELECT id, parent, photos_id from ( " +
			   "SELECT 1 as rank,  nodes.id as id, parent, photos_id, type, create_time "+
					"FROM nodes nodes  "+
					"LEFT OUTER JOIN photos photos on nodes.photos_id = photos.id  "+
					"where ((photos.type = 2) and (parent = ?1)) "+
                "union "+
				"SELECT 2 as rank,   nodes.id as id, parent, photos_id, type, create_time "+
						"FROM nodes nodes "+
						"LEFT OUTER JOIN photos photos on nodes.photos_id = photos.id  "+
						"where ((photos.type != 2) and (parent = ?1)) "+
				") nodes "+ 
			"order by rank, create_time", nativeQuery = true)	
	public Iterable<Node>  getOrderedNative( String parentId );
	
	
	@Query("SELECT c FROM Node c LEFT OUTER JOIN c.photo d WHERE d.type = 2 and  c.parent = ?1  ORDER BY d.createTime ASC")	
	public Page<Node> findFolders(String parentId, Pageable pageable);
	
	@Query("SELECT c FROM Node c LEFT OUTER JOIN c.photo d WHERE d.type = 2 and  c.parent = ?1 ORDER BY d.createTime ASC")	
	public ArrayList<Node> findFolders(String parentId);	

	@Query("SELECT c FROM Node c LEFT OUTER JOIN c.photo d WHERE d.type!= 2 and  c.parent = ?1 ORDER BY d.createTime ASC")	
	public Page<Node> findPhotos(String parentId, Pageable pageable);

	@Query("SELECT c FROM Node c LEFT OUTER JOIN c.photo d WHERE d.type!= 2 and  c.parent = ?1 ORDER BY d.createTime ASC")	
	public ArrayList<Node> findPhotos(String parentId);		
	

	//  Queryes for root elements
	
	@Query("SELECT c FROM Node c LEFT OUTER JOIN c.photo d WHERE d.type = 2 and  c.parent IS NULL ORDER BY d.createTime ASC")	
	public Page<Node> findFolders(Pageable pageable);	
	
    @Query("SELECT c FROM Node c LEFT OUTER JOIN c.photo d WHERE d.type = 2 and  c.parent IS NULL ORDER BY d.createTime ASC")	
	public ArrayList<Node> findFolders();
	
	@Query("SELECT c FROM Node c LEFT OUTER JOIN c.photo d WHERE d.type != 2 and  c.parent IS NULL ORDER BY d.createTime ASC")	
	public Page<Node> findPhotos(Pageable pageable);	
	
    @Query("SELECT c FROM Node c LEFT OUTER JOIN c.photo d WHERE d.type != 2 and  c.parent IS NULL ORDER BY d.createTime ASC")	
	public ArrayList<Node> findPhotos();
	
	
}

package home.abel.photohub.model;

import java.io.Serializable;

import javax.persistence.*;

import org.eclipse.persistence.annotations.JoinFetch;
import org.eclipse.persistence.annotations.JoinFetchType;


/**
 * The persistent class for the nodes database table.
 * 
 */
@Entity
@Table(name="nodes")
@NamedQuery(name="Node.findAll", query="SELECT n FROM Node n")

@TableGenerator(
        name="NodeSeqGenerator", 
        table="SEQUENCE", 
        pkColumnName="SEQ_NAME", 
        valueColumnName="SEQ_COUNT", 
        pkColumnValue="NODE_ID", 
        allocationSize=5)


public class Node implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(columnDefinition = "BIGINT") 
    @GeneratedValue(strategy=GenerationType.TABLE, generator="NodeSeqGenerator")
	private String id;


	private String parent;

	//bi-directional many-to-one association to Photo
	@ManyToOne
	//@JoinColumn(name="photos_id", columnDefinition = "BIGINT")
	@JoinColumn(columnDefinition = "BIGINT")
	//@JoinFetch(value=JoinFetchType.OUTER)  //Use Join for getting tata from Photos
	private Photo photo;

	public Node() {
	}
	
	public Node ( Photo photo, Node parentNode) {
		if (parentNode != null) {
			setParent(parentNode.getId());
		}
		photo.addNode(this);
		//setPhoto(photo);
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getParent() {
		return this.parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public Photo getPhoto() {
		return this.photo;
	}

	public void setPhoto(Photo photo) {
		this.photo = photo;
	}

//	public boolean isRoot() {
//		return this.root;
//	}
//	
//	public void setRoot(boolean root) {
//		this.root = root;
//	}
	
	
	public String toString() {
		return (this.id==null?"null":this.id) + "(parent="+ (this.parent==null?"null":this.parent)+ ")";
	}
	
}
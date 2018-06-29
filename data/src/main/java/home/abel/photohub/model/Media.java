package home.abel.photohub.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name="medias")
@NamedQuery(name="Media.findAll", query="SELECT s FROM Media s")
@TableGenerator(
        name="MediaSeqGenerator", 
        table="SEQUENCE", 
        pkColumnName="SEQ_NAME", 
        valueColumnName="SEQ_COUNT", 
        pkColumnValue="MEDIA_ID", 
        allocationSize=5)

public class Media implements Serializable{
	private static final long serialVersionUID = 1L;

	public static final int MEDIA_THUMB = 11;
	public static final int MEDIA_IMAGE = 12;
	public static final int MEDIA_VIDEO = 13;


	public static final int ACCESS_NET = 21;
	public static final int ACCESS_LOCAL = 22;

	@JsonIgnore
	public static String getMediaTypeName(int mediaType) {
		switch (mediaType) {
			case MEDIA_THUMB:
				return "MEDIA_THUMB";
			case MEDIA_IMAGE:
				return "MEDIA_IMAGE";
			case MEDIA_VIDEO:
				return "MEDIA_VIDEO";
			default:
				return "MEDIA_UNKNOWN";
		}
	}

	@JsonIgnore
	public static String getAccessTypeName (int assessType) {
		switch (assessType) {
			case ACCESS_NET:
				return "ACCESS_NET";
			case ACCESS_LOCAL:
				return "ACCESS_LOCAL";
			default:
				return "ACCESS_UNKNOWN";
		}
	}

	@Id
	@Column(columnDefinition = "BIGINT") 
    //@TableGenerator(name="node_gen", allocationSize=1)
    @GeneratedValue(strategy=GenerationType.TABLE, generator="MediaSeqGenerator")
	//@GeneratedValue(strategy=GenerationType.AUTO)
	//@GeneratedValue(strategy=GenerationType.IDENTITY)
	private String id;
	
	private int type;
	private String mimeType;
	private String path;
	private long size;
	private int width;
	private int height;
	private int accessType;
	
	@JsonIgnore
	@ManyToOne
	@JoinColumn(name="photoId")
	private Photo photo;
	
	
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public Photo getPhoto() {
		return photo;
	}

	public void setPhoto(Photo photo) {
		this.photo = photo;
	}	
	
	public int getAccessType() {
		return accessType;
	}

	public void setAccessType(int accessType) {
		this.accessType = accessType;
	}

	public String toString() {
		return "(id="+getId()+", type="+getMediaTypeName(getType())+", access="+getAccessTypeName(getAccessType()) + ")";
	}
}

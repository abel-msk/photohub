package home.abel.photohub.model;

import java.io.Serializable;
import java.net.URL;

import javax.persistence.*;
import org.eclipse.persistence.annotations.Index;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * The persistent class for the photos database table.
 * 
 */
@Entity
@Table(name="photos")
@NamedQuery(name="Photo.findAll", query="SELECT p FROM Photo p")

@TableGenerator(
        name="PhotoSeqGenerator", 
        table="SEQUENCE", 
        pkColumnName="SEQ_NAME", 
        valueColumnName="SEQ_COUNT", 
        pkColumnValue="PHOTO_ID", 
        allocationSize=5)


public class Photo implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public Photo(int type, String name, String descr, Site site) {
		this.name = name;
		this.type = type;
		this.descr = descr;
		this.siteBean = site;
	}

	@Id	
	@Column(columnDefinition = "BIGINT") 
    @GeneratedValue(strategy=GenerationType.TABLE, generator="PhotoSeqGenerator")	
	private String id;
	
	//@JsonIgnore
	@Index
	private String onSiteId;
	
	private String name;
	private String descr;
	private int type;
	private URL realUrl;
	private String  mediaType;
	private boolean hidden = false;
	
	@Temporal(TemporalType.TIMESTAMP)
	private Date updateTime;

	//@OneToMany(mappedBy="photo", orphanRemoval=true)
	@JsonIgnore
	@OneToMany(mappedBy="photo", orphanRemoval=true)
	private List<Node> nodes;

	//@OneToMany(cascade = CascadeType.REMOVE, mappedBy="photo")
	//@OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, mappedBy="photo")
	@OneToMany(cascade = CascadeType.ALL, mappedBy="photo")
	private List<Media> mediaObjects;
	
	//@JsonIgnore
	@ManyToOne
	@JoinColumn(name="site")
	private Site siteBean;


	/*-------------------------------------
	 *    Methods
	 -------------------------------------*/
	public Photo() {
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public String getOnSiteId() {
		return onSiteId;
	}

	public void setOnSiteId(String onSiteId) {
		this.onSiteId = onSiteId;
	}
	
	public URL getRealUrl() {
		return realUrl;
	}

	public void setRealUrl(URL realUrl) {
		this.realUrl = realUrl;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getType() {
		return this.type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public Date getUpdateTime() {
		return this.updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public List<Node> getNodes() {
		return this.nodes;
	}

	public void setNodes(List<Node> nodes) {
		this.nodes = nodes;
	}

	public Node addNode(Node node) {
		if (getNodes() == null ) {
			this.nodes = new ArrayList<Node>();
		}
		getNodes().add(node);
		node.setPhoto(this);
		return node;
	}

	public Node removeNode(Node node) {
		getNodes().remove(node);
		node.setPhoto(null);
		return node;
	}

	public Site getSiteBean() {
		return this.siteBean;
	}

	public void setSiteBean(Site siteBean) {
		this.siteBean = siteBean;
	}
	
	public String getMediaType() {
		return mediaType;
	}

	public void setMediaType(String mediaType) {
		this.mediaType = mediaType;
	}

	public Date getCreateTime() {
		return this.createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public String getDescr() {
		return this.descr;
	}

	public void setDescr(String descr) {
		this.descr = descr;
	}
	
	public String toString() {
		
		String typeName = "UNK";
		
		if (this.type == ModelConstants.OBJ_SINGLE) {
			typeName = "OBJ/" + this.getMediaType();
		}
		else if (this.type == ModelConstants.OBJ_FOLDER ) {
			typeName = "FLD";
		}
		else if  (this.type == ModelConstants.OBJ_SERIES ) {
			typeName = "SER";
		}
		
		return typeName + "-" + this.name +"("+typeName+"/"+this.id+")";
	}
	
	public List<Media> getMediaObjects() {
		return mediaObjects;
	}

	public void setMediaObjects(List<Media> mediaObjects) {
		this.mediaObjects = mediaObjects;
	}
	
	public Media addMediaObject(Media mediaObject) {
		if (getMediaObjects() == null ) {
			this.mediaObjects = new ArrayList<Media>();
		}
		getMediaObjects().add(mediaObject);		
		mediaObject.setPhoto(this);
		return mediaObject;
	}
	
	public Media removeMediaObject(Media mediaObject) {
		getMediaObjects().remove(mediaObject);
		mediaObject.setPhoto(null);
		return mediaObject;
	}
	
	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}
	
	
	/*-------------------------------------
	 * 
	 *    ExIF mata tags
	 *    
	 -------------------------------------*/
	
	private String unicId;
	private String aperture;
	private String camMake;
	private String camModel;
	private String shutterSpeed;
	
	@Index
	@Temporal(TemporalType.TIMESTAMP)
	private Date createTime;

	@Temporal(TemporalType.TIMESTAMP)
	private Date digitTime;
	private String dpi;
	private String expTime;

	@Column(name="focal_len")
	private String focalLen;
	private String focusDist;
	private String isoSpeed;

	@Lob
	private String gpsAlt;

	private double gpsDir;

	private double gpsLat;

	private double gpsLon;


	
	
	/*-------------------------------------
	 * 
	 *    ExIF tags methods
	 *    
	 -------------------------------------*/
	public String getUnicId() {
		return unicId;
	}

	public void setUnicId(String unicId) {
			this.unicId = unicId;
	}
	
	public String getAperture() {
		return this.aperture;
	}

	public void setAperture(String aperture) {
		this.aperture = aperture;
	}

	public String getCamMake() {
		return this.camMake;
	}

	public void setCamMake(String camMake) {
		this.camMake = camMake;
	}

	public String getCamModel() {
		return this.camModel;
	}

	public void setCamModel(String camModel) {
		this.camModel = camModel;
	}


	public Date getDigitTime() {
		return this.digitTime;
	}

	public void setDigitTime(Date digitTime) {
		if (digitTime != null ) {
			this.digitTime = digitTime;
		}
	}

	public String getDpi() {
		return this.dpi;
	}

	public void setDpi(String dpi) {
		this.dpi = dpi;
	}

	public String getShutterSpeed() {
		return this.shutterSpeed;
	}

	public void setShutterSpeed(String shutterSpeed) {
		this.shutterSpeed = shutterSpeed;
	}

	public String getExpTime() {
		return this.expTime;
	}

	public void setExpTime(String expTime) {
		this.expTime = expTime;
	}

	public String getFocalLen() {
		return this.focalLen;
	}

	public void setFocalLen(String focalLen) {
		this.focalLen = focalLen;
	}

	public String getFocusDist() {
		return this.focusDist;
	}

	public void setFocusDist(String focusDist) {
		this.focusDist = focusDist;
	}

	public String getGpsAlt() {
		return this.gpsAlt;
	}

	public void setGpsAlt(String gpsAlt) {
		this.gpsAlt = gpsAlt;
	}

	public double getGpsDir() {
		return this.gpsDir;
	}

	public void setGpsDir(double gpsDir) {
		this.gpsDir = gpsDir;
	}

	public double getGpsLat() {
		return this.gpsLat;
	}

	public void setGpsLat(double gpsLat) {
		this.gpsLat = gpsLat;
	}

	public double getGpsLon() {
		return this.gpsLon;
	}

	public void setGpsLon(double gpsLon) {
		this.gpsLon = gpsLon;
	}

	public String getIsoSpeed() {
		return this.isoSpeed;
	}

	public void setIsoSpeed(String isoSpeed) {
		this.isoSpeed = isoSpeed;
	}

	
}
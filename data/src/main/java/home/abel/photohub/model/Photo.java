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
	private long allMediaSize = 0;
	private String backupSrc = null;
	
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

	@Temporal(TemporalType.TIMESTAMP)
	private Date lastScanDate = null;

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
			typeName = "OBJ|" + this.getMediaType();
		}
		else if (this.type == ModelConstants.OBJ_FOLDER ) {
			typeName = "FLD|";
		}
		else if  (this.type == ModelConstants.OBJ_SERIES ) {
			typeName = "SER|";
		}
		
		return "<type="+typeName + ", name=" + this.name +", id="+this.id+">";
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

	public Media getBaseMedia() {
		int mediaType = Media.MEDIA_IMAGE;
		if ( getMediaType().startsWith("video")) {
			mediaType = Media.MEDIA_VIDEO;
		}

		Media mediaObject = null;
		for(Media media: getMediaObjects()) {
			if (media.getType() == mediaType) {
				mediaObject = media;
				break;
			}
		}
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
	private double aperture;
	private String camMake;
	private String camModel;
	private double shutterSpeed;
	
	@Index
	@Temporal(TemporalType.TIMESTAMP)
	private Date createTime;

	@Temporal(TemporalType.TIMESTAMP)
	private Date digitTime;
	private String dpi;
	private double expTime;

	//@Column(name="focal_len")
	private double focalLen;
	private String focusDist;
	private int iso;

	//@Lob
	private double gpsAlt;

	private double gpsDir;

	private double gpsLat;

	private double gpsLon;


	private int flash;
	private int orientation;
	private String software;
	private String userComment;
	private double brightness;


	
	
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



	public String getFocusDist() {
		return this.focusDist;
	}

	public void setFocusDist(String focusDist) {
		this.focusDist = focusDist;
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


	public double getAperture() {
		return aperture;
	}

	public void setAperture(double aperture) {
		this.aperture = aperture;
	}

	public double getShutterSpeed() {
		return shutterSpeed;
	}

	public void setShutterSpeed(double shutterSpeed) {
		this.shutterSpeed = shutterSpeed;
	}

	public double getExpTime() {
		return expTime;
	}

	public void setExpTime(double expTime) {
		this.expTime = expTime;
	}

	public double getFocalLen() {
		return focalLen;
	}

	public void setFocalLen(double focalLen) {
		this.focalLen = focalLen;
	}

	public int getIso() {
		return iso;
	}

	public void setIso(int iso) {
		this.iso = iso;
	}

	public double getGpsAlt() {
		return gpsAlt;
	}

	public void setGpsAlt(double gpsAlt) {
		this.gpsAlt = gpsAlt;
	}

	public int getFlash() {
		return flash;
	}

	public void setFlash(int flash) {
		this.flash = flash;
	}

	public int getOrientation() {
		return orientation;
	}

	public void setOrientation(int orientation) {
		this.orientation = orientation;
	}

	public String getSoftware() {
		return software;
	}

	public void setSoftware(String software) {
		this.software = software;
	}

	public String getUserComment() {
		return userComment;
	}

	public void setUserComment(String userComment) {
		this.userComment = userComment;
	}

	public double getBrightness() {
		return brightness;
	}

	public void setBrightness(double brightness) {
		this.brightness = brightness;
	}

	public Date getLastScanDate() {
		return lastScanDate;
	}

	public void setLastScanDate(Date lastScanDate) {
		this.lastScanDate = lastScanDate;
	}


	public String getBackupSrc() {
		return backupSrc;
	}

	public void setBackupSrc(String backupSrc) {
		this.backupSrc = backupSrc;
	}

	public long getAllMediaSize() {
		return allMediaSize;
	}

	public void setAllMediaSize(long allMediaSize) {
		this.allMediaSize = allMediaSize;
	}



}
package home.abel.photohub.model;

import java.io.Serializable;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * The persistent class for the site database table.
 * 
 */
@Entity
@Table(name="sites")
@NamedQuery(name="Site.findAll", query="SELECT s FROM Site s")
@TableGenerator(
        name="SiteSeqGenerator", 
        table="SEQUENCE", 
        pkColumnName="SEQ_NAME", 
        valueColumnName="SEQ_COUNT", 
        pkColumnValue="SITE_ID", 
        allocationSize=5)

public class Site implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(columnDefinition = "BIGINT") 
    @GeneratedValue(strategy=GenerationType.TABLE, generator="SiteSeqGenerator")
	private String id;

	//   Site name entered by user at creation time
	private String name;

	//   Folder on for store objects locally
	private String root;
	
	@JsonIgnore
	@OneToMany(cascade = CascadeType.REMOVE, mappedBy = "site", fetch = FetchType.LAZY, orphanRemoval = true)
	private List<Schedule> schedules;

	//    Connector type.  Returned from Connector class.
	//@Column(name="connectorType")
	private String connectorType;
	
	//@Column(name="connectorState")
	private String connectorState;
	
	//bi-directional many-to-one association to sites property
	@OneToMany(cascade = CascadeType.ALL, mappedBy = "site", fetch = FetchType.EAGER, orphanRemoval = true)
	private List<SiteProperty> properties;

	//bi-directional many-to-one association to Photo
	@JsonIgnore
	@OneToMany(mappedBy="siteBean")
	private List<Photo> photos;

	@Temporal(TemporalType.TIMESTAMP)
	//@Column(name="lastScanDate")
	private Date lastScanDate;

	//   Entered by user, used for Create connector
	private String siteUser;
	
	
	@JsonIgnore
	@OneToMany(cascade = {CascadeType.REMOVE,CascadeType.PERSIST,CascadeType.MERGE }, mappedBy="siteBean", fetch = FetchType.LAZY, orphanRemoval = true)
	private List<TaskRecord> tasksLog;



	/*
		Method definition

	 */
	public Site() {
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getConnectorType() {
		return connectorType;
	}

	public void setConnectorType(String connectorType) {
		this.connectorType = connectorType;
	}
	
	/**
	 * Return path for sites folder for store temp objects
	 * @return
	 */
	public String getRoot() {
		return this.root;
	}

	public void setRoot(String root) {
		this.root = root;
	}

//	public int getType() {
//		return this.type;
//	}
//
//	public void setType(int type) {
//		this.type = type;
//	}
//
//	public String getUrl() {
//		return this.url;
//	}
//
//	public void setUrl(String url) {
//		this.url = url;
//	}

	public List<Photo> getPhotos() {
		return this.photos;
	}

	public void setPhotos(List<Photo> photos) {
		this.photos = photos;
	}

	public Photo addPhoto(Photo photo) {
		getPhotos().add(photo);
		photo.setSiteBean(this);
		return photo;
	}

	public Photo removePhoto(Photo photo) {
		getPhotos().remove(photo);
		photo.setSiteBean(null);
		return photo;
	}
	
	public List<SiteProperty> getProperties() {
		return this.properties;
	}

	public void setProperties(List<SiteProperty> properties) {
		if (properties != null ) {
	 		for (SiteProperty prop: properties) {
				this.addProperty(prop);
			}
		}
		//this.properties = properties;
	}

	public SiteProperty removeProperty(SiteProperty property) {
		getProperties().remove(property);
		property.setSite(null);
		return property;
	}
	
	
	public String getSiteUser() {
		return siteUser;
	}

	public void setSiteUser(String siteUser) {
		this.siteUser = siteUser;
	}

	
	public String getConnectorState() {
		return connectorState;
	}

	public void setConnectorState(String state) {
		this.connectorState = state;
	}

	public List<TaskRecord> getTasksLog() {
		return tasksLog;
	}

	public void setTasksLog(List<TaskRecord> tasksLog) {
		this.tasksLog = tasksLog;
	}

	public TaskRecord addTaskRecord(TaskRecord task) {
		if (getTasksLog() == null) {
			setTasksLog(new ArrayList<TaskRecord>());
		}
		getTasksLog().add(task);
		task.setSiteBean(this);
		return task;
	}

	public TaskRecord removeTaskRecord(TaskRecord task) {
		if (getTasksLog() == null) {
			return task;
		}
		getTasksLog().remove(task);
		task.setSiteBean(null);
		return task;
	}
		
	
	
//	public String getAccessPasswd() {
//		return accessPasswd;
//	}
//
//	public void setAccessPasswd(String accessPasswd) {
//		this.accessPasswd = accessPasswd;
//	}
//
//	public void setDefaultSite(boolean isDefault) {
//		this.defaultSite = isDefault;
//	}
//	public boolean isDefaultSite() {
//		return this.defaultSite;
//	}
	

	public String getProperty(String name) {
		List<SiteProperty> sitePropertiesList = this.getProperties();
		if (sitePropertiesList != null ) {
			for (SiteProperty prop : sitePropertiesList) {
				if ( name.equals(prop.getName())) {
					return prop.getValue();
				}
			}
		}
		return null;
	}
	
	public SiteProperty getPropertyObj(String name) { 
		List<SiteProperty> sitePropertiesList = this.getProperties();
		if (sitePropertiesList != null ) {
			for (SiteProperty prop : this.getProperties()) {
				if ( name.equals(prop.getName())) {
					return prop;
				}
			}
		}
		return null;
	}
	
	public SiteProperty addProperty(SiteProperty property) {
		addProperty(property.getName(),property.getValue());
//		property.setSite(this);
//		getProperties().add(property);
		return property;
	}
	
	public void addProperty(String name, String value) {
		SiteProperty prop = getPropertyObj(name);
		if  (prop == null) {
			prop = new SiteProperty();
			prop.setSite(this);
			prop.setName(name);
			prop.setValue(value);
			if (this.getProperties() == null) {
				properties = new ArrayList<SiteProperty>(); 
			}
			this.getProperties().add(prop);  //Add new propertyes to list
		}
		else {
			prop.setValue(value);
		}
	}

	public String toString() {
		return this.name+"("+this.id+")";
	}
	
	
	public Date getLastScanDate() {
		return lastScanDate;
	}

	public void setLastScanDate(Date lastScanDate) {
		this.lastScanDate = lastScanDate;
	}
	
	public List<Schedule> getSchedules() {
		return this.schedules;
		
	}

	public Schedule getSchedule(String taskName) {
		if (this.schedules != null) {
			for (Schedule current: this.schedules ) {
				if (current.getTaskName().equals(taskName) ) { return current; }
			}
		}
		return null;
	}
	
	public void  setSchedules(List<Schedule> schedules) {
		this.schedules = schedules;
	}
	
	
	public void  addScedule(Schedule theSch) {
		if (this.schedules == null) {
			this.schedules = new ArrayList<Schedule>();
		}
		this.schedules.add(theSch);
	}
	
	public Schedule removeSchedule(Schedule theSch) {
		getSchedules().remove(theSch);
//		theSch.setSite(null);
//		theSch.setTaskName(null);
		return theSch;
	}
	
	
	
}
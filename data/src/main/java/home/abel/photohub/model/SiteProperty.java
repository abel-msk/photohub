package home.abel.photohub.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;

class SiteProperiesCK {
    private String site;
    private String name;
}


@Entity
@IdClass(SiteProperiesCK.class)
@Table(name="site_properties")
//@NamedQuery(name="SiteProperties.findAll", query="SELECT n FROM SiteProperties n")
public class SiteProperty implements Serializable {
	private static final long serialVersionUID = 1L;
	
//	@Id
//	@Column(columnDefinition = "BIGINT") 
//	@GeneratedValue(strategy=GenerationType.AUTO)
//	//@GeneratedValue(strategy=GenerationType.IDENTITY)
//	private String id;
//	
	
	@NotNull
	@Id
	private String name;
	
	@NotNull
	@Id
	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="id")
	private Site site;
	
//	
//	@ManyToOne
//	@JoinColumn(name="site_id")
//	private Site siteBean;
	
	//private String siteId;
	//private String name;
	
	private String value;
	private String description;
	
	
//	public String getId() {
//		return id;
//	}
//	public void setId(String id) {
//		this.id = id;
//	}
	
	
	public SiteProperty() {
	}
		
	public SiteProperty(String name, String descr) {
		this.name = name;
		this.description = descr;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public Site getSite() {
		return site;
	}
	public void setSite(Site site) {
		this.site = site;
	}
//	public String getSiteId() {
//		return siteId;
//	}
//	public void setSiteId(String siteId) {
//		this.siteId = siteId;
//	}

	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	
}

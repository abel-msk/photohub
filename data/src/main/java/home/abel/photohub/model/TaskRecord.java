package home.abel.photohub.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.querydsl.core.annotations.PropertyType;
import com.querydsl.core.annotations.QueryType;

@Entity
@Table(name="task_records")
@NamedQuery(name="TaskRecord.findAll", query="SELECT s FROM TaskRecord s")
@TableGenerator(
        name="TasksSeqGenerator", 
        table="SEQUENCE", 
        pkColumnName="SEQ_NAME", 
        valueColumnName="SEQ_COUNT", 
        pkColumnValue="TASK_REC_ID", 
        allocationSize=20)

public class TaskRecord implements Serializable {
	private static final long serialVersionUID = 1L;
	
	@Id
	@Column(columnDefinition = "BIGINT") 
    @GeneratedValue(strategy=GenerationType.TABLE, generator="TasksSeqGenerator")	
	private String id;
	private String name;
	private String status;
	
	@Column(columnDefinition = "VARCHAR(256)") 
	private String message;
	
	
	@JsonIgnore
	//@ManyToOne (fetch = FetchType.LAZY)
	@ManyToOne
	@JoinColumn(name="site")
	private Site siteBean = null;

	@Column(name="schedule_id", columnDefinition = "BIGINT")
	private String scheduleId = null;

	@QueryType(PropertyType.DATETIME)
	@Temporal(TemporalType.TIMESTAMP)
	private Date startTime;

	@QueryType(PropertyType.DATETIME)
	@Temporal(TemporalType.TIMESTAMP)
	private Date stopTime;	
	

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Site getSiteBean() {
		return siteBean;
	}

	public void setSiteBean(Site siteBean) {
		this.siteBean = siteBean;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getStopTime() {
		return stopTime;
	}

	public void setStopTime(Date stopTime) {
		this.stopTime = stopTime;
	}


	public String getScheduleId() {
		return scheduleId;
	}

	public void setScheduleId(String scheduleId) {
		this.scheduleId = scheduleId;
	}

	public String toString() {
		return name+"(id="+id+",st="+status+",site="+siteBean+")";
	}

}

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
@Table(name="schedules")
@NamedQuery(name="Schedule.findAll", query="SELECT n FROM Schedule n")

@TableGenerator(
        name="SchedSeqGenerator", 
        table="SEQUENCE", 
        pkColumnName="SEQ_NAME", 
        valueColumnName="SEQ_COUNT", 
        pkColumnValue="SCHED_ID", 
        allocationSize=5)

public class Schedule implements Serializable {
	private static final long serialVersionUID = 1L;

	public Schedule() {	
	}
	
	@Id	
	@Column(columnDefinition = "BIGINT") 
    @GeneratedValue(strategy=GenerationType.TABLE, generator="SchedSeqGenerator")	
	private String id;
	
	@JsonIgnore
	@ManyToOne
	private Site site = null;
	
	private String taskName;
	
	private String seconds = "*"; 
	private String minute = "*";     //0-59
	private String hour = "*";       //0-23
	private String dayOfMonth = "*"; //1-31
	private String month = "*";      //1-12
	private String dayOfWeek= "*";   //0-6
	private boolean enable = false;
	

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getTaskName() {
		return taskName;
	}
	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}
	public Site getSite() {
		return site;
	}
	public void setSite(Site site) {
		this.site = site;
		if (site != null) {
			this.site.addScedule(this);
		}
	}
	
	public String getSeconds() {
		return seconds;
	}
	public void setSeconds(String seconds) {
		this.seconds = seconds;
	}

	public String getMinute() {
		return minute;
	}
	public void setMinute(String minute) {
		this.minute = minute;
	}
	public String getHour() {
		return hour;
	}
	public void setHour(String hour) {
		this.hour = hour;
	}
	public String getDayOfMonth() {
		return dayOfMonth;
	}
	public void setDayOfMonth(String dayOfMonth) {
		this.dayOfMonth = dayOfMonth;
	}
	public String getMonth() {
		return month;
	}
	public void setMonth(String month) {
		this.month = month;
	}
	public String getDayOfWeek() {
		return dayOfWeek;
	}
	public void setDayOfWeek(String dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
	}
	public boolean isEnable() { return enable; }
	public void setEnable(boolean enable) {this.enable = enable;}
	
	public String toString() {
		return seconds + " " + minute + " " + hour + " " + dayOfMonth + " " + month + " " + dayOfWeek;
	}
	
}

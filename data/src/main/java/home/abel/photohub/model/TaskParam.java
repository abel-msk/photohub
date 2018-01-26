package home.abel.photohub.model;


import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name="TASK_PARAM")
@NamedQuery(name="TaskParam.findAll", query="SELECT s FROM TaskParam s")
@TableGenerator(
        name="TParamSeqGen",
        table="SEQUENCE",
        pkColumnName="SEQ_NAME",
        valueColumnName="SEQ_COUNT",
        pkColumnValue="TPARAM_ID",
        allocationSize=5)

public class TaskParam implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(columnDefinition = "BIGINT")
    @GeneratedValue(strategy=GenerationType.TABLE, generator="TParamSeqGen")
    private String id;
    private String name = null;
    private String value = null;
    private String type = null;


    @JsonIgnore
    //@ManyToOne (fetch = FetchType.LAZY)
    @ManyToOne
    private Schedule schedule = null;

    public TaskParam() {
    }

    public TaskParam(String name, String value, String type) {
        this.name = name;
        this.type=type;
        this.value=value;
    }

    /*
        Getters and Setters
     */
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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }

    @Override
    public String toString() {
        return (type!=null?type+":":"")+name+"("+(id==null?"null":id)+")="+(value!=null?value:"null");
    }
}

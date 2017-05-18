package home.abel.photohub.model;

import java.io.Serializable;

import javax.persistence.*;


/**
 * The persistent class for the group_members database table.
 * 
 */
@Entity
@Table(name="group_members")
@NamedQuery(name="GroupMember.findAll", query="SELECT g FROM GroupMember g")
public class GroupMember implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(columnDefinition = "BIGINT") 
	@GeneratedValue(strategy=GenerationType.AUTO)
	//@GeneratedValue(strategy=GenerationType.IDENTITY)
	private String id;

	private String username;

	//bi-directional many-to-one association to Group
	@ManyToOne
	private Group group;

	public GroupMember() {
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Group getGroup() {
		return this.group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}

}
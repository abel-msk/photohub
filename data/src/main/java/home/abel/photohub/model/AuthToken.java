package home.abel.photohub.model;

import java.io.Serializable;

import javax.persistence.*;
import java.util.Date;


/**
 * The persistent class for the auth_token database table.
 * 
 */
@Entity
@Table(name="auth_token")
@NamedQuery(name="AuthToken.findAll", query="SELECT a FROM AuthToken a")
public class AuthToken implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	private String token;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="last_used")
	private Date lastUsed;

	@Column(name="live_time")
	private int liveTime;

	@Column(name="user_agent")
	private String userAgent;

	//bi-directional many-to-one association to User
	@ManyToOne
	@JoinColumn(name="username")
	private User user;

	public AuthToken() {
	}

	public String getToken() {
		return this.token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public Date getLastUsed() {
		return this.lastUsed;
	}

	public void setLastUsed(Date lastUsed) {
		this.lastUsed = lastUsed;
	}

	public int getLiveTime() {
		return this.liveTime;
	}

	public void setLiveTime(int liveTime) {
		this.liveTime = liveTime;
	}

	public String getUserAgent() {
		return this.userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public User getUser() {
		return this.user;
	}

	public void setUser(User user) {
		this.user = user;
	}

}
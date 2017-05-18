package home.abel.photohub.connector.prototype;

import java.io.Serializable;
import java.net.URL;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;


public interface SiteCredentialInt extends Serializable{
	
	public enum AuthReceiveType {
		AUTH_TYPE_NET,
		AUTH_TYPE_DIRECT
	}
	
	/**
	 *   The url user must connect for  perform authentication and get access code
	 */
	public URL getUserLoginFormUrl();
	/**
	 *   The url user must connect for  perform authentication and get access code
	 * @param url
	 */
	public void setUserLoginFormUrl(URL url);
	
	@JsonIgnore
	public SiteConnectorInt getConnector();
	
	/**
	 *   Access code returned from site on success authentication
	 * @return
	 */
	public String getAccessToken();
	/**
	 *   Access code returned from site on success authentication
	 * @param token
	 */
	public void setAccessToken(String token);
	
	/**
	 *   The status of site connection an code exchange
	 * @return
	 */
	public SiteStatusEnum getState();
	/**
	 * The status of site connection an code exchange
	 * @param cred
	 */
	public void setState(SiteStatusEnum cred);
	
	/**
	 *   The message we display for user before redirec to authentication sites url
	 * @param msg
	 */
	public void setUserMessage(String msg);
	/**
	 *   The message we display for user before redirec to authentication sites url 
	 * @return
	 */
	public String getUserMessage();
	
	
	/**
	 *  Return type of auth we are waiting.  It can be auth throught redirect to http listener, 
	 *  or direct auth code insertion throught tthe instrface.
	 * @return
	 */
	public AuthReceiveType getAuthReceiveType();
	public void setAuthReceiveType(AuthReceiveType type);
	
	public void setProperties(Map<String, SitePropertyInt> porpMap);
	public Map<String, SitePropertyInt> getProperties();
	
	@JsonIgnore
	public SitePropertyInt getPropertyObj(String name);
	@JsonIgnore
	public String getProperty(String name);
	@JsonIgnore
	public void setProperty(SitePropertyInt prop);
	@JsonIgnore
	public void setProperty(String name, String value);
	@JsonIgnore
	public void addProperty(SitePropertyInt prop);
	
}

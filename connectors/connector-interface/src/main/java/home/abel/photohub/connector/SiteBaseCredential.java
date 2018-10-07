package home.abel.photohub.connector;

import home.abel.photohub.connector.prototype.SiteConnectorInt;
import home.abel.photohub.connector.prototype.SiteCredentialInt;
import home.abel.photohub.connector.prototype.SitePropertyInt;
import home.abel.photohub.connector.prototype.SiteStatusEnum;

import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class SiteBaseCredential implements SiteCredentialInt, Serializable  {
	protected static final long serialVersionUID = 1L;
		
	protected URL userLoginFormUrl = null;
	protected String accessToken  = null;
	protected String userMessage = null;
	protected SiteConnectorInt connector= null;
	protected Map<String, SitePropertyInt>properties = new HashMap<String, SitePropertyInt>();
	protected SiteStatusEnum state;
	protected AuthReceiveType authReceiveType = AuthReceiveType.AUTH_TYPE_NET;
	public SiteBaseCredential(){}

	public SiteBaseCredential(SiteConnectorInt connector) {
		this.connector = connector;
	}
	public SiteConnectorInt getConnector() {
		return this.connector;
	}

	@Override
	public URL getUserLoginFormUrl() {
		return this.userLoginFormUrl;
	}

	@Override
	public void setUserLoginFormUrl(URL url) {
		this.userLoginFormUrl = url;
	}

	@Override
	public String getAccessToken() {
		return accessToken;
	}

	@Override
	public void setAccessToken(String token) {	
		this.accessToken = token;
	}

	@Override
	public void setUserMessage(String msg) {
		this.userMessage = msg;
	}

	@Override
	public String getUserMessage() {
		return this.userMessage;
	}

	@Override
	public void setProperties(Map<String, SitePropertyInt> porpMap) {
		properties  = porpMap;
	}

	@Override
	public Map<String, SitePropertyInt> getProperties() {
		return properties;
	}

	@Override
	public SitePropertyInt getPropertyObj(String name) {
		return properties.get(name);
	}
	
	@Override
	public String getProperty(String name) {
		String retValue  = null;
		if ( properties.get(name) != null) {
			retValue  = properties.get(name).getValue();
		};
		return retValue;
	}
	
	@Override
	public void setProperty(String name, String value) {
		SitePropertyInt propObj = properties.get(name);
		if (propObj != null) {
			propObj.setValue(value);
		}
	}
	
	@Override
	public void setProperty(SitePropertyInt prop) {
		if ((prop != null) &&( properties.get(prop.getName()) != null)) {
			properties.put(prop.getName(),prop );
		}
	}
	
	@Override
	public void addProperty(SitePropertyInt prop) {
		properties.put(prop.getName(),prop );
	}
	
	@Override
	public void setState(SiteStatusEnum state) {
		this.state = state;
	}
	
	@Override
	public SiteStatusEnum getState() {
		return this.state;
	}
	
	
	@Override
	public AuthReceiveType getAuthReceiveType() {
		return authReceiveType;
	}
	
	@Override
	public void setAuthReceiveType(AuthReceiveType type) {
		this.authReceiveType =  type;
		
	}

	
}

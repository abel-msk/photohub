package home.abel.photohub.web.model;

import java.net.URL;

import home.abel.photohub.connector.prototype.SiteCredentialInt;

public class AuthWaitSession {
	SiteCredentialInt connectorCred = null;
	URL returnToPageUrl = null;
	
	public AuthWaitSession(SiteCredentialInt connectorCred, URL accessMeUrl ) {
		this.connectorCred = connectorCred;
		this.returnToPageUrl = accessMeUrl;
	}
	
	public SiteCredentialInt getConnectorCred() {
		return connectorCred;
	}
	public void setConnectorCred(SiteCredentialInt connectorCred) {
		this.connectorCred = connectorCred;
	}
	public URL getReturnToPageUrl() {
		return returnToPageUrl;
	}
	public void setReturnToPageUrl(URL returnToPageUrl) {
		this.returnToPageUrl = returnToPageUrl;
	}

}

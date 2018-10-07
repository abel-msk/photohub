package home.abel.photohub.connector.google;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.api.client.util.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

//extends GenericJson

public class GoogleAPIKeys {
	final Logger logger = LoggerFactory.getLogger(GoogleAPIKeys.class);


	public final static String REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";


	@Key("client_id")
    private String clientId;
    @Key("auth_uri")
    private String authUri;
    @Key("token_uri")
    private String tokenUri;
	@Key("client_email")
    private String clientEmail;
    @Key("client_secret")
    private String clientSecret;
    @Key("redirect_uris")
    private List<String> redirectUris;
    
    @Key("client_x509_cert_url")
    private String client_x509_cert_url;    
    @Key("auth_provider_x509_cert_url")
    private String auth_provider_x509_cert_url;
   
    @JsonIgnore
    private URI listenerUri;
    
    @JsonIgnore
    private boolean canUseCallback = false;
    
	@JsonIgnore
    private String resourceFile;
    
    @JsonIgnore
    private String structureStartLabel;

    private URI defaultRedirectUri= null;
   
    
    
    public GoogleAPIKeys() throws URISyntaxException {
//    	List<String> list = new ArrayList<String>();
//    	list.add(REDIRECT_URI);
//    	this.setRedirectUris(list);
    	listenerUri = new URI(REDIRECT_URI);
		defaultRedirectUri = listenerUri;
    }
    
    public static URI getDefaultUri() throws URISyntaxException  {
    	return new URI(REDIRECT_URI);
    }
    
    public boolean iskeysLoaded() {
    	return (clientSecret != null);
    }
    
    
	public String getClientId() {
		return clientId;
	}
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	public String getAuthUri() {
		return authUri;
	}
	public void setAuthUri(String authUri) {
		this.authUri = authUri;
	}
	public String getTokenUri() {
		return tokenUri;
	}
	public void setTokenUri(String tokenUri) {
		this.tokenUri = tokenUri;
	}
	public String getClientEmail() {
		return clientEmail;
	}
	public void setClientEmail(String clientEmail) {
		this.clientEmail = clientEmail;
	}
	public String getClientSecret() {
		return clientSecret;
	}
	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}
	public List<String> getRedirectUris() {
		return redirectUris;
	}
	public void setRedirectUris(List<String> redirectUris) {
		this.redirectUris = redirectUris;
	}
	public String getClient_x509_cert_url() {
		return client_x509_cert_url;
	}
	public void setClient_x509_cert_url(String client_x509_cert_url) {
		this.client_x509_cert_url = client_x509_cert_url;
	}
	public String getAuth_provider_x509_cert_url() {
		return auth_provider_x509_cert_url;
	}
	public void setAuth_provider_x509_cert_url(String auth_provider_x509_cert_url) {
		this.auth_provider_x509_cert_url = auth_provider_x509_cert_url;
	}
	
    public String getResourceFile() {
		return resourceFile;
	}
	public void setResourceFile(String resourceFile) {
		this.resourceFile = resourceFile;
	}
	public String getStructureStartLabel() {
		return structureStartLabel;
	}
	public void setStructureStartLabel(String structureStartLabel) {
		this.structureStartLabel = structureStartLabel;
	}
	
    public URI getListenerUri() { return listenerUri; }

    /**
     *   Сохраняет URI на которое надо пересести возварат кода авторизации.
     *   Обязательно проверяет совпадает ли этот урл с урлом прописанных у гугла для этого приложения и ключа.
     *   Если улс совпадает то выставляет canUseCallback true, иначе  false.
     * @param listenerUri
     */
	public void setListenerUri(URI listenerUri) {
		this.listenerUri = listenerUri;


		if (listenerUri.toString().equalsIgnoreCase(defaultRedirectUri.toString())) {
			logger.trace("[Google.setListenerUri] Listener URI set to default uri for app - " + listenerUri);
			setCanUseCallback(false);
			return;
		}

		for ( String redirURIStr : getRedirectUris()) {
			try {

				URI redirURI = new URI(redirURIStr);

				logger.trace("[Google.setListenerUri] Check redirect uri in list " + redirURI + ", is equal with " + listenerUri +
						" - " + redirURI.getHost() + "=" + listenerUri.getHost());

				if (redirURIStr.equalsIgnoreCase(listenerUri.toString())
						|| (
							redirURI.getHost().equalsIgnoreCase(listenerUri.getHost())
						    && (redirURI.getScheme().equalsIgnoreCase(listenerUri.getScheme()))
							)
						) {
					setCanUseCallback(true);
					return;
				}
			}
			catch (URISyntaxException synErr) {
				logger.warn("Cannot parse URI", synErr);
			}
			catch (NullPointerException npe) {
				// skip
			}
		}
		setCanUseCallback(false);
	}

	/**
	 * Определяет будет ли гугл делать перевод вызова после успешной авторизации на  setListenerUri
	 * Или придется вводит авторизационный код вручную.
	 * @return
	 */
	public boolean isCanUseCallback() {
		return canUseCallback;
	}

	public void setCanUseCallback(boolean canUseCallback) {
		this.canUseCallback = canUseCallback;
	}
	
}
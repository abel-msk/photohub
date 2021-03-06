package home.abel.photohub.connector.google;


import com.google.api.client.auth.oauth2.*;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class GoogleOAuth2 {
    final Logger logger = LoggerFactory.getLogger(GoogleOAuth2.class);
    
    private List<String> SCOPE = Arrays.asList(
        "https://www.googleapis.com/auth/plus.me",
        "https://www.googleapis.com/auth/plus.stream.read",
        "https://www.googleapis.com/auth/plus.login",
        "https://www.googleapis.com/auth/plus.stream.write",   // ?
        "https://www.googleapis.com/auth/plus.media.upload",
        "https://picasaweb.google.com/data/");  // ?
	//https://www.googleapis.com/auth/drive;
	//https://www.googleapis.com/auth/drive.appfolder  - application data folder
    
    private java.io.File DATA_STORE_DIR;
    private FileDataStoreFactory LocaldataStore;    
    private HttpTransport httpTransport;
    private JsonFactory jsonParserFactory;
    private String userId = null;
    private GoogleAPIKeys keysObj;
	private Credential credential  = null;
	private AuthorizationCodeFlow authFlow = null;
	private String storeKey = "";
	//private URL extCallbackUrl = null;
	
    //private String REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";
    //private String REDIRECT_URI = "http://localhost";
    
    public GoogleOAuth2() throws Exception {
    	DATA_STORE_DIR = new java.io.File(System.getProperty("user.home"), ".store/photohub");
    	LocaldataStore = new FileDataStoreFactory(DATA_STORE_DIR);
    	jsonParserFactory = new JacksonFactory();
    	httpTransport = new NetHttpTransport();
    	
//        HttpRequestFactory requestFactory =
//            HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
//              @Override
//              public void initialize(HttpRequest request) throws IOException {
//                credential.initialize(request);
//                request.setParser(new JsonObjectParser(JSON_FACTORY));
//              }
//            });
    }
    
    /**
     *  Return fake UserID  for persist user credential
     * @return
     */
    public String getUserId() {
    	return this.userId;
    }

    /**
     * Set user id  fro store auth token
     * @param userId
     */
    public void setUserId(String userId) {
    	this.userId = userId;
    }
    
    /**
     *  Return fake UserID  for persist user credential
     * @return
     */
    public String getStoreKey() {
    	return this.storeKey;
    }

    /**
     * Set user id  fro store auth token
     * @param theKey
     */
    public void setStoreKey(String theKey) {
    	this.storeKey = theKey;
    }
    
    /**
     * Return generated  HttpTransport object for google connect
     * @return
     */
	public HttpTransport getHttpTransport() {
		return httpTransport;
	}
	
	/**
	 * return Json Parser factory for google interaction parse
	 * @return
	 */
	public JsonFactory getJsonParserFactory() {
		return jsonParserFactory;
	}

	/** 
	 * 
	 *   Return Authorized information object
	 * 
	 * @return
	 */
	public Credential getCredential() {
		return this.credential;
	}
	
	
	//installed
	/**
	 * Return object with all required keys for this application authorization
	 * Keys loaded from resource file witch should be placed in resources folder.
	 * @param resourceJsonPath  - keys resource file name
	 * @return
	 * @throws Throwable 
	 */
	
//	public GoogleAPIKeys loadKeys (GoogleAPIKeys keysObj) throws Exception {
//		
//		keysObj =  parser.parseAndClose(GoogleAPIKeys.class);
//		
//		
//		return loadKeys(resourceJsonPath, "installed");
//		
//	}
	//public GoogleAPIKeys loadKeys (String resourceJsonPath, String keySrtuctureName) throws Exception {

	/**
	 *    Load keys data form file and create GoogleAPIKeys class instance
	 *
	 * @param inputKeys
	 * @return
	 * @throws Exception
	 */
	public GoogleAPIKeys loadKeys (GoogleAPIKeys inputKeys) throws Exception {
		
		if ( inputKeys.getStructureStartLabel() == null) {
			inputKeys.setStructureStartLabel("installed");
		}
	
		if (! inputKeys.iskeysLoaded())  {
			if ( inputKeys.getResourceFile() == null) {
				throw new Exception("Resource file with google key structure reuqired.");
			}
			logger.debug("Load key from :"+inputKeys.getResourceFile());
			JsonParser parser =  jsonParserFactory.createJsonParser(this.getClass().getResourceAsStream(inputKeys.getResourceFile()));
			parser.skipToKey(inputKeys.getStructureStartLabel());

			//   Данные ключа загружаются из JSON файла
			keysObj =  parser.parseAndClose(GoogleAPIKeys.class);
			
			keysObj.setResourceFile(inputKeys.getResourceFile());
			keysObj.setStructureStartLabel(inputKeys.getStructureStartLabel());
			keysObj.setListenerUri(inputKeys.getListenerUri());
		}
		else {
			keysObj = inputKeys;
		} 
		return keysObj;
		
	
	}
	        
    /**
     * Builds auth flow. And return Credential
     * @return
     * @throws Throwable
     */
    Credential initAuthFlow(GoogleAPIKeys inputKeyObj) throws Exception {
    	keysObj = inputKeyObj;
    	return initAuthFlow();
    }
    
	
    Credential initAuthFlow() throws Exception {
    	//   Set up authorization code flow
    	//extCallbackUrl = callback;
    	logger.trace("Start auth flow with TokenUri: "+keysObj.getTokenUri()
				+ ", ClientId: "+keysObj.getClientId()
				+ ", AuthUri: "+keysObj.getAuthUri()
		);
    	authFlow = new AuthorizationCodeFlow.Builder(BearerToken.authorizationHeaderAccessMethod(),
    			httpTransport,
    			jsonParserFactory,
    			new GenericUrl(keysObj.getTokenUri()),
    			new ClientParametersAuthentication(keysObj.getClientId(), keysObj.getClientSecret()),
    			keysObj.getClientId(),
    			keysObj.getAuthUri()
	    	)
	    	.setScopes(SCOPE)
	    	.setDataStoreFactory(LocaldataStore)
	    	.build();  
    
    	if ((this.getUserId() == null) || (this.getStoreKey() == null)) {
    		throw new ExceptionBreakAuthFlow("[Google.initAuthFlow] User id or store key is not defined.");
    	}

    	this.credential = authFlow.loadCredential(this.getUserId() +"."+ this.getStoreKey());

    	return this.credential;
    }
    
    /**
     * Return google url for set access permeations and get auth token
     * @return
     * @throws Throwable
     */
    public URL getAuthUrl() throws Exception {

    	URL gAuthURL = null;

    	if (authFlow == null ) {
    		throw new ExceptionBreakAuthFlow("Auth flow not build yet.");
    	}

    	if (keysObj.isCanUseCallback() ) {
			gAuthURL =  new URL(authFlow.newAuthorizationUrl().setRedirectUri(keysObj.getListenerUri().toString()).build());
		}
		else {
			gAuthURL =  new URL(authFlow.newAuthorizationUrl().setRedirectUri(GoogleAPIKeys.getDefaultUri().toString()).build());
		}

    	logger.trace("[google.getAuthUrl] Prepare redirect auth URL = : "+ gAuthURL);
    	return gAuthURL;
    }
    
    /**
     * Try to authirize with received token string. If authorized, save token for future use.
     * @param authCode token string for auth.
     * @return
     * @throws Throwable
     */
    public Credential doAuth(String authCode) throws Exception {
    	if (authFlow == null ) {
    		throw new ExceptionBreakAuthFlow("Auth flow not build yet.");
    	}
    	String redirectURI = keysObj.getListenerUri().toString();
		if ( ! keysObj.isCanUseCallback() ) {
			redirectURI = GoogleAPIKeys.getDefaultUri().toString();
		}
		logger.trace("[google.doAuth] Do Auth with received token. Redirect URI = " + redirectURI);
		TokenResponse tokenResponse = authFlow.newTokenRequest(authCode).setRedirectUri(redirectURI).execute();

		//   Save auth credential with received token. User ID is a key.
    	this.credential = authFlow.createAndStoreCredential(tokenResponse, this.getUserId() +"."+ this.getStoreKey());
    	return this.credential;
    }
 
    
    /**
     *  
     *  Remove stored credential from store
     * 
     * @throws IOException
     */
    public void resetAuth() throws IOException {
    	authFlow.getCredentialDataStore().delete(this.getUserId());  
    }
}

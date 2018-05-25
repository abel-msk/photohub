package home.abel.photohub.connector.google;


import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.gdata.client.Service;
import com.google.gdata.data.photos.UserFeed;
import com.google.gdata.util.ServiceException;
import home.abel.photohub.connector.*;
import home.abel.photohub.connector.prototype.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.services.plus.Plus;
import com.google.api.services.plus.model.Person;
import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.media.MediaFileSource;
import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.data.photos.PhotoEntry;

import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.InputStreamResource;

/**
 * 
 * 
 *    Google developers console 
 *    https://console.developers.google.com
 * 
 * 
 * @author abel
 *
 */

//public class LocalSiteConnector implements SiteConnectorInt {
public class GoogleSiteConnector extends SiteBaseConnector {

	final Logger logger = LoggerFactory.getLogger(GoogleSiteConnector.class);
	//public final static String GOOGLE_PERSON_ID = "gPerson";
	public final static String GOOGLE_PERSON_ID = "Login alias";
	
	
//	protected String user = null;
	public final static String SITE_TYPE = "Google";
	public final static String APPLICATION_NAME  = "PhotoHUB"; // Applicatin name should be registered on gogole dev console
	//protected SiteStatusEnum connectionState;
	protected String connectorId = null;
	protected Plus gAppObject = null;
	protected Person profile = null;
	protected PicasawebService picasaService = null;
	protected Credential googleCred;
	protected Properties prop;
	protected GoogleAPIKeys apiKeys = null;
	protected GoogleOAuth2 googleAuthLib = null;
	
	public GoogleSiteConnector() {
		super();
		super.sitePropertiesMap.put(GOOGLE_PERSON_ID,
				new SiteBaseProperty(GOOGLE_PERSON_ID, "The gogole profile person ID or Google account email.",""));

		//connectionState = SiteStatusEnum.DISCONNECT;   //  Local site always connected
		setState(SiteStatusEnum.DISCONNECT);   //  Local site always connected

		try {
			googleAuthLib = new GoogleOAuth2();
		} catch (Throwable e) {
			logger.error("Cannot init GoogleOAuth2 object.", e);
		}
		
		//Load connector property
		try {
			prop = new Properties();
			InputStream is = this.getClass().getResourceAsStream("/google-conector.properties");
			prop.load(is);
			
			// Load connectors keys object
			apiKeys = new GoogleAPIKeys();
			apiKeys.setResourceFile(prop.getProperty("google.client.secret.file","/google_client_secret.json"));
			apiKeys.setStructureStartLabel(prop.getProperty("google.client.secret.blockname","installed"));
			apiKeys = googleAuthLib.loadKeys(apiKeys);
			
			
		} catch (Exception e) {
			logger.warn("Cannot load property: GoogleConnector.properties. "+e.getMessage());
		}
		
		
		
	}

	@Override
	public String getSiteType() {
		return SITE_TYPE;
	}

	@Override
	public void setUser(String username) {
		super.setUser(username);
		googleAuthLib.setUserId(username);	
	}
	
	@Override
	public void setProperties(Map<String,SitePropertyInt> propMap) {
		super.setProperties(propMap);
		//  Check for presend required property
		if ( (propMap == null)  || (propMap.get(GOOGLE_PERSON_ID) == null))  {
			throw new ExceptionIncorrectParams("Property "+GOOGLE_PERSON_ID+" is required.");
		}
		
		String keyId = propMap.get(GOOGLE_PERSON_ID).getValue();
		
		if ( (keyId != null) && (keyId.length() > 0) ) {
			googleAuthLib.setStoreKey(keyId);
		}
		else {
			throw new ExceptionIncorrectParams("Property "+GOOGLE_PERSON_ID+" cannot be Empty");
		}
	}
	
	@Override
	public void setProperty(SitePropertyInt propertyObj) {
		super.setProperty(propertyObj);
		if (propertyObj.getName().equalsIgnoreCase(GOOGLE_PERSON_ID)) {
			if ( (propertyObj.getValue() != null) && (propertyObj.getValue().length() > 0) ) {
				googleAuthLib.setStoreKey(propertyObj.getValue());
			}
			else {
				throw new ExceptionIncorrectParams("Property "+GOOGLE_PERSON_ID+" cannot be Empty");
			}
		}
	}
	
	
	@Override
	public boolean isCanWrite() {
		return true;
	}
	
	protected Credential getCredential() throws Exception {
		if (this.googleCred == null ) {
			throw new ExceptionNotAuthorized();
		}
 		return this.googleCred;
	}

	/**-------------------------------------------------------------------------
	 * 
	 *   Этот метод производит все необхоимые действия для загрузки процесса автооизации у гугла.
	 * 
	 *   Если пользователь ужк (раньше) подтверждал полномочия нашей проги обращаться в гугл 
	 *   и у нас есть сохраненный авторизационный токен то мы  просто берем его и соединемся с гуглом.
	 * 
	 *   Если авторизационого токена нет или тот что етсь "протух" то  запускаем механизм авторизвации
	 *   для чего позвращаем пользователю URL(в гугле) куда пользовательдолжен зайти и подтвертить полномочия наше программы.
	 *
	 *   Далее: В ответ гугл вернет авторизационный токен.  Его надо сохоанить в объекте  который мы
	 *   возвращаем (SiteCredentialInt)  и вернуть через  вызов doAuth.
	 *   
	 * @param callback the url for back redirect after success authentication
	 * @return cred
	 -------------------------------------------------------------------------*/
	@Override
	public SiteCredentialInt doConnect(URL callback) throws Exception {
		this.callback = callback;
		SiteBaseCredential excahgeCread = new SiteBaseCredential(this);
		logger.trace("[Google.doConnect] callback="+callback);

		if ((googleCred == null)  ||  ( !refreshAuth()) ) {
			if (callback != null) {

				//   Save callback url
				//apiKeys.setListenerUri(callback == null?GoogleAPIKeys.getDefaultUri():callback.toURI());
				apiKeys.setListenerUri(callback.toURI());
				if (apiKeys.isCanUseCallback()) {
					excahgeCread.setAuthReceiveType(SiteCredentialInt.AuthReceiveType.AUTH_TYPE_NET);
				} else {
					excahgeCread.setAuthReceiveType(SiteCredentialInt.AuthReceiveType.AUTH_TYPE_DIRECT);
				}
				logger.trace("Listener url = " + apiKeys.getListenerUri().toString() + ", Auth receive type =" + excahgeCread.getAuthReceiveType().toString());


			} else {
				apiKeys.setListenerUri(GoogleAPIKeys.getDefaultUri());
				excahgeCread.setAuthReceiveType(SiteCredentialInt.AuthReceiveType.AUTH_TYPE_DIRECT);
			}

			googleCred = googleAuthLib.initAuthFlow(apiKeys);
			logger.trace("[Google.doConnect] googleCred = " + (googleCred == null ? "null" : "not null") + ",  STATE=" + getState());
		}
		//
		//   У нас уже есть авторизационный токен и сайт не помечен как Disconnect
		//   Восстанавливаем соединение		
		if ((googleCred != null) && (getState() != SiteStatusEnum.DISCONNECT)) {
			try { 
				getProfile(); //  Проверяем  соединение если соединения нет то вылетаем по Exception и уст. DISCONECT
				setState(SiteStatusEnum.CONNECT);
				picasaService = new PicasawebService(GoogleSiteConnector.APPLICATION_NAME);
				picasaService.setOAuth2Credentials(googleCred);
				logger.debug("[Google.doConnect] Check connection (getProfile) OK");

			} 
			catch (Exception e){
				logger.debug("[Google.doConnect] Соединениея нет. (проверка по getProfile)");
				setState(SiteStatusEnum.DISCONNECT);
			}
		}
		logger.trace("[Google.doConnect] googleCred = "+ (googleCred==null?"null":"not null") +  ",  STATE="+ getState());
		
		//
		//   Не удалось восстановить соединение или его еще нет.  Идем на ааторизацию.
		//   
		if ((googleCred == null) || (getState() == SiteStatusEnum.DISCONNECT)) {
			// Prepage exchange creadential for state = AUTH_WAIT
			// And full up ExcahgeCread ro show to user for obtain auth token from google.
			
			setState(SiteStatusEnum.AUTH_WAIT);
			excahgeCread.setUserMessage("Use this URL for access to Google authentication page. Check access permition for this application and receive auth code token.");
			excahgeCread.setUserLoginFormUrl(googleAuthLib.getAuthUrl());	
			logger.trace("[Google.doConnect] Do Reconnect. User login Form URL="+googleAuthLib.getAuthUrl() + ",  STATE="+ getState());
		}
		
		excahgeCread.setState(getState());
		return (SiteCredentialInt)excahgeCread;
	}


	/**-------------------------------------------------------------------------
	 *
	 *    Try to refresh auth token
	 *
	 * @return
	 *
	 --------------------------------------------------------------------------*/
	public boolean refreshAuth() {
		boolean ret = false;
		try {
			if (googleCred != null) {
				logger.trace("[Google.refreshAuth] Refresh Credential.");
				ret = googleCred.refreshToken();
			}
		}
		catch (IOException ioe) {
			logger.warn("[Google.refreshAuth] Refresh Credential Error.",ioe.getMessage());
			return false;
		}
		return ret;
	}


	/**-------------------------------------------------------------------------
	 * 
	 *   Берет авторизацтоный токен из объекта (SiteCredentialInt cred)
	 *   И пытается  с ним авторизоваться у гугла. Если все ок 
	 *    - сохраняет токен в файле для будущей авто загрузки и авто авторизации.
	 *    
	 * @param excahgeCread
	 * @return
	 -------------------------------------------------------------------------*/
	@Override
	public SiteCredentialInt doAuth(SiteCredentialInt excahgeCread) throws Exception{
		
		try {
			googleCred = googleAuthLib.doAuth(excahgeCread.getAccessToken());
			getProfile();
			picasaService = new PicasawebService(GoogleSiteConnector.APPLICATION_NAME);	    
			picasaService.setOAuth2Credentials(googleCred);	
			setState(SiteStatusEnum.CONNECT);
			logger.debug("Google auth passed.");
			
		} catch (Throwable e) {
			setState(SiteStatusEnum.DISCONNECT);
			excahgeCread.setState(SiteStatusEnum.DISCONNECT);
			logger.warn("[GoogleConnector.doAuth] Cannot auth with recived token = " + excahgeCread.getAccessToken());
			throw new ExceptionBreakAuthFlow("Cannot auth with recived token.",e);
		}
		excahgeCread.setState(getState());
		return excahgeCread;
	}

	
	/**-------------------------------------------------------------------------
	 * 
	 *  Возвращает Гугловый профайл авторизованного пользователя
	 *  Return googlePlus this account profile
	 *  @return
	 *  @throws Exception
	 * 
	 -------------------------------------------------------------------------*/
	protected Person getProfile() throws Exception {
		return getProfile("me");
	}
	
	protected Person getProfile(String personName) throws Exception {
		if ( profile == null) {
			gAppObject = new Plus.Builder(googleAuthLib.getHttpTransport(),
					googleAuthLib.getJsonParserFactory(),
					googleAuthLib.getCredential())
		    .setApplicationName(APPLICATION_NAME)
		    .build();
			try {
				profile = gAppObject.people().get(personName).execute();
			}
			catch (TokenResponseException e) {
				logger.error("Connect to site error : " + e.getMessage());
				setState(SiteStatusEnum.DISCONNECT);
				throw new ExceptionNotAuthorized(e.getMessage());
			}
		}
		return profile;
	}
	
	/**-------------------------------------------------------------------------
	 * 
	 *    Возвращает google plus person profile ID на основе существующей авторизвции.
	 *    
	 * @return
	 ------------------------------------------------------------------------- */
//	@Override
//	public String getUser() {
//		String userId = this.user;
//		if (userId == null) {
//			try {
//				userId = this.getProfile().getId();
//			} catch (Exception e) {
//				logger.error("[GoogleConnector.getPersonProfile] load profile error : "+ e.getMessage(),e);
//			}
//		}
//		return userId;
//	}
	
	
	/**-------------------------------------------------------------------------
	 * 
	 *  Init google picasa object for access to Images store
	 *  @return
	 * 
	 -------------------------------------------------------------------------*/
	protected  PicasawebService getPicasaService() throws Exception{
		if (this.picasaService == null) {
			this.picasaService = new PicasawebService(APPLICATION_NAME);	    
			this.picasaService.setOAuth2Credentials(this.getCredential());	
		}
		return this.picasaService;
	}
	
	
//	/*-------------------------------------------------------------------------
//	 *  Scan google picasa site
//	 * @see home.abel.photohub.connector.SiteBaseConnector#doScan(home.abel.photohub.connector.prototype.ConnectorCallbackInt)
//	 */
//	@Override
//	public void doScan(ConnectorCallbackInt cb) throws Exception {
//		
//		GoogleRootObject root = new GoogleRootObject(this);
//		for (  PhotoObjectInt albumItem: root.listSubObjects()) {
//			String nodeId = cb.loadPhotoObj(null, null, albumItem);
//			
//			for (PhotoObjectInt photoItem: albumItem.listSubObjects()) {
//				
//				cb.loadPhotoObj(nodeId, albumItem, photoItem);
//			}
//		}
//		
//		GooglePicasaScanner scanner = new GooglePicasaScanner(this,cb);
//		scanner.scannRoot();
//	}
	
	
	@Override
	/**
	 *  Disconnect Site
	 * (non-Javadoc)
	 * @see home.abel.photohub.connector.SiteBaseConnector#disconnectSite()
	 */
	public void disconnectSite() throws Exception {
		googleAuthLib.resetAuth(); 
		setState(SiteStatusEnum.DISCONNECT);
	}

	@Override
	public List<String> getRootObjects() throws Exception {
		GoogleRootObject root = new GoogleRootObject(this);
		return root.listSubObjects();
	}
	
	
	/**
	 * Загружает объект из гугла. Объект загружается по его ID.
	 * Для фото ID состоит из ID альбома и ID фото разделенных  '.'
	 * Для альбома ID сотсоит тоько из ID альбома
	 * Елси ID отсутствует или пустой то загружается корневой объект.
	 */
	@Override
	public PhotoObjectInt loadObject(String combinedObjectId) throws Exception {
		//   Object is Albumm
		if (combinedObjectId == null) {
			return (PhotoObjectInt) new GoogleRootObject(this);
		}  
		else if (combinedObjectId.indexOf('.') < 0 ) {
			return (PhotoObjectInt) new GoogleAlbumObject(this,combinedObjectId);
		}
		else {
			// Object is photo object
			String albumId = combinedObjectId.substring(0,combinedObjectId.indexOf('.'));
			String objectId = combinedObjectId.substring(combinedObjectId.indexOf('.')+1);
			return (PhotoObjectInt) new GooglePhotoObject(this,albumId,objectId);
		}	
	}

	
	@Override
	public PhotoObjectInt createObject(String name, PhotoObjectInt parent, InputStream is) throws Exception {
		
		if ( parent != null) {
			logger.trace("[Google] Create object. Name="+name+", ParentName="+(parent.getName()==null?"null":parent.getName()));
		}
		else {
			logger.trace("[Google] Create object. Name="+name+", Parent=null");
		}
		
		
		String parentId = parent.getId();
		String albumId = parentId;
		
		if (parentId.indexOf('.') >= 0 ) {
			 albumId = parentId.substring(0,parentId.indexOf('.'));
		}
		
		
		URL albumPostUrl = new URL("https://picasaweb.google.com/data/feed/api/user/"+getProfile().getId()+"/albumid/"+albumId);

		PhotoEntry myPhoto = new PhotoEntry();
		myPhoto.setTitle(new PlainTextConstruct(name));
		//myPhoto.setDescription(new PlainTextConstruct("Puppies are the greatest."));
		//myPhoto.setClient("myClientName");

		//   Create temp file	
		String fileExt = name.substring(name.lastIndexOf('.')+1);
		if ( fileExt == null) {
			fileExt=new String("jpg");
		}
		
		File tempFile = File.createTempFile("temp-file-name", "."+fileExt); 
		FileOutputStream fos = new FileOutputStream(tempFile);

		try {	
			
			//  Copy image from input stream to temp file
            //
		    FileChannel outChannel = fos.getChannel();       
		    ReadableByteChannel inChannel = Channels.newChannel(is);
		    ByteBuffer buffer = ByteBuffer.allocate(1024);
			logger.trace("[Google] Create temp file="+tempFile.getAbsolutePath());

		    while (inChannel.read(buffer) >= 0 || buffer.position() > 0) {
				buffer.flip();
				outChannel.write(buffer);
				buffer.compact();
		    }
	
		    inChannel.close();
		    outChannel.close();
		    fos.close();
		    is.close();
		
			//    Check image type and create mimetype
		    //
			String MimeType = null;
			
			if ( (fileExt.compareToIgnoreCase("jpg") == 0 ) || (fileExt.compareToIgnoreCase("jpeg") == 0 ) ) {
				MimeType = "image/jpeg";
			}
			else if ( fileExt.compareToIgnoreCase("png") == 0 ) {
				MimeType = "image/png";
			}
			
			MediaFileSource myMedia = new MediaFileSource(tempFile, MimeType);
			logger.trace("[Google] Upload object="+tempFile.getAbsolutePath()+", MimeType="+MimeType+", URL="+albumPostUrl);
			myPhoto.setMediaSource(myMedia);
			myPhoto = getPicasaService().insert(albumPostUrl, myPhoto);
			
		}
		finally {
			//fos.close();
			tempFile.delete();
		}
		
		logger.trace("[Google] reload photo object after upload. albumId="+albumId+", photoId="+myPhoto.getGphotoId());
		GooglePhotoObject gObject = new GooglePhotoObject(this,albumId,myPhoto.getGphotoId());

		return gObject;	
	}

	
	/*
	 * 	 Generate temp folder in the subfolder  where it should be placed (see genObjectsFolderPath) 
	 *   but with temp name.
	 *   Also generate new (real) path where folder will be moved at save method call.
	 *   The object will have state = loaded = true
	 *   but cannot access to object ID
	 *   
	 * (non-Javadoc)
	 * @see home.abel.photohub.connector.SiteBaseConnector#createFolder(java.lang.String, home.abel.photohub.connector.prototype.PhotoObjectInt)
	 */
	public PhotoObjectInt createFolder(String name, PhotoObjectInt parent) throws Exception {
		
		logger.trace("[Google] Create folder. Name="+name+", Parent="+(parent==null?"null":"not null"));

		URL postUrl = null;
		AlbumEntry myAlbum = new AlbumEntry();
		myAlbum.setTitle(new PlainTextConstruct(name));
        myAlbum.setDescription(new PlainTextConstruct(""));
		try {
			postUrl = new URL("https://picasaweb.google.com/data/feed/api/user/"+getProfile().getId());		
			myAlbum = getPicasaService().insert(postUrl, myAlbum);	
		}
		catch (Exception e) {
			logger.error("ERROR: Create folder,  PostUrl="+postUrl+", Exception="+e.getMessage(),e);
			throw new AccessException("Cannot create folder in picasa web album. ("+e.getMessage()+")");
		}

//		myAlbum.setDescription(new PlainTextConstruct("Updated album description")); 
//		myAlbum.update();

		GoogleAlbumObject gObject = new GoogleAlbumObject(this,myAlbum.getGphotoId());
		
		return (PhotoObjectInt)gObject;

	}



	/**---------------------------------------------------------------------
	 *
	 *    PHOTO DELETION
	 *
	 ---------------------------------------------------------------------*/
	/**
	 * Indicate that object on this site can be deleted.
	 * @see home.abel.photohub.connector.prototype.SiteConnectorInt#isCanDelete()
	 */
	@Override
	public boolean isCanDelete() {
		return false;
	}

//	@Override
//	public void deleteObject(PhotoObjectInt obj) throws Exception {
//		if ( ! isCanDelete() ) throw new AccessException("Cannot delete object on readonly site.");
//		obj.delete();
//	}


	/**
	 *
	 *
	 *       Loading resources from site
	 *
	 *
	 */

	private final int retries = 10;
	public static final String[] responsHeaders = {
			"Content-Type",
			"Content-Length",
			"Content-Disposition",
			"Accept-Ranges",
			"Content-Range",
			"If-Range",
			"Range",
			"Date",
			"Last-Modified",
			"Expires",
			"E-Tag"
	};
	/**
	 *   Load media by its URL through picasa service connection
	 *
	 * @param path url for loading
	 * @return resource with inpunStram of opened connection and respons headers placed in description
	 * @throws Exception
	 */
	public SiteMediaPipe loadMediaByPath(String path, HeadersContainer headers) throws Exception {

		if (headers == null) {
			headers = new HeadersContainer();
		}
		SiteMediaPipe thePipe = new SiteMediaPipe();
		Service.GDataRequestFactory factory = getPicasaService().getRequestFactory();
		Service.GDataRequest request = null;


		String location = path;
		int counter = 0;

		while ((location != null)  && (counter < retries)) {

			logger.trace("[loadMediaByPath] Request to  URL:" +location );
			//Service.GDataRequestFactory factory = getPicasaService().getRequestFactory();
			//
			//   Create request
			//
			URL requestUrl = new URL(location);
			request = factory.getRequest(
					Service.GDataRequest.RequestType.QUERY,
					requestUrl,
					com.google.gdata.util.ContentType.ANY
			);

			//
			//   Set headers
			//

			for (String key : headers.getHdrKeys()) {
				for (String value: headers.getHdrValues(key)) {
					request.setHeader(key, value);
				}
			}

			try {
				//request.setHeader();
				request.execute();
				location = null;
				thePipe.setInputStream(request.getResponseStream());
			}
			catch (com.google.gdata.util.RedirectRequiredException e) {
				location = e.getRedirectLocation();
				logger.trace("[loadMediaByPath] Response REDIRECT. Trying count="+counter);
				request.end();
			}
			catch (com.google.gdata.util.NotModifiedException e) {
				request.end();
				thePipe.addHeadersList("ETag", e.getHttpHeader("ETag"));
				thePipe.setStatus("304"); //SC_NOT_MODIFIED
				logger.trace("[loadMediaByPath] Response NOT MODIFIED.  ETag header = "+ e.getHttpHeader("ETag"));
				return thePipe;
			}
			catch (ServiceException se) {
				logger.trace("[loadMediaByPath] Response UNKNOWN. " + se.getMessage());
				request.end();
				this.doConnect(null);
			}
			counter++;
		}

		if (counter >=retries) {
			throw new ExceptionObjectAccess("Seems we got too match redirects. Last location: " + location );
		}

		if ( request != null ) {
			for (String hdrName : responsHeaders) {
				String hdrValue = request.getResponseHeader(hdrName);
				if (hdrValue != null) {
					thePipe.addHeader(hdrName, hdrValue);
				}
			}
		}

		return thePipe;
	}

}

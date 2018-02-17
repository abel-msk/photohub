package home.abel.photohub.connector.prototype;

import home.abel.photohub.connector.HeadersContainer;
import home.abel.photohub.connector.SiteMediaPipe;
import org.springframework.core.io.AbstractResource;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;


public interface SiteConnectorInt {
	
	/*      
	 * 
	 *     SITE INIT PATAMS
	 *     
	 */
	/**
	 * Set site user, for identify connection and site login.
	 * @param siteUser
	 */
	public void setUser (String siteUser);
	public String getUser ();
	public void setId(String id);
	public String getId();
	public void setLocalStore(String store);
	public String getLocalStore();

	/*
	 * 
	 *     SITE PROPERTIES
	 * 
	 */
	
	public List<PhotoObjectInt> getRootObjects() throws Exception;

	
	/**
	 * Return type of site we can connect to.
	 * @return
	 */
	public String getSiteType();
	
	/** 
	 * Identify is it possible update photos meta information on site
	 * @return
	 */
	public boolean isCanUpdate();
	
	/** 
	 * Identify is it possible to add new photo objects on site
	 * @return
	 */
	public boolean isCanWrite();
	
	/** 
	 * Identify is it possible to remove photo or folder
	 * @return
	 */
	public boolean isCanDelete();
	
	public Map<String,SitePropertyInt> getProperties();
	public SitePropertyInt getPropertyObj(String name);
	public String getProperty(String name);
	
	public void setProperties(Map<String,SitePropertyInt> propMap);	
	public void setProperty(SitePropertyInt propertyObj);	
	public void setProperty(String name, String value);
	
	/*
	 * 
	 *    SITE CONNECTION
	 * 
	 */
	/**
	 * Site Connection status  connected, disconnected, wait_auth 
	 * @return
	 */
	public SiteStatusEnum getState();
	public void setState(SiteStatusEnum state);
	
	/**
	 * 
	 * Sute connec to for auth. For OAuth2 it is first phase  
	 * @param callback
	 * @throws Throwable 
	 */
	public SiteCredentialInt doConnect(URL callback) throws Exception;
	
	/**
	 * 
	 * Site Auth for OAuth2 it is second phase
	 * @return
	 * @throws Throwable 
	 */
	public SiteCredentialInt doAuth(SiteCredentialInt cred) throws Exception;



	public void disconnectSite() throws Exception;
	
	//public void  doScan(ConnectorCallbackInt cb) throws Exception;
	
	public PhotoObjectInt loadObject(String ObjectId) throws Exception;	
	
	public PhotoObjectInt createObject(String name, PhotoObjectInt parent, InputStream is) throws Exception;
	public PhotoObjectInt createObject(String name, PhotoObjectInt parent, File file) throws Exception;
	
	public PhotoObjectInt createFolder(String name, PhotoObjectInt parent) throws Exception;
	
	public void deleteObject(PhotoObjectInt obj) throws Exception;


	public SiteMediaPipe loadMediaByPath(String path, HeadersContainer headers) throws Exception;

}

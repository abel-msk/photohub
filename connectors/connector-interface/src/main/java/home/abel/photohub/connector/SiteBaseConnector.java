package home.abel.photohub.connector;

import home.abel.photohub.connector.prototype.AccessException;
import home.abel.photohub.connector.prototype.PhotoObjectInt;
import home.abel.photohub.connector.prototype.SiteConnectorInt;
import home.abel.photohub.connector.prototype.SiteCredentialInt;
import home.abel.photohub.connector.prototype.SitePropertyInt;
import home.abel.photohub.connector.prototype.SiteStatusEnum;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SiteBaseConnector implements SiteConnectorInt{
	protected String user = null;
	protected String  SITE_TYPE = "BASE";
	protected SiteStatusEnum state = SiteStatusEnum.DISCONNECT;
	protected String Id = null;
	protected Map<String, SitePropertyInt>sitePropertiesMap = new HashMap<String, SitePropertyInt>();
	protected String localStore = null;
	protected URL callback = null;
	
	@Override
	public String getSiteType() {
		return SITE_TYPE;
	}
	
	@Override
	public void setId(String Id) {
		this.Id = Id;
	}
	
	@Override
	public String getId() {
		return Id;
	}
	
	
	@Override
	public void setUser(String user) {
			this.user = user;
	}
	
	@Override
	public String getUser() {
		return this.user;
	}
	
	@Override
	public List<PhotoObjectInt> getRootObjects() throws Exception {
		return new ArrayList<PhotoObjectInt>();
	}
	
	@Override
	public SiteStatusEnum getState() {
		return state;
	}
	
	@Override
	public void setState(SiteStatusEnum state) {
		this.state = state;
	}
	
	
	@Override
	public SiteCredentialInt doConnect(URL callback) throws Exception {
		this.callback = callback;
		state = SiteStatusEnum.CONNECT;
		SiteBaseCredential cred = new SiteBaseCredential(this);
		cred.setState(state);
		return cred;
	}
	
	@Override
	public SiteCredentialInt doAuth(SiteCredentialInt cred) throws Exception {
		state = SiteStatusEnum.CONNECT;	
		cred.setState(state);
		return cred;
	}



//	@Override
//	public void doScan(ConnectorCallbackInt cb) throws Exception{
//
//	}

	@Override
	public PhotoObjectInt loadObject(String ObjectId) throws Exception {
		return null;
	}

	/**
	 * Indicate that new objects can be added to sites
	 * @see home.abel.photohub.connector.prototype.SiteConnectorInt#isUpdateble()
	 */
	@Override
	public boolean isCanUpdate() {
		return false;
	}


	/**
	 * Indicate that exiten object or theirs metadata can bee changed.
	 * @see home.abel.photohub.connector.prototype.SiteConnectorInt#isWritable()
	 */
	@Override
	public boolean isCanWrite() {
		return false;
	}

	/**
	 * Indicate that exiten object or theirs metadata can bee changed.
	 * @see home.abel.photohub.connector.prototype.SiteConnectorInt#isWritable()
	 */
	@Override
	public boolean isCanDelete() {
		return false;
	}
	
	@Override
	public void disconnectSite() throws Exception {
		state = SiteStatusEnum.DISCONNECT;
	}
	
	
	/*
	 * 
	 *    PRPERTY WORK
	 */
	
	/*
	 * 
	 * (non-Javadoc)
	 * @see home.abel.photohub.connector.prototype.SiteConnectorInt#createObject(java.lang.String, home.abel.photohub.connector.prototype.PhotoObjectInt, java.io.InputStream)
	 */
	@Override
	public PhotoObjectInt createObject(String name, PhotoObjectInt parent, InputStream is) throws Exception {
		if ( ! isCanWrite() ) throw new AccessException("Cannot create object on readonly site.");
		return null;
	}
	
	@Override
	public PhotoObjectInt createObject(String name, PhotoObjectInt parent, File file) throws Exception {
		if ( ! isCanWrite() ) throw new AccessException("Cannot create object on readonly site.");
		FileInputStream  ifs = new FileInputStream(file);
		return createObject(name,parent,(InputStream)ifs);
	}

	@Override
	public PhotoObjectInt createFolder(String name, PhotoObjectInt parent) throws Exception {
		if ( ! isCanWrite() ) throw new AccessException("Cannot create object on readonly site.");
		return null;
	}

	@Override
	public void deleteObject(PhotoObjectInt obj) throws Exception {
		if ( ! isCanDelete() ) throw new AccessException("Cannot delete object on readonly site.");
		obj.delete();
	}

/*
 * 
 *    PROPERTIES WORK
 * 
 * 
 */
	
	@Override
	public Map<String, SitePropertyInt> getProperties() {
		return sitePropertiesMap;
	}

	@Override
	public void setProperties(Map<String, SitePropertyInt> SrcPropMap) {
		if ( SrcPropMap != null) {
			for (String key: SrcPropMap.keySet()) {
				sitePropertiesMap.get(key).setValueObj(SrcPropMap.get(key));
			}
		}
	}

	@Override
	public SitePropertyInt getPropertyObj(String name) {
		return sitePropertiesMap.get(name);
	}

	@Override
	public String getProperty(String name) {
		return sitePropertiesMap.get(name) == null? null:sitePropertiesMap.get(name).getValue();
	}

	@Override
	public void setProperty(SitePropertyInt propertyObj) {
		if ( propertyObj != null) {
			if (sitePropertiesMap.get(propertyObj.getName()) != null) {
				sitePropertiesMap.get(propertyObj.getName()).setValueObj(propertyObj);
			}
		}
	}
	
	@Override
	public void setProperty(String name, String value) {
		if (sitePropertiesMap.get(name) != null) {
			sitePropertiesMap.get(name).setValue(value);
		}
	}

	@Override
	public void setLocalStore(String store) {
		this.localStore = store;	
	}

	@Override
	public String getLocalStore() {
		return localStore;
	}
	

}

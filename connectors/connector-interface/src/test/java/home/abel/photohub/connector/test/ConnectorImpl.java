package home.abel.photohub.connector.test;

import home.abel.photohub.connector.BasePhotoObj;
import home.abel.photohub.connector.SiteBaseConnector;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import home.abel.photohub.connector.BasePhotoObj;
import home.abel.photohub.connector.SiteBaseConnector;
import home.abel.photohub.connector.SiteBaseCredential;
import home.abel.photohub.connector.prototype.InitializationException;
import home.abel.photohub.connector.prototype.PhotoObjectInt;
import home.abel.photohub.connector.prototype.SiteCredentialInt;
import home.abel.photohub.connector.prototype.SiteConnectorInt;
import home.abel.photohub.connector.prototype.SitePropertyInt;
import home.abel.photohub.connector.prototype.SiteStatusEnum;


public class ConnectorImpl extends SiteBaseConnector {

	protected static String  SITE_TYPE = "Local-Test";


	
	@Override
	public SiteStatusEnum getState() {
		return state;
	}


//	@Override
//	public void doScan(ConnectorCallbackInt cb) {
//		// TODO Auto-generated method stub
//		File theParentFile = new File(getLocalStore());
//		BasePhotoObj parent = new BasePhotoObj();
//		parent.setName(theParentFile.getName());
//		String parentExtId = "2";
//		
//		for (File curFile : theParentFile.listFiles()) {
//			BasePhotoObj photo = new BasePhotoObj();
//			photo.setName(curFile.getName());
//			String extId = cb.loadPhotoObj(parentExtId, parent, photo);
//		}
//	}

	@Override
	public PhotoObjectInt loadObject(String ObjectId) {
		BasePhotoObj photo = new BasePhotoObj(this);
		photo.setName(ObjectId);
		return photo;
	}

//	@Override
//	public boolean isCanUpdate() {
//		return false;
//	}
//
//	@Override
//	public boolean isCanWrite() {
//		return false;
//	}
//
//	@Override
//	public boolean isCanUpdate() {
//		return false;
//	}
	
	@Override
	public void disconnectSite() throws Exception {
		state = SiteStatusEnum.DISCONNECT;
	}

	@Override
	public PhotoObjectInt createObject(String name, PhotoObjectInt parent, InputStream is)
			throws Exception {
		BasePhotoObj obj = new BasePhotoObj(this);
		return obj;
	}

	@Override
	public PhotoObjectInt createFolder(String name, PhotoObjectInt parent)
			throws Exception {
		BasePhotoObj obj = new BasePhotoObj(this);
		return obj;
	}

}

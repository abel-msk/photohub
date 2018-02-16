package home.abel.photohub.connector.local;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import home.abel.photohub.connector.prototype.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.uuid.Generators;

import home.abel.photohub.connector.SiteBaseConnector;
import home.abel.photohub.connector.SiteBaseProperty;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.UrlResource;

public class LocalSiteConnector extends SiteBaseConnector {

	final Logger logger = LoggerFactory.getLogger(LocalSiteConnector.class);

	protected static String SITE_TYPE = "Local";
	public final static String ROOT_PATH_PNAME = "root";  //siteRootPath
	protected SiteStatusEnum connectionState;
	protected String connectorId = null;

	
	public LocalSiteConnector() {
		super();
		super.sitePropertiesMap.put(ROOT_PATH_PNAME,new SiteBaseProperty(ROOT_PATH_PNAME,"Root path on disk for keeping photos.",""));
		connectionState = SiteStatusEnum.CONNECT;   //  Local site always connected
	}
	
	@Override
	public String getSiteType() {
		return SITE_TYPE;
	}
	
	@Override
	public boolean isCanUpdate() {
		return true;
	}

	@Override
	public boolean isCanWrite() {
		return true;
	}

	@Override
	public boolean isCanDelete() {
		return true;
	}
	
	@Override
	public SiteStatusEnum getState() {
		return connectionState;
	}
	
	/**
	 * 	Возвращает значение проперти ROOT_PATH_PNAME, если такого проперти не установлено,
	 *  то возвращает из переменной localStore базового класса
	 * @param localStore
	 */
	@Override
	public void setLocalStore(String localStore) {
		if ((sitePropertiesMap == null) 
				|| (sitePropertiesMap.get(ROOT_PATH_PNAME) == null) 
				// || (sitePropertiesMap.get(ROOT_PATH_PNAME).getValue() == null) 
				) 
		{
			this.localStore  = localStore;
		}
		else {
			sitePropertiesMap.get(ROOT_PATH_PNAME).setValue(localStore);
			this.localStore  = localStore;
		}
	}
	
	/**
	 * 	Возвращает локальный пкть, взятый из проперт ROOT_PATH_PNAME
	 * 		если проперти  не установлено, то возвращает из переменной localStore базового класса
	 * @return
	 */
	@Override
	public String getLocalStore() {
		if (sitePropertiesMap.get(ROOT_PATH_PNAME) != null) {
			return sitePropertiesMap.get(ROOT_PATH_PNAME).getValue();
		}
		return this.localStore;
	}
	
	
	/**
	 * 	Копирует проперти из параметра во внутренний список пропертей. 
	 * 	Если среди провертей парамтре нет проперти ROOT_PATH_PNAME,  то добавляет ее  
	 *  	из переменной localStore базового класса.
	 * 
	 * @param props
	 */
	@Override
	public void setProperties(Map<String, SitePropertyInt>  props) {
		String rootVal = null;
		if ( props != null) {
			for (String key: props.keySet()) {
				if ( sitePropertiesMap.get(key) == null) {
					sitePropertiesMap.put(key,props.get(key));
				}
				else {
					sitePropertiesMap.get(key).setValueObj(props.get(key));
				}
				
				if (key.equalsIgnoreCase(ROOT_PATH_PNAME)) {
					if (props.get(key).getValue() == null) rootVal = "";
					else rootVal = props.get(key).getValue();
				}
			}
		}
		
		//  Проверяем есть ли среди пропертей root и если  нет то пытаемся его выставить из locaStore
		if ((localStore != null) && ( rootVal == null ))
			sitePropertiesMap.put(ROOT_PATH_PNAME,
					new SiteBaseProperty(ROOT_PATH_PNAME,"Root path on disk for keeping photos.",localStore));
		else if ((localStore != null) && (rootVal.length() == 0)) 
			sitePropertiesMap.get(ROOT_PATH_PNAME).setValue(localStore);
		
		
	}

//	@Override
//	public void doScan(ConnectorCallbackInt cb) throws Exception {
//		if (getLocalStore() == null) {
//			throw new InitializationException("Local store for site connector id=" + getId() +" must be defined.");				
//		}
//
//		LocalSiteScanner scanner = new LocalSiteScanner(this);
//		logger.trace("Start scanning site " + this.getId() + ",  for path="+getLocalStore());
//		scanner.doScan(cb, getLocalStore());
//	}

	@Override
	public PhotoObjectInt loadObject(String ObjectId) throws IOException {
		return new LocalPhotoObject(this,ObjectId);
	}

	public PhotoObjectInt loadObject(File photoSource) throws IOException {
		return new LocalPhotoObject(this,photoSource);
	}

	@Override
	public List<PhotoObjectInt> getRootObjects() throws Exception {
		File rootPath = new File(getLocalStore());
		if (! rootPath.exists() ) {
			throw new InitializationException("[LocalSiteConnector.getRootObjects] Loacl store path does not exist. Path=" + getLocalStore());
			//return null;
		}
		PhotoObjectInt rootObject =(PhotoObjectInt) new LocalPhotoObject(this,rootPath);
		logger.trace("[getRootObjects] Load root object = " + rootObject.getId());
		return rootObject.listSubObjects();
	}

//	@Override
//	public PhotoObjectInt createFolder(String name, PhotoObjectInt parent) throws Exception {
//		LocalPhotoObject object = new LocalPhotoObject(this);
//		object.setName(name);
//		object.setParent(parent);
//		object.setFolder();
//		return object;
//	}
	
	@Override
	public void disconnectSite() throws Exception {
		connectionState = SiteStatusEnum.DISCONNECT;
	}

	@Override
	public void deleteObject(PhotoObjectInt obj) throws Exception {
		
		if ( ! obj.getConnector().getId().equals(this.getId()) ) {
			throw new RuntimeException("[LocalSiteConnector.deleteObject] Cannot delete object from different connector. Objects connector='"+obj.getConnector().getId()+"'");
		}
		if (obj instanceof LocalPhotoObject) {
			LocalPhotoObject me = (LocalPhotoObject)obj;
			logger.debug("[Local] Delete source file '" +  me.getFile().getAbsolutePath()+"'");
			me.delete();
		}
		else {
			throw new RuntimeException("Class for deletion is not an instance of 'LocalPhotoObject'");
		}	
	}
	
	/*
	 *   Generate temp file in the folder where the source file should be placed 
	 *   (see genObjectsFolderPath) but with temp  name. Store the photo data to this file.
	 *   Also generate new (real) path where file will be moved at save method call.
	 *   The object will have state = loaded = true ( we can access to thumbnail and metadata)
	 *   but cannot access to object ID
	 *
	 * (non-Javadoc)
	 * @see home.abel.photohub.connector.SiteBaseConnector#createObject(java.lang.String, home.abel.photohub.connector.prototype.PhotoObjectInt, java.io.InputStream)
	 */
	@Override
	public PhotoObjectInt createObject(String name, PhotoObjectInt parent, InputStream is) throws Exception {
		String folderSaveTo = genObjectsFolderPath(parent);
		if (name == null) {
			UUID uuid = Generators.randomBasedGenerator().generate();
			name = uuid.toString();
		}

		File newFileLocationObject = FileUtils.getUnicName(
				new File(folderSaveTo + File.separator + name));
		FileUtils.saveFile(is,newFileLocationObject);
		logger.debug("[Local] Save source file '" +  newFileLocationObject.getAbsolutePath()+"'");	
		LocalPhotoObject newPhotoObject = new LocalPhotoObject(this,newFileLocationObject);
		return newPhotoObject;	
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
		File newFileLocationObject = null;
		File folderSaveTo = new File(genObjectsFolderPath(parent));	

	    if (!folderSaveTo.canWrite()) {
	    	throw new ExceptionObjectAccess("WARNING: Trying write to readonly folder "+folderSaveTo.getAbsolutePath());
	    }
		newFileLocationObject = FileUtils.getUnicName(
				new File(folderSaveTo.getAbsolutePath() + File.separator + name));
			
		newFileLocationObject.mkdirs();

		logger.debug("[Local/createFolder] Create folder '" +  newFileLocationObject.getAbsolutePath()+"'");			
		LocalPhotoObject newPhotoObject = new LocalPhotoObject(this,newFileLocationObject);
		return newPhotoObject;
	}

	/**
	 *  Generate local parent folder path for future object place based on parent object.
	 *  If parent object is from local site, get its connectors id as parent.
	 *  For parents from other site, jest get its connectors id and create 
	 *     folder = this objects site root + parent folder id ( usually type+user name)
	 *  If there is not parent add object to its sites root
	 * 
	 * @return Generated path
	 * @throws Exception
	 */
	private  String genObjectsFolderPath(PhotoObjectInt parent) {

		String folderSaveTo = null;
		String saveToSuffix = "";
		logger.trace("[genObjectsFolderPath] Generate parent objects folder path " +  (parent == null? "null":parent.getId()));

	
		//  Check if parent from the same site
		if ( parent != null) {
			if ( PhotoObjectInt.class.isAssignableFrom(LocalPhotoObject.class) ) { 
				folderSaveTo  = parent.isFolder() ? parent.getId() : (new File(parent.getId())).getParent() ; 
				logger.debug("[genObjectsFolderPath] Pаrent object from same site, so save to filder " +  folderSaveTo);
			}			
			else {
				saveToSuffix = parent.getConnector().getId();
				logger.debug("[genObjectsFolderPath] Pаrent object from other site, so append suffix " +  saveToSuffix);
			}
		}
		
		//   Steel do not know where to save.  Get the root of current site
		if ( folderSaveTo == null ) {			
//			String property = this.getProperty(LocalSiteConnector.ROOT_PATH_PNAME);
//			if ((property == null )  || property.isEmpty()) {
//				throw new InitializationException("Root path for site do not defined.");
//			}
//			folderSaveTo = property;
			folderSaveTo = getLocalStore();
			logger.debug("Unknown parent, so use this sites root as parent. " +  folderSaveTo);
		}	

		folderSaveTo = folderSaveTo + (saveToSuffix.isEmpty() ? "" :  File.separator + saveToSuffix);
		logger.trace("Finally folder to save object is " +  folderSaveTo);

		return folderSaveTo;
	}


	/**
	 *
	 * @param path  The media path&  CAn be obtained from PhotoMediaObject.getPath()
	 * @return
	 * @throws Exception
	 */
	public AbstractResource loadMediaByPath(String path, String headers) throws Exception {
		File resFile = new File(path);
		if ( ! resFile.exists())
			throw new ExceptionIncorrectParams("Is not valid path : "+path+". Use MediaObject.getPath()");

		return new FileSystemResource(resFile);
	}
	
}

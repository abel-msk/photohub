package home.abel.photohub.service;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import home.abel.photohub.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import home.abel.photohub.connector.ConnectorsFactory;
import home.abel.photohub.connector.SiteBaseProperty;
import home.abel.photohub.connector.prototype.SiteConnectorInt;
import home.abel.photohub.connector.prototype.SiteCredentialInt;
import home.abel.photohub.connector.prototype.SitePropertyInt;
import home.abel.photohub.connector.prototype.SiteStatusEnum;


@Service
public class SiteService {
	final Logger logger = LoggerFactory.getLogger(SiteService.class);
	
	/*
	 *    Site type constants
	 */
	
	private List<String> siteTypes = null;
	private Map<String,Map<String,SitePropertyInt>> propertiesNamesByType = null;
	
	
	@Autowired
	private home.abel.photohub.model.SiteRepository siteRepo;

	@Autowired
	private TaskQueueService taskQService;
	
	@Autowired
	ThreadPoolTaskExecutor threadPoolTaskExecutor;
	
	@Autowired
	PhotoService photoSvc;
	
	@Autowired
	ConnectorsFactory connectorsFactory;
		
	@Autowired
	private home.abel.photohub.model.NodeRepository nodeRepo;

	@Autowired
	TaskQueueService  queue;
	
	@PersistenceContext
	private EntityManager em;

//
//	ConnectorsFactory cfBackup;
//
//	public  SiteService () {
//	}
//
//	public void setConnectorsFactory(ConnectorsFactory cf) {
//		cfBackup = cf;
//	}
//
	
	/**
	 * 
	 *    Get available sites types and collect default property for each type connector
	 * 
	 * @throws Exception
	 */
	@PostConstruct
	public void Init() throws Exception {

		logger.debug("[SiteService.INIT]  Init started");
		//   Save sites types
		siteTypes = new ArrayList<String>();
		for ( String type: connectorsFactory.getAvailableTypes()) {
			siteTypes.add(type);
		}
		
		//  Save default property for each sites type
		propertiesNamesByType = new HashMap<String, Map<String,SitePropertyInt>>();
		for ( String type: siteTypes) {
			logger.trace("Retrieve propertyes for syte type " + type );
			Map<String, SitePropertyInt> defProperties = connectorsFactory.getSiteDefaultPreperties(type);
			for (String propName: defProperties.keySet()) {
				logger.trace("Site type "+ type +", property "+propName+", with value "+ defProperties.get(propName).getValue());
			}
			propertiesNamesByType.put(type,defProperties);
		}


		//
		//   Init Queue Service.
		//

		try {
			queue.Init();
		}
		catch (Throwable thException) {
			logger.error("[SiteService.Init.Queue]  Error :"+thException.getMessage(),thException);
		}

	}
	
	
	/**
	 *   Get all sites list from DB
	 *   
	 * @return
	 */
	public Iterable<Site> getSitesList() {
		return siteRepo.findAll();
	};
	
	/**
	 *     Find and return Site object from DB, by ID
	 * 
	 * @param siteId
	 * @return
	 */
	public Site getSite(String siteId){
		Site theSite = null;
		if ( siteId != null ) {
			theSite = siteRepo.findOne(siteId);
//		logger.debug("Get site "+ theSite +
//				", type=" + theSite.getConnectorType() +
//				", root=" + theSite.getRoot()
//				);
		}
		return theSite;
	}

	/**
	 *     Возвращает список доступныз типов сайтов
	 * @return
     */
	public List<String> getSiteTypes() {
		if ( this.siteTypes == null) {
			this.siteTypes = new ArrayList<String>();
			for ( String type: connectorsFactory.getAvailableTypes()) {
				siteTypes.add(type);
			}
		}
		return this.siteTypes;
	}

	/**
	 *    Возвращает список дефолтных пропкртей для конкретного типа сайта
	 * @param type
	 * @return
	 * @throws Exception
     */
	public Map<String, SitePropertyInt> getSitesDefaultProperties(String type) throws Exception { 
		Map<String, SitePropertyInt> sitesDefaultProperties = propertiesNamesByType.get(type);
		if ( sitesDefaultProperties == null) {
			sitesDefaultProperties = connectorsFactory.getSiteDefaultPreperties(type);
			propertiesNamesByType.put(type, sitesDefaultProperties);
		}
		return propertiesNamesByType.get(type);
	}

	/**
	 *   Возвращает список свойств сайта из базы.   Возвращает в фомате для передачи через коннектор.
	 *
	 * @param theSite
	 * @return
	 */
	public Map<String,SitePropertyInt> createPropertyMap(Site theSite) {
		if (theSite == null) throw new ExceptionInvalidArgument("Site parameter cannot be null.");
		
		if (theSite.getProperties() == null) return null;
		
		Map<String,SitePropertyInt> propMap = new HashMap<String,SitePropertyInt>();
		for(SiteProperty property : theSite.getProperties()) {
			propMap.put(property.getName(), new SiteBaseProperty(property.getName(),property.getValue()));
		}
		return propMap;
	}
	
//	public List<SiteProperty> convertPropertyToDB(Map<String,SitePropertyInt> inputMap) {
//		
//		
//	}
//	
	/**
	 *     Check for active connector with  siteId = connectorId.
	 *     if not found create new instance for this type connector.
	 *     All parameters for connector, will get from Site object
	 * @param theSite
	 * @return
	 * @throws Exception 
	 */
	public SiteConnectorInt getOrLoadConnector(Site theSite)  throws Exception {
		logger.trace("[getOrLoadConnector] Load connector for site="+theSite);
		logger.trace("[getOrLoadConnector] connectorsFactory =  " +(connectorsFactory!= null?"NOT NULL":"IS NULL"));

		SiteConnectorInt connector =  connectorsFactory.getConnector(theSite.getId());
		if ( connector == null) {
			connector =  connectorsFactory.getConnector(
				theSite.getConnectorType(),
				theSite.getSiteUser(), //TODO Это алиас для ключа хранения сертификатов
				theSite.getId(),
				theSite.getRoot(),
				createPropertyMap(theSite)
				);		
			
			if (logger.isDebugEnabled()) {
				for (String key: connector.getProperties().keySet() ) {
					logger.debug("[getOrLoadConnector] Sites property name=" + key 
					 + ", value=" +connector.getProperties().get(key).getValue());
				}
			}
			connector.setState(SiteStatusEnum.valueOf(theSite.getConnectorState()));
		}
		
		
		//  Check connector status!
		logger.debug("[getOrLoadConnector] Site "+theSite+", connector loaded with STATE="+connector.getState().toString());
		if (! connector.getState().toString().equalsIgnoreCase(theSite.getConnectorState()) ) {
			theSite.setConnectorState(connector.getState().toString());
			logger.trace("[getOrLoadConnector] Site "+theSite+", save new connectors STATE="+connector.getState().toString());
			siteRepo.save(theSite);
		}
		
		return connector;
	}
	

//	public SiteConnectorInt getConnector(String connectorId) throws Exception {
//		return connectorsFactory.getConnector(connectorId);
//	}
	
//	/**
//	 * 	   Check for active connector with  siteId = connectorId.
//	 *     if not found create new instance for this type connector.
//	 *     All parameters for connector, will get from Site object except site properties
//	 *     NOTE property in site object may differ property that send to connector.
//	 * @param theSite
//	 * @param properties  connector property
//	 * @return
//	 * @throws Exception
//	 */
//	public SiteConnectorInt getConnector(Site theSite, Map<String,SitePropertyInt> properties) throws Exception{
//		SiteConnectorInt connector =  connectorsFactory.getConnector(
//				theSite.getConnectorType(),
//				theSite.getSiteUser(), 
//				theSite.getId(),
//				theSite.getRoot(),
//				properties
//				);
//			connector.setState(SiteStatusEnum.valueOf(theSite.getConnectorState()));
//		return connector;
//	}
		
	/**
	 * Create new site in DB and send newly created site for create connector.
	 * @param name      - User defined site name
	 * @param type      - Site type should be one getSiteTypes
	 * @param rootDir   - Root dir where to sync objects
	 * @param properties
	 * @return
	 * @throws Exception
	 */
	public Site createSite(
			String name,
			String type,
			String rootDir,
			Map<String,SitePropertyInt> properties
			) throws Exception {
		logger.debug("Create site. " +  "name="+name+ ", type="+type+ ", rootDir="+rootDir );
		
		Site newSite = new Site();
		newSite.setName(name);
		newSite.setConnectorType(type);
		newSite.setRoot(rootDir);
		newSite.setSiteUser("admin");
		

		Map<String,SitePropertyInt> defaultProperties = getSitesDefaultProperties(type);
		//   Если при создании сайта указали прорерти,
		//   то листаем дефолтные проперти и прооверяем в присланных.
		//   Если прислали новое значение добавляем его в сайт.
		//   Если не прислали добавляем дефолтное значение		
		if ( properties != null ) {
			for (String key: defaultProperties.keySet() ) {
				SitePropertyInt theProperty = properties.get(key);
				if ( theProperty != null ) {
					logger.debug("Copy received property '" + key + "' to site, with value " + theProperty.getValue());
					newSite.addProperty(key,theProperty.getValue());
				}
				else {
					logger.debug("Copy default property '" + key + "' to site.");
					newSite.addProperty(key,defaultProperties.get(key).getValue());
				}
				logger.debug("Set property for site '"+name+"': "+
						"name="+key+ ", value="+ newSite.getProperty(key)
						);
			}
		}
		//    Если при создании сайта проперти не указали, то просто копируем все дефолтные в сайт. 
		else {
			for (String key: defaultProperties.keySet() ) {
				logger.debug("Copy default property '" + key + "' to site.");
				newSite.addProperty(key,defaultProperties.get(key).getValue());
			}
		}
		

//		//    Принудительно добавляем root в проперти
//		if (newSite.getProperty("root") == null ) {
//			newSite.addProperty("root",rootDir);
//			logger.debug("Set property for site '"+newSite.getName()+
//					"': name=root, value="+ rootDir!=null?rootDir:"NULL"
//					);
//		}
		
		
		newSite.setConnectorState(SiteStatusEnum.DISCONNECT.toString());
		newSite = siteRepo.save(newSite);

		return newSite;
	}
	

	
	/**
	 * Connect to site, return connection credential object  and save connector state
	 * @param siteId
	 * @return
	 */
	public SiteCredentialInt connectSite(String siteId, URL caller) throws Exception{
		Site theSite = siteRepo.findOne(siteId);
		if (theSite == null) throw new ExceptionInternalError("The site id in empty");
		return connectSite(theSite,caller);
	}
	/**
	 * Connect to site, return connection credential object  and save connector state
	 * @param theSite
	 * @return
	 * @throws Exception
	 */
	@Transactional
	public SiteCredentialInt connectSite(Site theSite, URL caller) throws Exception{
		logger.trace("[connectSite] Connect site." + theSite);
		SiteConnectorInt connector =  getOrLoadConnector(theSite);
		SiteCredentialInt  authCred = connector.doConnect(caller);
		
		//logger.trace("DB State ="+ theSite.getConnectorState() + ",  connector state="+connector.getState().toString()); 
		
		if ( ! theSite.getConnectorState().equalsIgnoreCase(connector.getState().toString()))  {
			theSite.setConnectorState(connector.getState().toString());
			//logger.debug("Change connection state to='"+connector.getState().toString()+"' For site "+theSite);
			siteRepo.save(theSite);
		}
		logger.debug("[connectSite] Connector state='"+connector.getState().toString()+"' For site "+theSite);
		return authCred;
	}
	
	
	/**
	 *	Perform second auth stage, based on properties returned by user.
	 * @param siteId
	 * @param auth user returned properties 
	 * @return
	 * @throws Exception
	 */
	public SiteCredentialInt authSite(String siteId, SiteCredentialInt auth) throws Exception {
		Site theSite = siteRepo.findOne(siteId);
		return authSite(theSite,auth);
	}
	
	/**
	 *	Perform second auth stage, based on properties returned by user.
	 * @param theSite
	 * @param auth  user returned properties 
	 * @return
	 * @throws Exception
	 */
	public SiteCredentialInt authSite(Site theSite, SiteCredentialInt auth) throws Exception {
		
		logger.debug("Auth site "+theSite);
		SiteConnectorInt connector =  getOrLoadConnector(theSite);
		//connector.doConnect();
		auth = connector.doAuth(auth);
		if ( ! theSite.getConnectorState().equalsIgnoreCase(connector.getState().toString()))  {
			theSite.setConnectorState(connector.getState().toString());
			siteRepo.save(theSite);
		}
		logger.debug("Change connection state='"+connector.getState().toString()+"'. For site "+theSite);
		return auth;
	}
	
	
	/**
	 * Update sites parameters and properties.
	 * Currently updateble fileds
	 *     name
	 *     properties
	 *     
	 * if connection state related property was changed, do reconnect.
	 * 
	 * @param theSite
	 * @return Updated Site opbject
	 * @throws Exception
	 */
	public Site updateSite(Site theSite) throws Exception {
		boolean needReconnect = false;
		//SiteCredentialInt authObj = null;
		Site origSite = siteRepo.findOne(theSite.getId());
		//logger.trace("Update site "+origSite+", with site " + theSite);

		//
		//   Update name athribute
		//
		if (theSite.getName() != null) {
			origSite.setName(theSite.getName());
		}


		//  Check is property changed.  if changed property require reconnect, set needReconnect flag
		//  Map<String, SitePropertyInt> sitesPropMap = propertiesNamesByType.get(theSite.getConnectorType());
		
		
		//
		//   Update properties athribute
		//
		
		if (theSite.getProperties() != null ) {
			Map<String, SitePropertyInt> sitesPropMap = getSitesDefaultProperties(origSite.getConnectorType());		
	
			for (SitePropertyInt sitePropItem : sitesPropMap.values()) {
				String propName = sitePropItem.getName();
				
				// Skip update if property not updatable
				if ( sitePropItem.isUpdatable() ) {
					SiteProperty newProp  = theSite.getPropertyObj(propName);
					SiteProperty origProp  = origSite.getPropertyObj(propName);

					if ((newProp != null))  {
						if ( origProp == null ) {
							origSite.addProperty(propName, newProp.getValue());
							needReconnect = true;
						}
						else if (!newProp.getValue().equals(origProp.getValue())) {
							origProp.setValue(newProp.getValue());
							needReconnect = true;
						}
					}	
				}
			}
			
			if (needReconnect) {
				theSite.setConnectorState(SiteStatusEnum.DISCONNECT.toString());
				
				//  Если конектор звгружен, то нужно обновить статус и в конектере
				SiteConnectorInt  connector = connectorsFactory.getConnector(theSite.getId());
				if ( connector != null) {
					connector.setState(SiteStatusEnum.DISCONNECT);
					connector.disconnectSite();
				}
			}
		}
		
		siteRepo.save(origSite);
		return origSite;
	}
	
	
	
	public void saveSite(Site site) {
		siteRepo.save(site);
	}
	
	
	
	/**
	 *   Disconnect from site, and mark ad Disconnected in db
	 *   
	 * @param siteId
	 * @throws Exception
	 * @return Disconnected site object
	 */
	public Site disconnectSite(String siteId) throws Exception {
		Site theSite = siteRepo.findOne(siteId);
		
		theSite.setConnectorState(SiteStatusEnum.DISCONNECT.toString());
		siteRepo.save(theSite);
		
		//  Если конектор звгружен, то нужно обновить стату и в конектере
		SiteConnectorInt connector =  connectorsFactory.getConnector(theSite.getId());
		if (connector != null ) {
			connector.disconnectSite();
		}

		return theSite;
	}
	
	/*=====================================================================================
	 * 
	 *     SITE CLEANING AND REMOVING
	 * 
	 =====================================================================================*/
	/**
	 * Remove Site from db.
	 * And remove all objects related to this site from db.
	 * @param siteId
	 * @throws Exception 
	 */
	public void removeSite(String siteId) throws Exception {
		Site theSite = siteRepo.findOne(siteId);
		if (theSite == null) throw new ExceptionInvalidArgument("Cannot find site id="+siteId);
		removeSite(theSite);	
	}
	
	/**
	 * Remove Site from db.
	 * And remove all objects related to this site from db.
	 * @param theSite - the site db object for site to be removed
	 * @throws Exception 
	 */
	@Transactional
	public void removeSite(Site theSite) throws Exception {
		if ((theSite == null) || (theSite.getId() == null)) 
			throw new ExceptionInternalError("Remove site. Parameter Site is null or has no id.");

		//  Clean task records abs schedules
		logger.trace("[SiteService.removeSite] Stop sheduled tasks.");

		//  Stop abd remove all tasks for tis site
		taskQService.stopTasksForSite(theSite);

		//   Remove sites photo object
		cleanSite(theSite);
		
		if (logger.isDebugEnabled() ) {
			logger.trace("[SiteService.removeSite] Look for not removed objects.");
			Iterable<Node> notRemovedNodes = nodeRepo.findAll(QNode.node.photo.siteBean.id.eq(theSite.getId()));
			for (Node theNode : notRemovedNodes) {
				logger.error("Found not removed object for site "+theSite+" : Object id=" + theNode + "/"+ theNode.getPhoto().getName() + ".");
			}
		}
		logger.trace("[SiteService.removeSite] Remove site" + theSite);	
		
		//
		//   При удалении сейта необходимо отсоединить коннектор и удалить авторизвационный токен. 
		try {
			SiteConnectorInt connector = getOrLoadConnector(theSite);
			connector.disconnectSite();
		} catch (Exception e) {
			logger.error("Cannot disconnect site, id=" + theSite,e);
		}
		logger.trace("[SiteService.removeSite] Disconnect connector for site " + theSite + ", connector type ="+theSite.getConnectorType());	
		
		siteRepo.delete(theSite);
			
	}
	
	/** 
	 *   Remove sites content
	 *   
	 * @param siteId Site id in db
	 * @throws Exception
	 */
	@Transactional
	public void cleanSite(String siteId) throws Exception {
		Site theSite = siteRepo.findOne(siteId);
		if (theSite == null) throw new ExceptionInvalidArgument("Cannot find site id="+siteId);		
		cleanSite(theSite);
		
	}
	
	/**
	 *    Remode sites content
	 *    
	 * @param theSite db site object
	 * @throws Exception
	 */		
	@Transactional
	public void cleanSite(Site theSite) throws Exception {
		logger.trace("Remove site "+theSite+" content.");
		Iterable<Node> nodesList = listSitesRoot(theSite);		
		try {
			for (Node theNode : nodesList) {
				photoSvc.deleteObject(theNode,true,false);
			}		
		}
		catch(Exception e) {
			logger.error("Got error when remove object: " + e.getMessage());
			throw new ExceptionDBContent("Cannot remove sites content. Nested exception: " + e.getMessage(),e);
		}
	}
	

	/*=============================================================================================
	 * 
	 *    Couple Site processing utilities
	 *      
	 =============================================================================================*/

	/**
	 *  Возвращает список корневых объектов для сайта
	 *  
	 *  List all root objects for this site
	 * @param theSite
	 * @return - List of root objects for this site
	 */
	//@Transactional(propagation=Propagation.SUPPORTS)
	public Iterable<Node> listSitesRoot(Site theSite) {
		Iterable<Node> nodesList = null;
//		Iterable<Node> nodesList = nodeRepo.findAll(QNode.node.photo.siteBean.id.eq(theSite.getId())
//				.and(QNode.node.root.isTrue()));
//		Node rootFolder = getRootFolder();
//		if (rootFolder != null) {
//			nodesList = nodeRepo.findAll(
//					QNode.node.photo.siteBean.id
//					.eq(theSite.getId())
//					.and(QNode.node.parent.eq(rootFolder.getId())));		
//		}
//		else {
			nodesList = nodeRepo.findAll(
					QNode.node.photo.siteBean.id
					.eq(theSite.getId())
					.and(QNode.node.parent.isNull()));	
//		}
		return nodesList;
	}


}

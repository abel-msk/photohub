package home.abel.photohub.connector;

import home.abel.photohub.connector.prototype.*;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  Collect available  classes for generate class connector.
 *	Generate connector by its type and store it in activated connector list.
 *  
 *  SiteConnector is identified by its id.
 *  ID is the ID of record in db.
 *  Record contains:  
 *     type - type of site this connector can connect.
 *     userId - user Id or user name for site connecting.
 *  
 *  After activation, connector saved in hash with its ID as key
 *  We can retrieve active connector by its Id.
 *  
 *  At the initial state we need to add all available connector class 
 *  by method - addConnectorClass
 *  
 *  At the runtime, we can get all loaded class by be retrieving all available connector types
 *  by - getAvailableTypes
 *  
 *  Create connector working with site 
 *  by createConnector
 *  
 *  Get activated connector from list.
 *  getConnector
 *  
 *  
 *  
 *  
 *  Class is thread safe.
 * 
 * @author abel
 *
 */
public class ConnectorsFactory {

	protected  ClassLoader currentClassLoader = null;
	protected final Map<String,Class<?>> connectorsClassesMap  =  new ConcurrentHashMap<String,Class<?>>();
	protected final Map<String,SiteConnectorInt> activeConectors  =  new ConcurrentHashMap<String,SiteConnectorInt>();
	protected final Map<String,SiteConnectorInt> sampleConectors  =  new ConcurrentHashMap<String,SiteConnectorInt>();
	protected DataSource dataSource = null;
	protected KeyStoreFactory keyStoreFactory = null;

	public ConnectorsFactory() {
		currentClassLoader = this.getClass().getClassLoader();
	}
	
	/**
	 * Load site connectors from  property file.
	 * @param configName  - name of properties file located in CLASSPATH
	 * @throws IOException
	 */
//	public void loadConnectors(String configName) throws IOException {
//		Properties prop = new Properties();
//		InputStream in = currentClassLoader.getResourceAsStream(configName);
//		prop.load(in);
//		in.close();
//	}

	/**
	 * Append new class name to list of available connector classes
	 * @param className
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public void addConnectorClass(String className) throws ClassNotFoundException, InstantiationException, IllegalAccessException {					
		Class<?> ConnectorImplClass = currentClassLoader.loadClass(className);
		 if ( SiteConnectorInt.class.isAssignableFrom(ConnectorImplClass) )  {	
			 SiteConnectorInt connectorImplInstance = (SiteConnectorInt)ConnectorImplClass.newInstance(); 
			 
			 if ( ! connectorsClassesMap.containsKey(connectorImplInstance.getSiteType()) ) {
				 connectorsClassesMap.put(connectorImplInstance.getSiteType(),ConnectorImplClass);
				 sampleConectors.put(connectorImplInstance.getSiteType(),connectorImplInstance);
			 } 
			 else {
				 throw new InstantiationException("Implementation of class '"+className+" has empty type.");
			 }
		 }
		 else 
			 throw new ClassNotFoundException("Class '" + className + "'should implament '" + SiteConnectorInt.class.getCanonicalName()+"'");
	}

	/**
	 * Add data source for access O2Auth key store.
	 * If set, method will generate new {@link KeyStoreInt} object and pass it to connector
	 * @param ds
	 */
	public void setDataSource(DataSource ds) {
		keyStoreFactory = new KeyStoreFactory(ds);
		dataSource = ds;
	}

	/**
	 * Add access class for obtain  O2Auth key from DB. Passed to connector when created.
	 * @param ksf factory for generate {@link KeyStoreInt} class for exact site
	 */
	public void setKeyStoreFactory(KeyStoreFactory ksf) {
		keyStoreFactory = ksf;
	}

	/**
	 *    Return available connectors types
	 * @return
	 */
	public Set<String> getAvailableTypes() {
		return connectorsClassesMap.keySet();
	}
	
	/** 
	 * 
	 *   Return Map of default properties for selected site type
	 * 
	 * @param siteType
	 * @return
	 * @throws ClassNotFoundException
	 */
	public Map<String, SitePropertyInt> getSiteDefaultPreperties(String siteType) throws ClassNotFoundException {
		SiteConnectorInt sampleSite = sampleConectors.get(siteType);
		if (sampleSite == null) throw new ClassNotFoundException("Thereis no site type :"+siteType);
		Map<String, SitePropertyInt> defProperties = sampleSite.getProperties();		
		return defProperties;
	}

	/*
	 * 
	 * 
	 *    GET CONNECTOR
	 * 
	 * 
	 */
	/**
	 * Get connector class from from loaded connectors list.  Return null if connector not loaded.
	 * @param connectorId
	 * @return  Connector instance or null.
	 */
	public SiteConnectorInt getConnector(String  connectorId) {
		return activeConectors.get(connectorId);
	}
	

	/**
	 *    Get connector class from loaded connectors list.  If not found try to load it.
	 * 
	 * @param type
	 * @param siteUser
	 * @param connectorId
	 * @param localStore
	 * @param inputPropertiesMap
	 * @return  Connector instance
	 * @throws ConnectorLoadException
	 */
	public SiteConnectorInt getConnector(
			String type, 
			String siteUser,
			String connectorId,
			String localStore,
			String savedState,
			Map<String, SitePropertyInt> inputPropertiesMap
			) throws ConnectorLoadException {	

		SiteConnectorInt connector = null;
		if (activeConectors.get(connectorId) == null) {
			connector = createConnector(type,siteUser,connectorId,localStore,savedState,inputPropertiesMap);
		}		
		else {
			connector =  activeConectors.get(connectorId);
//			connector.setLocalStore(localStore);
//			connector.setUser(siteUser);
//			connector.setProperties(inputPropertiesMap);
		}
		return connector;
	}

	/**
	 *
	 *  Create new site connector with given type and user name
	 *
	 * @param siteType
	 * @param siteUser
	 * @param connectorId
	 * @param localStore
	 * @param inputPropertiesMap
	 * @return
	 * @throws ConnectorLoadException
	 */
	public SiteConnectorInt createConnector(
			String siteType, 
			String siteUser, 
			String connectorId,
			String localStore,
			String savedState,
			Map<String, SitePropertyInt> inputPropertiesMap
			) throws ConnectorLoadException {

		SiteConnectorInt connector = null;

		Class<?> ConnectorImplClass = connectorsClassesMap.get(siteType);
		if (ConnectorImplClass == null) {
			throw new ConnectorLoadException("Connector class for type " + siteType + " not found");
		}

		try {
			connector = (SiteConnectorInt) ConnectorImplClass.newInstance();
			connector.setUser(siteUser);
			connector.setId(connectorId);
			connector.setLocalStore(localStore);
			connector.setProperties(inputPropertiesMap);
			connector.setState(SiteStatusEnum.valueOf(savedState));
			if (keyStoreFactory != null) {
				connector.setKeyStore(keyStoreFactory.getKeyStore(connectorId));
			}
			activeConectors.put(connectorId, connector);

		} catch (InstantiationException e) {
			throw new ConnectorLoadException("Cannot instantiate connector class " + ConnectorImplClass.getClass().getName(), e);
		} catch (IllegalAccessException e) {
			throw new ConnectorLoadException("Cannot access to instance of connector class " + ConnectorImplClass.getClass().getName(), e);
		}

		//----------------------------------
		//  Try to connect the connector
		if (SiteStatusEnum.valueOf(savedState) == SiteStatusEnum.CONNECT) {
			try {
				//connector.doConnect(null);
				connector.doReconnect();
			} catch (Throwable e) {
				connector.setState(SiteStatusEnum.DISCONNECT);
			}
		}

		return connector;
	}
	
//	public SiteConnectorInt createConnector(
//			String siteType, 
//			String siteUser, 
//			String connectorId,
//			List<SitePropertyInt> inputPropertiesList
//			) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
//	
//		Map<String, SitePropertyInt> inputPropertiesMap = new HashMap<String, SitePropertyInt>();	
//		if (inputPropertiesList != null) {
//			for (SitePropertyInt inputProp: inputPropertiesList ) {
//				inputPropertiesMap.put(inputProp.getName(), inputProp);
//			}
//		}
//		return createConnector(siteType,siteUser,connectorId, inputPropertiesMap);
//	}	
		
}

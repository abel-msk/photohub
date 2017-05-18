package home.abel.photohub.connector.google;

import home.abel.photohub.connector.ConnectorsFactory;
import home.abel.photohub.connector.SiteBaseProperty;
import home.abel.photohub.connector.prototype.PhotoObjectInt;
import home.abel.photohub.connector.prototype.SiteConnectorInt;
import home.abel.photohub.connector.prototype.SiteCredentialInt;
import home.abel.photohub.connector.prototype.SitePropertyInt;
import home.abel.photohub.connector.prototype.SiteStatusEnum;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleConnectorTest {
    final Logger logger = LoggerFactory.getLogger(GoogleConnectorTest.class);

	private static final BufferedReader IN
			= new BufferedReader(new InputStreamReader(System.in));

    @Test
	public void initFactory() {  
      	
		try {
			ConnectorsFactory factory   = new ConnectorsFactory();
			factory.addConnectorClass("home.abel.photohub.connector.google.GoogleSiteConnector");

			String hasType = null;
			String connectorId = "1";
			SitePropertyInt prop = new SiteBaseProperty(GoogleSiteConnector.GOOGLE_PERSON_ID,"","abel");
			
			Map<String, SitePropertyInt> sitePropertiesMap = new HashMap<String, SitePropertyInt>();	
			sitePropertiesMap.put(GoogleSiteConnector.GOOGLE_PERSON_ID,prop);
			
			for (String type: factory.getAvailableTypes()) {
				hasType = type;
				logger.debug("Factory has type '"+type+"'");
			}
			
			//   Create connector thought factory 
			//Map<String, SitePropertyInt> propMap = new HashMap<String, SitePropertyInt>();
			
			SiteConnectorInt connector  = factory.createConnector(
					hasType,
					"abel",
					connectorId,
					"/tmp",
					sitePropertiesMap   //Propertyes Map
					);
			
			//  Эмулируем что токен уже загружен
			connector.setState(SiteStatusEnum.CONNECT);
			
			
			
//			SiteCredentialInt exchangeCred = connector.doConnect(new URL("http://localhost:6443/api"));
			SiteCredentialInt exchangeCred = connector.doConnect(null);			 
			if ( exchangeCred.getState() == SiteStatusEnum.CONNECT) {
				System.out.println("+++ Connected successfuly.");
			}
			else {
				String code ="";
				System.out.println("+++" + exchangeCred.getUserMessage() + "+++");
				System.out.println(exchangeCred.getUserLoginFormUrl());
				System.out.println("->");
				connector  = factory.getConnector(connectorId);
		    	
				//Scanner s = new Scanner(System.in);
				//code = s.nextLine();
 				//s.close();

                String result = IN.readLine();
                code = result.trim();




				
//				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//		    	String code = br.readLine();
//		    	br.close();


		    	//System.out.println("Got code:" + code);
				logger.debug("The code = " + code );
				System.out.println("+++ GO +++");

		    	exchangeCred.setAccessToken(code);	
		    	connector.doAuth(exchangeCred);
		    	
		    	System.out.println("+++ AUTH OK +++");
			}
			
//			List<PhotoObjectInt> rootList = connector.getRootObjects();
//			
//			for ( PhotoObjectInt Item : rootList) {
//				logger.debug("Item="+Item.getName());
//			}
//			
//			List<PhotoObjectInt> filePhotos = null;
//			PhotoObjectInt AlbmObject = rootList.get(0);
//			if (AlbmObject.isFolder()) {
//				filePhotos  = AlbmObject.listSubObjects(); 
//			}
//			filePhotos.get(0).getThumbnail(new Dimension(250,250));
//			
			//PhotoMetadataInt meta = filePhotos.get(0).getMeta();
			//6060383542025899969.5795087093546466562
			String theIdString = new String("6060383542025899969.5795087093546466562");
			PhotoObjectInt gObject = connector.loadObject(theIdString);
			
			gObject.getThumbnail(new Dimension(250,250));
			
			//   Check metadata editing
//			String theIdString = new String("5383296798370565889.5383296918768456850");
//			PhotoObjectInt gObject = connector.loadObject(theIdString);
//			PhotoMetadataInt meta2 = gObject.getMeta();
//			meta2.setLatitude(55.634875);
//			meta2.setLongitude(38.0831805);
//			gObject.setMeta(meta2);
			
			
			//listSubObjects
//			ConnectorCallbackInt cb =  new  ConnectorCallbackInt() {
//				@Override
//				public String loadPhotoObj(String paretnExtId, PhotoObjectInt parent, PhotoObjectInt photo) {
//					if ( parent == null )
//						logger.debug("Callback called for object = ROOT/" + photo.getName());
//					else 
//						logger.debug("Callback called for object = "+ parent.getId()+ "/" + photo.getName());
//						photo.getMeta();
//					return "1";
//				}
//			};
			
			
			
//			connector  = factory.getConnector(connectorId);
//			this.doScann(connector.getRootObjects());
			
//			
//			System.out.println("+++ Create Folder +++");
//			PhotoObjectInt gFolder = null;
//			try {
//				gFolder = connector.createFolder("test-folder", null);
//			} catch (Exception e) {
//				gFolder.delete();
//				throw e;
//			}
//			
//			System.out.println("+++ Create Photo Object +++");
//			PhotoObjectInt gImage = null;
//			URL resourceUrl = System.class.getResource("/sample_image2.jpg");			
//			File imgFile = new File(resourceUrl.getFile());
//			FileInputStream fis = new FileInputStream(imgFile);
//			
//			try {
//				gImage = connector.createObject("sample_image2.jpg",gFolder,fis);
//			} catch (Exception e) {
//				throw e;
//			} finally {
//				gImage.delete();
//				gFolder.delete();
//			}
						
    	}
    	catch (Throwable e) {
    		System.out.println("ERROR: " + e.getMessage() ) ;
    		e.printStackTrace();
    	}
	}
    
    
    
	public void doScann(List<PhotoObjectInt> objList) throws Exception {
		if ( objList != null ) {
			for (PhotoObjectInt Item: objList) {
				logger.trace("Add scanned object to db. objectId="+Item.getId());
				//  Если это фоолдер то сканируем его содержимое
				if ( Item.isFolder() ) {
					doScann(Item.listSubObjects());
				}
			}
		}		
	}
}

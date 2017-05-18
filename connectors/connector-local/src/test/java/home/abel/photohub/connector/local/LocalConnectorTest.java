package home.abel.photohub.connector.local;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
//import java.nio.file.Path;
//import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import home.abel.photohub.connector.BasePhotoMetadata;
import home.abel.photohub.connector.ConnectorsFactory;
import home.abel.photohub.connector.prototype.PhotoMediaObjectInt;
import home.abel.photohub.connector.prototype.PhotoMetadataInt;
import home.abel.photohub.connector.prototype.PhotoObjectInt;
import home.abel.photohub.connector.prototype.SiteConnectorInt;
import home.abel.photohub.connector.prototype.SitePropertyInt;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalConnectorTest {
	final Logger logger = LoggerFactory.getLogger(LocalConnectorTest.class);


    @Test
	public void initFactory() {  	
		try {
			ConnectorsFactory factory   = new ConnectorsFactory();
			String hasType = null;
			
			//   Load resources and get resources parent folder name
			//URL resourceUrl = this.getClass().getClassLoader().getResource("/sample_image2.jpg");
			URL resourceUrl = System.class.getResource("/sample_image2.jpg");

			String sampeImagePath = resourceUrl.getFile();
			
			System.out.println("Got sample image URL '"+sampeImagePath+"'");
//			
//			Path resourcePath = Paths.get(resourceUrl.toURI());
//			String sampeImagePath = resourcePath.toAbsolutePath().toString();
//			
			File sampeImageFile = new File(sampeImagePath);
			

			factory.addConnectorClass("home.abel.photohub.connector.local.LocalSiteConnector");
			String connectorId = "1";
			for (String type: factory.getAvailableTypes()) {
				hasType = type;
				System.out.println("Factory has type '"+type+"'");
			}
			//   Create connector thought factory 
			
			Map<String, SitePropertyInt> propMap = new HashMap<String, SitePropertyInt>();
			
			SiteConnectorInt connector  = factory.createConnector(
					hasType,
					"abel",
					connectorId,
					sampeImagePath,
					propMap
					);
			
			//String connctorId = connector.getConnectorId();
			System.out.println("Got connector with ID '"+connector.getId()+"'");
			
			//  Update property with site root path. get from sample image enclosure folder
			//rootPаthPropertyObj.setValue(sampeImageFile.getParent());
			connector.setProperty(LocalSiteConnector.ROOT_PATH_PNAME,"/tmp");
			connector.setLocalStore("/tmp");
			
			
			System.out.println("\n------------ Testing loadObject ------------");
			//File simpleImage = resourcePath.toFile();	
			PhotoObjectInt photoObj = connector.loadObject(sampeImagePath);
			System.out.println("Load photo object id='"+photoObj.getId()+"', name='"+photoObj.getName()+"'");
			
			PhotoMediaObjectInt thumbMedia = photoObj.getThumbnail(new Dimension(250,250));
			InputStream is = thumbMedia.getInputStream();
			Assert.assertNotNull(is);
			is.close();
			
			//   Get Metadata from object
			PhotoMetadataInt metadata= photoObj.getMeta();
			Assert.assertNotNull(metadata);
			System.out.println("Metadata. UnicId = " + metadata.getUnicId() );
			System.out.println("Metadata. CameraMake = " + metadata.getCameraMake() );
			System.out.println("Metadata. CameraModel = " + metadata.getCameraModel() );		
			System.out.println("Metadata. Date = " + metadata.getCreationTime());	
			
			
			System.out.println("\n------------ Testing Edit metadata ------------");
		
			//metadata = new BasePhotoMetadata();
			metadata.setUnicId(UUID.randomUUID().toString());
			metadata.setLatitude(55.634875);
			metadata.setLongitude(38.0831805);
			metadata.setAperture("2.8");
			metadata.setCameraMake("Abel home");
			metadata.setCameraModel("A-5");
			photoObj.setMeta(metadata);
			
			//  Reload photo object
			photoObj = connector.loadObject(photoObj.getId());
			metadata= photoObj.getMeta();
			Assert.assertNotNull(metadata);
			System.out.println("Metadata. UnicId = " + metadata.getUnicId() );
			System.out.println("Metadata. CameraMake = " + metadata.getCameraMake() );
			System.out.println("Metadata. CameraModel = " + metadata.getCameraModel() );		
			System.out.println("Metadata. Date = " + metadata.getCreationTime());	
			
			System.out.println("\n------------ Testing doScan ------------");

			//System.out.println("Connector root path = " + connector.getRootObjects());
			
			doScann(connector.getRootObjects());
			
				
				
			System.out.println("\n------------ Testing createObject with parent = null ------------");
			// Add photo object to site
			FileInputStream fis = new FileInputStream(sampeImageFile);
			photoObj = connector.createObject(sampeImageFile.getName(), null, fis);
			System.out.println("PhotoObject ID (path) = " + photoObj.getId());

			metadata= photoObj.getMeta();
			Assert.assertNotNull(metadata);
			System.out.println("Metadata. UnicId = " + metadata.getUnicId() );
			System.out.println("Metadata. CameraMake = " + metadata.getCameraMake() );
			System.out.println("Metadata. CameraModel = " + metadata.getCameraModel() );		
			System.out.println("Metadata. Date = " + metadata.getCreationTime());		
			
			
			System.out.println("\n------------ Testing delete ------------");
			connector.deleteObject(photoObj);
			System.out.println("Deleted !");

			
			
    	}
    	catch (Exception e) {
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
					List<PhotoObjectInt> subList =  Item.listSubObjects();
					if ( subList != null ) {
						doScann(Item.listSubObjects());
					}
					else {
						logger.warn("folder contnet list is null for object="+Item.getId() );
					}
				}
			}
		}		
	}
}

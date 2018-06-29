package home.abel.photohub.connector.local;

import java.awt.Dimension;
import java.io.*;
import java.net.URL;
//import java.nio.file.Path;
//import java.nio.file.Paths;
import java.util.*;

import home.abel.photohub.connector.BasePhotoMetadata;
import home.abel.photohub.connector.ConnectorsFactory;
import home.abel.photohub.connector.HeadersContainer;
import home.abel.photohub.connector.SiteMediaPipe;
import home.abel.photohub.connector.prototype.*;

import home.abel.photohub.utils.image.Metadata;
import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.InputStreamResource;

import static org.assertj.core.api.Assertions.assertThat;

public class LocalConnectorTest {
	final Logger logger = LoggerFactory.getLogger(LocalConnectorTest.class);

	public final static String TMP_ROOT_PATH = "/tmp/photohub_local_conn_test";
	public final static String RESOURCE_IMAGE_FN = "sample_image2.jpg";
	public final static String LOCAL_CONNECTOR_TYPE = "Local";
	public File sampeImageFile = null;
	public static File rootFolder = new File(TMP_ROOT_PATH);
	public ConnectorsFactory factory = null;
	public SiteConnectorInt connector = null;

	@BeforeClass
	public static  void beforeClass() throws Exception {
		System.out.println("\n*** beforeClass is invoked");
		if ( ! rootFolder.exists() ) {
			rootFolder.mkdirs();
		}
		System.out.println("Create temp directory: " + rootFolder.getAbsolutePath());
	}

	@AfterClass
	public static void afterClass() throws Exception {
		System.out.println("***After Class is invoked");
		System.out.println("***Remove site structure");
		FileUtils.deleteDirectory(rootFolder);
	}

	@Before
	public void prepareConnector() throws Exception{

		URL resourceUrl = System.class.getResource("/" + RESOURCE_IMAGE_FN);
		String resourceImagePath = resourceUrl.getFile();
		System.out.println("Got sample image URL '" + resourceImagePath + "'");
		File resourceFile = new File(resourceImagePath);

		sampeImageFile = new  File(rootFolder.getAbsolutePath() +  File.separator + RESOURCE_IMAGE_FN);
		FileUtils.copyFile(resourceFile, sampeImageFile);

//		if ( ! sampeImageFile.exists()) {
//			FileUtils.copyFile(resourceFile, sampeImageFile);
//		}

		if ( factory == null ) {
			System.out.println("Init connectors factory, add LocalSiteConnector");
			factory = new ConnectorsFactory();
			factory.addConnectorClass("home.abel.photohub.connector.local.LocalSiteConnector");

		}

		if ( connector == null ) {
			String connectorId = "1";

			Map<String, SitePropertyInt> propMap = new HashMap<String, SitePropertyInt>();

			connector = factory.createConnector(
					LOCAL_CONNECTOR_TYPE,
					"abel",
					connectorId,
					rootFolder.getAbsolutePath(),  // set local store
					propMap
			);
			System.out.println("Create local site connector id="+connector.getId());
//			connector.setLocalStore(rootFolder.getAbsolutePath());
		}

	}

    @Test
	public void loadImageTest() {
		try {
//			ConnectorsFactory factory   = new ConnectorsFactory();
//			String hasType = null;
//
//			//   Load resources and get resources parent folder name
//			//URL resourceUrl = this.getClass().getClassLoader().getResource("/sample_image2.jpg");
//
//			URL resourceUrl = System.class.getResource("/sample_image2.jpg");
//			String sampeImagePath = resourceUrl.getFile();
//			System.out.println("Got sample image URL '"+sampeImagePath+"'");
////
////			Path resourcePath = Paths.get(resourceUrl.toURI());
////			String sampeImagePath = resourcePath.toAbsolutePath().toString();
////
//			File sampeImageFile = new File(sampeImagePath);
//
//			factory.addConnectorClass("home.abel.photohub.connector.local.LocalSiteConnector");
//			String connectorId = "1";
//			for (String type: factory.getAvailableTypes()) {
//				hasType = type;
//				System.out.println("Factory has type '"+type+"'");
//			}
//			//   Create connector thought factory
//
//			Map<String, SitePropertyInt> propMap = new HashMap<String, SitePropertyInt>();
//
//			SiteConnectorInt connector  = factory.createConnector(
//					hasType,
//					"abel",
//					connectorId,
//					sampeImagePath,
//					propMap
//					);
//
//			//String connctorId = connector.getConnectorId();
//			System.out.println("Got connector with ID '"+connector.getId()+"'");
//
//			//  Update property with site root path. get from sample image enclosure folder
//			//rootPаthPropertyObj.setValue(sampeImageFile.getParent());
//			connector.setProperty(LocalSiteConnector.ROOT_PATH_PNAME,"/tmp");
//			connector.setLocalStore("/tmp");
			
			
			System.out.println("\n------------ Testing loadObject ------------");
			//File simpleImage = resourcePath.toFile();	
			PhotoObjectInt photoObj = connector.loadObject(sampeImageFile.getAbsolutePath());
			System.out.println("Load photo object id='"+photoObj.getId()+"', name='"+photoObj.getName()+"'");
			
			PhotoMediaObjectInt thumbMedia = photoObj.getThumbnail(new Dimension(250,250));
			SiteMediaPipe resource = thumbMedia.getContentStream(null);
			assertThat(resource).isNotNull();
			assertThat(resource.getInputStream()).isNotNull();
			resource.getInputStream().close();
			
			//   Get Metadata from object
			PhotoMetadataInt metadata= photoObj.getMeta();
			Assert.assertNotNull(metadata);
			System.out.println("Metadata. UnicId = " + metadata.getUnicId() );
			System.out.println("Metadata. CameraMake = " + metadata.getCameraMake() );
			System.out.println("Metadata. CameraModel = " + metadata.getCameraModel() );		
			System.out.println("Metadata. Date = " + metadata.getDateCreated());
			
			
			System.out.println("\n------------ Testing Edit metadata ------------");
		
			//metadata = new BasePhotoMetadata();
			metadata.setUnicId(Metadata.generateUUID());
			metadata.setLatitude(55.634875);
			metadata.setLongitude(38.0831805);
			metadata.setAperture(new Double("2.8"));
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
			System.out.println("Metadata. Date = " + metadata.getDateCreated());
			
			//System.out.println("\n------------ Testing doScan ------------");

			//System.out.println("Connector root path = " + connector.getRootObjects());
			
			//doScann(connector, connector.getRootObjects());

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
			System.out.println("Metadata. Date = " + metadata.getDateCreated());

			PhotoMediaObjectInt media = photoObj.getMedia(EnumMediaType.IMAGE);

			HeadersContainer headers = new HeadersContainer();
			headers.addHeader("Range","bytes=500-");
			SiteMediaPipe pipe = connector.loadMediaByPath(media.getPath(),headers);

			System.out.println("\nGot headers:");
			for (String key : pipe.getHdrKeys()) {
				for (String value: pipe.getHdrValues(key)) {
					System.out.println("    "+key+ ": "+value);
				}
			}
			System.out.println("    Error: "+pipe.getError());
			System.out.println("    Status: "+pipe.getStatus());

			assertThat(pipe.getInputStream() != null).isTrue();

			byte[] buffer = new byte[1024];
			int length;
			int totoalLength = 0;
			while ((length = pipe.getInputStream().read(buffer)) != -1) {
				totoalLength += length;
			}

			System.out.println("Read " + totoalLength +" bytes");
			pipe.getInputStream().close();

			System.out.println("\n------------ Testing delete ------------");
			connector.deleteObject(photoObj);
			System.out.println("Deleted !");
			
    	}
    	catch (Exception e) {
    		System.out.println("ERROR: " + e.getMessage() ) ;
    		e.printStackTrace();
    	}
	}

	@Test
	public void testRotate() throws Exception {
		PhotoObjectInt photoObj = connector.loadObject(sampeImageFile.getAbsolutePath());
		PhotoObjectInt rotateObj = photoObj.rotate90(PhotoObjectInt.rotateEnum.CLOCKWISE);

		assertThat(photoObj.getHeight()).isEqualTo(rotateObj.getWidth());

	}


	@Test
	public void testScan() throws Exception {
		System.out.println("\n------------ Testing doScan ------------");
		doScann(connector, connector.getRootObjects());
	}

	public void doScann(SiteConnectorInt connector, List<String> objList) throws Exception {
		if ( objList != null ) {
			for (String ItemKey: objList) {
				logger.trace("Add scanned object to db. objectId="+ItemKey);

				PhotoObjectInt ItenObj = connector.loadObject(ItemKey);

				//  Если это фоолдер то сканируем его содержимое
				//List<String> subList =  ItenObj.listSubObjects();

				if ( ItenObj.isFolder() ) {
					List<String> subList =  ItenObj.listSubObjects();
					if ( subList != null ) {
						doScann(connector, ItenObj.listSubObjects());
					}
					else {
						logger.warn("folder contnet list is null for object="+ItenObj.getId() );
					}
				}
			}
		}		
	}
}

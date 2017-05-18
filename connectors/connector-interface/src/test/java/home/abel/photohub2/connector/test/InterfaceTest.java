package home.abel.photohub2.connector.test;
import static org.junit.Assert.*;

import java.io.File;

import home.abel.photohub.connector.BasePhotoObj;
import home.abel.photohub.connector.ConnectorsFactory;
import home.abel.photohub.connector.prototype.SiteConnectorInt;

import org.junit.Test;



public class InterfaceTest {
	
    @Test
	public void initFactory() {  	
		ConnectorsFactory factory   = new ConnectorsFactory();
		String hasType = null;
		try {
			factory.addConnectorClass("home.abel.photohub2.connector.test.ConnectorImpl");
			//factory.Init();
			for (String type: factory.getAvailableTypes()) {
				hasType = type;
				System.out.println("Factory has type '"+type+"'");
			}
			String connctorId = "1";
			SiteConnectorInt connector  = factory.getConnector(hasType, "abel", connctorId , "/tmp" , null);

			
			//  Test scanning
    	}
    	catch (Exception e) {
    		System.out.println("ERROR: " + e.getMessage() ) ;
    		e.printStackTrace();
    		
    	}

	}
}

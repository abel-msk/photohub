package home.abel.photohub.connector.google;


import home.abel.photohub.connector.ConnectorsFactory;
import home.abel.photohub.connector.SiteBaseProperty;
import home.abel.photohub.connector.prototype.SiteConnectorInt;
import home.abel.photohub.connector.prototype.SiteCredentialInt;
import home.abel.photohub.connector.prototype.SitePropertyInt;
import home.abel.photohub.connector.prototype.SiteStatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


/**
 * Created by abel on 03.04.17.
 */


public class GoogleActivator {
    final Logger logger = LoggerFactory.getLogger(GoogleActivator.class);


    public static void main(String[] args) {

        Scanner s = null;
        String code = null;

        try {
            ConnectorsFactory factory   = new ConnectorsFactory();
            factory.addConnectorClass("home.abel.photohub.connector.google.GoogleSiteConnector");

            String hasType = null;
            String connectorId = "1";

            for (String type: factory.getAvailableTypes()) {
                hasType = type;
                System.out.println("Factory has type '"+type+"'");
            }


            SitePropertyInt prop = new SiteBaseProperty(GoogleSiteConnector.GOOGLE_PERSON_ID,"","abel");

            Map<String, SitePropertyInt> sitePropertiesMap = new HashMap<String, SitePropertyInt>();
            sitePropertiesMap.put(GoogleSiteConnector.GOOGLE_PERSON_ID,prop);


            //   Create connector thought factory
            SiteConnectorInt connector  = factory.createConnector(
                    hasType,
                    "abel",
                    connectorId,
                    "/tmp",
                    SiteStatusEnum.CONNECT.toString(),
                    sitePropertiesMap   //Propertyes Map
            );

            //  Эмулируем что токен уже загружен
           // connector.setState(SiteStatusEnum.CONNECT);


            SiteCredentialInt exchangeCred = connector.doConnect(null);
            if ( exchangeCred.getState() == SiteStatusEnum.CONNECT) {
                System.out.println("+++ Connected successfuly.");
            }
            else {
                connector  = factory.getConnector(connectorId);

                System.out.println("+++" + exchangeCred.getUserMessage() + "+++");
                System.out.println(exchangeCred.getUserLoginFormUrl());
                System.out.println("->");

                s = new Scanner(System.in);
                code = s.nextLine();
                s.close();

                System.out.println("The code = " + code );
                System.out.println("+++ GO +++");

                exchangeCred.setAccessToken(code);
                connector.doAuth(exchangeCred);

                System.out.println("+++ AUTH OK +++");
            }


        }
        catch (Throwable e) {
            System.out.println("ERROR: " + e.getMessage() ) ;
            e.printStackTrace();
        }
    }
}

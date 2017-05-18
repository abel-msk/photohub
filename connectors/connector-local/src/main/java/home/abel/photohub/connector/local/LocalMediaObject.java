package home.abel.photohub.connector.local;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import home.abel.photohub.connector.BaseMediaObject;
import home.abel.photohub.connector.prototype.EnumMediaType;
import home.abel.photohub.connector.prototype.SiteConnectorInt;

public class LocalMediaObject extends BaseMediaObject {
	final Logger logger = LoggerFactory.getLogger(LocalMediaObject.class);

	
	protected SiteConnectorInt connector = null;
	protected  File sourceFile;
	protected String requestType;

	
	public LocalMediaObject(SiteConnectorInt connector, File source, String requestType) throws IOException {
		sourceFile = source;
		this.connector = connector;
		this.requestType =requestType;
		accType = EnumMediaType.ACC_LOACL;
	}

		
	public InputStream getInputStream() throws IOException {
		
		InputStream is = null;
		if (sourceFile != null) {
			if ( requestType.equalsIgnoreCase("THUMB")) {
				//logger.trace("Scale image to width=" +getWidth()+", height="+getHeight());
				ImageScaler scaller = new ImageScaler();
				is = scaller.doScale(sourceFile, new Dimension(getWidth(),getHeight()));
			} else {
				is = new FileInputStream(sourceFile);
			}
		}
		else {
			logger.warn("Access to media object when source file not defined");
		}
		return is;		
	}
	


}

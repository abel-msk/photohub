package home.abel.photohub.connector.google;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import home.abel.photohub.connector.BaseMediaObject;
import home.abel.photohub.connector.prototype.SiteConnectorInt;

public class GoogleMediaObject extends BaseMediaObject {
	final Logger logger = LoggerFactory.getLogger(GoogleMediaObject.class);

	
	protected SiteConnectorInt connector = null;
	
	public GoogleMediaObject(SiteConnectorInt connector) {
		this.connector = connector;
	}
	
	public InputStream getInputStream() throws IOException {
		InputStream is = null;
		if ( getPath() != null) {
			//ImageScaler scaller = new ImageScaler();
			//is = scaller.doScale(sourceFile, new Dimension(getWidth(),getHeight()));
			URL imagePath = new URL(getPath());
			is = imagePath.openStream();
		}
		else {
			logger.warn("Access to google media object when path file not defined");
		}
		return is;		
	}

}

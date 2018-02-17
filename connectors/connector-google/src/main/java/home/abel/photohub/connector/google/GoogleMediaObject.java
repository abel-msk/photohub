package home.abel.photohub.connector.google;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.google.gdata.client.Service;
import com.google.gdata.data.media.mediarss.MediaContent;
import com.google.gdata.util.ContentType;
import home.abel.photohub.connector.HeadersContainer;
import home.abel.photohub.connector.SiteMediaPipe;
import home.abel.photohub.connector.prototype.ExceptionObjectAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import home.abel.photohub.connector.BaseMediaObject;
import home.abel.photohub.connector.prototype.SiteConnectorInt;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.UrlResource;

import static javax.swing.text.DefaultStyledDocument.ElementSpec.ContentType;

public class GoogleMediaObject extends BaseMediaObject {
	final Logger logger = LoggerFactory.getLogger(GoogleMediaObject.class);

	
	protected GoogleSiteConnector connector = null;
	protected MediaContent media = null;
	
	public GoogleMediaObject(GoogleSiteConnector connector) {
		this.connector = connector;
	}
	public void setMedia(MediaContent mediaObj) {
		this.media = mediaObj;
	}
	
	public SiteMediaPipe getContentStream(HeadersContainer headers) throws Exception {
		if ( this.media == null) {
			throw new ExceptionObjectAccess("Media object was not loaded from site.");
		}
		return connector.loadMediaByPath(this.media.getUrl().toString(), headers);
	}
}

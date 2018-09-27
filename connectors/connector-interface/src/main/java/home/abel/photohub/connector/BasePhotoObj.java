package home.abel.photohub.connector;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import home.abel.photohub.connector.prototype.EnumMediaType;
import home.abel.photohub.connector.prototype.PhotoMediaObjectInt;
import home.abel.photohub.connector.prototype.PhotoMetadataInt;
import home.abel.photohub.connector.prototype.PhotoObjectInt;
import home.abel.photohub.connector.prototype.SiteConnectorInt;
//import org.springframework.core.io.AbstractResource;
//import org.springframework.core.io.UrlResource;

public class BasePhotoObj implements PhotoObjectInt {

	protected String id = null;
	protected SiteConnectorInt connector = null;
	protected String name = null;
	protected String descr = null;
	protected URL thumbUrl = null;
	protected URL srcUrl = null;
	protected String type = null;
	protected String mimeType = null;
	
	protected long size = 0;
	protected int width = 0;
	protected int height = 0;

	public BasePhotoObj(SiteConnectorInt conn) {
		this.connector = conn;
	}
	
	@Override
	public String getId() {
		return this.id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getMimeType() {
		return this.mimeType;
	}

	protected void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	@Override
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}


	@Override
	public String getDescr() {
		return descr;
	}
	
	public void setDescr(String descr) {
		this.descr = descr;
	}

	@Override
	public URL getThumbUrl() {
		return thumbUrl;
	}
	
	public void setThumbUrl(URL thumbUrl) {
		this.thumbUrl = thumbUrl;
	}

	@Override
	public URL getSrcUrl() {
		return srcUrl;
	}
	
	public void setSrcUrl(URL srcUrl) {
		this.srcUrl = srcUrl;
	}

	@Override
	public long getSize() {
		return size;
	}
	
	public void setSize( long size) {
		this.size = size;
	}
	
	@Override
	public int getWidth() {
		return width;
	}

	public void setWidth( int width) {
		this.width = width;
	}
	
	@Override
	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}
	
	@Override
	public SiteConnectorInt getConnector() {
		return connector;
	}

	protected void setConnector(SiteConnectorInt connector) {
		this.connector = connector;
	}

	@Override
	public PhotoMetadataInt getMeta()  throws Exception {
		return null;
	}
	@Override
	public void setMeta(PhotoMetadataInt newMetaData) throws Exception {
		
	}


	@Override
	public boolean isFolder() {
		return false;
	}

	@Override
	public List<String> listSubObjects() throws Exception{
		return null;
	}

	@Override
	public boolean hasPhotoSource() {
		return false;
	}

	@Override
	public boolean hasThumbnailSource() {
		return true;
	}

	@Override
	public PhotoMediaObjectInt getMedia(EnumMediaType type) throws IOException {
		return null;
	}

	@Override
	public PhotoMediaObjectInt getThumbnail(Dimension dim) throws IOException {
		return null;
	}

	@Override
	public PhotoMediaObjectInt update() throws Exception {
		return (PhotoMediaObjectInt)this;
	}

	@Override
	public void delete() throws Exception {
	}

	@Override
	public SiteMediaPipe getSource() throws Exception {
		return null;
	}

	public String toString() {
		return getName() + "("+ getId() + ")";
	}


	@Override
	public PhotoObjectInt rotate90(rotateEnum direction) throws Exception {
		return null;
	}

}

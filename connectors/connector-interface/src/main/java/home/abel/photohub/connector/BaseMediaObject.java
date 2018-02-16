package home.abel.photohub.connector;

import home.abel.photohub.connector.prototype.EnumMediaType;
import home.abel.photohub.connector.prototype.PhotoMediaObjectInt;
import org.springframework.core.io.AbstractResource;

import java.io.IOException;
import java.io.InputStream;

public class BaseMediaObject implements PhotoMediaObjectInt{
	protected int width = -1;
	protected int height = -1;
	protected long size = -1;
	protected String type = null;
	protected String mimeType = null;
	protected String path = null;
	protected EnumMediaType accType = EnumMediaType.ACC_NET;
	protected EnumMediaType mediaType= EnumMediaType.UNKNOWN;

	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	
//	public String getType() {
//		return type;
//	}
//	public void setType(String type) {
//		this.type = type;
//	}
	public String getMimeType() {
		return mimeType;
	}
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
	public AbstractResource getContentStream( String headers ) throws Exception {
		return null;
	}
	public AbstractResource getContentStream() throws Exception {
		return getContentStream(null);
	}


	public int getWidth() {
		return width;
	}
	public void setWidth(int width) {
		this.width = width;
	}
	public int getHeight() {
		return height;
	}
	public void setHeight(int height) {
		this.height = height;
	}
	public long getSize() {
		return size;
	}
	public void setSize(long size) {
		this.size = size;
	}
	@Override
	public EnumMediaType getType() {
		return mediaType;
	}
	@Override
	public void setType(EnumMediaType mediaType) {
		this.mediaType = mediaType;
	}

	@Override
	public EnumMediaType getAccessType() {
		return accType;
	}
	@Override
	public void setAccessType(EnumMediaType accType) {
		this.accType = accType;
	}
	
		
	
}

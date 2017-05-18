package home.abel.photohub.connector.prototype;

import java.io.IOException;
import java.io.InputStream;

public interface PhotoMediaObjectInt {

	
	public String getPath();
	public void setPath(String path);
	public EnumMediaType getType();
	public void setType(EnumMediaType type);
	public String getMimeType();
	public void setMimeType(String mimeType);
	public InputStream getInputStream() throws IOException;
	public void setInputStream(InputStream inputStream);
	
	public int getWidth();
	public void setWidth(int width);
	public int getHeight();
	public void setHeight(int height);
	public long getSize();
	public void setSize(long size);
	
	public EnumMediaType getAccessType();
	public void setAccessType(EnumMediaType accType);
}

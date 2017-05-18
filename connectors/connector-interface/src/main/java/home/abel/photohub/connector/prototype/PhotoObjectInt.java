package home.abel.photohub.connector.prototype;

import home.abel.photohub.connector.BaseMediaObject;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

public interface PhotoObjectInt {


	public String getName();
	public void setName(String name);
	
	public String getType();
	public String getDescr();
	public URL getThumbUrl();
	public URL getSrcUrl();
	public long getSize();
	public int getWidth();
	public int getHeight();

	public SiteConnectorInt getConnector();
	//public void setConnector(SiteConnectorInt conn);
	
	//public void setId(String objId) throws Exception;
	public String getId();
	
	public PhotoMetadataInt getMeta() throws Exception;
	public void setMeta(PhotoMetadataInt newMetaData) throws Exception;
	
	public boolean isFolder();
	public List<PhotoObjectInt> listSubObjects() throws Exception;
	
	public boolean hasPhotoSource();
	/**
	 * @return
	 * @throws IOException
	 */
	public PhotoMediaObjectInt  getMedia(EnumMediaType type) throws IOException;
	
	public boolean hasThumbnailSource();
	
	/**
	 * @param dim
	 * @return
	 * @throws IOException
	 */
	public PhotoMediaObjectInt  getThumbnail(Dimension dim) throws IOException;
	
	//public void setPhotoSource(InputStream is) throws Exception;
	
	//public void save() throws Exception;
	
	public PhotoMediaObjectInt update() throws Exception;
	public void delete() throws Exception;
	
}

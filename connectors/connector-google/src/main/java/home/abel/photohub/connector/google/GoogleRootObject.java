package home.abel.photohub.connector.google;

import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.data.photos.GphotoEntry;
import com.google.gdata.data.photos.UserFeed;
import com.google.gdata.util.ServiceException;
import home.abel.photohub.connector.BasePhotoObj;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class GoogleRootObject extends BasePhotoObj {
    final Logger logger = LoggerFactory.getLogger(GoogleAlbumObject.class);
   
	protected GoogleSiteConnector connector = null;
	protected PicasawebService service = null;

    
	public GoogleRootObject(GoogleSiteConnector connector) throws Exception {
		super(connector);
		this.connector = connector;
		this.service = connector.getPicasaService();
		this.id = "0";
		this.name = "root";
		this.descr = "Picasa web srvice";
	}

	
	/**---------------------------------------------------------------------
	 * 
	 *    Getter and Setters
	 * 
	 ---------------------------------------------------------------------*/

	public String  getGoogleProfileId() throws Exception {
		return this.connector.getProfile().getId();
	}
	
	/**---------------------------------------------------------------------
	 * 
	 *    Get contained objects
	 * 
	 ---------------------------------------------------------------------*/
	@Override
	public boolean isFolder() {
		return true;
	}
	
	/**  
	 *   List google albums objects
	 * 
	 * (non-Javadoc)
	 * @see home.abel.photohub.connector.BasePhotoObj#listSubObjects()
	 */
	@Override
	public List<String> listSubObjects() throws Exception {
		List<String> resultList = new ArrayList<>();
		URL feedUrl = new URL("https://picasaweb.google.com/data/feed/api/user/"+getGoogleProfileId()+"?kind=album");
		UserFeed userFeed = null;

		try {
			userFeed = service.getFeed(feedUrl, UserFeed.class);

		} catch (ServiceException fe) {
			logger.warn("[Google.loadObject] Get entry error. "+ fe.getMessage());
			try { //   Try to reconnect
				//this.connector.doConnect(null);
				this.connector.doRefresh();
				service = this.connector.getPicasaService();
				userFeed = service.getFeed(feedUrl, UserFeed.class);
			} catch (Exception e) {
				throw new IOException("Cannot reconnect.",e);
			}
		}

		logger.trace("Got root albums feed " + userFeed.getDescription() + ", from url=" + feedUrl.toString());
        for (GphotoEntry gEntry : userFeed.getEntries()) {
        	AlbumEntry albumEntry = new AlbumEntry(gEntry);
        	
        	//logger.trace("Gogole root album "+albumEntry.getMediaGroup().getTitle().getPlainTextContent());

        	try {
				GoogleAlbumObject album = new GoogleAlbumObject(this.connector,albumEntry);
        		resultList.add(album.getId());
        	} catch(Exception e)  {
        		logger.error("Cannot load album " + albumEntry.getMediaGroup().getTitle().getPlainTextContent(), e);
        	}
        }
		return resultList;
	}
	
}

package home.abel.photohub.connector.google;

import java.awt.Dimension;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import home.abel.photohub.connector.BasePhotoObj;
import home.abel.photohub.connector.prototype.EnumMediaType;
import home.abel.photohub.connector.prototype.PhotoMediaObjectInt;
import home.abel.photohub.connector.prototype.PhotoObjectInt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.media.mediarss.MediaThumbnail;
import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.data.photos.AlbumFeed;
import com.google.gdata.data.photos.GphotoEntry;
import com.google.gdata.data.photos.PhotoEntry;
import com.google.gdata.util.ServiceException;

public class GoogleAlbumObject extends BasePhotoObj {
    final Logger logger = LoggerFactory.getLogger(GoogleAlbumObject.class);
    
	protected GoogleSiteConnector googleConnector = null;
	protected PicasawebService service = null;
	protected AlbumEntry albumObject = null;
	//protected List<GoogleThumbObject> googleThumbObjects = null;
	protected MediaThumbnail maxThumb = null;
    
	public GoogleAlbumObject(GoogleSiteConnector connector, String albumId) throws Exception {
		super(connector);
		googleConnector = connector;
		this.setId(albumId); 
		this.service = connector.getPicasaService();	
		this.albumObject = loadObject(this.getId());
		loadObjectInfo(albumObject);
	}
	
	
	public GoogleAlbumObject(GoogleSiteConnector connector, AlbumEntry album) throws Exception {
		super(connector);
		googleConnector = connector;
		this.service = connector.getPicasaService();
		this.albumObject = album;
		loadObjectInfo(album);
	}
	/**
	 * 
	 *  Load album meta data by Album id,  Loading from google 
	 * 
	 * @param albumId
	 * @return
	 * @throws Exception
	 */
	public AlbumEntry  loadObject(String albumId) throws Exception {
		URL entryUrl = new URL("https://picasaweb.google.com/data/entry/api/user/"+
				getGoogleProfileId() +
				"/albumid/"+this.getId() 
				);	
		
		AlbumEntry album = service.getEntry(entryUrl, AlbumEntry.class);
		logger.debug("Load album "+ 
				album.getMediaGroup().getTitle().getPlainTextContent() + 
				"("+album.getId() +")");
		return album;
	}
	
	/**
	 * Load album data from album entry
	 * @param album
	 */
	public void loadObjectInfo (AlbumEntry album) {	
    	// ---------------------------------------
    	//   Load albums info
    	// ---------------------------------------
		
		this.setId(album.getGphotoId());
    	this.name = album.getMediaGroup().getTitle().getPlainTextContent();
    	this.descr = album.getMediaGroup().getDescription().getPlainTextContent();
	    logger.trace("Load album info name="+this.name+", id="+this.getId());

    	// ---------------------------------------
    	//   Load albums thumbnail info
    	// ---------------------------------------
    	MediaThumbnail maxThumb = null;
    	for (  MediaThumbnail thumb : album.getMediaGroup().getThumbnails() ) {
    		if ((maxThumb != null) && (thumb.getWidth() > maxThumb.getWidth())) {
    			maxThumb = thumb;
    		}
    	}
    	
//    	for (  MediaThumbnail thumb : album.getMediaGroup().getThumbnails() ) {
//    		
//    		try {
//    			googleThumbObjects.add(new GoogleThumbObject(thumb));
//    		}
//    		catch (Exception e) {
//    			logger.warn("Incorrect url format "+thumb.getUrl(),e);
//    		}
//    	}
    	
    	//  Sort Thumbs list by size
//    	if ( googleThumbObjects != null) {
//			Collections.sort(googleThumbObjects, new Comparator<GoogleThumbObject>() {
//				@Override
//		        public int compare(GoogleThumbObject arg0, GoogleThumbObject arg1) {
//		        	return (arg1.getWeight() > arg0.getWeight()) ? 1 : -1;
//		        }
//		    });
//    	}	
	}
	
	
	/**---------------------------------------------------------------------
	 * 
	 *    Getter and Setters
	 * 
	 ---------------------------------------------------------------------*/

	public String  getGoogleProfileId() throws Exception {
		return this.googleConnector.getProfile().getId();
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

	/*
	 *   Return list of photo objects contained in this album
	 * 
	 * (non-Javadoc)
	 * @see home.abel.photohub.connector.BasePhotoObj#listSubObjects()
	 */
	@Override
	public List<PhotoObjectInt> listSubObjects() throws Exception {		
		List<PhotoObjectInt> googlePhotoObjectsList = new ArrayList<PhotoObjectInt>();
		//
		//    Can append   ?fields=media:group/media:*&imgmax=1600
		//    imgmax = 94, 110, 128, 200, 220, 288, 320, 400, 512, 576, 640, 720, 800, 912, 1024, 1152, 1280, 1440, 1600
		//    imgmax=d - for source size
		//    Most short form: &fields=entry/gphoto:id,entry/title
		//    Example:
		//    https://picasaweb.google.com/data/feed/api/user/106296620586818474851/albumid/6018620506929910241?imgmax=d&prettyprint=true&fields=entry/gphoto:id,entry/title
		
		URL albumFeedUrl = new URL("https://picasaweb.google.com/data/feed/api/user/"+ getGoogleProfileId()  + "/albumid/"+this.getId()+"?imgmax=d");
		logger.trace("Get album feed from url: " + albumFeedUrl.toString());
		AlbumFeed albumFeed = service.getFeed(albumFeedUrl, AlbumFeed.class);
		
		logger.trace("Got photos list for album: "+ this.getName() +"("+this.getId()+")" +
		", from url="+albumFeedUrl.toString());

		for(GphotoEntry entry : albumFeed.getEntries()) {	
			PhotoEntry photoEntry = new PhotoEntry(entry);
			//logger.trace("Got photo entry id="+ photoEntry.getGphotoId() + ", type="+ photoEntry.getMediaContents().get(0).getMedium());

			try {
				GooglePhotoObject photoObject= new GooglePhotoObject( this.googleConnector, photoEntry);
				if ( ! photoObject.getType().equalsIgnoreCase("unknown") ) {
					logger.trace("Got album entry id="+ photoObject.getId() + ", type="+ photoObject.getType());
					googlePhotoObjectsList.add(photoObject);
				}
			} catch ( Exception e) {
				logger.error("Cannot cast to PhotoEntry object ",e );
			}

//			if ( photoEntry.getMediaContents().get(0).getMedium().compareToIgnoreCase("image") == 0 ) {
//				//TODO: Переделать.
//				logger.trace("Got photo entry id="+ photoEntry.getGphotoId() + ", type="+ photoEntry.getMediaContents().get(0).getMedium());
//
//				try {
//					GooglePhotoObject photoObject= new GooglePhotoObject( this.googleConnector, photoEntry);
//					googlePhotoObjectsList.add(photoObject);
//				} catch ( Exception e) {
//					logger.error("Cannot cast to PhotoEntry object ",e );
//				}
//			}



		}
		return googlePhotoObjectsList;
	}
	
	/**---------------------------------------------------------------------
	 * 
	 *    Thumbnail processing
	 * 
	 ---------------------------------------------------------------------*/
	@Override
	public boolean hasThumbnailSource() {
		return this.maxThumb!= null;
	}
	
//	protected GoogleThumbObject  getThumb(Dimension dim) throws IOException {
//		GoogleThumbObject prevThumb = null;						
//		for ( GoogleThumbObject thumb: googleThumbObjects) {
//			logger.trace("Look for thumb for object "+getName()+". DIM w="+thumb.getWidth()+"/h="+thumb.getHeight()+" URL = "+thumb.getUrl());
//			if ((prevThumb != null ) && (thumb.isGE(dim.getWidth(), dim.getHeight()))) {
//				logger.trace("Retrive thumbnail w="+thumb.getWidth()+"/h="+thumb.getHeight()+" " +
//							"for request w="+dim.getWidth()+"/h="+dim.getHeight());
//				return thumb;
//			}
//		}
//		//  Stream with required size not found
//		//  get most relevant
//		if (! googleThumbObjects.isEmpty() ) {
//			return googleThumbObjects.get(0);
//		}
//		return null;	
//	}

	@Override
	public PhotoMediaObjectInt getThumbnail(Dimension dim) throws IOException {
		GoogleMediaObject mediaObject = null;

		if ( maxThumb != null) {
			mediaObject = new GoogleMediaObject(this.getConnector());

			mediaObject.setHeight(maxThumb.getHeight());
			mediaObject.setWidth(maxThumb.getWidth());
			mediaObject.setPath(maxThumb.getUrl().toString());
			mediaObject.setType(EnumMediaType.THUMB_NET);
			
			String ext  = maxThumb.getUrl().toString().substring(maxThumb.getUrl().toString().lastIndexOf('.') + 1);
			mediaObject.setMimeType("image/"+ext.toLowerCase());
			
		}
		return mediaObject;
	}
	
	@Override
	public void delete() throws Exception {
		this.albumObject.delete();
	}
}

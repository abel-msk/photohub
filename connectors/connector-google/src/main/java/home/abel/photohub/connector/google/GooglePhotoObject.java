package home.abel.photohub.connector.google;

import java.awt.Dimension;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.geo.Point;
import com.google.gdata.data.media.mediarss.MediaContent;
import com.google.gdata.data.photos.ExifTags;
import com.google.gdata.data.photos.PhotoEntry;
import com.google.gdata.data.photos.impl.ExifTag;
import com.google.gdata.util.ServiceException;
import com.google.gdata.util.ServiceForbiddenException;

import home.abel.photohub.connector.BasePhotoMetadata;
import home.abel.photohub.connector.BasePhotoObj;
import home.abel.photohub.connector.prototype.AccessException;
import home.abel.photohub.connector.prototype.ExifMetadataTags;
import home.abel.photohub.connector.prototype.EnumMediaType;
import home.abel.photohub.connector.prototype.PhotoMediaObjectInt;
import home.abel.photohub.connector.prototype.PhotoMetadataInt;
import home.abel.photohub.connector.prototype.PhotoObjectInt;
import home.abel.photohub.connector.prototype.SiteConnectorInt;

public class GooglePhotoObject extends BasePhotoObj {
    final Logger logger = LoggerFactory.getLogger(GooglePhotoObject.class);
    protected final int[] predefinedSizes = {94, 110, 128, 200, 220, 288, 320, 400, 512, 576, 640, 720, 800, 912, 1024, 1152, 1280, 1440, 1600};
    
	protected GoogleSiteConnector googleConnector = null;
	protected PicasawebService service = null;
	protected String objectId = null;
	protected String albumId = null;
	protected String photoId = null; 
	protected PhotoEntry thePhotoEntryObject = null;
	protected String contentType = null;
	protected GoogleMediaObject mediaInfo = null;
	//protected List<GoogleThumbObject> googleThumbObjects = null;
	
	GooglePhotoObject(GoogleSiteConnector connector, String combinedObjectId) throws Exception {
		super(connector);
		this.googleConnector = connector;
		this.setId(combinedObjectId);
		this.albumId= combinedObjectId.substring(1,combinedObjectId.indexOf('.'));
		this.objectId = combinedObjectId.substring(combinedObjectId.indexOf('.')+1);
		this.service = connector.getPicasaService();
		this.thePhotoEntryObject = loadObject(this.albumId, this.objectId,null);
		mediaInfo = loadImageInfo(thePhotoEntryObject);
		mediaToObjectInfo(mediaInfo);
	}
	
	GooglePhotoObject(GoogleSiteConnector connector, String albumId, String objectId ) throws Exception {
		super(connector);
		this.googleConnector = connector;
		this.albumId = albumId; 
		this.objectId = objectId;
		this.id = this.albumId + "." + this.objectId;
		this.service = connector.getPicasaService();
		this.thePhotoEntryObject = loadObject(this.albumId, this.objectId,null);
		mediaInfo = loadImageInfo(thePhotoEntryObject);
		mediaToObjectInfo(mediaInfo);
	}

	GooglePhotoObject(GoogleSiteConnector connector, PhotoEntry entry) throws Exception {
		super(connector);
		this.googleConnector = connector;		
		this.albumId = entry.getAlbumId();
		this.objectId = entry.getGphotoId();
		this.id = this.albumId + "." + this.objectId;
		this.service = connector.getPicasaService();
		this.thePhotoEntryObject = entry;
		mediaInfo = loadImageInfo(this.thePhotoEntryObject);
		mediaToObjectInfo(mediaInfo);
	}
	
	/**
	 * Load google photo entry object
	 * @param albumId
	 * @param objectId
	 * @return
	 * @throws Exception
	 */
	public PhotoEntry  loadObject(String albumId, String objectId, String imgSize) throws Exception {
		String imgmax = imgSize;
		if ( imgSize == null) {
			imgmax = "d";
		}
//		URL feedUrl = new URL("https://picasaweb.google.com/data/feed/api/user/"+
//				this.connector.getProfile().getId()+
//				"/albumid/"+albumId+
//				"/photoid/"+objectId+"/?imgmax=d"
//				);
//		PhotoFeed feed = service.getFeed(feedUrl, PhotoFeed.class);
		
		URL entryUrl = new URL("https://picasaweb.google.com/data/entry/api/user/"+
				getGoogleProfileId() +
				"/albumid/"+albumId+
				"/photoid/"+objectId+"?imgmax="+imgmax
				);	
		
		logger.trace("[Google.loadObject] Load entry from url : " + entryUrl
				+", requested size="+ (imgSize==null?"Undefined":imgSize)
				);
		
		PhotoEntry gPhoto = null;
		try {
			gPhoto = service.getEntry(entryUrl, PhotoEntry.class);
		} catch (ServiceForbiddenException fe) {
			try { //   Try to reconnect
				this.connector.doConnect(null);
				gPhoto = service.getEntry(entryUrl, PhotoEntry.class);
			} catch (Exception e) {
				throw new IOException(e);
			}
		}
		return gPhoto;
	}
		
	/**
	 * 
	 *   Загружаем даный по фотографии в GoogleMediaObject объект
	 *   
	 * @param thePhotoEntryObject
	 */
	public GoogleMediaObject loadImageInfo(PhotoEntry thePhotoEntryObject)  {
		
	    this.name = thePhotoEntryObject.getMediaGroup().getTitle().getPlainTextContent();
	    this.descr = thePhotoEntryObject.getMediaGroup().getDescription().getPlainTextContent();
		
		GoogleMediaObject mediaFile = new GoogleMediaObject(this.getConnector());
		mediaFile.setType(EnumMediaType.IMAGE_NET);
		try {
			mediaFile.setHeight(Long.valueOf(thePhotoEntryObject.getHeight()).intValue());
			mediaFile.setWidth(Long.valueOf(thePhotoEntryObject.getWidth()).intValue());
			mediaFile.setSize(thePhotoEntryObject.getSize());  
			
		} catch  (ServiceException fe) {
			logger.warn("[Google.loadImageInfo] Cannot obtain image info : " + thePhotoEntryObject.getId());
		}

		//   Get media access URL
    	for (  MediaContent content : thePhotoEntryObject.getMediaGroup().getContents()) {
    		if ( content.getMedium().equalsIgnoreCase("image")) {
    			try {
    				mediaFile.setMimeType(content.getType());
    				mediaFile.setPath(content.getUrl().toString());
    				//this.srcUrl = new URL(content.getUrl().toString());	
    			}
    			catch (Exception e) {
    				logger.warn("[Google.loadImageInfo] Cannot convert image source url : " + content.getUrl().toString() );
    			}
    			break;
    		}
    	}
	    
	    logger.trace("[Google.loadImageInfo] Load photo info:"
	    		+ " width="+mediaFile.getWidth()
	    		+ ", height="+mediaFile.getHeight()
	    		+ ", size="+mediaFile.getSize()
	    		+ ", URL="+ mediaFile.getPath()
	    		);
	    
	    return mediaFile;    	
	}
	

	/**
	 *   Сохраняет параметры основного фото файла в свойствах базового класса 	
	 *  
	 * @param mediaInfo
	 */
	public void mediaToObjectInfo(GoogleMediaObject mediaInfo ) {
		this.size = mediaInfo.getSize();
		this.width = mediaInfo.getWidth();
		this.height = mediaInfo.getHeight();
		this.contentType = mediaInfo.getMimeType();
		try {
			if (mediaInfo.getPath() != null) {
				this.srcUrl = new URL(mediaInfo.getPath());
			}
		} catch (MalformedURLException e) {
			logger.warn("[Google.mediaToObjectInfo] Cannot convert image source url : " + mediaInfo.getPath() );
		}
	}
	
	
	/**---------------------------------------------------------------------
	 * 
	 *    Getter and Setters
	 * 
	 ---------------------------------------------------------------------*/

	/**
	 * Return curently loaded google image entry object
	 * @return
	 */
	public PhotoEntry getPhotoEntry() throws Exception{
		if (thePhotoEntryObject == null) {
			thePhotoEntryObject = loadObject(this.albumId, this.objectId,null);
			loadImageInfo(thePhotoEntryObject);	
		}
		return thePhotoEntryObject;
	}
		
	public String  getGoogleProfileId() throws Exception {
		return this.googleConnector.getProfile().getId();
	}

	//public PhotoMetadataInt getMeta();
	@Override
	public boolean isFolder() {
		return false;
	}
	
	@Override
	public List<PhotoObjectInt> listSubObjects() {
		return null;
	}
	
	/*
	 * (non-Javadoc)
	 * @see home.abel.photohub.connector.BasePhotoObj#getConnector()
	 */
	@Override
	public SiteConnectorInt getConnector() {
		return (SiteConnectorInt)connector;
	};
		
	/**---------------------------------------------------------------------
	 * 
	 *    Load metadata
	 * 
	 ---------------------------------------------------------------------
	 * @throws Exception */	
	/*
	 * (non-Javadoc)
	 * @see home.abel.photohub.connector.BasePhotoObj#getMeta()
	 */
	@Override
	public PhotoMetadataInt getMeta() throws Exception {
		ExifTags  metas = getPhotoEntry().getExifTags();
		BasePhotoMetadata metaDataObject = new BasePhotoMetadata();
		
		for ( ExifTag tag: metas.getExifTags()) {
			if (tag.hasValue() && (tag.getName() != null)) {
				try {
					metaDataObject.setMetaTag(tagNameToEnum(tag.getName()),tag.getValue());
					logger.trace("[getMeta] Get meta tag name " +tag.getName()+", value="+ tag.getValue());
				} catch (Exception e) {
					logger.warn("[getMeta] Cannot get tag="+(tag.getName()!=null?tag.getName():"NULL"));
				}
			}
		}
    	
    	Point geoPoint = getPhotoEntry().getGeoLocation();
    	if (geoPoint != null ) {
    		metaDataObject.setLatitude(geoPoint.getLatitude());
    		metaDataObject.setLongitude(geoPoint.getLongitude());
    		logger.trace("Photo geoLocation - Latitude=" + geoPoint.getLatitude() + ", Longitude=" + geoPoint.getLongitude() );
    	}
		return (PhotoMetadataInt)metaDataObject;
	}

	
	/**---------------------------------------------------------------------
	 * 
	 *    Image source processing
	 * 
	 ---------------------------------------------------------------------*/	
	@Override
	public boolean hasPhotoSource() {
		try {
			if ( (getPhotoEntry() != null) && (this.srcUrl != null)) {
				return true;
			}
		} catch (Exception e) {
			return false;
		}
		return false;
	}

	
	@Override 
	public PhotoMediaObjectInt getMedia(EnumMediaType requestType) throws IOException {
		mediaInfo.setType(EnumMediaType.IMAGE_NET);
		return mediaInfo;
	}

	
	/**---------------------------------------------------------------------
	 * 
	 *    Thumbnail processing
	 * 
	 ---------------------------------------------------------------------*/
	@Override
	public boolean hasThumbnailSource() {
		return this.srcUrl != null;
	}
		
	/**
	 * Return MediaObject for thumbnail
	 */
	@Override
	public PhotoMediaObjectInt getThumbnail(Dimension dim) throws IOException {
		MediaContent mediaItem = null;
		GoogleMediaObject mediaFile = null;
		
		try {
			mediaItem = getMediaItem(dim);
		
		} catch (com.google.gdata.util.ServiceForbiddenException fe) {
			//   Try to reconnect
			try {
				this.connector.doConnect(null);
				mediaItem = getMediaItem(dim);
			} catch (Exception e) {
				throw new IOException(e);
			}
		} catch (Exception e) {
			throw new IOException(e);
		}

		if ( mediaItem != null) {
			mediaFile = new GoogleMediaObject(this.getConnector());
			mediaFile.setHeight(mediaItem.getHeight());
			mediaFile.setWidth(mediaItem.getWidth());
			mediaFile.setPath(mediaItem.getUrl());
			mediaFile.setSize(mediaItem.getFileSize());
			mediaFile.setMimeType(mediaItem.getType());
			mediaFile.setType(EnumMediaType.THUMB_NET);
		}
		return mediaFile;
	}
		
	/**---------------------------------------------------------------------
	 * 
	 *    Utils
	 * 
	 ---------------------------------------------------------------------*/
	
	/** 
	 * Load smallest PhotoEntry statisfyed requested dim 
	 * @param dim
	 * @return  contnet item with clsest size
	 * @throws Exception
	 */
	protected MediaContent getMediaItem(Dimension dim) throws Exception {		
		int scaleSize = 0;
		
		if ( getHeight() < getWidth()) {
			float aspect = (float)getWidth()/(float)getHeight();
			scaleSize =  Math.round(aspect * dim.height);
		}
		else {
			float aspect = (float)getHeight()/(float)getWidth();
			scaleSize = Math.round(aspect * dim.width);
		}

		for (int defSize: predefinedSizes) {
			if (defSize > scaleSize)  {
				scaleSize = defSize;
				break;
			}
		}
		
		PhotoEntry smallImageEntry = loadObject(this.albumId, this.objectId, Integer.toString(scaleSize));
		for (MediaContent contentItem : smallImageEntry.getMediaGroup().getContents()) {
			if ( contentItem.getMedium().equals("image")) {
				logger.trace("[Google.getMediaItem] Retrive image as thumbnail w="+contentItem.getWidth()+"/h="+contentItem.getHeight()+" " +
						"for requested w="+dim.getWidth()+"/h="+dim.getHeight());
				return contentItem;
			}
		}
		return null;
	}

	/**---------------------------------------------------------------------
	 * 
	 *    Delete Photo Object
	 * 
	 ---------------------------------------------------------------------*/
	@Override
	public void delete() throws Exception {
		logger.trace("Delete photo object. albumId="+getPhotoEntry().getAlbumId()+", photoId="+getPhotoEntry().getGphotoId());
		if ( ! this.googleConnector.isCanDelete() ) throw new AccessException("Cannot delete object on readonly site.");
		getPhotoEntry().delete();
	}

	/**---------------------------------------------------------------------
	 * 
	 *    Update photo description
	 * 
	 ---------------------------------------------------------------------*/	
	@Override
	public PhotoMediaObjectInt update() throws Exception  {
		PhotoEntry googlePhotoObject = getPhotoEntry();
		
		if ( ! this.googleConnector.isCanUpdate() ) throw new AccessException("Cannot Update object on readonly site.");

		try {
			googlePhotoObject.update();
		}
		catch (Exception e) {
			logger.error("Update error. " + e.getMessage());
			throw new IOException();
		}
		
		String thisId = this.getId();
		this.albumId= thisId.substring(1,thisId.indexOf('.'));
		this.objectId = thisId.substring(thisId.indexOf('.')+1);
		this.thePhotoEntryObject = loadObject(this.albumId, this.objectId,null);
		loadImageInfo(this.thePhotoEntryObject);

		return  (PhotoMediaObjectInt)this;
	}
	
	/**---------------------------------------------------------------------
	 * 
	 *    Update metadata
	 * 
	 ---------------------------------------------------------------------*/
	@Override
	public void setMeta(PhotoMetadataInt newMetaData) throws Exception{
		
		PhotoEntry googlePhotoObject = getPhotoEntry();
		ExifTags  metas = getPhotoEntry().getExifTags();
		ExifMetadataTags[] values = ExifMetadataTags.values();
		
		Double geoLat = null;
		Double geoLon = null;
		
		for (int i = 0; i < values.length; i++) {
			ExifMetadataTags tagEnum = values[i];
			if ( newMetaData.getMetaTag(tagEnum) != null) {
					switch (tagEnum) {
					case CAMERA_MAKE:
						metas.setCameraMake(newMetaData.getCameraMake());
						break;
					case CAMERA_MODEL:
						metas.setCameraModel(newMetaData.getCameraModel());
						break;
					case DATE_CREATED:
						metas.setTime(newMetaData.getCreationTime());
						break;
					case APERTURE:
						metas.setApetureFNumber(new Float(newMetaData.getAperture()));
						break;
					case EXPOSURE_TIME:
						metas.setExposureTime(new Float(newMetaData.getExposureTime()));
						break;
					case FOCAL_LENGTH:
						metas.setFocalLength(new Float(newMetaData.getFocal()));
						break;
					case FLASH:
						short flashValue = newMetaData.getFlash().shortValue();
						byte  flashFlag = (byte)(flashValue & 0x0001);
						metas.setFlashUsed(flashFlag==0?false:true);
						break;
					case ISO_EQUIVALENT:
						metas.setIsoEquivalent(new Integer(newMetaData.getIso()));
						break;
					case GPS_LATITUDE:
						geoLat = newMetaData.getLatitude();
						break;
					case GPS_LONGITUDE:
						geoLon = newMetaData.getLongitude();
						break;
					case UNIQUE_ID:
						metas.setImageUniqueID(newMetaData.getUnicId());
						break;
					default:	
				}
			}
		} /// end for
		
		
		if ((geoLat != null) && (geoLon != null)) {
			googlePhotoObject.setGeoLocation(geoLat,geoLon);
		}
		//googlePhotoObject.update();
	}
	
	/**
	 *   Convert google exif tag names to ExifMetaTagEnum
	 * @param name
	 * @return
	 */
	protected ExifMetadataTags tagNameToEnum(String name) {
		if ( name.equals("fstop")) {
			return ExifMetadataTags.APERTURE;
		} else if( name.equals("make")) {
			return ExifMetadataTags.CAMERA_MAKE;
		} else if( name.equals("model")) {
			return ExifMetadataTags.CAMERA_MODEL;
		} else if( name.equals("flash")) {
			return ExifMetadataTags.FLASH;
		} else if( name.equals("focallength")) {
			return ExifMetadataTags.FOCAL_LENGTH;
		} else if( name.equals("iso")) {
			return ExifMetadataTags.ISO_EQUIVALENT;
		} else if( name.equals("time")) {
			return ExifMetadataTags.DATE_CREATED;
		} else if( name.equals("imageUniqueID")) {
			return ExifMetadataTags.UNIQUE_ID;
		}
		return null;		
	}
}

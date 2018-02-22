package home.abel.photohub.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.querydsl.core.types.dsl.BooleanExpression;
import home.abel.photohub.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.QPageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.uuid.Generators;

import static com.querydsl.core.group.GroupBy.*;
import com.querydsl.core.Tuple;
import com.querydsl.core.group.Group;
import com.querydsl.core.group.GroupBy;
import com.querydsl.jpa.impl.JPAQuery;

import com.querydsl.core.types.dsl.BooleanExpression;



import home.abel.photohub.connector.BasePhotoMetadata;
import home.abel.photohub.connector.prototype.EnumMediaType;
import home.abel.photohub.connector.prototype.PhotoMediaObjectInt;
import home.abel.photohub.connector.prototype.PhotoMetadataInt;
import home.abel.photohub.connector.prototype.PhotoObjectInt;
import home.abel.photohub.connector.prototype.SiteConnectorInt;
import home.abel.photohub.connector.prototype.SiteStatusEnum;
import home.abel.photohub.utils.FileUtils;
import home.abel.photohub.utils.image.ImageMetadataProcessor;


/***
 * 
 * @author abel
 * 
 * Сервис управления фотографиями
 * Основные методы:
 *   Добавлене новой фотографии
 *   Добавление Фолдера
 *   Копирование фотографии/фолдера
 *   Перенос фотографии/фолдера
 *   Удаление фотографии/фолдера
 *   
 *   Примечание
 *   Для локальной фотографии путь от корневой директории записывается в колонку PATH
 *   а доступ к фотографии осуществляется по ID
 *   
 *   
 *   
 *   Node = addPhoto(thePhoto, parent, site);
 *   addPhotoObject();
 *   linkPhoto(OnSitePhoto, DbPhoto)
 *   
 *   upload(Photo thePhoto
 *   download(Photo thePhoto)
 *
 */
@Service
public class PhotoService {
	
	final Logger logger = LoggerFactory.getLogger(PhotoService.class);
			
	
	public static int DEFAULT_PAGE_SIZE = 70;
	
	
	@PersistenceContext
	private EntityManager em;
			
	@Autowired
	PlatformTransactionManager txManager;
	
	@Autowired
	private PhotoRepository photoRepo;
	
	@Autowired
	private SiteRepository siteRepo;
	
	@Autowired
	private NodeRepository nodeRepo;
	
	@Autowired
	private ConfigService conf;
	
	@Autowired 	
	private ThumbService thumbService;
	
	@Autowired 
	private SiteService siteService; 	
	
	@Autowired
	private PhotoAttrService photoAttrService;

	public  PhotoService() {

	}

	/*=============================================================================================
	 * 
	 * 
	 *    WORK WITH DB
	 *      
	 *      
	 =============================================================================================*/
	
	public Node getNodeById(String nodeId) {
		return nodeRepo.findOne(nodeId);
	}
	
	public Photo getPhotoById(String photoId) {
		return photoRepo.findOne(photoId);
	}	
	
	/**
	 *    Add new folder to DB.  If parentId not set, the folder marked as root
	 * 
	 * @param name
	 * @param descr
	 * @param parentId
	 * @param siteId
	 * @return
	 * @throws ExceptionInvalidArgument
	 * @throws ExceptionPhotoProcess 
	 */
	@Transactional
	public Node addFolder(String name, String descr, String parentId, String siteId) throws ExceptionInvalidArgument, ExceptionPhotoProcess, ExceptionFileIO{
		Site theSite = null;
		Node theParentNode = null;
		PhotoObjectInt onSiteObject = null;
			
		
		logger.debug("[addFolder]  Name="+name+", parentId="+parentId+", siteId="+siteId);
		if (parentId  !=  null ) {
			theParentNode = nodeRepo.findOne(QNode.node.id.eq(parentId));
			if (theParentNode == null) throw new ExceptionInvalidArgument("Cannot find node with id="+parentId);
		}

		theSite = siteService.getSite(siteId);
		if ( theSite == null) {
			throw new ExceptionInvalidArgument("Cannot find site with id="+parentId);
		}

//		if (siteId != null) {
//			theSite = siteRepo.findOne(QSite.site.id.eq(siteId));
//			if (theSite == null) throw new ExceptionInvalidArgument("Cannot find site with id="+parentId);
//		}
//		else {
//			throw new ExceptionInvalidArgument("Site argument cannot be null");
//		}

		Photo thePhoto = new Photo(ModelConstants.OBJ_FOLDER, name, descr, theSite);	
		Node theNode = new Node(thePhoto,theParentNode);
		
		//photoRepo.save(thePhoto);
		//nodeRepo.save(theNode);
		SiteConnectorInt connector = null;
		try {
			connector = siteService.getOrLoadConnector(theSite);
			
			//   Check for state
			
		} catch (Exception e1) {
			throw new ExceptionInvalidArgument("Invalid site " + theSite);
		}
		
		if (! connector.isCanWrite()) {
			throw new ExceptionInvalidArgument("The site with type=" + connector.getSiteType()+" is not writable.");
		}
		
		//   Create on site folder
		try {
			logger.debug("[addFolder] Create рhoto folder(uploadPhotoObject) : " + thePhoto + ", Node=" + theNode);
			onSiteObject = uploadPhotoObject((File)null,theNode);
		} catch (NullPointerException e) {
			throw new ExceptionInvalidArgument(e);
		}
		 

		logger.debug("[addFolder] Create рhoto folder : " + thePhoto + ", Node=" + theNode);
		thePhoto = photoRepo.save(thePhoto);
		
		//   Create thumbnail for folder
		try { 
			if ( onSiteObject != null) {
				logger.debug("onSiteObject="+onSiteObject.getId());
				thePhoto = convertToPhoto(onSiteObject,thePhoto,null);
				thePhoto = photoRepo.save(thePhoto);
				thumbService.uploadThumbnail(onSiteObject,thePhoto);
			}
			else {
				logger.debug("onSiteObject=null");
				thumbService.setDefaultThumb(thePhoto);
			}
		} catch (IOException e) {
			throw new ExceptionInternalError("Cannot create thumbnail.",e);
		}
		
		//theNode.setPhoto(thePhoto);
		theNode = nodeRepo.save(theNode);
		logger.debug("Create and save object type='folder', node='"+theNode+"', photo='"+thePhoto+"', site='"+thePhoto.getSiteBean()+"'");
		return theNode;
	}
	
	/**
	 *  
	 *   Add new Photo to DB.
	 *     
	 *  
	 * @param name
	 * @param descr
	 * @param parentId
	 * @param siteId
	 * @return
	 * @throws ExceptionPhotoProcess 
	 * @throws ExceptionInvalidArgument
	 */
	public Node addPhoto(String tmpFileToUpload, String name, String descr, String parentId, String siteId) throws ExceptionInvalidArgument, ExceptionPhotoProcess{
		Node theNode = null;
		File inputFile = new File(tmpFileToUpload);
		if (inputFile.exists()) { 
			theNode = addPhoto(inputFile, name, descr, parentId, siteId);
		}
		else  {
			throw new ExceptionInvalidArgument("Cannot add photo. File "+tmpFileToUpload+ " Not found.");
		}
		return theNode;
	}
	/**
	 *  
	 *   Add new Photo to DB.
	 *     
	 *  
	 * @param inputImageFile
     * @param name
	 * @param descr
	 * @param parentId
	 * @param siteId
	 * @return
	 * @throws ExceptionInvalidArgument
	 * @throws ExceptionPhotoProcess
	 */
	@Transactional
	public Node addPhoto(File inputImageFile, String name, String descr, String parentId, String siteId) throws ExceptionInvalidArgument, ExceptionPhotoProcess{
		
		logger.trace("[addPhoto] Name="+name+", parentId="+parentId+", siteId=" + siteId);
		Site theSite = null;
		Node theParentNode = null;
		//File workFile = inputImageFile;
		File workFile = null;
		PhotoMetadataInt metadata = null;
		Node theNode = null;
		Photo thePhoto = null;
		PhotoObjectInt onSiteObject = null;
		
		try {
			if ( ImageMetadataProcessor.isImgHasMeta(inputImageFile) ) {
				//
				//   Проверяем и подготавливаем метаданные, вставляем UUID
				//
				ImageMetadataProcessor imgProcessor = new ImageMetadataProcessor(inputImageFile);
	
				
				logger.trace("[addPhoto] Load image metadata for file " + inputImageFile.getPath());
				try {
					metadata = imgProcessor.getMeta();
				} catch (Exception e1) {
					throw new ExceptionPhotoProcess("Gannot retrieve image metadata",e1);
				}
				
				if (metadata.getUnicId() == null ) {
					UUID uuid = Generators.timeBasedGenerator().generate();
					String uuidStr = uuid.toString().replace('-', '\0');
					metadata.setUnicId(uuidStr);
					logger.trace("[addPhoto] Set UUID "+ uuid.toString()+" for file "+inputImageFile.getPath());
		
					try {						
						workFile = imgProcessor.setMeta(metadata);
						//workFile = imgProcessor.getImgFile();
					} catch (Exception e) {
						throw new ExceptionPhotoProcess("Cannot save UnicId metatag value.",e);
					}
				}
			}
			//
			//   Готовим родительскую директорию (если не указана) на сайте для загрузки файла
		    //
			logger.trace("[addPhoto]  Prepare parent directory." );	
			if (parentId  !=  null ) {
				theParentNode = nodeRepo.findOne(QNode.node.id.eq(parentId));
				if (theParentNode == null) throw new ExceptionInvalidArgument("Cannot find node with id="+parentId);
			}
			else {
				if (siteId == null) {
					throw new ExceptionInvalidArgument("Parent argument for photo object cannot be null.");
				}
	
	            //   Родительскую ноду не передали в параметрах.
				//   Считаем что это загрузка фоток на сайт.
				//   тогда проверяем  генерировали ли мы сегодня спец загрузочный фодер
				//   Если да то грузим в него, иначе создаем новый.		
				DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
				Date date = new Date();
				String flName = "Upload-" + dateFormat.format(date);
				
				//
				//   CREATE UPLOAD FOLDER
				//			
				logger.trace("[addPhoto]  Create folder to upload to. Name="+flName );	

				Iterable<Node> nodes=  nodeRepo.findAll(QNode.node.photo.name.equalsIgnoreCase(flName));
				if ( nodes.iterator().hasNext()) {
					theParentNode = nodes.iterator().next();
				}

				if (theParentNode == null) {
					logger.trace("[addPhoto] Upload folder not found. Create new one. Name="+flName);
			
					theParentNode = addFolder(flName,
							 "Automaticaly creaded folder for photos upload", 
							 null, siteId);
				}
			}
			logger.trace("[addPhoto]  Lookup Site. Id="+ (siteId==null?"null":siteId));
			if (siteId != null) {
				theSite = siteRepo.findOne(siteId);
				if (theSite == null) throw new ExceptionInvalidArgument("Cannot find site with id="+parentId);
	
			} else {
				throw new ExceptionInvalidArgument("Site argument cannot be null");
			}
			
			//
			//   CREATE PHOTO OBJECT AND UPLOAD
			//
			logger.trace("[addPhoto]  Create DB phot's object");
			//   Создаем объекты в базе для файла (фото объекта)
			thePhoto = new Photo(ModelConstants.OBJ_SINGLE, name, descr, theSite);
			
			//  Create Node object for this photo object
			theNode = new Node(thePhoto,theParentNode);
			//  theParentNode  has not have  path on site
			
			
			//   Заливаем исходное фото на сайт
			logger.trace("[addPhoto] Upload photo object, Name="+thePhoto+", Parent="+(theParentNode==null?"null":theParentNode.getPhoto().getName()));

			if (workFile != null )  {
				onSiteObject = uploadPhotoObject(workFile,theNode);
				//workFile.delete();
			}
			else {
				onSiteObject = uploadPhotoObject(inputImageFile,theNode);
			}
		
		//
		//   К этому моменту вайл уже отправлен на сайт.  Так что временный файл надо стереть.
		} finally {
			if (workFile != null ) workFile.delete();
		}
			
	
		//
		//   STORE IMAGE'S METADATA
		//
		thePhoto = convertToPhoto(onSiteObject,thePhoto, metadata);
		
		//  Thumbnail need to know photo ID in db for ctraete path
		thePhoto = photoRepo.save(thePhoto);
		theNode = nodeRepo.save(theNode);
		
		//
		//   SET THUMBNAIL
		//		
		try { 
			thumbService.uploadThumbnail(onSiteObject,thePhoto);
		} catch (IOException e) {
			throw new ExceptionInternalError("Cannot create thumbnail.",e);
		}

		//
		//   SAVE DB OBJECTS
		//			
		thePhoto = photoRepo.save(thePhoto);
		//theNode = nodeRepo.save(theNode);
		
		//return saveObject(thePhoto, theParentNode);
		logger.debug("[addPhoto] Create and save object type='photo', node='"+theNode+"', photo='"+thePhoto+"', site='"+thePhoto.getSiteBean()+"'");
		return theNode;
	}
	
		
	/**
	 * 		Сохраняем фото объект и создаем Node обект, привязываем к нем фото обект и тоже сохраняем.
	 *		Save photo object in db. Create node object (set it root is no parent) 
	 * 
	 * @param thePhoto
     * @param theParent
	 * @return
	 */
	public Node saveObject(Photo thePhoto, Node theParent) {
		
		if (thePhoto.getId() == null) {
			thePhoto = photoRepo.save(thePhoto);
		}
		
		Node theNode = new Node(thePhoto,theParent);			
		theNode = nodeRepo.save(theNode);
		logger.debug("Create and save node'="+theNode+"', photo='"+thePhoto+"', site='"+thePhoto.getSiteBean()+"'");
		return theNode;
	 }
	/*=============================================================================================
	 * 
	 *    IMPORT  photoObject form connector
	 *      
	 =============================================================================================*/
	/**
	 * Chack if object with this id exist, and if so return it.
	 * @param siteObjectId the object id returned from connector
	 * @return
	 */
	public Node isPhotoExist(String siteObjectId) {
		//return photoRepo.findOne(QPhoto.photo.onSiteId.eq(siteObjectId));
		JPAQuery<?> query = new JPAQuery<Void>(em);
		QNode node = QNode.node;
		QPhoto photo = QPhoto.photo;
		
		Node theNode = query.select(node).from(node)
				.where(node.photo.onSiteId.eq(siteObjectId)).fetchOne();

		return theNode;
	}
	
	/**
	 * 
	 *		Load photo object from site, create and save corresponding node in DB
	 *	    Функция вызывается при сканировании сайта. Каждй объект (image filder video) добавляется в базу и для
	 *	    каждого объекта скачивается и сохраняется  иконка.
	 * 
	 * @param onSiteObject
	 * @param parentId
	 * @param siteId
	 * @return
	 */
	@Transactional
	public Node addObjectFromSite(PhotoObjectInt onSiteObject, String parentId, String siteId) throws ExceptionInvalidArgument, ExceptionPhotoProcess {
		Site theSite = null;
		Node theParentNode = null;
		
		if (parentId  !=  null ) {
			theParentNode = nodeRepo.findOne(QNode.node.id.eq(parentId));
			if (theParentNode == null) throw new ExceptionInvalidArgument("Cannot find node with id="+parentId);
		}

		theSite = siteService.getSite(siteId);
		if (theSite == null) {
			throw new ExceptionInvalidArgument("Cannot find site with id=" + siteId);
		}

		//--------------------------------------------------------------------------
		//     Load object information metadata and media info
		//     Звгружаем  информацию по объекту плюс метаданные и медиаинфо
		//--------------------------------------------------------------------------
		Photo thePhoto = convertToPhoto(onSiteObject, null, null);
		thePhoto.setSiteBean(theSite);
		//thePhoto = photoRepo.save(thePhoto);

		//--------------------------------------------------------------------------
		//	Increase site total space amount
		//--------------------------------------------------------------------------
		//TODO:  Check for site limit.
		//
		if ( ! onSiteObject.isFolder()) {
			theSite.setSizeTotal(theSite.getSizeTotal() + onSiteObject.getSize());
			logger.trace("[addObjectFromSite] Increase site size = " + theSite.getSizeTotal());
		}

		try { 
			thePhoto = photoRepo.save(thePhoto);
			//--------------------------------------------------------------------------
			//    load thumbnail inf^ and generate name based on the photo id
			//--------------------------------------------------------------------------
			thumbService.uploadThumbnail(onSiteObject,thePhoto);
		} catch (IOException e) {
			throw new ExceptionPhotoProcess("Cannot create thumbnail.",e);
		}

		setScanDate(thePhoto);

		//--------------------------------------------------------------------------
		//    Finally  generate Nod and links all together
		//--------------------------------------------------------------------------
		thePhoto = photoRepo.save(thePhoto);
		theSite = siteRepo.save(theSite);
		Node theNode = new Node(thePhoto,theParentNode);
		theNode = nodeRepo.save(theNode);
		logger.debug("[addObjectFromSite] Add photo object, type="+
				(thePhoto.getType()==ModelConstants.OBJ_FOLDER?"folder":"photo")
				+",node='"+theNode+"', photo='"+thePhoto+"', site='"+thePhoto.getSiteBean()+"'");
		
		return theNode;
		
	}

	/**
	 *
	 *
	 *
	 * @param thePhoto
	 */
	public void setScanDate(Photo thePhoto) {
		thePhoto.setLastScanDate(new Date());
	}


//	/**
//	 *
//	 *   Extract Media info datas and add to db' Photo object.
//	 *
//	 * @param sitesPhotoObject
//	 * @param thePhoto
//	 * @return
//	 * @throws ExceptionPhotoProcess
//	 */
//	public Media convertMediaInfo(PhotoObjectInt sitesPhotoObject, Photo thePhoto) throws ExceptionPhotoProcess  {
//		logger.debug("[convertMediaInfo] Extract objects media and save to DB entity=" +thePhoto);
//		PhotoMediaObjectInt mObject = null;
//		Media dbMObject = null;
//
//		if ((thePhoto != null) && ( ! sitesPhotoObject.isFolder()) ) {
//			mObject = null;
//			try {
//
//				if ( sitesPhotoObject.getType().equalsIgnoreCase("VIDEO")) {
//					logger.trace("[convertMediaInfo] Load objects media info.");
//					mObject =  sitesPhotoObject.getMedia(EnumMediaType.VIDEO);
//
//				}
//
//
//
//
//				logger.trace("[convertMediaInfo] Load objects media info.");
//				mObject =  sitesPhotoObject.getMedia(EnumMediaType.IMAGE);
//			} catch (Exception e) {
//				throw new ExceptionPhotoProcess("ERROR:  Extract image metadata. "+e.getMessage(),e);
//			}
//
//			if (mObject == null) {
//				logger.warn("Cannot retrieve media info from object Object="+sitesPhotoObject.getId());
//			}
//			else {
//				logger.trace("[convertMediaInfo] Create local media object");
//				dbMObject = new Media();
//				dbMObject.setType(ModelConstants.MEDIA_PHOTO);
//				dbMObject.setHeight(mObject.getHeight());
//				dbMObject.setWidth(mObject.getWidth());
//				dbMObject.setSize(mObject.getSize());
//				dbMObject.setMimeType(mObject.getMimeType());
//				dbMObject.setPath(mObject.getPath());
//				if ( mObject.getType() == EnumMediaType.IMAGE_FILE ) {
//					dbMObject.setAccessType(ModelConstants.ACCESS_LOCAL);
//				} else {
//					dbMObject.setAccessType(ModelConstants.ACCESS_NET);
//				}
//			}
//		} else {
//			if (sitesPhotoObject.isFolder()) {
//				logger.warn("[convertMediaInfo] Try to extract metas from folder.");
//			}
//		}
//		return dbMObject;
//	}
	
	/**
	 * 
	 *   Reload photo object from site connector received object to photo object entity in db
	 *   If photo entity object param 'thePhoto'  is null, create new photo entity object 
	 *   
	 *   Копирует мата-информацию о фотографии из объекта полученного из конектора в объект базы.
	 *   
	 * @param thePhoto
	 * @param sitesPhotoObject
	 * @return
	 * @throws ExceptionFileIO
	 */
	public Photo convertToPhoto(PhotoObjectInt sitesPhotoObject, Photo thePhoto, PhotoMetadataInt metadata) throws ExceptionPhotoProcess  {
		logger.debug("Convert sites object to DB entity=" +thePhoto);
		//String newThumbPath = null;
		
		if ( thePhoto == null ) {
			thePhoto = new Photo();
			thePhoto.setName(sitesPhotoObject.getName());
			thePhoto.setDescr(sitesPhotoObject.getDescr());
			
			if ( sitesPhotoObject.isFolder() ) {
				thePhoto.setType(ModelConstants.OBJ_FOLDER);
			}
			else {
				thePhoto.setType(ModelConstants.OBJ_SINGLE);
			}
			//  No other type can be received from site
		}
		
		try {
			thePhoto.setOnSiteId(sitesPhotoObject.getId());
			thePhoto.setUpdateTime(new Date());
			thePhoto.setMediaType(sitesPhotoObject.getMimeType());
		}
		catch (Exception e) {
			throw new ExceptionPhotoProcess("[convertToPhoto] Canot process photo object. ON Site object ="+sitesPhotoObject+", photo="+thePhoto,e);
		}


		//--------------------------------------------------------------------------
		//
		//   For object that is media, additionally load media info and metadata
		//
		//--------------------------------------------------------------------------

		if ( ! sitesPhotoObject.isFolder() ) {
			String baseMediaObject = thePhoto.getMediaType().substring(0,thePhoto.getMediaType().indexOf("/"));
			Media dbMedia = new Media();

			//--------------------------------------------------------------------------
			//    Вытаскиваем и сохраняем информацию по media объекту
			//--------------------------------------------------------------------------
			try {
				PhotoMediaObjectInt sitesMedia = null;
				//   Main media object is video
				if (baseMediaObject.equalsIgnoreCase("video")){
					sitesMedia  = sitesPhotoObject.getMedia(EnumMediaType.VIDEO);
					dbMedia.setType(Media.MEDIA_VIDEO);
				}
				else {
					sitesMedia  = sitesPhotoObject.getMedia(EnumMediaType.IMAGE);
					dbMedia.setType(Media.MEDIA_IMAGE);
				}

				dbMedia.setAccessType(
						(sitesMedia.getAccessType()==EnumMediaType.ACC_LOACL)?Media.ACCESS_LOCAL:Media.ACCESS_NET
						);
				dbMedia.setHeight(sitesMedia.getHeight());
				dbMedia.setWidth(sitesMedia.getWidth());
				dbMedia.setSize(sitesMedia.getSize());
				dbMedia.setMimeType(sitesMedia.getMimeType());
				dbMedia.setPath(sitesMedia.getPath());
				thePhoto.addMediaObject(dbMedia);

				//   Add addition image for video asset.
				//TODO: Не факт что это надо делать.
				if (baseMediaObject.equalsIgnoreCase("video")){
					dbMedia = new Media();
					sitesMedia  = sitesPhotoObject.getMedia(EnumMediaType.IMAGE);

					dbMedia.setType(Media.MEDIA_IMAGE);
					dbMedia.setAccessType(
							(sitesMedia.getAccessType()==EnumMediaType.ACC_LOACL)?Media.ACCESS_LOCAL:Media.ACCESS_NET
					);
					dbMedia.setHeight(sitesMedia.getHeight());
					dbMedia.setWidth(sitesMedia.getWidth());
					dbMedia.setSize(sitesMedia.getSize());
					dbMedia.setMimeType(sitesMedia.getMimeType());
					dbMedia.setPath(sitesMedia.getPath());
					thePhoto.addMediaObject(dbMedia);
				}

				
			} catch (Exception e) {
				logger.warn("[convertToPhoto] Cannot get media object. Site="+sitesPhotoObject+", photo="+thePhoto,e);
			}

			//--------------------------------------------------------------------------
			//    Вытаскиваем и сохраняем Метаданные
			//--------------------------------------------------------------------------

			//  Если не получили метаданные в качестве параметра, пробуем загрузить из файла
			if (metadata == null ) {
				try {
					metadata = sitesPhotoObject.getMeta();
				} catch (Exception e) {
					logger.warn("[convertToPhoto] Cannot retrieve extif metadata. Site="+sitesPhotoObject+", photo="+thePhoto,e);
				}
			}
			
			//	Если удалось получить из параметра или загрузить копируем данные 
			if ( metadata != null) {

				thePhoto.setUnicId(metadata.getUnicId());
				thePhoto.setCamMake(metadata.getCameraMake());
				thePhoto.setCamModel(metadata.getCameraModel());
				thePhoto.setAperture(metadata.getAperture());
				thePhoto.setCreateTime(metadata.getCreationTime());
				thePhoto.setExpTime(metadata.getExposureTime());
				//thePhoto.setExpMode("");
				thePhoto.setFocalLen(metadata.getFocal());
				//thePhoto.setFocusDist("");
				//thePhoto.setDpi("");
				if ( metadata.getAltitude() != null )
				thePhoto.setGpsAlt(metadata.getAltitude());
				if ( metadata.getLongitude() != null )
					thePhoto.setGpsLon(metadata.getLongitude());
				if ( metadata.getLatitude() != null )
					thePhoto.setGpsLat(metadata.getLatitude());
				thePhoto.setIsoSpeed(metadata.getIso());
			}
		}
		
		return thePhoto;
	}

	/*=============================================================================================
	 * 
	 * 
	 *    UPLOAD photo object
	 *      
	 *      
	 =============================================================================================*/

	/**
	 * 
	 *   Upload  photo file to site and save to DB
	 * 
	 * @param fileToUpload
     * @param theNode
     * @return  return object from site
	 * @throws ExceptionPhotoProcess
	 * @throws ExceptionInvalidArgument 
	 */
	
	@Transactional
	public  PhotoObjectInt uploadPhotoObject(
			String fileToUpload,
			Node theNode
			) throws ExceptionPhotoProcess, ExceptionInvalidArgument 
	{	
		PhotoObjectInt onSiteObj = null;
		File inputImageFile = new File(fileToUpload);
		if ( inputImageFile.exists() ) {
			onSiteObj = uploadPhotoObject(inputImageFile,theNode);
		}
		else {
			throw new ExceptionInvalidArgument("The File "+fileToUpload+ " Not found.");
		}
		return onSiteObj;
	}
	
	/**
	 *  Upload  photo file to site and save to DB
	 * 
	 * @param inputImgFile  Image file
     * @param theNode the node  where photo object will linked
	 * @return
	 * @throws ExceptionPhotoProcess
	 * @throws ExceptionInvalidArgument
	 */
	@Transactional
	public  PhotoObjectInt uploadPhotoObject(
			File inputImgFile,
			Node theNode
			) throws ExceptionPhotoProcess, ExceptionInvalidArgument
	{	
		logger.trace("[uploadPhotoObject] node="+(theNode==null?"null":theNode));
		
		PhotoObjectInt sitesPhotoObject  = null;
		SiteConnectorInt connector = null;
		Photo thePhoto = theNode.getPhoto();
		Node parentNode = null;
		
		if ( theNode.getParent() != null ) {
			parentNode = nodeRepo.findOne(theNode.getParent());
			if ( parentNode == null ) throw new ExceptionInvalidArgument("The db object node="+theNode+", object="+thePhoto+" has broken parent link");
		}
		
		try {
			logger.trace("[uploadPhotoObject] Get site connector " + thePhoto.getSiteBean());
			connector = siteService.getOrLoadConnector(thePhoto.getSiteBean());
			
			if (connector.getState() != SiteStatusEnum.CONNECT) throw new RuntimeException("Trying to add object to non connected site " + thePhoto.getSiteBean());
		} catch (Exception e) {
			throw new ExceptionPhotoProcess("Cannot upload file. Nested Exception : " + e.getMessage(),e);
		}
	
		try {
			PhotoObjectInt parentPhotoObject = null;
			
			//  Если родительский объект существует и из того же сайта то используем его.
			//  Иначе посылаем null - тогда объект будет создан в корне сайта
			if ((parentNode != null ) && 
					(parentNode.getPhoto().getSiteBean().getId() == thePhoto.getSiteBean().getId())) {
				parentPhotoObject = getOnSiteObject(parentNode);
				logger.trace("[uploadPhotoObject]Get parent object=" + (parentPhotoObject==null?"null":parentPhotoObject.getName())  + " for parent node="+parentNode);
			}

			//-------------------------
			//   Create folder
			//-------------------------
			if ( thePhoto.getType() == ModelConstants.OBJ_FOLDER) {
				logger.trace("[uploadPhotoObject] Create folder on remote site. Name="+thePhoto.getName());
				sitesPhotoObject = connector.createFolder(thePhoto.getName(), parentPhotoObject);
			}
			//-------------------------
			//   Create Object
			//-------------------------
			else {
				logger.trace("[uploadPhotoObject] Upload object as plain photo. Name="+thePhoto.getName()+
						", parent object=" + (parentPhotoObject!=null?parentPhotoObject.getId():"null"));
				if ( ! inputImgFile.exists() ) 
					throw new ExceptionInvalidArgument("Input stream for upload is null, but object is nnot a folder.");
				sitesPhotoObject = connector.createObject(thePhoto.getName(), parentPhotoObject, inputImgFile);
			}
		}
		catch (Exception e) {
			throw new ExceptionPhotoProcess("Cannot create object. Nested Exception : " + e.getMessage(),e);
		}
			
		logger.trace("[uploadPhotoObject]  Object created. id="+sitesPhotoObject.getId()+", name="+sitesPhotoObject.getName());
		return sitesPhotoObject;
	}

	
	/**
	 *    Соединяемся с конектором (Если еще не соединились) вытаскиваем Фото объект в терминах коннектора
	 *    по используя информацию фото объекта из базы и возвращаем.
	 *    
	 * 	  Retrieve onSite object for the object in DB.  
	 *    Get loaded connector for dbObjects site, and get object from connector by is onSiteId (stored in db obejct).
	 *    If Node is root dont get connector and try to load object, just return null. 
	 *    
	 * @param theNode
	 * @return
	 * @throws ExceptionInternalError when db object isnot root and getOnSiteId is empty
	 * @throws Exception when cannot find connector
	 */
	public PhotoObjectInt getOnSiteObject(Node theNode) throws ExceptionInternalError, Exception {
		Photo thePhoto = theNode.getPhoto();
		if ( thePhoto.getOnSiteId() == null) 
			throw new ExceptionInternalError("Empty pathOnSite property for object " + thePhoto ); 
		SiteConnectorInt connector = siteService.getOrLoadConnector(thePhoto.getSiteBean());
		logger.trace("[getOnSiteObject] Load sites object "+thePhoto+", ID="+thePhoto.getOnSiteId());
		PhotoObjectInt onSiteObject = connector.loadObject(thePhoto.getOnSiteId());
		return onSiteObject;
	}
	
	
	/*=============================================================================================
	 * 
	 *    DELETE  photoObject and folder
	 *      
	 =============================================================================================*/
	/**
	 * Находит указанный объект и удаляет его  через deleteObject
	 * 
	 * @param strNodeId - Node ID  - удаляемого объекта 
	 * @throws ExceptionPhotoProcess
	 * @throws ExceptionDBContent 
	 */
	@Transactional
	public void deleteObject(String strNodeId, boolean forseDelete) throws ExceptionPhotoProcess, ExceptionDBContent {
		Node theNode = nodeRepo.findOne(QNode.node.id.eq(strNodeId));
		deleteObject(theNode,forseDelete,false);
	}	
	
	/**
	 * Удаляет Фото объект из базы
	 * Удаляет только ссылку  на объект (node) если на объект photo есть другие ссылки.
	 * Если объект содержит другие объекты, и forseDelete = true, то удаляет все содержащиеся 
	 *    объекты или  ссылки рекурсивно.
	 * Если forseDelete = false то пытаемся удалить ссылку или объект если у объекта 
	 *    нет вложенных объектов, в противном случае выдаем ошибку. 
	 *    
	 * @param theNode
	 * @throws ExceptionPhotoProcess
	 * @throws ExceptionDBContent
	 */
	@Transactional
	public boolean deleteObject(Node theNode, boolean forseDelete) throws ExceptionPhotoProcess, ExceptionDBContent {
		return deleteObject(theNode,forseDelete,false);
	}		
	
	/**
	 * Находит указанный объект и удаляет его  через deleteObject
	 * 
	 * @param strNodeId - Node ID  - удаляемого объекта 
	 * @param forseDelete - True удаляет папку даже если она не пустая
	 * @throws ExceptionPhotoProcess
	 * @throws ExceptionDBContent 
	 */
	@Transactional
	public boolean deleteObject(String strNodeId, boolean forseDelete, boolean isDeleteOnSite ) throws ExceptionPhotoProcess, ExceptionDBContent, ExceptionInternalError {
		Node theNode = nodeRepo.findOne(strNodeId);
		return deleteObject(theNode,forseDelete,isDeleteOnSite);
	}
	
	/**
	 * Удаляет Фото объект из базы
	 * Удаляет только ссылку  на объект (node) если на объект photo есть другие ссылки.
	 * Если объект содержит другие объекты, и forseDelete = true, то удаляет все содержащиеся 
	 *    объекты или  ссылки рекурсивно.
	 * Если forseDelete = false то пытаемся удалить ссылку или объект если у объекта 
	 *    нет вложенных объектов, в противном случае выдаем ошибку. 
	 *    
	 * @param theNode
     * @param forseDelete - Для Объектов  типа 'folder',  если  true удаляет все содержимое
	 * 						если false и есть подобъекты то генерирует Exception
	 * @param isDeleteOnSite    - Если true то объект из DB удаляется вместе с объектов на сайте.
	 * @throws ExceptionPhotoProcess
	 * @throws ExceptionDBContent 
	 */
	//@Transactional(propagation=Propagation.REQUIRED,readOnly=false, rollbackFor = Exception.class)
	@Transactional(propagation=Propagation.SUPPORTS)
	public boolean  deleteObject(Node theNode, boolean forseDelete,boolean isDeleteOnSite) throws ExceptionPhotoProcess, ExceptionDBContent, ExceptionInternalError {
		Photo thePhoto = theNode.getPhoto();
		String newThumbPath = thumbService.getThumbPath(thePhoto);
		boolean allDeleted = true;

		logger.debug("[deleteObject] Remove Photo object: " +
				"  NodeId = " + theNode.getId() +
				", PhotoId = " + thePhoto.getId() +
				", Type =" + thePhoto.getType() +
				", Name = " + thePhoto.getName() +
				", Description = " + thePhoto.getDescr() +
				", Thumb = " + newThumbPath +
				", options[forseDelete=" + forseDelete + "]");
		
		//--- look for sub nodes
		//    If this node has sub nodes delete subnodes first
		if ( thePhoto.getType() != ModelConstants.OBJ_SINGLE) {
			Iterable<Node> containedPhotos = nodeRepo.findAll(QNode.node.parent.eq(theNode.getId()));
			if (containedPhotos.iterator().hasNext()) {
				if (forseDelete) {			
					for ( Node subNode: containedPhotos) {
						allDeleted = allDeleted &&  deleteObject(subNode,forseDelete,isDeleteOnSite);
					}
				//  All subnodes deleted, so delete folder
				}
				else {
					//thePhoto.getNodes().
					if (thePhoto.getNodes().size() <= 1) {
						//   We cannot delete node (without subnodes) 
						//   if this Photo object haven't more references from other nodes
						throw new  ExceptionDBContent("Delete non empty or non single linked object.");
					}
				}
			}
		}
		
		//---   If this photo is not linked anywhere else, so delete photo object too

		logger.debug("[deleteObject] Get nodes for photo id="+thePhoto.getId());
		List<Node> allNodes =  thePhoto.getNodes();


		if (allNodes.size() <= 1) {	
			logger.debug("[deleteObject] Object "+thePhoto+", has just one reference, so we can delete it.");

			//--- IF required, delete photoObject on site
			if ( isDeleteOnSite ) {
				boolean wasRemoved = false;
				if ( thePhoto.getType() == ModelConstants.OBJ_SINGLE ) {
					wasRemoved = deleteObjectFromSite(theNode);
				}
				else {
					if (allDeleted) {
						wasRemoved = deleteObjectFromSite(theNode);
					}
				}
				//  if not all deleted on site or we can delete this photo, just hide it and exit.
				if (! wasRemoved)  {
					thePhoto.setHidden(true);
					photoRepo.save(thePhoto);
					return false;
				}
			}

			//---  Delete Thumbnail file
			try {
				FileUtils.fileDelete(newThumbPath, false);
			}
			catch (ExceptionFileIO e) {
				logger.warn("[deleteObject] Cannot delete thumbnail "+newThumbPath+ ", for photo obj " + theNode.getPhoto() + ".  File delete report:"+e.getLocalizedMessage(),e);
			}

			//--- Delete PhotoObject from DB
			String msg = "[deleteObject] Photo NodeId=" + theNode + ", PhotoID=" + thePhoto;
			theNode.setPhoto(null);
			nodeRepo.delete(theNode);
			photoRepo.delete(thePhoto);
			logger.debug("[deleteObject] Deleted " + msg);

		}
		else {
			logger.debug("[deleteObject] Photo object has anather link, so delete only the node "+theNode);
			nodeRepo.delete(theNode);
		}
		return true;
	}



	protected boolean  deleteObjectFromSite(Node theNode) throws ExceptionInternalError {
		Photo thePhoto = theNode.getPhoto();
		try {
			SiteConnectorInt connector = siteService.getOrLoadConnector(thePhoto.getSiteBean());
			try {
				if (connector.isCanDelete()) {
					PhotoObjectInt onSiteObject = getOnSiteObject(theNode);
					long objectSize = onSiteObject.getSize();
					logger.debug("[deleteObjectFromSite] Delete on site. Node="+theNode+", object="+ thePhoto.getName()+"(id="+thePhoto.getId()+")  site " + thePhoto.getSiteBean());
					onSiteObject.delete();
					Site theSite = theNode.getPhoto().getSiteBean();
					theSite.setSizeTotal(theSite.getSizeTotal() - objectSize);
					siteRepo.save(theSite);
				}
				//
				//   else throw new ExceptionInternalError("Cannot delete object " + thePhoto.getName() + ". The site read only.");
				//
				//   If site read-only  just hide object in db
				//
				else {
					return false;
				}
			} catch (ExceptionInternalError eie) {
				throw eie;
			} catch (Exception e) {
				logger.error("[deleteObjectFromSite] Cannot delete object from site.", e);
				throw new ExceptionInternalError("Cannot delete object from site.", e);
			}
		} catch (ExceptionInternalError eie) {
			throw eie;
		} catch (Exception e) {
			logger.error("[deleteObjectFromSite] Cannot get object from site. ", e);
			throw new ExceptionInternalError("Cannot get object from site.", e);
		}
		return true;
	}

	/*=============================================================================================
	 * 
	 *    LIST photos and nodes object
	 *      
	 =============================================================================================*/
	
	/**
	 * Возвращает список Photo объектов сортрованных по доятк и с учетом фильтров
	 * Объекты тива FOLDER или с признаком  HIDDEN  не возвращаются.
	 * 
	 * List photo objects with requested filter and offset/limit
	 * @param filter
	 * @param offset
	 * @param limit
	 * @return
	 */
	public List<Photo> listPhotos(PhotoListFilter filter,  long offset, int limit ) {
        JPAQuery<?> query = new JPAQuery<Void>(em);
		QPhoto photo = QPhoto.photo;


		BooleanExpression criteria = QPhoto.photo.type.eq(ModelConstants.OBJ_SINGLE)
				.and(QPhoto.photo.hidden.isFalse());

		BooleanExpression sqlFilter = filter.getCriteria();
		
		if (sqlFilter != null) {
			criteria = sqlFilter.and(criteria);
		}
		 
		
		List<Photo> photos = query.select(photo).from(photo)
				.where(criteria)
				.orderBy(photo.createTime.desc())
				.limit((long)limit).offset(offset)
				.fetch();

		return photos;
	}
	
	
	
	/**
	 * 
	 * Возвращает список объектов которые являются childs для указанной ноды
	 * 
	 * Return  full list of photo object.  Select all photos that is childs of specifyed node 
	 * or the childs of root if node not specifyed.
	 * @param theNodeId
	 * @return
	 * @throws ExceptionPhotoProcess
	 */
	@Transactional
	public Iterable<Node> listFolder (String theNodeId) throws ExceptionPhotoProcess {
		Collection<Node> theList;
		logger.debug("List folder. NodeID=" + theNodeId);

		if (theNodeId == null) {
			theList = nodeRepo.findFolders();
			theList.addAll(nodeRepo.findPhotos());
		}
		else {
			theList = nodeRepo.findFolders(theNodeId);
			theList.addAll(nodeRepo.findPhotos(theNodeId));
		}
				
		return theList;
	}	
	
	/**
	 * Return  list of photo object by page.  Select all photos that is child of pecifyed node 
	 * 		or the chils of root if node not specifyed. 
	 * @param theNodeId  - parent node id
	 * @param page
     * @return
	 * @throws ExceptionPhotoProcess
	 */
	@Transactional
	public Page<Node> listFolder (String theNodeId, Pageable page) throws ExceptionPhotoProcess {
		Page<Node> result;
		Pageable pageProperty = page;
		
		if ( pageProperty == null) {
			pageProperty = new QPageRequest(0, DEFAULT_PAGE_SIZE, QNode.node.photo.createTime.desc());
		}

		logger.debug("List folder. Page=" + page.getPageNumber() + ", for NodeID=" + theNodeId);
		
		if (theNodeId == null) {
			result = nodeRepo.findAll(QNode.node.parent.isNull(),pageProperty);
		}
		else {
			result = nodeRepo.findAll(QNode.node.parent.eq(theNodeId),pageProperty);
		}
		return result;
	}
	
	
	
} // End of class

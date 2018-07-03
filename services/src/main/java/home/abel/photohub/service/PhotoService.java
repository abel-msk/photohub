package home.abel.photohub.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.querydsl.core.types.dsl.BooleanExpression;
import home.abel.photohub.connector.prototype.*;
import home.abel.photohub.model.*;
import home.abel.photohub.utils.image.ImageData;
import home.abel.photohub.utils.image.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.QPageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.jpa.impl.JPAQuery;


import home.abel.photohub.utils.FileUtils;


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
	
//	@Autowired
//	private PhotoAttrService photoAttrService;

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
	//@Transactional(propagation=Propagation.SUPPORTS)
	public Node addFolder(String name, String descr, String parentId, String siteId) throws ExceptionInvalidArgument, ExceptionPhotoProcess, ExceptionFileIO{
		Site theSite = null;
		Node theParentNode = null;
		PhotoObjectInt onSiteObject = null;
			
		
		logger.trace("[addFolder]  Name="+name+", parentId="+parentId+", siteId="+siteId);
		if (parentId  !=  null ) {
			theParentNode = nodeRepo.findOne(QNode.node.id.eq(parentId));
			if (theParentNode == null) throw new ExceptionInvalidArgument("Cannot find node with id="+parentId);
		}

		theSite = siteService.getSite(siteId);
		if ( theSite == null) {
			throw new ExceptionInvalidArgument("Cannot find site with id="+parentId);
		}

		Photo thePhoto = new Photo(ModelConstants.OBJ_FOLDER, name, descr, theSite);	
		Node theNode = new Node(thePhoto,theParentNode);

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
			onSiteObject = uploadPhotoObject((ImageData)null,theNode);
			thePhoto = convertToPhoto(onSiteObject,thePhoto,null);
		} catch (NullPointerException e) {
			throw new ExceptionInvalidArgument(e);
		}

		thePhoto = photoRepo.save(thePhoto);
		theNode = nodeRepo.save(theNode);

		thumbService.setDefaultThumb(thePhoto);
		logger.debug("[addFolder] Create and save object type='folder', node="+theNode+", photo="+thePhoto+", on site ID="+onSiteObject.getId());
		return theNode;
	}
	
//	/**
//	 *
//	 *   Add new Photo to DB.
//	 *
//	 *
//	 * @param name
//	 * @param descr
//	 * @param parentId
//	 * @param siteId
//	 * @return
//	 * @throws ExceptionPhotoProcess
//	 * @throws ExceptionInvalidArgument
//	 */
//	public Node addPhoto(String tmpFileToUpload, String name, String descr, String parentId, String siteId) throws ExceptionInvalidArgument, ExceptionPhotoProcess{
//		Node theNode = null;
//		File inputFile = new File(tmpFileToUpload);
//		if (inputFile.exists()) {
//			theNode = addPhoto(inputFile, name, descr, parentId, siteId);
//		}
//		else  {
//			throw new ExceptionInvalidArgument("Cannot add photo. File "+tmpFileToUpload+ " Not found.");
//		}
//		return theNode;
//	}
	/**
	 *  
	 *      Add new Photo to DB and site
	 *      Добавляем  файл с картинкой ( возможно видео )  на выбранный сайт
	 *      и записываем информациюю и метаданные в базу
	 *  
	 * @param inputImageFile input image file
     * @param name           photo name os file name
	 * @param descr          any addition comments
	 * @param parentId       link to parent object  like folder or any other container
	 * @param siteId         Site where object should be stored
	 * @return
	 * @throws ExceptionInvalidArgument
	 * @throws ExceptionPhotoProcess
	 */
	@Transactional
	public Node addPhoto(File inputImageFile, String name, String descr, String parentId, String siteId) throws ExceptionInvalidArgument, ExceptionPhotoProcess{
		
		logger.info("[addPhoto] Name="+name+", parentId="+parentId+", siteId=" + siteId);


		if ( ! ImageData.isValidImage(inputImageFile)) {
			logger.warn("Unsupported image  file type " +inputImageFile.getName() );
			return null;
		}


		Site theSite = null;
		Node theParentNode = null;
		//File workFile = inputImageFile;
		//File workFile = null;
		Metadata metadata = null;

		Node theNode = null;
		Photo thePhoto = null;
		PhotoObjectInt onSiteObject = null;
		ImageData image = null;
		
		try {

			image = new ImageData(new FileInputStream(inputImageFile));
			metadata = image.getMetadata();
			if (metadata.getUnicId() == null) {
				metadata.setUnicId(Metadata.generateUUID());
				image.setMetadata(metadata);
			}


			//
			//   Готовим родительскую директорию (если не указана) на сайте для загрузки файла
		    //
			logger.info("[addPhoto]  Prepare parent directory." );
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
							 "Automatically created folder for photos upload",
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
			logger.trace("[addPhoto]  Create DB photo object");
			//   Создаем объекты в базе для файла (фото объекта)
			thePhoto = new Photo(ModelConstants.OBJ_SINGLE, name, descr, theSite);
			
			//  Create Node object for this photo object
			theNode = new Node(thePhoto,theParentNode);
			//  theParentNode  has not have  path on site
			
			
			//   Заливаем исходное фото на сайт
			logger.trace("[addPhoto] Upload photo object, Name="+thePhoto+", Parent="+(theParentNode==null?"null":theParentNode.getPhoto().getName()));
			onSiteObject = uploadPhotoObject(image,theNode);

		
		//
		//   К этому моменту файл уже отправлен на сайт.  Так что временный файл надо удалить.
		} catch ( IOException e ) {
			throw new ExceptionInvalidArgument("Cannot read file : " + inputImageFile.getAbsolutePath(),e);
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
	 * 	    Save photo object in db. Create node object (set it root is no parent)
	 * 		Сохраняем фото объект и создаем Node обект, привязываем к нем фото обект и тоже сохраняем.
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
		logger.debug("[saveObject] Create and save node'="+theNode+"', photo='"+thePhoto+"', site='"+thePhoto.getSiteBean()+"'");
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
	 *		Load (image fоlder video) object from site, create and save corresponding node in DB
	 *		and download thumbnail
	 *	    Функция вызывается при сканировании сайта. Каждй объект (image fоlder video) добавляется в базу
	 *	    и для каждого объекта скачивается  иконка.
	 * 
	 * @param onSiteObject
	 * @param parentId
	 * @param siteId
	 * @return
	 */
	@Transactional
	public Node addObjectFromSite(PhotoObjectInt onSiteObject, String parentId, String siteId) throws ExceptionInvalidArgument, ExceptionPhotoProcess, ExceptionUnknownFormat {
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
		//    Finally  cross link all  node, photos and site objects
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
	 *   Сохраняем текущую дату как дату последнего сканирования сайта  для заданного объекта
	 *
	 * @param thePhoto
	 */
	public void setScanDate(Photo thePhoto) {
		thePhoto.setLastScanDate(new Date());
	}


	
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
		logger.info("[convertToPhoto] Convert sites object to DB entity=" +sitesPhotoObject);
		//String newThumbPath = null;
		
		if ( thePhoto == null ) {
			thePhoto = new Photo();
			thePhoto.setName(sitesPhotoObject.getName());
			thePhoto.setDescr(sitesPhotoObject.getDescr());

			if ( sitesPhotoObject.isFolder() ) {
				thePhoto.setType(ModelConstants.OBJ_FOLDER);
				thePhoto.setAllMediaSize(0);
			}
			else {
				thePhoto.setType(ModelConstants.OBJ_SINGLE);
				thePhoto.setAllMediaSize(sitesPhotoObject.getSize());
				logger.trace("[convertToPhoto] Get object size =" + sitesPhotoObject.getSize());

			}
			//  No other type can be received from site
		}
		
		try {
			thePhoto.setOnSiteId(sitesPhotoObject.getId());

			//TODO:  Delte from here
			thePhoto.setUpdateTime(new Date());
			thePhoto.setMediaType(sitesPhotoObject.getMimeType());
			logger.trace("[convertToPhoto] MimeType = "+sitesPhotoObject.getMimeType());
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
			//TODO:  Check for unknown mimeType
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

				logger.debug("[convertToPhoto] Convert media. " + sitesPhotoObject
						+ ", type="+dbMedia.getType()
						+ ", acc_type="+dbMedia.getAccessType()
						+ ", mimetype="+dbMedia.getMimeType()
						+ ", width="+dbMedia.getWidth()
						+ ", height="+dbMedia.getHeight()
						+ ", size="+dbMedia.getSize()
						+ ", path="+dbMedia.getPath()
				);


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
				thePhoto.setCreateTime(metadata.getDateCreated());
				thePhoto.setUpdateTime(metadata.getDateUpdate());
				if ( thePhoto.getUpdateTime() == null ) {
					thePhoto.setUpdateTime(new Date());
				}
				thePhoto.setExpTime(metadata.getExposureTime());
				//thePhoto.setExpMode("");
				thePhoto.setFocalLen(metadata.getFocalLength());
				//thePhoto.setFocusDist("");
				//thePhoto.setDpi("");
				if ( metadata.getAltitude() != 0 )
				thePhoto.setGpsAlt(metadata.getAltitude());
				if ( metadata.getLongitude() != 0 )
					thePhoto.setGpsLon(metadata.getLongitude());
				if ( metadata.getLatitude() != 0 )
					thePhoto.setGpsLat(metadata.getLatitude());
				thePhoto.setIso(metadata.getIso());
			}
			else {
				thePhoto.setUpdateTime(new Date());
			}
		}
		logger.debug("[convertToPhoto] Convert sites object to DB. On site=" + sitesPhotoObject + ", DB entry=" +thePhoto);

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
	 *   Create or Upload  image to site
	 *   Создает(загружает)  объект на удаленном сайте.
 	 *
	 * @param inputImage  input image
     * @param theNode the node  where photo object will be linked as child
	 * @return
	 * @throws ExceptionPhotoProcess
	 * @throws ExceptionInvalidArgument
	 */
	@Transactional
	public  PhotoObjectInt uploadPhotoObject(
			ImageData inputImage,
			Node theNode
			) throws ExceptionPhotoProcess, ExceptionInvalidArgument
	{	
		PhotoObjectInt parentPhotoObject = null;
		PhotoObjectInt sitesPhotoObject  = null;
		SiteConnectorInt connector = null;
		Photo thePhoto = theNode.getPhoto();
		Node parentNode = null;
		
		if ( theNode.getParent() != null ) {
			parentNode = nodeRepo.findOne(theNode.getParent());
			if ( parentNode == null ) throw new ExceptionInvalidArgument("The db object node="+theNode+", object="+thePhoto+" has broken parent link");
		}
		
		try {
			connector = siteService.getOrLoadConnector(thePhoto.getSiteBean());
			if (connector.getState() != SiteStatusEnum.CONNECT) throw new RuntimeException("Trying to add object to non connected site " + thePhoto.getSiteBean());
		} catch (Exception e) {
			throw new ExceptionPhotoProcess("Cannot upload file. Nested Exception : " + e.getMessage(),e);
		}

		//
		//   Check is connector writable
		//
		if ( ! connector.isCanWrite()) {
			throw new ExceptionInvalidArgument("Non writable site "+connector.getSiteType());
		}

		try {
			//  Если родительский объект существует и из того же сайта то используем его.
			//  Иначе посылаем null - тогда объект будет создан в корне сайта
			if ((parentNode != null) &&
					(parentNode.getPhoto().getSiteBean().getId().equals(thePhoto.getSiteBean().getId()))) {
				parentPhotoObject = getOnSiteObject(parentNode);
			}

			//-------------------------
			//   Create folder
			//-------------------------
			if (thePhoto.getType() == ModelConstants.OBJ_FOLDER) {
				sitesPhotoObject = connector.createFolder(thePhoto.getName(), parentPhotoObject);
			}
			//-------------------------
			//   Create Object
			//-------------------------
			else {
				if (inputImage == null)
					throw new ExceptionInvalidArgument("Input stream for upload is null, but object is not a folder.");
				sitesPhotoObject = connector.createObject(thePhoto.getName(), parentPhotoObject, inputImage.saveJPEG());
			}
		} catch (Exception e) {
			throw new ExceptionPhotoProcess("Cannot create object. Nested Exception : " + e.getMessage(), e);
		}

		logger.debug("[uploadPhotoObject] Remote create " + (sitesPhotoObject.isFolder() ? "Folder" : "Object") + " = " + sitesPhotoObject +
				", with parent=" + (parentPhotoObject == null ? "null" : parentPhotoObject)
		);

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
		return getOnSiteObject(theNode.getPhoto());
	}


	public PhotoObjectInt getOnSiteObject(Photo thePhoto) throws ExceptionInternalError, Exception {
		if ( thePhoto.getOnSiteId() == null)
			throw new ExceptionInternalError("Empty pathOnSite property for object " + thePhoto );
		SiteConnectorInt connector = siteService.getOrLoadConnector(thePhoto.getSiteBean());
		//logger.trace("[getOnSiteObject] Load sites object "+thePhoto+", ID="+thePhoto.getOnSiteId());
		PhotoObjectInt onSiteObject = connector.loadObject(thePhoto.getOnSiteId());
		return onSiteObject;
	}

	/*=============================================================================================
	 *
	 *    PROCESS MEDIA
	 *
	 =============================================================================================*/

	/**
	 *    Return media with specified type for object thePhoto.
	 *    Types are:
	 *       Media.MEDIA_THUMB=11
	 *       Media.MEDIA_IMAGE=12
	 *       Media.MEDIA_VIDEO=13
	 *
	 * @param thePhoto  the photo object
	 * @param type      search media type
	 * @return
	 */
	public Media getMediaByType(Photo thePhoto, int type) {

		Media mediaObject = null;
		for(Media media: thePhoto.getMediaObjects()) {
			if (media.getType() == type) {
				mediaObject = media;
				break;
			}
		}
		return mediaObject;
	}

	/**
	 *     Return media object with type as whole object type.
	 *     if here is more media with base type, return local one.
	 *
	 * @param thePhoto the photo object
	 * @return
	 */
	public Media getBaseMedia(Photo thePhoto) {
		int mediaType = 0;
		Media result = null;

		if (thePhoto.getMediaType().startsWith("video")) {
			mediaType = Media.MEDIA_VIDEO;
		}
		else if (thePhoto.getMediaType().startsWith("image")) {
			mediaType = Media.MEDIA_IMAGE;
		}
		List<Media> foundMedia = new ArrayList<>();
		Media mediaObject = null;

		for(Media media: thePhoto.getMediaObjects()) {
			if (media.getType() == mediaType) {
				foundMedia.add(media);
			}
		}

		if ( foundMedia.size() == 1 ) {
			result =  foundMedia.get(0);
		}
		else  if (foundMedia.size() > 1) {
			for (Media media: foundMedia) {
				if (media.getAccessType() == Media.ACCESS_LOCAL)
					result =  media;
			}
			if ( result == null) {
				result =  foundMedia.get(0);
			}
		} else {
			throw new ExceptionInternalError("[getMediaByType] Cannot find base media type = "+thePhoto.getMediaType()
					+", for object = "+thePhoto );
		}

		logger.debug("[getBaseMedia] Found base media "+ (result==null?"NULL":result));
		return result;
	}


	/*=============================================================================================
	 *
	 *    TRANSFORM photo object
	 *
	 =============================================================================================*/
	public Photo rotate90(Photo thePhoto, PhotoObjectInt.rotateEnum rotateDirection) throws ExceptionInternalError, Exception{
		PhotoObjectInt onSiteObj = getOnSiteObject(thePhoto);

		// TODO: UPDATE LOCALY
		if ( ! onSiteObj.getConnector().isCanUpdate() ) {
			logger.debug("[rotate90]  Item cannot be modified. Site "+thePhoto.getSiteBean() +" - Readonly.");
			return null;
		}

		PhotoObjectInt newObject = onSiteObj.rotate90(rotateDirection);

		//
		//   Remove media object from DB
		//
		//TODO:  Удаляем только базовый медиа  и иконку
		List<Media> mList = new ArrayList<>();
		for (Media mItem : thePhoto.getMediaObjects() ) {
			mList.add(mItem);
		}
		for (Media mItem : mList ) {
			thePhoto.removeMediaObject(mItem);
		}
		logger.trace("[rotate90]  Clean media objects. Load info for newly created media.");

		//
		//   Update media and metadata
		//

		//thePhoto = photoRepo.save(thePhoto);
		thePhoto = convertToPhoto(newObject,thePhoto, null );

		//
		//   Create thumbnail
		//

		String thumbPath = thumbService.getThumbPath(thePhoto.getId());
		//---  Delete old Thumbnail file
		try {
			FileUtils.fileDelete(thumbPath, false);
		}
		catch (ExceptionFileIO e) {
			logger.warn("[rotate90] Cannot delete thumbnail "+thumbPath+ ", for photo obj " + thePhoto + ".  File delete report:"+e.getLocalizedMessage(),e);
		}
		//---  Load New Thumbnail file
		thumbService.uploadThumbnail(newObject,thePhoto);

		photoRepo.save(thePhoto);
		logger.debug("[rotate90] Rotate object "+ thePhoto + ", Direction " + (rotateDirection==PhotoObjectInt.rotateEnum.CLOCKWISE?"CLOCKWISE":"COUNTER_CLOCKWISE"));

		return thePhoto;
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
		String newThumbPath = thumbService.getThumbPath(thePhoto.getId());
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

		logger.info("[deleteObject] Get nodes for photo id="+thePhoto.getId());
		List<Node> allNodes =  thePhoto.getNodes();


		if (allNodes.size() <= 1) {	
			logger.trace("[deleteObject] Object "+thePhoto+", has just one reference, so we can delete it.");

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
					logger.trace("[deleteObject] Object '" + theNode.getPhoto() + "' was not removed from site.  So mark object hidden. ");
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


	/**
	 *
	 *
	 *   Delete object from remote site
	 *
	 * @param theNode
	 * @return
	 * @throws ExceptionInternalError
	 */
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
					logger.debug("[deleteObjectFromSite] Cannot delete "+ theNode.getPhoto()+". Site: "+theNode.getPhoto().getSiteBean()+" readonly.");
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
	 * Возвращает список Photo объектов сортрованных по дате и с учетом фильтров
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
		logger.info("[listFolder] List folder. NodeID=" + theNodeId);

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


	/**
	 *
	 *    Get objects with scan date older then date send as parameter
	 *
	 * @param fromDate
	 * @return
	 */
	public Iterable<Node> listdeletedObjects(Date fromDate,Site theSite) {

		//return photoRepo.findOne(QPhoto.photo.onSiteId.eq(siteObjectId));
		JPAQuery<?> query = new JPAQuery<Void>(em);
		QNode node = QNode.node;
		QPhoto photo = QPhoto.photo;

		Iterable<Node> nodes = query.select(node).from(node)
				.where(node.photo.onSiteId.eq(theSite.getId()).and(node.photo.lastScanDate.lt(fromDate)))
				.orderBy(node.photo.type.asc())
				.fetch();
		return nodes;
	}

	
} // End of class

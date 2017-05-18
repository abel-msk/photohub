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
import home.abel.photohub.model.Media;
import home.abel.photohub.model.ModelConstants;
import home.abel.photohub.model.Node;
import home.abel.photohub.model.Photo;
import home.abel.photohub.model.QNode;
import home.abel.photohub.model.QPhoto;
import home.abel.photohub.model.QSite;
import home.abel.photohub.model.QTaskRecord;
import home.abel.photohub.model.Site;
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
	private home.abel.photohub.model.PhotoRepository photoRepo;
	
	@Autowired
	private home.abel.photohub.model.SiteRepository siteRepo;
	
	@Autowired
	private home.abel.photohub.model.NodeRepository nodeRepo;
	
	@Autowired
	private home.abel.photohub.service.ConfigService conf;
	
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
		
		if (siteId != null) {
			theSite = siteRepo.findOne(QSite.site.id.eq(siteId));
			if (theSite == null) throw new ExceptionInvalidArgument("Cannot find site with id="+parentId);
		} 
		else {
			throw new ExceptionInvalidArgument("Site argument cannot be null");
		}

		Photo thePhoto = new Photo(ModelConstants.PHOTO_FOLDER, name, descr, theSite);	
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
			logger.trace("[addPhoto]  Lookup Site. Id="+ siteId==null?"null":siteId );		
			if (siteId != null) {
				theSite = siteRepo.findOne(QSite.site.id.eq(siteId));
				if (theSite == null) throw new ExceptionInvalidArgument("Cannot find site with id="+parentId);
	
			} else {
				throw new ExceptionInvalidArgument("Site argument cannot be null");
			}
			
			//
			//   CREATE PHOTO OBJECT AND UPLOAD
			//
			logger.trace("[addPhoto]  Create DB phot's object");
			//   Создаем объекты в базе для файла (фото объекта)
			thePhoto = new Photo(ModelConstants.PHOTO_SINGLE, name, descr, theSite);
			
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
	 * 
	 * @param onSiteObject
	 * @param parentId
	 * @param siteId
	 * @return
	 */
	public Node addObjectFromSite(PhotoObjectInt onSiteObject, String parentId, String siteId) throws ExceptionInvalidArgument, ExceptionPhotoProcess {
		Site theSite = null;
		Node theParentNode = null;
		
		if (parentId  !=  null ) {
			theParentNode = nodeRepo.findOne(QNode.node.id.eq(parentId));
			if (theParentNode == null) throw new ExceptionInvalidArgument("Cannot find node with id="+parentId);
		}
		
		if (siteId != null) {
			theSite = siteRepo.findOne(QSite.site.id.eq(siteId));
			if (theSite == null) throw new ExceptionInvalidArgument("Cannot find site with id="+siteId);

		} else {
			throw new ExceptionInvalidArgument("Site argument cannot be null");
		}
				
		Photo thePhoto = convertToPhoto(onSiteObject, null, null);
		thePhoto.setSiteBean(theSite);
		//thePhoto = photoRepo.save(thePhoto);
		
		try { 
			thePhoto = photoRepo.save(thePhoto);
			thumbService.uploadThumbnail(onSiteObject,thePhoto);
		} catch (IOException e) {
			throw new ExceptionPhotoProcess("Cannot create thumbnail.",e);
		}
		
		thePhoto = photoRepo.save(thePhoto);
		Node theNode = new Node(thePhoto,theParentNode);
		theNode = nodeRepo.save(theNode);
		logger.debug("Add object from site, type="+
				(thePhoto.getType()==ModelConstants.PHOTO_FOLDER?"folder":"photo")
				+", create and save node='"+theNode+"', photo='"+thePhoto+"', site='"+thePhoto.getSiteBean()+"'");
		
		return theNode;
		
	}
	/**
	 * 
	 *   Extract Media info datas and add to db' Photo object.
	 * 
	 * @param sitesPhotoObject
	 * @param thePhoto
	 * @return
	 * @throws ExceptionPhotoProcess
	 */
	public Media convertMediaInfo(PhotoObjectInt sitesPhotoObject, Photo thePhoto) throws ExceptionPhotoProcess  {
		logger.debug("[convertMediaInfo] Extract objects media and save to DB entity=" +thePhoto);
		PhotoMediaObjectInt mObject = null;
		Media dbMObject = null;
		
		if ((thePhoto != null) && ( ! sitesPhotoObject.isFolder()) ) {
			mObject = null;
			try {
				logger.trace("[convertMediaInfo] Load objects media info.");
				mObject =  sitesPhotoObject.getMedia(EnumMediaType.IMAGE);
			} catch (Exception e) {
				throw new ExceptionPhotoProcess("ERROR:  Extract image metadata. "+e.getMessage(),e);
			}
			
			if (mObject == null) {
				logger.warn("Cannot retrieve media info from object Object="+sitesPhotoObject.getId());
			}
			else {
				logger.trace("[convertMediaInfo] Create local media object");
				dbMObject = new Media();
				dbMObject.setType(ModelConstants.MEDIA_PHOTO);
				dbMObject.setHeight(mObject.getHeight());
				dbMObject.setWidth(mObject.getWidth());
				dbMObject.setSize(mObject.getSize());
				dbMObject.setMimeType(mObject.getMimeType());
				dbMObject.setPath(mObject.getPath());
				if ( mObject.getType() == EnumMediaType.IMAGE_FILE ) {
					dbMObject.setAccessType(ModelConstants.ACCESS_LOCAL);
				} else {
					dbMObject.setAccessType(ModelConstants.ACCESS_NET);
				}	
			}
		} else {
			if (sitesPhotoObject.isFolder()) {
				logger.warn("[convertMediaInfo] Try to extract metas from folder.");
			}			
		}
		return dbMObject;
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
		logger.debug("Convert sites object to DB entity=" +thePhoto);
		//String newThumbPath = null;
		
		if ( thePhoto == null ) {
			thePhoto = new Photo();
			thePhoto.setName(sitesPhotoObject.getName());
			thePhoto.setDescr(sitesPhotoObject.getDescr());
			
			if ( sitesPhotoObject.isFolder() ) {
				thePhoto.setType(ModelConstants.PHOTO_FOLDER);
			}
			else {
				thePhoto.setType(ModelConstants.PHOTO_SINGLE);
			}
		}
		
		try {
			thePhoto.setOnSiteId(sitesPhotoObject.getId());
			thePhoto.setUpdateTime(new Date());
		}
		catch (Exception e) {
			throw new ExceptionPhotoProcess("[convertToPhoto] Canot process photo object. ON Site object ="+sitesPhotoObject+", photo="+thePhoto,e);
		}
		
		if ( ! sitesPhotoObject.isFolder() ) {
			//thePhoto.setRealUrl(sitesPhotoObject.getSrcUrl());
			
			//  
			//    Вытаскиваем и сохраняем информацию по фото объекту 
			//
			try {
				Media dbMedia = new Media();
				PhotoMediaObjectInt sitesMedia  = sitesPhotoObject.getMedia(EnumMediaType.IMAGE);

				dbMedia.setType(ModelConstants.MEDIA_PHOTO);
				dbMedia.setAccessType(
						(sitesMedia.getAccessType()==EnumMediaType.ACC_LOACL)?ModelConstants.ACCESS_LOCAL:ModelConstants.ACCESS_NET
						);
				dbMedia.setHeight(sitesMedia.getHeight());
				dbMedia.setWidth(sitesMedia.getWidth());
				dbMedia.setSize(sitesMedia.getSize());
				dbMedia.setMimeType(sitesMedia.getMimeType());
				dbMedia.setPath(sitesMedia.getPath());
				
				thePhoto.addMediaObject(dbMedia);	
				
			} catch (Exception e) {
				logger.warn("[convertToPhoto] Cannot get media object. Site="+sitesPhotoObject+", photo="+thePhoto,e);
			}
			
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
			
			if ( thePhoto.getType() == ModelConstants.PHOTO_FOLDER) {
				logger.trace("[uploadPhotoObject] Create folder on remote site. Name="+thePhoto.getName());
				//logger.trace("[uploadPhotoObject]        Parent object=" + parentPhotoObject==null?"null":parentPhotoObject.getName() );
				
				//logger.trace("[uploadPhotoObject] Create folder on remote site. Name="+thePhoto.getName()+()"");	
				sitesPhotoObject = connector.createFolder(thePhoto.getName(), parentPhotoObject);
				
			}
			else {
				logger.trace("[uploadPhotoObject] Upload object as plain photo. Name="+thePhoto.getName()+
						", parent object=" + (parentPhotoObject!=null?parentPhotoObject.getId():"null"));
				if ( ! inputImgFile.exists() ) 
					throw new ExceptionInvalidArgument("Input stream for upload is null, but object is nnot a folder.");
				sitesPhotoObject = connector.createObject(thePhoto.getName(), parentPhotoObject, inputImgFile);
			}
		} catch (Exception e) {
			throw new ExceptionPhotoProcess("Cannot create object. Nested Exception : " + e.getMessage(),e);
		}
			
		logger.trace("[uploadPhotoObject]  Object created. id="+sitesPhotoObject.getId()+", name="+sitesPhotoObject.getName());
		return sitesPhotoObject;
	}
	
	/**
	 * Проверяет является ли обект корневым
	 * @param theNode  the node object
	 * @return
	 */
	public boolean isRoot(Node theNode) {
		return theNode.getParent() == null;
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
		//if (theNode.isRoot()) return null;
		Photo thePhoto = theNode.getPhoto();
		if ( thePhoto.getOnSiteId() == null) 
			throw new ExceptionInternalError("Empty pathOnSite property for object " + thePhoto ); 
		SiteConnectorInt connector = siteService.getOrLoadConnector(thePhoto.getSiteBean());
		logger.trace("[getOnSiteObject] Load onSite object ID="+thePhoto.getOnSiteId() + ", onSiteObject = " );
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
	public void deleteObject(Node theNode, boolean forseDelete) throws ExceptionPhotoProcess, ExceptionDBContent {
		deleteObject(theNode,forseDelete,false);
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
	public void deleteObject(String strNodeId, boolean forseDelete, boolean withFile ) throws ExceptionPhotoProcess, ExceptionDBContent {
		Node theNode = nodeRepo.findOne(QNode.node.id.eq(strNodeId));
		deleteObject(theNode,forseDelete,withFile);
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
	 * @param withFile    - Если true то объект из DB удаляется вместе с файлом на диске. Иначе файл на диске остается.
	 * @throws ExceptionPhotoProcess
	 * @throws ExceptionDBContent 
	 */
	//@Transactional(propagation=Propagation.REQUIRED,readOnly=false, rollbackFor = Exception.class)
	@Transactional(propagation=Propagation.SUPPORTS)
	public void deleteObject(Node theNode, boolean forseDelete,boolean withFile) throws ExceptionPhotoProcess, ExceptionDBContent {
		Photo thePhoto = theNode.getPhoto();
		String newThumbPath = thumbService.getThumbPath(thePhoto);
	
		logger.debug("Remove Photo object: " +
				"  NodeId = " + theNode.getId() +
				", PhotoId = " + thePhoto.getId() +
				", Type =" + thePhoto.getType() +
				", Name = " + thePhoto.getName() +
				", Description = " + thePhoto.getDescr() +
				", Thumb = " + newThumbPath +
				", options[forseDelete=" + forseDelete + "]");
		
		//--- look for sub nodes
		//    If this node has sub nodes delete subnodes first
		if ( thePhoto.getType() != ModelConstants.PHOTO_SINGLE) {
			Iterable<Node> containedPhotos = nodeRepo.findAll(QNode.node.parent.eq(theNode.getId()));
			if (containedPhotos.iterator().hasNext()) {
				if (forseDelete) {			
					for ( Node subNode: containedPhotos) {		
						deleteObject(subNode,forseDelete,withFile);
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

		logger.debug("Get nodes for photo id="+thePhoto.getId());
		List<Node> allNodes =  thePhoto.getNodes();

		if (allNodes.size() <= 1) {	
			logger.debug("Object "+thePhoto+", has just one reference, so we can delete it.");	
			
			//---  Delete Thumbnail file
			try {
				FileUtils.fileDelete(newThumbPath, false);
			}
			catch (ExceptionFileIO e) {
				logger.warn("Cannot delete thumbnail "+newThumbPath+ ", for photo obj " + theNode.getPhoto() + ".  File delete report:"+e.getLocalizedMessage(),e);
			} 			
			
			//--- Delete PhotoObject from DB
			String  msg = "Photo NodeId="+ theNode +", PhotoID=" + thePhoto;	
			theNode.setPhoto(null);
			nodeRepo.delete(theNode);
			photoRepo.delete(thePhoto);	
			logger.debug("Deleted " + msg);		
		}
		else {
			logger.debug("Photo object has anather link, so delete only the node "+theNode);
			nodeRepo.delete(theNode);
		}		
	}

	/*=============================================================================================
	 * 
	 *    MOVE  photoObject to other folder 
	 *      
	 =============================================================================================*/
	

	
	/*=============================================================================================
	 * 
	 *    COPY  photoObject 
	 *      
	 =============================================================================================*/
	

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


		BooleanExpression criteria = QPhoto.photo.type.eq(ModelConstants.PHOTO_SINGLE)
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

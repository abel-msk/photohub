package home.abel.photohub.service;
import static org.assertj.core.api.Assertions.anyOf;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Future;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

import home.abel.photohub.connector.prototype.SiteCredentialInt;
import home.abel.photohub.connector.prototype.SiteStatusEnum;
import home.abel.photohub.model.Node;
import home.abel.photohub.model.NodeRepository;
import home.abel.photohub.model.Photo;
import home.abel.photohub.model.PhotoRepository;
import home.abel.photohub.model.QPhoto;
import home.abel.photohub.model.ScheduleRepository;
import home.abel.photohub.model.Site;
import home.abel.photohub.model.SiteRepository;
import home.abel.photohub.model.TaskRecord;
import home.abel.photohub.model.TaskRepository;
import home.abel.photohub.model.User;
import home.abel.photohub.model.UserRole;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Condition;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import home.abel.photohub.service.auth.UserAuthentication;
import home.abel.photohub.tasks.BaseTask;
import home.abel.photohub.tasks.TaskFactory;
import home.abel.photohub.tasks.TaskNamesEnum;
import home.abel.photohub.tasks.TaskStatusEnum;



@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes=home.abel.photohub.service.PhotoServiceTest.ServiceTestContextCfg.class)
@PropertySource("classpath:test-settings.properties")

@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,
    TransactionalTestExecutionListener.class,
    DbUnitTestExecutionListener.class})
//@TransactionConfiguration(transactionManager="transactionManager", defaultRollback=true)
@Transactional
@DatabaseSetup("classpath:db-test-data.xml")

public class PhotoServiceTest {

	final Logger logger = LoggerFactory.getLogger(PhotoServiceTest.class);
	
	@Configuration
	@PropertySource("classpath:test-settings.properties")
	@Import({home.abel.photohub.model.SpringDataConfig.class,home.abel.photohub.service.SpringConfig.class})
	@ComponentScan(basePackages={"home.abel.photohub.service","home.abel.photohub.utils"})
	public static class ServiceTestContextCfg {
		@Bean
		UserService userService() {
			return new UserService();
		}
		
	}
	
	//  AssertJ condition
	class photoObjId extends  Condition<home.abel.photohub.model.Photo> {
		final Logger logger = LoggerFactory.getLogger(photoObjId.class);

		protected String id = "";
		public photoObjId(String LookedID) {
			super("Check photo object by its on site ID.");
			this.id = LookedID;
		}
		
		@Override
		public boolean matches(home.abel.photohub.model.Photo value) {
			boolean found = value.getOnSiteId().equals(id);
			if ( found ) {
				logger.trace("[photoObjId.matches] Matched value=" + value.getOnSiteId() + ", with pattern=" + id);
			}
			return found;
			
		}
	};


	@Autowired
	TaskQueueService queue;
	
	@Autowired
	PhotoService photoService;
	
	@Autowired
	SiteRepository siteRepository;
	
	@Autowired
	SiteService siteService;	
	
	@Autowired
	SiteRepository siteRepo;	
	
	@Autowired
	PhotoAttrService photoAttrSvc; 
	
	@Autowired
	NodeRepository nodeRepo; 
	
	@Autowired
	PhotoRepository photoRepo; 
	
	@PersistenceContext
	EntityManager entityManager;
	
	@Autowired
	DataSource dataSource;
			
	@Autowired
	PlatformTransactionManager txManager;
	
	@Autowired
	UserService userService;
	
	@Autowired
	TokenService tokenService;
	
	@Autowired
	TaskQueueService taskQueueService;
	
	@Autowired
	TaskFactory taskFactory;
	
	@Autowired
	TaskRepository taskRepo;
	
	@Autowired
	ScheduleService scheduleService;
	
	@Autowired
	ScheduleRepository scheduleRepository;
	
	
	
	public final static String SITE_ROOT_PATH = "/tmp/photohub_test";
	public final static String TEST_FOLDER_NAME = "foldertest";
	public static final String RESOURCE_IMAGE_FN = "photo1.JPG";
	public static final String TMP_IMAGE_FILE = "/tmp/photo1.JPG";
	public final static String TMP_ROOT_PATH = "/tmp/photohub_test_2";
	
	//private final ClassLoader currentClassLoader = ClassLoader.getSystemClassLoader();

	@BeforeClass
	public static void beforeClass() throws Exception {
		System.out.println("***Before Class is invoked");
		
		File rootFolder = new File(SITE_ROOT_PATH);
		if (rootFolder.exists()) {
			FileUtils.deleteDirectory(rootFolder);
		}
		rootFolder.mkdirs();
		rootFolder.deleteOnExit();
		
		File folder = new File(SITE_ROOT_PATH + File.separator + TEST_FOLDER_NAME);
		folder.mkdirs();
		folder.deleteOnExit();
		
		URL resourceUrl = ClassLoader.getSystemClassLoader().getResource(RESOURCE_IMAGE_FN);
		String sampeImagePath =  resourceUrl.toURI().getPath(); 				
		File sampeImageFile = new File(sampeImagePath);
		
		//   Copy image file to site root folder
		FileUtils.copyFileToDirectory(sampeImageFile,folder);
		
		File testImageFile = new File(
				SITE_ROOT_PATH + File.separator +
				TEST_FOLDER_NAME + File.separator + 
				RESOURCE_IMAGE_FN);
		testImageFile.deleteOnExit();
		
		System.out.println("Create local site structure: ");
		System.out.println("\t" + rootFolder.getAbsolutePath());
		System.out.println("\t" + folder.getAbsolutePath());
		System.out.println("\t" + testImageFile.getAbsolutePath());	
		
		//   Copy image file to /tmp
		FileUtils.copyFile(sampeImageFile,new File(TMP_IMAGE_FILE));
		
		//	Remove appended file
		new File(TMP_IMAGE_FILE).deleteOnExit();	
		
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		System.out.println("***After Class is invoked");
		System.out.println("***Remove sites structure");
		FileUtils.deleteDirectory(new File(SITE_ROOT_PATH));
    }
	
	@Before
	public void before() throws Throwable {
//		System.out.println("____________________");
//		System.out.println("\t Before is invoked");
				
		File newFolder = new File(TMP_ROOT_PATH + File.separator + TEST_FOLDER_NAME);
		if ( ! newFolder.exists() ) {
			newFolder.mkdirs();
		}
		
		URL resourceUrl = ClassLoader.getSystemClassLoader().getResource(RESOURCE_IMAGE_FN);
		String sampeImagePath =  resourceUrl.toURI().getPath(); 				
		File sampeImageFile = new File(sampeImagePath);
		
		File imgFile = new  File(newFolder.getAbsolutePath() +  File.separator + RESOURCE_IMAGE_FN) ;
		if ( ! imgFile.exists()) {
			FileUtils.copyFileToDirectory(sampeImageFile,newFolder);
			System.out.println("Copy file " + imgFile.getAbsolutePath());
		}
	}
	
	@After
	public void after() {
//		System.out.println("\t After is invoked");
//		System.out.println("=================");

	}
	
	

	public static final String defSiteType = "Local";
	public static final String newSiteName = "TEST";
	public static final String defSiteId = "2"; 
	

	@Test
    @Transactional(propagation=Propagation.SUPPORTS)
	public void shceduleTest() throws Throwable {
		System.out.println("--- Testing add/remove Site, add/remove Schedule to site");
		
		//	Create site
		Site theSite =  siteService.createSite(newSiteName, defSiteType, TMP_ROOT_PATH, null);
		assertThat(theSite).isNotNull();
		SiteCredentialInt siteCred = siteService.connectSite(theSite,null);
		assertThat(siteCred.getState()).isEqualTo(SiteStatusEnum.CONNECT);

		//  Add schedule for site
		BaseTask sch1 = scheduleService.setShcedule( null, TaskNamesEnum.TNAME_EMPTY, true, "*/10", "*", "*", "*", "*", "*" );
		BaseTask sch2 = scheduleService.getScheduledTask((String)null, TaskNamesEnum.TNAME_EMPTY);
		assertThat(sch1).isEqualToComparingFieldByField(sch2);
		
		BaseTask userSch1 = scheduleService.setShcedule( theSite.getId(), TaskNamesEnum.TNAME_EMPTY, true, "*/10", "*", "*", "*", "*", "*" );
		BaseTask userSch2 = scheduleService.getScheduledTask(theSite.getId(), TaskNamesEnum.TNAME_EMPTY);
		assertThat(userSch1).isEqualToComparingFieldByField(userSch2);
	
		//  Add Scan task
		BaseTask theTask = taskFactory.createTask(TaskNamesEnum.TNAME_SCAN, theSite);
		taskQueueService.put(theTask);
		TaskStatusEnum curStatus = theTask.getStatus();

		assertThat(curStatus).is(anyOf(
			new Condition<TaskStatusEnum>() {
				@Override
				public boolean matches(TaskStatusEnum value) {
					return TaskStatusEnum.FIN == value;
				}
			},
			new Condition<TaskStatusEnum>() {
				@Override
				public boolean matches(TaskStatusEnum value) {
					return TaskStatusEnum.RUN == value;
				}
			}
		));

		Future<?> taskProcess = theTask.getThisTaskProcess();
		//	Wait for task complete
		taskProcess.get();
		
		//   Check for Scanned file assites attached object
		Iterable<Photo> phList= photoRepo.findAll(QPhoto.photo.siteBean.eq(theSite));
		assertThat(phList).haveExactly(1,new photoObjId(
				TMP_ROOT_PATH+File.separator+
				TEST_FOLDER_NAME +File.separator +
				RESOURCE_IMAGE_FN
				));
		
		//dumpDB();
		//entityManager.flush();
		
		System.out.println("\tWAIT 10s");
		Thread.sleep(10*1000);
		
		//   Stop scheduled tasks
		scheduleService.removeTask(sch1);
		scheduleService.removeTask(userSch1);
		assertThat(scheduleService.getScheduledTask((String)null, TaskNamesEnum.TNAME_EMPTY)).isNull();
		assertThat(scheduleService.getScheduledTask(theSite.getId(), TaskNamesEnum.TNAME_EMPTY)).isNull();
		
		//  Clear system schedule records
		List<TaskRecord> taskList = scheduleService.getLogByTask(null,TaskNamesEnum.TNAME_EMPTY);
		assertThat(taskList).isNotEmpty();
		taskRepo.delete(taskList);
		
		taskList = scheduleService.getLogByTask(theSite.getId(),TaskNamesEnum.TNAME_EMPTY);
		assertThat(taskList).isNotEmpty();
		
		//entityManager.flush();
		String theSiteId = theSite.getId();
		theSite = siteRepo.findOne(theSite.getId());
		siteService.removeSite(theSite);
		
		//  Check site was removed with photo objects
		
		taskList = scheduleService.getLogByTask(theSiteId,TaskNamesEnum.TNAME_EMPTY);
		assertThat(taskList).isNullOrEmpty();
		
		//dumpDB();
	}
	  
	public final static String L2_FOLDER_NAME = "Folder_L2";
	
	@Test
	@Transactional
	public void createSiteTest() throws Exception{
		System.out.println("--- Testing add folder and upload file to site");

		Site theSite = siteRepo.findOne(defSiteId);
		assertThat(theSite).isNotNull();
		
		//  Create folder
		Node rootNode = photoService.addFolder(L2_FOLDER_NAME, "Test", null, theSite.getId());
		assertThat(rootNode).isNotNull();
		String rootNodeId = rootNode.getId();
		assertThat(rootNodeId).isNotNull();
		
		Photo thePhoto = rootNode.getPhoto();
		assertThat(thePhoto).isNotNull();
		assertThat(thePhoto.getName()).isEqualTo(L2_FOLDER_NAME);
		String rootPhotoId = thePhoto.getId();
		assertThat(rootPhotoId).isNotNull();
		
		//  Upload photo object unde the rootNode folder
		Node  theNewNode = photoService.addPhoto(new File(TMP_IMAGE_FILE), RESOURCE_IMAGE_FN, "", rootNode.getId(), theSite.getId());
		assertThat(theNewNode.getPhoto().getUnicId()).isNotNull();
		Iterable<Photo> phList = photoRepo.findAll(QPhoto.photo.siteBean.id.eq(defSiteId));
		assertThat(phList).haveExactly(1, new photoObjId(
				SITE_ROOT_PATH+File.separator+
				L2_FOLDER_NAME+File.separator+
				RESOURCE_IMAGE_FN
				));
				
		photoService.deleteObject(rootNode,true,true);
		phList = photoRepo.findAll(QPhoto.photo.siteBean.id.eq(defSiteId));
		assertThat(phList).doNotHave(new photoObjId(
				SITE_ROOT_PATH+File.separator+
				L2_FOLDER_NAME
				));
	}
	
	@Test
	@Transactional
	public void filterSitesObject() throws Exception{
		System.out.println("--- Testing Filetring objects");
		
		Site theSite = siteRepo.findOne(defSiteId);
		assertThat(theSite).isNotNull();
		
		//PhotoListFilter filter,  long offset, int limit
		PhotoListFilter  filter = new PhotoListFilter();
		
		String minDate = "2017-03-25T21:12:40.074";
		String maxDate = "2017-03-25T21:12:43.074";
		filter.setMinDate(minDate);
		filter.setMaxDate(maxDate);
		List<Photo> phList = photoService.listPhotos(filter,0,10000);
		assertThat(phList).areExactly(1,new photoObjId(
				SITE_ROOT_PATH+File.separator+
				TEST_FOLDER_NAME+File.separator+
				RESOURCE_IMAGE_FN
				));
	}
	
	public final static String  DEF_USER_NAME = "abel-test-test";
    @Test
    @Transactional
    public void manageUserTest()  throws Exception {
    	System.out.println("--- Testing Create User");

    	User currentUser = userService.addUser(DEF_USER_NAME,"Ateroh");
    	logger.info("TOKEN: Create user  : " + currentUser.getUsername());
    	currentUser.grantRole(UserRole.USER);
    	currentUser.setExpires(System.currentTimeMillis() + TokenService.EXPIRES_DAY );

		final UserAuthentication userAuthentication = new UserAuthentication(currentUser);
		// Add the authentication to the Security context
		SecurityContextHolder.getContext().setAuthentication(userAuthentication);
    	assertThat(userAuthentication.getName()).isEqualTo(DEF_USER_NAME);

    	String token = tokenService.createTokenForUser(currentUser);		
    	//logger.info("TOKEN: Create token : " + token);

    	User tokenUser = tokenService.parseUserFromToken(token); 
    	assertThat(tokenUser.getUsername()).isEqualTo(DEF_USER_NAME);
    	
		userService.deleteUser(currentUser);

    }
	

    private void dumpDB() throws Exception {
		
		java.sql.Connection con = dataSource.getConnection();
		IDatabaseConnection connection = new DatabaseConnection(con);
		IDataSet fullDataSet = connection.createDataSet();
        FlatXmlDataSet.write(fullDataSet, new FileOutputStream("/tmp/full-dataset.xml"));
		
    }
    
	
}

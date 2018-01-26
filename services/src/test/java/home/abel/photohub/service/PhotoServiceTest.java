package home.abel.photohub.service;
import static org.assertj.core.api.Assertions.anyOf;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

import com.github.springtestdbunit.TransactionDbUnitTestExecutionListener;
import home.abel.photohub.connector.prototype.SiteCredentialInt;
import home.abel.photohub.connector.prototype.SiteStatusEnum;
import home.abel.photohub.model.*;
import home.abel.photohub.model.TaskRecordRepository;
import home.abel.photohub.tasks.*;
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
import org.springframework.security.access.method.P;
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


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes=home.abel.photohub.service.PhotoServiceTest.ServiceTestContextCfg.class)
@PropertySource("classpath:test-settings.properties")
@TestExecutionListeners(value = { DbUnitTestExecutionListener.class},
		mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@Transactional
@DatabaseSetup("classpath:db-test-data.xml")

public class PhotoServiceTest {
	final Logger logger = LoggerFactory.getLogger(PhotoServiceTest.class);
	
	@Configuration
	@PropertySource("classpath:test-settings.properties")
	@Import({home.abel.photohub.model.SpringDataConfig.class,home.abel.photohub.service.SpringConfig.class})
	@ComponentScan(basePackages={"home.abel.photohub.service","home.abel.photohub.utils"})
	public static class ServiceTestContextCfg {
//		@Bean
//		UserService userService() {
//			return new UserService();
//		}
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
    TaskRecordRepository taskRepo;


	@Autowired
	TaskParamRepository taskParamRepository;

	@Autowired
	ScheduleRepository scheduleRepository;

	@Autowired
	TaskRecordRepository taskRecordRepository;

	@Autowired ConfigService configService;
	
	public final static String SITE_ROOT_PATH = "/tmp/photohub_test";
	public final static String TEST_FOLDER_NAME = "foldertest";
	public static final String RESOURCE_IMAGE_FN = "photo1.JPG";
	public static final String TMP_IMAGE_FILE = "/tmp/photo1.JPG";
	public final static String TMP_ROOT_PATH = "/tmp/photohub_test_2";

	public final String TASK_PARAM_NAME = "PARAM1";


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


	@Before
	public void before() throws Throwable {
		System.out.println("------------------------------------------------------------------------");
		System.out.println("\t Before is invoked");
		System.out.println("------------------------------------------------------------------------");

		configService.Init();

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


	@AfterClass
	public static void afterClass() throws Exception {
		System.out.println("***After Class is invoked");
		System.out.println("***Remove sites structure");
		FileUtils.deleteDirectory(new File(SITE_ROOT_PATH));
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

		BaseTask task1 = taskFactory.createTask(TaskNamesEnum.TNAME_EMPTY.toString(),
				null,
				"*/5",
				"*",
				"*",
				"*",
				"*",
				"*"
				);

		List<TaskParam> newParam = taskFactory.getEmptyParamList(TaskNamesEnum.TNAME_EMPTY.toString());
		for (TaskParam p : newParam) {
			p.setValue(p.getName()+"_VALUE1");
		}

		task1.setParams(newParam);

		for( TaskParam p : task1.getSchedule().getParams()) {
			System.out.println("TaskParam name="+p.getName() + ",  ID="+(p.getId()==null?"null":p.getId()));
		}
		assertThat(task1.getSchedule().getParams()).have(
				new Condition<TaskParam>() {
					@Override
					public boolean matches(TaskParam inParam) {
						return inParam.getId() != null;
					}
				}
		);


		task1 = taskQueueService.put(task1);
		BaseTask queuedTask  = taskQueueService.getTaskById(task1.getId());
		assertThat(queuedTask).isNotNull();
		assertThat(queuedTask.getStatus()).isNotNull();
		assertThat(queuedTask.getSite()).isNull();
		assertThat(queuedTask).isEqualToComparingFieldByField(task1);
		task1 = queuedTask;
		System.out.println("//---------------   Create schedule Task 1 ID="+task1);





		BaseTask task2 = taskFactory.createTask(TaskNamesEnum.TNAME_EMPTY.toString(),
				theSite.getId(),
				"*/5",
				"*",
				"*",
				"*",
				"*",
				"*"
		);
		task2 = taskQueueService.put(task2);

		queuedTask  = taskQueueService.getTaskById(task2.getId());
		assertThat(queuedTask).isNotNull();
		assertThat(queuedTask.getStatus()).isNotNull();
		assertThat(queuedTask.getSite()).isNotNull();
		assertThat(queuedTask).isEqualToComparingFieldByField(task2);

		assertThat(queuedTask.getParams()).hasSize(1);

		task2=queuedTask;




		System.out.println("//---------------   Create schedule Task 2 ID="+task2);
	
		//  Add Scan task
		BaseTask scanTask = taskFactory.createTask(TaskNamesEnum.TNAME_SCAN.toString(), theSite.getId());
		taskQueueService.put(scanTask);
		TaskStatusEnum curStatus = scanTask.getStatus();

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
			},
				new Condition<TaskStatusEnum>() {
					@Override
					public boolean matches(TaskStatusEnum value) {
						return TaskStatusEnum.IDLE == value;
					}
				}
		));

		System.out.println("//---------------   Create schedule Task scanTask ID="+scanTask);
		Future<?> taskProcess = scanTask.getThisTaskProcess();
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
//		entityManager.flush();
		
		System.out.println("\tWAIT 10s");
		Thread.sleep(5*1000);
		
		//   Stop scheduled tasks
		System.out.println("//---------------   Stop Task 1 ID="+task1);
		taskQueueService.stopTask(task1,true);
		assertThat(taskQueueService.getTaskById((task1.getId()))).isNull();


		//   Update shceduled task
		System.out.println("//---------------   Update schedule Task 2 ID="+task2);



		List<TaskParam> newParamList = taskFactory.getEmptyParamList(TaskNamesEnum.TNAME_EMPTY.toString());
		for (TaskParam p : newParam) {
			p.setValue(p.getName()+"_VALUE1");
		}
		task2.setParams(newParam);


		Schedule task2Schedule = task2.getSchedule();
		task2Schedule.setMinute("10");



		//
		//    Обновить рассписание без параметров
		//

		task2 = taskQueueService.updateTaskSchedule(task2,task2Schedule,null,true);

		assertThat(task2.getId()).isNotNull();
		assertThat(taskQueueService.getTaskById((task2.getId()))).isNotNull();
		assertThat(taskQueueService.getTaskById((task2.getId())).getStatus()).is(anyOf(
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
				},
				new Condition<TaskStatusEnum>() {
					@Override
					public boolean matches(TaskStatusEnum value) {
						return TaskStatusEnum.IDLE == value;
					}
				}
		));
		assertThat(taskQueueService.getTaskById((task2.getId())).getSchedule().getMinute()).isEqualToIgnoringCase("10");
		assertThat(task2.getSchedule().getParams()).hasSize(0);

		//
		//    Обновить рассписание вместе с добавлением  параметров
		//


		task2Schedule = task2.getSchedule();
		task2Schedule.setMinute("*");
		task2Schedule.setSeconds("*/15");

		newParamList = new ArrayList<>();
		newParamList.add(new TaskParam(TASK_PARAM_NAME,"VALUE3","String"));

		task2 = taskQueueService.updateTaskSchedule(task2,task2Schedule,newParamList,true);

		assertThat(taskQueueService.getTaskById((task2.getId())).getSchedule().getMinute()).isEqualToIgnoringCase("*");
		assertThat(task2.getSchedule().getParams()).have(
				new Condition<TaskParam>() {
					@Override public boolean matches(TaskParam inParam) { return inParam.getId() != null; }
				}
		);

		System.out.println("\tWAIT 5s");
		Thread.sleep(5*1000);



		//
		//    Remove task 2
		//

		System.out.println("//---------------   Stop Task 2 ID="+task2);
		taskQueueService.stopTask(task2,true);


		System.out.println("//---------------   Print and Remove log fot Task 1 ID="+task1);
		//  Clear system schedule records
		List<TaskRecord>  taskList = taskQueueService.getTaskLog(task1.getId());
		assertThat(taskList).isNotEmpty();
		for (TaskRecord tr: taskList) {
			logger.trace("Task record for task="+task1+", record "+tr.getStatus()+", msg:"+tr.getMessage());
		}
		taskRepo.delete(taskList);


		System.out.println("//---------------   Print and Remove log for Task 2 ID="+task2);
		taskList = taskQueueService.getTaskLog(task2.getId());
		assertThat(taskList).isNotEmpty();
		for (TaskRecord tr: taskList) {
			logger.trace("Task record for task="+task2+", record "+tr.getStatus()+", msg:"+tr.getMessage());
		}
		taskRepo.delete(taskList);


		System.out.println("//---------------   Check for forgotten records");
		theSite = siteRepo.findOne(theSite.getId());
		for (TaskRecord  logRecord : theSite.getTasksLog()) {
			System.out.println("Log record: " + logRecord);
		}


		for (TaskRecord  logRecord : taskRecordRepository.findAll()) {
			System.out.println("ALL Log record: " + logRecord);
		}

		for (Schedule sch : theSite.getSchedules()) {
			System.out.println("Schedule: " + sch);
		}

		System.out.println("//---------------   Remove Site");
		//entityManager.flush();
		String theSiteId = theSite.getId();
		theSite = siteRepo.findOne(theSite.getId());
		siteService.removeSite(theSite);

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
        FlatXmlDataSet.write(fullDataSet, new FileOutputStream("/tmp/full1-dataset.xml"));
		
    }
    
	
}

package home.abel.photohub.web;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.TransactionDbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import home.abel.photohub.connector.prototype.SiteConnectorInt;
import home.abel.photohub.model.*;
import home.abel.photohub.service.*;
import home.abel.photohub.tasks.BaseTask;
import home.abel.photohub.tasks.TaskDescription;
import home.abel.photohub.tasks.TaskNamesEnum;
import home.abel.photohub.tasks.TaskStatusEnum;
import home.abel.photohub.web.model.BatchResult;
import home.abel.photohub.web.model.DefaultObjectResponse;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URL;
import java.util.*;

import javax.servlet.http.Cookie;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Condition;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
	Accept:text/javascript, application/javascript, application/ecmascript, application/x-ecmascript, ...; q=0.01
	Accept-Encoding:gzip, deflate
	Accept-Language:en-US,en;q=0.8
	Cache-Control:no-cache
	Connection:keep-alive
	Content-Length:46
	Content-Type:application/x-www-form-urlencoded; charset=UTF-8
	Cookie:_ga=GA1.1.1249671794.1446242679; x-auth-token="eyJpZCI6NTUxLCJ1c2VybmFtZSI6ImFkbWluIiwiZXhwaXJlcyI6MCwicm9sZXMiOlsiVVNFUiIsIkFETUlOIl19.jV+MJJUHsgGvAyaBp7+T8shBQkLREGJDbVGhpaX2sEs="
	Host:localhost:8081
	Origin:http://localhost:63342
	Pragma:no-cache
	Referer:http://localhost:63342/photohub2-client2/src/index.html
	User-Agent:Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.86 Safari/537.36
	X-Requested-With:XMLHttpRequest
*/


@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes=home.abel.photohub.web.SiteTest.ServiceTestContextCfg.class)
@PropertySource("classpath:test-settings.properties")

//https://github.com/springtestdbunit/spring-test-dbunit
//@TestExecutionListeners(value = { DbUnitTestExecutionListener.class,
//		TransactionDbUnitTestExecutionListener.class},
//		mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)

@TestExecutionListeners(value = { DbUnitTestExecutionListener.class },
		mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)

@DatabaseSetup("classpath:db-test-data.xml")

public class SiteTest   {
	final Logger logger = LoggerFactory.getLogger(SiteTest.class);

//  Removed with 	changing ContextConfiguration
	@Configuration
	@PropertySource("classpath:test-settings.properties")
	@Import({
		home.abel.photohub.webconfig.standalone.AppInit.class,
		home.abel.photohub.model.SpringDataConfig.class,
		home.abel.photohub.service.SpringConfig.class
		})

	public static class ServiceTestContextCfg {

	}
	
	//  AssertJ condition
	class checkSiteId extends  Condition<home.abel.photohub.model.Site> {
		final Logger logger = LoggerFactory.getLogger(checkSiteId.class);
	
		protected String id = "";
		public checkSiteId(String id) {
			super("Check site by ID="+id);
			this.id = id;
		}
		
		@Override
		public boolean matches(home.abel.photohub.model.Site value) {			
			return value.getId().equals(id);	
		}
	};

	public static boolean firstRun =false;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired MockHttpSession session;
    @Autowired MockHttpServletRequest request;
    @Autowired ConfigService configService;
	@Autowired TaskQueueService taskQueue;
	@Autowired
	SiteService siteService;
	@Autowired
	PhotoService photoService;

//	@Autowired
//	private TaskRecordRepository taskRepository;


    @Autowired
    private FilterChainProxy filterChain;
    
    private MockMvc mockMvc;
	private Cookie[] cookies = null;
	
	
	public static final String L2_FOLDER = "testFolder";
	public static final String DEF_SITE_NAME="TEST1";
	public static final String NEW_SITE_NAME="TEST_NEW";
	public static final String DEF_SITE_TYPE="Local";
	
	public final static String SITE_ROOT_PATH = "/tmp/photohub_test";
	public final static String TEST_FOLDER_NAME = "foldertest";
	public static final String RESOURCE_IMAGE_FN = "photo1.JPG";
	public static final String TMP_IMAGE_FILE = "/tmp/photo1.JPG";
	public static final String TMP_IMAGE_ID = "3";
	public final static String TMP_ROOT_PATH = "/tmp/photohub_test_2";

	public final static String TMP_IMAGE_FULLPATH = TMP_ROOT_PATH + File.separator + TEST_FOLDER_NAME + File.separator + RESOURCE_IMAGE_FN;


	public final static String TEST_SITE_ID = "2";
	
    
	/**
	 * Before Test
	 * @throws Throwable
	 */
	@Before
	public void before() throws Throwable {

		System.out.println("------------------------------------------------------------------------");
		System.out.println("\t Before is invoked");
		System.out.println("------------------------------------------------------------------------");

		if( ! firstRun ) {
			configService.Init();
			//taskQueue.Init();

			firstRun = false;
		}
//		File newFolder = new File(TMP_ROOT_PATH + File.separator + TEST_FOLDER_NAME);
//		if ( ! newFolder.exists() ) {
//			newFolder.mkdirs();
//		}
//
//		URL resourceUrl = ClassLoader.getSystemClassLoader().getResource(RESOURCE_IMAGE_FN);
//		String sampeImagePath =  resourceUrl.toURI().getPath();
//		File sampeImageFile = new File(sampeImagePath);
//
//		File imgFile = new  File(newFolder.getAbsolutePath() +  File.separator + RESOURCE_IMAGE_FN) ;
//		if ( ! imgFile.exists()) {
//			FileUtils.copyFile(sampeImageFile,imgFile);
//			System.out.println("Copy file " + imgFile.getAbsolutePath());
//			imgFile.deleteOnExit();
//		}
		
		//   Start Mockmvc
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
        		.addFilter(filterChain)
                .build();



		//    Prepare image file

		File newFolder = new File(TMP_ROOT_PATH + File.separator + TEST_FOLDER_NAME);
		if ( ! newFolder.exists() ) {
			newFolder.mkdirs();
		}
		System.out.println("Create temp directory: " + newFolder.getAbsolutePath());

		URL resourceUrl = ClassLoader.getSystemClassLoader().getResource(RESOURCE_IMAGE_FN);
		String sampleImagePath =  resourceUrl.toURI().getPath();
		File sampleImageFile = new File(sampleImagePath);

		File imgFile = new  File(newFolder.getAbsolutePath() +  File.separator + RESOURCE_IMAGE_FN) ;
		if ( ! imgFile.exists()) {
			FileUtils.copyFile(sampleImageFile,imgFile);
			System.out.println("Copy file exist : from "+sampleImageFile.getAbsolutePath()+", to " + imgFile.getAbsolutePath());

			//System.out.println("Copy file " + imgFile.getAbsolutePath());
			imgFile.deleteOnExit();
		}
		else {
			System.out.println("Image file exist on place: " + imgFile.getAbsolutePath());
		}


//		//  Prepare  folder for  for LocalSite
//		File rootPath = new File(SITE_ROOT_PATH);
//		if ( ! rootPath.exists() ) {
//			rootPath.mkdirs();
//		}
//		System.out.println("Create Local site root path: " + rootPath.getAbsolutePath());
	}

	@BeforeClass
	public static  void beforeClass() throws Exception {
		System.out.println("\n*** beforeClass is invoked");







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
		System.out.println("***Remove site structure");
		FileUtils.deleteDirectory(new File(SITE_ROOT_PATH));
    }

	private Cookie[]  httpAuth(String userName,String password) throws Throwable {
		logger.debug("[httpAuth] Auth for user="+userName+", password="+password);
		cookies = initCookies();
		ResultActions actions = null;
		actions = mockMvc.perform(post("/api/login/login").cookie(cookies)
				.contentType(MediaType.APPLICATION_JSON)
				.param("username", userName)
				.param("password", password))
				;

		if  ( actions.andReturn().getResponse().getStatus() != 200 ) {
			logger.warn("[httpAuth] Auth FAILED rc=" +actions.andReturn().getResponse().getStatus()+
					", error="+ actions.andReturn().getResponse().getErrorMessage());
			throw new ExceptionAccessDeny(actions.andReturn().getResponse().getErrorMessage());
		}

		return updateCookies(cookies, actions);
	}


	@Test
	public void authTest()  throws Throwable{

		System.out.println("\n------ Test: authTest ------\n");


		try {
			cookies = httpAuth("test", "test");
			assertThat(true);
		}
		catch (ExceptionAccessDeny e) {
			try {
				cookies = httpAuth("admin", "admin");
			}
			catch (ExceptionAccessDeny e2) {
				Assert.isTrue(true, "Authentication for user Admin failed. " +  e.getMessage());
				assertThat(true);
			}
		}
	}

	/**
	 * Test Base site operations
	 * @throws Throwable
	 */

	@Test
	public void testSiteCreation() throws Throwable {

		System.out.println("\n------ Test: testSiteCreation ------\n");

		ObjectMapper mapper = new ObjectMapper();
		MvcResult result = null;
		
		//    DO Auth
		//cookies = authHTTPConnection();

		try {
			cookies = httpAuth("admin", "admin");
		}
		catch (ExceptionAccessDeny e) {
			Assert.isTrue(true, "Authentication for user Admin failed. " +  e.getMessage());
		}


		//   Get site Types
       	result = mockMvc.perform(get("/api/site/types").cookie(cookies))
    		.andDo(print())
    		.andExpect(status().isOk())
    		.andReturn();
       	
       	String strObject = result.getResponse().getContentAsString();
       	DefaultObjectResponse<List<String>> ObjectResponse_1 = mapper.readValue(strObject, new TypeReference<DefaultObjectResponse<List<String>>>() { });
       	assertThat(ObjectResponse_1.getObject()).contains(DEF_SITE_TYPE);
    	
       	//   Prepare site object
    	Site theNewSite = new Site();
    	theNewSite.setName(NEW_SITE_NAME);
    	theNewSite.setRoot(TMP_ROOT_PATH);
    	theNewSite.setSiteUser("Abel");
    	theNewSite.setConnectorType(DEF_SITE_TYPE);    	
    	theNewSite.addProperty(new SiteProperty("root",TMP_ROOT_PATH));
    	
    	//	Add site
    	theNewSite = siteAdd(theNewSite);
       	assertThat(theNewSite).isNotNull();
       	String newSiteId = theNewSite.getId();
       	assertThat(newSiteId).isNotNull();
       	
    	assertThat(getSitesList()).haveAtLeastOne(new checkSiteId(newSiteId));

    	//	Site connect
    	result = mockMvc.perform(
    			get("/api/site/"+newSiteId+"/connect")
    			.cookie(cookies)
    			.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(theNewSite))
    			)
    		.andDo(print())
    		.andExpect(status().isOk())
    		.andReturn();

    	//	Start site scan
		System.out.println("\n------  Start scanning for site="+theNewSite+"\n");

		Schedule scanTaskSchedule = new Schedule();
		scanTaskSchedule.setSite(theNewSite);
		scanTaskSchedule.setTaskName(TaskNamesEnum.TNAME_SCAN.toString());
		scanTaskSchedule.setEnable(false);

		BaseTask task1 = startTask(scanTaskSchedule);

		String status = "RUN";
    	//	Check  while task running
		while (status.equals(TaskStatusEnum.RUN.toString()) || status.equals(TaskStatusEnum.IDLE.toString())) {
    		Thread.sleep(1*1000);
			status = getTaskStatus(cookies,newSiteId,task1.getId());
    	}

    	//   Site Clean
		System.out.println("\n------  Run clean task for siteId="+newSiteId+"\n");

		scanTaskSchedule = new Schedule();
		scanTaskSchedule.setSite(theNewSite);
		scanTaskSchedule.setTaskName(TaskNamesEnum.TNAME_CLEAN.toString());
		scanTaskSchedule.setEnable(false);
		BaseTask task2 = startTask(scanTaskSchedule);

		status = "RUN";
		//	Check  while task running
		while (status.equals(TaskStatusEnum.RUN.toString()) || status.equals(TaskStatusEnum.IDLE.toString())) {
			Thread.sleep(1*1000);
			status = getTaskStatus(cookies,newSiteId,task1.getId());
		}

		System.out.println("\n------  Remove site siteId="+newSiteId+"\n");
    	mockMvc.perform(
    			delete("/api/site/"+newSiteId)
    			.cookie(cookies)
    			)
    		.andDo(print())
    		.andExpect(status().isOk());

    	Thread.sleep(2*1000);
    	assertThat(getSitesList()).doNotHave(new checkSiteId(newSiteId));
	}


	boolean isTaskRun(TaskRecord taskRecord) {
		return ((taskRecord != null) && ((taskRecord.getStatus().equals("RUN")) || (taskRecord.getStatus().equals("IDLE"))));
	}


	@Test
	public void testRotate() throws Throwable {
		System.out.println("\n------ Test: rotate ------\n");

		ObjectMapper mapper = new ObjectMapper();
		MvcResult result = null;

		try {
			cookies = httpAuth("admin", "admin");
		}
		catch (ExceptionAccessDeny e) {
			Assert.isTrue(true, "Authentication for user Admin failed. " +  e.getMessage());
		}

		//
		//	Get and check site description
		//
		result = apiGetRequest(cookies,"/api/photo/"+TMP_IMAGE_ID,true);
		DefaultObjectResponse<Photo> ObjectResponse = mapper.readValue(
				result.getResponse().getContentAsString(),
				new TypeReference<DefaultObjectResponse<Photo>>() { });

		Photo srcPhoto = ObjectResponse.getObject();

		int srcPhotoWidth = photoService.getBaseMedia(srcPhoto).getWidth();

		result = apiGetRequest(cookies,"/api/photo/"+TMP_IMAGE_ID+"/rotate",true);
		DefaultObjectResponse<Photo> ObjectResponse1 = mapper.readValue(
				result.getResponse().getContentAsString(),
				new TypeReference<DefaultObjectResponse<Photo>>() { });

		Photo rotatePhoto = ObjectResponse1.getObject();
		assertThat(photoService.getBaseMedia(rotatePhoto).getHeight()).isEqualTo(srcPhotoWidth);
	}





	/**
	 * Test Shedule object processing
	 * @throws Throwable
	 */
	@Test
	public void testTaskSchedule() throws Throwable {

		System.out.println("\n------ Test: testTaskSchedule ------\n");

		ObjectMapper mapper = new ObjectMapper();
		try {
			cookies = httpAuth("admin", "admin");
		}
		catch (ExceptionAccessDeny e) {
			Assert.isTrue(true, "Authentication for user Admin failed. " +  e.getMessage());
		}

		//
		//	Get and check site description
		//
		MvcResult result = apiGetRequest(cookies,"/api/site/"+TEST_SITE_ID+"/tasksdescr",true);
		DefaultObjectResponse<List<TaskDescription>> ObjectResponse = mapper.readValue(
				result.getResponse().getContentAsString(),
				new TypeReference<DefaultObjectResponse<List<TaskDescription>>>() { });

		List<TaskDescription> taskDescrList = ObjectResponse.getObject();


		TaskDescription emptyTaskDescr = null;
		for (TaskDescription etd : taskDescrList) {
			if ( etd.getName().equals(TaskNamesEnum.TNAME_EMPTY.toString())){
				emptyTaskDescr = etd;
			}
		}
		if (emptyTaskDescr == null) {
			Assert.isTrue(true,
					"[testTaskSchedule] Task TNAME_EMPTY not present in task description list. ");
		}
		//
		//	Create scheduled task
		//
		Schedule schedule = new Schedule();
		schedule.setTaskName(TaskNamesEnum.TNAME_EMPTY.toString());
		schedule.setEnable(true);
		schedule.setSeconds("*/5");

		for(String tn:  emptyTaskDescr.getParams().keySet() ) {
			TaskParam param = new TaskParam();
			param.setName(tn);
			param.setValue("theVALUE");
			schedule.addParam(param);
		}

		result = apiPutRequest(cookies,
				"/api/site/"+TEST_SITE_ID+"/task",
				mapper.writeValueAsString(schedule),
				true);

		DefaultObjectResponse<BaseTask> ObjectResponse2 = mapper.readValue(
				result.getResponse().getContentAsString(),
				new TypeReference<DefaultObjectResponse<BaseTask>>() { });
		BaseTask task1 = ObjectResponse2.getObject();

		int count = 0;
		String status = "RUN";
		//	Check  while task running
		while (status.equals(TaskStatusEnum.RUN.toString()) || status.equals(TaskStatusEnum.IDLE.toString())) {
			Thread.sleep(1*1000);
			status=getTaskStatus(cookies,TEST_SITE_ID,task1.getId());
			count++;
			Assert.isTrue(count < 20, "[testTaskSchedule]  Scheduled task does not started. Task="+task1);
		}

		//  List tasks

		List<BaseTask> tasksList = getTasksList(cookies,TEST_SITE_ID);

		//	Update task

		schedule = task1.getSchedule();
		schedule.setSeconds("*/10");

		result = apiPutRequest(cookies,
				"/api/site/"+TEST_SITE_ID+"/task/"+task1.getId()+"/schedule",
				mapper.writeValueAsString(schedule),
				true);

		DefaultObjectResponse<Schedule> ObjectResponse3 = mapper.readValue(
				result.getResponse().getContentAsString(),
				new TypeReference<DefaultObjectResponse<Schedule>>() { });
		schedule  = ObjectResponse3.getObject();


		assertThat(schedule.getSeconds()).isEqualTo("*/10");
		status=getTaskStatus(cookies, TEST_SITE_ID, task1.getId());

		//	Stop Task
		result = apiDeleteRequest(cookies,
				"/api/site/"+TEST_SITE_ID+"/task/"+task1.getId(),
				null,
				true
		);

		tasksList = getTasksList(cookies,TEST_SITE_ID);

		String taskid = task1.getId();

		assertThat(tasksList).doNotHave(
				new Condition<BaseTask>() {
					@Override
					public boolean matches(BaseTask task) {
						return task.getId().equals(taskid) ;
					}
				}
		);
	}


	/**
	 * Batch deletion test
	 * @throws Throwable
	 */
	@Test
	public void testBatchDelete() throws Throwable {

		System.out.println("\n------ Test: testBatchDelete ------\n");



		Site theSite =  siteService.getSite("2");

		SiteConnectorInt connector = siteService.getOrLoadConnector(theSite);
		Node theNode1 = photoService.addPhoto(
				new File (TMP_IMAGE_FULLPATH),
				"test1.jpg",
				"",
				null,
				theSite.getId());
		Node theNode2 = photoService.addPhoto(
				new File (TMP_IMAGE_FULLPATH),
				"test2.jpg",
				"",
				null,
				theSite.getId());



		List<String> objList = new ArrayList<>();
		objList.add(theNode1.getPhoto().getId());
		objList.add(theNode2.getPhoto().getId());

		Cookie[] cookies = null;

		ObjectMapper mapper = new ObjectMapper();
		try {
			cookies = httpAuth("admin", "admin");
		}
		catch (ExceptionAccessDeny e) {
			Assert.isTrue(true, "Authentication for user Admin failed. " +  e.getMessage());
		}

		MvcResult result = apiDeleteRequest(cookies,
				"/api/objects",
				mapper.writeValueAsString(objList),
				true
		);

		DefaultObjectResponse<List<BatchResult>> ObjectResponse3 = mapper.readValue(
				result.getResponse().getContentAsString(),
				new TypeReference<DefaultObjectResponse<List<BatchResult>>>() { });

		List<BatchResult> resultList = ObjectResponse3.getObject();

		assertThat(resultList).hasSize(2);
		assertThat(resultList).doNotHave(
				new Condition<BatchResult>() {
					@Override
					public boolean matches(BatchResult retObj) {
						return retObj.getStatus() > 0;
					}
				}
		);
	}




	public MvcResult apiGetRequest(Cookie[] cookies, String path, boolean isPrint) throws Throwable {
		ObjectMapper mapper = new ObjectMapper();
		MvcResult result = null;
		result = mockMvc.perform
				(get(path)
						.cookie(cookies))
				.andExpect(status().isOk())
				.andReturn();
		if (isPrint) {
			System.out.println("Request: GET: " + path);
			System.out.println("Response: ");
			Object jsonObject = mapper.readValue(result.getResponse().getContentAsString(), Object.class);
			String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
			System.out.println(prettyJson);
		}
		return result;
	}

	public MvcResult apiPutRequest(Cookie[] cookies, String path, String jsonBody, boolean isPrint) throws Throwable{
		ObjectMapper mapper = new ObjectMapper();
		MvcResult result = null;
		result = mockMvc.perform
				(
						put(path)
						.cookie(cookies)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonBody)  //mapper.writeValueAsString(schedule)
				)
				.andExpect(status().isOk())
				.andReturn();

		if (isPrint) {
			System.out.println("Request: PUT: " + path);
			System.out.println("Response: ");
			if (result.getResponse().getContentAsString() != null) {
				Object jsonObject = mapper.readValue(result.getResponse().getContentAsString(), Object.class);
				String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
				System.out.println(prettyJson);
			}
		}
		return result;
	}


	public MvcResult apiDeleteRequest(Cookie[] cookies, String path, String jsonBody, boolean isPrint) throws Throwable {
		MvcResult result = null;


		MockHttpServletRequestBuilder reqBody = delete(path)
				.cookie(cookies)
				.contentType(MediaType.APPLICATION_JSON);

		if ( jsonBody != null ) {
			reqBody = reqBody.content(jsonBody);
		}

		result = mockMvc.perform(reqBody)
				.andExpect(status().isOk())
				.andReturn();

		if (isPrint) {
			ObjectMapper mapper = new ObjectMapper();
			System.out.println("Request: Delete: " + path);
			System.out.println("Response: ");
			if (result.getResponse().getContentAsString() != null) {
				Object jsonObject = mapper.readValue(result.getResponse().getContentAsString(), Object.class);
				String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
				System.out.println(prettyJson);
			}
		}
		return result;

	}



	/**
	 * Add site
	 * @param siteObject
	 * @return
	 * @throws Throwable
	 */
	public Site siteAdd(Site siteObject) throws Throwable  {

		System.out.println("\n------  Create site="+siteObject+"\n");

		ObjectMapper mapper = new ObjectMapper();
		MvcResult result = null;
    	result = mockMvc.perform(
    			post("/api/site/add")
    			.cookie(cookies)
    			.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(siteObject))
    			)
    		.andDo(print())
    		.andExpect(status().isOk())
    		.andReturn();
    	
       	DefaultObjectResponse<Site> ObjectResponse_2 = mapper.readValue(
       			result.getResponse().getContentAsString(),
       			new TypeReference<DefaultObjectResponse<Site>>() { });

		return ObjectResponse_2.getObject();
	}
	
	/**
	 * Retrieve sites list 
	 * @return
	 * @throws Throwable
	 */
	private List<Site> getSitesList() throws Throwable {
		ObjectMapper mapper = new ObjectMapper();
       	MvcResult result = mockMvc.perform(get("/api/site/")
    			.cookie(cookies))
    		.andExpect(status().isOk())
    		.andReturn();

       	DefaultObjectResponse<List<Site>> ObjectResponse = mapper.readValue(
       			result.getResponse().getContentAsString(), 
       			new TypeReference<DefaultObjectResponse<List<Site>>>() { });
 		return ObjectResponse.getObject();
	}
	
	/**
	 * Perform site scann request
	 * @param  schedule Новый объект рассписания который надо ставить в очередь
	 * @return вставленный объект
	 * @throws Throwable
	 */
	public BaseTask startTask(Schedule schedule) throws Throwable {
		ObjectMapper mapper = new ObjectMapper();
		MvcResult result = null;

		mockMvc.perform(
				options("/api/site/"+schedule.getSite().getId()+"/task")
						.cookie(cookies)
						.contentType(MediaType.APPLICATION_JSON)
			)
				.andExpect(status().isOk())
				.andReturn();

    	result = mockMvc.perform(
    			put("/api/site/"+schedule.getSite().getId()+"/task")
    			.cookie(cookies)
    			.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(schedule))
    			)
    		.andDo(print())
    		.andExpect(status().isOk())
    		.andReturn();

    	DefaultObjectResponse<BaseTask> resp = mapper.readValue(
    			result.getResponse().getContentAsString(),
    			new TypeReference<DefaultObjectResponse<BaseTask>>() { });
    	
    	return resp.getObject();
	}


	public BaseTask updateTask(BaseTask task,Schedule schedule) throws Throwable {
		return task;
	};


	public String getTaskStatus(Cookie[] cookies, String siteId, String taskId) throws Throwable{
		ObjectMapper mapper = new ObjectMapper();
		MvcResult result = null;

		///site/{id}/task/{id}/taskrecord
		result = apiGetRequest(cookies,
				"/api/site/"+siteId+"/task/"+taskId+"/taskrecord",
				true);
		DefaultObjectResponse<TaskRecord> ObjectResponse3 = mapper.readValue(
				result.getResponse().getContentAsString(),
				new TypeReference<DefaultObjectResponse<TaskRecord>>() { });
		TaskRecord tr = ObjectResponse3.getObject();
		return tr.getStatus();
	}




	public List<BaseTask> getTasksList(Cookie[] cookies, String siteId) throws Throwable{
		ObjectMapper mapper = new ObjectMapper();
		MvcResult result = null;

		result = apiGetRequest(cookies,
				"/api/site/"+siteId+"/tasks",
				true);
		DefaultObjectResponse<List<BaseTask>> ObjectResponse3 = mapper.readValue(
				result.getResponse().getContentAsString(),
				new TypeReference<DefaultObjectResponse<List<BaseTask>>>() { });
		List<BaseTask> theMap = ObjectResponse3.getObject();

		return theMap;
	}






//	/**
//	 * Perform site authentication save and return coockies
//	 * @return
//	 * @throws Throwable
//	 */
//	private Cookie[]  authHTTPConnection(String userName,String password) throws Throwable {
//		cookies = initCookies();
//
//		ResultActions actions = null;
//		//	Incorrect auth data
//		actions = mockMvc.perform(post("/api/login/login").cookie(cookies)
//				.contentType(MediaType.APPLICATION_JSON)
//				.param("username", "test")
//				.param("password", "test"))
//				.andDo(MockMvcResultHandlers.print())
//				.andExpect(status().is(400))
//		//.andExpect(status().isUnauthorized())
//		;
//
//
//		actions = mockMvc.perform(post("/api/login/login").cookie(cookies)
//				.contentType(MediaType.APPLICATION_JSON)
//				.param("username", "admin")
//				.param("password", "admin"))
//				.andDo(print())
//				.andExpect(status().isOk());
//
//		cookies = updateCookies(cookies, actions);
//		printCookie(cookies);
//		return cookies;
//	}

	/*--------------------------------------------------------------------------------------------

			Cookie Utils

	 --------------------------------------------------------------------------------------------*/

	/**
     * Pront out curent coocke array
     * @param cookieAr
     */
    public void printCookie(Cookie[]  cookieAr) {
		for (int i = 0; i < cookieAr.length; i++) {
			System.out.println("Cookie: "+cookieAr[i].getName()+":");
			System.out.println("      Value="+cookieAr[i].getValue());
			System.out.println("      MaxAge="+cookieAr[i].getMaxAge());
		}
    }
    
    /**
     * Merges the (optional) existing array of Cookies with the response in the
     * given MockMvc ResultActions.
     * <p>
     * This only adds or deletes cookies. Officially, we should expire old
     * cookies. But we don't keep track of when they were created, and this is
     * not currently required in our tests.
     */
    protected static Cookie[] updateCookies(final Cookie[] current,
      final ResultActions result) {

        final Map<String, Cookie> currentCookies = new HashMap<String, Cookie>();
        if (current != null) {
            for (Cookie c : current) {
                currentCookies.put(c.getName(), c);
            }
        }

        final Cookie[] newCookies = result.andReturn().getResponse().getCookies();
        for (Cookie newCookie : newCookies) {
            if ((newCookie.getValue() == null) || (newCookie.getValue().length() == 0)) {
                // An empty value implies we're told to delete the cookie
                currentCookies.remove(newCookie.getName());
            } else {
                // Add, or replace:
                currentCookies.put(newCookie.getName(), newCookie);
            }
        }

        return currentCookies.values().toArray(new Cookie[currentCookies.size()]);
    }
    
    /**
     * Creates an array with a dummy cookie, useful as Spring MockMvc
     * {@code cookie(...)} does not like {@code null} values or empty arrays.
     */
    protected static Cookie[] initCookies() {
        return new Cookie[] { new Cookie("unittest-dummy", "dummy") };
    }
    
}

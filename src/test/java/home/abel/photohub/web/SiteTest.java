package home.abel.photohub.web;

import home.abel.photohub.model.Site;
import home.abel.photohub.model.SiteProperty;
import home.abel.photohub.model.TaskRecord;
import home.abel.photohub.model.TaskRepository;
import home.abel.photohub.web.model.DefaultObjectResponse;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import home.abel.photohub.webconfig.standalone.AppInit;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Condition;
import org.assertj.core.api.IterableAssert;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
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
//@RunWith(SpringRunner.class)
//@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
//@ContextConfiguration(classes={AppInit.class})
@ContextConfiguration(classes=home.abel.photohub.web.SiteTest.ServiceTestContextCfg.class)


/*  
        mockMvc.perform(get("/api/todo"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].description", is("Lorem ipsum")))
                .andExpect(jsonPath("$[0].title", is("Foo")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].description", is("Lorem ipsum")))
                .andExpect(jsonPath("$[1].title", is("Bar")));
                
 */



public class SiteTest   {

//  Removed with 	changing ContextConfiguration
	@Configuration
	@PropertySource("classpath:test-settings.properties")
	@Import({
		home.abel.photohub.webconfig.standalone.AppInit.class,
		home.abel.photohub.model.SpringDataConfig.class,
		home.abel.photohub.service.SpringConfig.class
		})
	//@ComponentScan(basePackages={"home.abel.photohub.service","home.abel.photohub.utils"})
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

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired MockHttpSession session;
    @Autowired MockHttpServletRequest request;

//	@Autowired
//	private TaskRepository taskRepository;


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
	public final static String TMP_ROOT_PATH = "/tmp/photohub_test_2";
	
    
	/**
	 * Before Test
	 * @throws Throwable
	 */
	@Before
	public void before() throws Throwable {
				
		File newFolder = new File(TMP_ROOT_PATH + File.separator + TEST_FOLDER_NAME);
		if ( ! newFolder.exists() ) {
			newFolder.mkdirs();
		}
		
		URL resourceUrl = ClassLoader.getSystemClassLoader().getResource(RESOURCE_IMAGE_FN);
		String sampeImagePath =  resourceUrl.toURI().getPath(); 				
		File sampeImageFile = new File(sampeImagePath);
		
		File imgFile = new  File(newFolder.getAbsolutePath() +  File.separator + RESOURCE_IMAGE_FN) ;
		if ( ! imgFile.exists()) {
			FileUtils.copyFile(sampeImageFile,imgFile);
			System.out.println("Copy file " + imgFile.getAbsolutePath());
			imgFile.deleteOnExit();
		}
		
		//   Start Mockmvc
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
        		.addFilter(filterChain)
                .build();
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		System.out.println("***After Class is invoked");
		System.out.println("***Remove site structure");
		FileUtils.deleteDirectory(new File(SITE_ROOT_PATH));
    }
	
	
	/**
	 * Perform site authentication save and return coockies
	 * @return
	 * @throws Throwable 
	 */
	private Cookie[]  authHTTPConnection() throws Throwable {
        cookies = initCookies();
            	
    	ResultActions actions = null;
    	//	Incorrect auth data
    	actions = mockMvc.perform(post("/api/login/login").cookie(cookies)
    			.contentType(MediaType.APPLICATION_JSON)
	                .param("username", "test")
	                .param("password", "test"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().is(400))
                //.andExpect(status().isUnauthorized())
                ;
    	
    	
    	actions = mockMvc.perform(post("/api/login/login").cookie(cookies)
    			.contentType(MediaType.APPLICATION_JSON)
	                .param("username", "admin")
	                .param("password", "admin"))
    			.andDo(print())
    			.andExpect(status().isOk());
    	
    	cookies = updateCookies(cookies, actions);
    	printCookie(cookies);
    	return cookies;
	}
	
	@Test
	public void testSiteCreation() throws Throwable {
		ObjectMapper mapper = new ObjectMapper();
		MvcResult result = null;
		
		//    DO Auth
		cookies = authHTTPConnection();
		
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

		//	Objects List



    	//	Start site scan
    	TaskRecord tr = startTask("TNAME_SCAN",newSiteId);
		while (isTaskRun(tr)) {
    		Thread.sleep(1*1000);
    		tr = checkTask(newSiteId,tr.getId());
    	}

    	
    	//   Site Clean
		System.out.println("\n------  Run clean task for siteId="+newSiteId+"\n");

    	tr = startTask("TNAME_CLEAN",newSiteId);
    	while (isTaskRun(tr)) {
    		Thread.sleep(1*1000);
    		tr = checkTask(newSiteId,tr.getId());
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

	/**
	 * Add site  
	 * @param siteObject
	 * @return
	 * @throws Throwable
	 */
	public Site siteAdd(Site siteObject) throws Throwable  {


		System.out.println("\n------  Create Site site="+siteObject+"\n");

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
	 * Perfor site scann request
	 * @param siteId
	 * @return
	 * @throws Throwable
	 */
	public TaskRecord startTask(String taskName,  String siteId) throws Throwable {
		ObjectMapper mapper = new ObjectMapper();
		MvcResult result = null;
    	result = mockMvc.perform(
    			post("/api/site/"+siteId+"/task")
    			.cookie(cookies)
    			.contentType(MediaType.APPLICATION_JSON)
				.param("taskname", taskName)
    			)
    		.andDo(print())
    		.andExpect(status().isOk())
    		.andReturn();

    	DefaultObjectResponse<TaskRecord> ObjectResponse_3 = mapper.readValue(
    			result.getResponse().getContentAsString(),
    			new TypeReference<DefaultObjectResponse<TaskRecord>>() { });
    	
    	return ObjectResponse_3.getObject();
	}


	public TaskRecord checkTask(String siteId, String taskId) throws Throwable  {
		ObjectMapper mapper = new ObjectMapper();
		MvcResult result = null;
		result = mockMvc.perform(
				get("/api/site/"+siteId+"/task/"+taskId)
						.cookie(cookies)
						.contentType(MediaType.APPLICATION_JSON)
				)
				.andDo(print())
				.andExpect(status().isOk())
				.andReturn();

		DefaultObjectResponse<TaskRecord> ObjectResponse_3 = mapper.readValue(
				result.getResponse().getContentAsString(),
				new TypeReference<DefaultObjectResponse<TaskRecord>>() { });

		return ObjectResponse_3.getObject();
	}

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

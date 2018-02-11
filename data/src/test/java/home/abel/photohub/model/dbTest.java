package home.abel.photohub.model;

import com.querydsl.jpa.JPAExpressions;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;

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
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
//import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.slf4j.Logger;

import static com.querydsl.core.group.GroupBy.*;

import com.querydsl.jpa.impl.JPAQuery;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes=home.abel.photohub.model.dbTest.dbContextCfg.class)
//@ActiveProfiles("mysql")
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,
    TransactionalTestExecutionListener.class,
    DbUnitTestExecutionListener.class})
//@TransactionConfiguration(transactionManager="transactionManager", defaultRollback=true)
@Transactional
@DatabaseSetup("classpath:db-test-data.xml")
public class dbTest {
	final Logger logger = LoggerFactory.getLogger(dbTest.class);

	  @Configuration
	  //@PropertySource("classpath:db-config.properties")
	  @PropertySource("classpath:db-connector.properties")	  
	  @Import(home.abel.photohub.model.SpringDataConfig.class)
	  public static class dbContextCfg {

	  }
	
	@PersistenceContext
	private EntityManager entityManager;
	
	@Autowired
	private SiteRepository siteRepository;

	@Autowired
	private PhotoRepository photoRepo;
	
	@Autowired 
	private NodeRepository nodeRepo;
		
	@Autowired
	private UserRepository userRepository;	
	
	@Autowired
	private TaskRecordRepository taskRecordRepository;
	
	@Autowired
	private ScheduleRepository scheduleRepository;

	@Autowired
	private DataSource dataSource;



	@BeforeClass
	public static void beforeClass() {
		System.out.println("***Before Class is invoked");
		System.out.println("\nPrepareDB");
	}
	  
	@Before
	public void before() {
		System.out.println("____________________");
		System.out.println("\t Before is invoked");
	}
	
	@After
	public void after() {
		System.out.println("\t After is invoked");
		System.out.println("=================");
	}
	
	public static final String newSiteName = "Local-test-site1";
	public static final String newSitePropName = "Local path";
	

	@Test
	/**
	 *   Create and selete Site Object
	 */
	public void testSiteCreation() {
	   	Site theSite = new Site();
    	theSite.setName(newSiteName);
    	theSite.setRoot("/tmp");
    	theSite = siteRepository.save(theSite);
       	
    	Iterable<Site> sList = siteRepository.findAll();
    	assertThat(sList).hasSize(2);
    	assertThat(theSite.getName()).isEqualTo(newSiteName);
    	
    	theSite.addProperty(newSitePropName, "/tmp/");
    	theSite = siteRepository.save(theSite);

    	sList = siteRepository.findAll();
    	for (Site st: sList ) {
    		assertThat(st.getProperties()).hasSize(1);
    		
    		if (st.getName().equals(newSiteName)) {
    			assertThat(st.getProperties().get(0).getName()).isEqualTo(newSitePropName);
    		}	
    	}
    	
    	TaskRecord record = new TaskRecord();
    	theSite.addTaskRecord(record);
    	record = taskRecordRepository.save(record);
    	assertThat(record.getId()).isNotNull();
    	String trId = record.getId();
    	
    	siteRepository.delete(theSite);
    	
    	record = taskRecordRepository.findOne(trId);
    	assertThat(record).isNull();    	
    	
	}
	
	public static String defSiteName = "Test Local Name";
	public static String photo1Name = "photo1";
	
	@Test 
	/**
	 * 	Testing photo object and node object creation and delete
	 */
	public void photoObjTesting() {
		Site theSite = siteRepository.findOne("1");
		assertThat(theSite.getName()).isEqualTo(defSiteName);
		
    	Photo folder = new Photo();
    	folder.setName("folder");
    	folder.setType(ModelConstants.OBJ_FOLDER);
    	folder.setSiteBean(theSite);
    	photoRepo.save(folder);
    	
    	Node folderNode = new Node(folder,null);
    	Node fn1 = nodeRepo.save(folderNode);
    	assertThat(fn1.getId()).isNotNull();
    	
    	folderNode = nodeRepo.findOne(folderNode.getId());
    	assertThat(folderNode).isNotNull();
    	assertThat(folderNode.getPhoto()).isEqualToComparingFieldByField(folder);
    	
    	Photo photo = new Photo();
    	photo.setName("Test photo");
    	photo.setType(ModelConstants.OBJ_SINGLE);
    	photo.setUpdateTime(new Date());
    	photo.setSiteBean(theSite);
    	photoRepo.save(photo);
    	
    	Node photoNode = new Node(photo,folderNode);
    	nodeRepo.save(photoNode);
    	
    	Photo foundPhoto = photoRepo.findOne(photo.getId());
    	assertThat(foundPhoto).isEqualToComparingFieldByField(photo);	
    	
    	Node parentNode = nodeRepo.findOne(photoNode.getParent());
    	assertThat(parentNode).isEqualToComparingFieldByField(folderNode);	
    	
    	nodeRepo.delete(photoNode);
    	photoRepo.delete(photo);
    	
    	nodeRepo.delete(folderNode);
    	photoRepo.delete(folder);
    
    	Iterable<Photo> sitesList = photoRepo.findAll(QPhoto.photo.siteBean.id.eq(theSite.getId()));
    	assertThat(sitesList).isEmpty();
	}
	
	public static String scheduleName = "backup";
    @Test
    @Transactional
    public  void scheduleTest() {

    	Site theSite = null;
    	theSite = siteRepository.findOne("1");
    	assertThat(theSite.getName()).isEqualTo(defSiteName);

		Schedule sysSch  = new Schedule();
		sysSch.setTaskName(scheduleName);
		sysSch.setMinute("5");		
    	scheduleRepository.save(sysSch);	
    	
    	Schedule sch  = new Schedule();
    	sch.setSite(theSite);
    	sch.setTaskName(scheduleName);
    	sch.setMinute("7");
    	scheduleRepository.save(sch);

    	TaskParam tp =  new TaskParam();
    	tp.setName("PARAM1");
		tp.setValue("VALUE1");
//		List<TaskParam> params = new ArrayList<>();
//		params.add(tp);
    	sch.addParam(tp);
		scheduleRepository.save(sch);

    	List<Schedule> schList = scheduleRepository.findBySiteIdAndTaskName(theSite.getId(),scheduleName);
    	assertThat(schList).hasSize(1);
    	assertThat(schList.get(0).getSite()).isEqualToComparingFieldByField(theSite);
    	
    	schList = scheduleRepository.findBySiteIdAndTaskName(null,scheduleName);
    	assertThat(schList).hasSize(1);
    	assertThat(schList.get(0).getSite()).isNull();
    	    	
    	theSite.removeSchedule(sch);
    	siteRepository.save(theSite);
    	scheduleRepository.delete(sysSch);
    	schList = scheduleRepository.findBySiteIdAndTaskName(theSite.getId(),"backup");
    	assertThat(schList).hasSize(0);
    	
    }

	@Test
	@Transactional
	public  void scheduleParamsTest()  throws Exception{

		Schedule schedule = scheduleRepository.findOne("1");
		assertThat(schedule.getParams()).hasSize(2);

		TaskParam param1 =  schedule.getParam("PARAM1");

		String param1Name = param1.getName();
		schedule.deleteParam(param1Name);

		scheduleRepository.save(schedule);

		schedule = scheduleRepository.findOne("1");
		assertThat(schedule.getParams()).hasSize(1);

	}




	@Test
    @Transactional
    public  void usersTest()  throws Exception{
		User user = addUser("admin", "admin");
    	logger.info("Add user admin");
    	
		final BCryptPasswordEncoder pwEncoder = new BCryptPasswordEncoder();
		user.setPassword(pwEncoder.encode("Just test password"));
	  	logger.info("Set user password");		
		userRepository.save(user);

		//dumpDB();
    }
   
	private User addUser(String username, String password) throws Exception {
		User user = new User();
		user.setUsername(username);
		user.setPassword(new BCryptPasswordEncoder().encode(password));
		user.grantRole(username.equals("admin") ? UserRole.ADMIN : UserRole.USER);
		userRepository.save(user);
		Iterable<User> fUser = userRepository.findAll(QUser.user.username.eq(username));
		assertThat(fUser).hasSize(1);
		return user;
	}
	
	
	@Test
	@Transactional  
	public void testQDSL() throws Exception {
	
		Site theSite = siteRepository.findOne("1");
		assertThat(theSite.getName()).isEqualTo(defSiteName);


        JPAQuery<?> query = new JPAQuery<Void>(entityManager);
		QTaskRecord qtr = QTaskRecord.taskRecord;
		
		//  Групирует лог задач по имани и выводит  максимальную дату
		Map<String,Date>  resTuple = query.select().from(qtr)
				.where(qtr.siteBean.id.eq(theSite.getId()))
                .transform(groupBy(qtr.name).as(max(qtr.startTime)));
		
		assertThat(resTuple).hasSize(2);
		//String firstEl = resTuple.get(0);
		assertThat(resTuple.keySet()).contains("scan");

        for ( String tName: resTuple.keySet()) {
            System.out.println(" Got task name="+tName+" with date="+resTuple.get(tName));
        }

		assertThat(resTuple.get("scan")).isEqualToIgnoringMillis("2017-02-10T18:29:35.000");


        Map<String, Date> resMap  = query.select().from(qtr)
				.where(qtr.siteBean.id.eq(theSite.getId()))
                .transform(groupBy(qtr.name).as(max(qtr.startTime)));


//		Map<String, Group> resMap  = query.from(qtr)
//				.where(qtr.siteBean.id.eq(theSite.getId()))
//				.transform(GroupBy.groupBy(qtr.name).as(GroupBy.max(qtr.startTime),qtr.id))
//				;
		

		Assert.notNull(resMap,"Returned list is empty");
		for (String key: resMap.keySet() ) {
			System.out.println("Task name="+resMap.get(key) +", task time="+resMap.get(key) );
		}


//		TaskRecord tr = (TaskRecord)resMap.get("scan").toArray()[2];
//		assertThat(tr.getStartTime()).isEqualToIgnoringMillis("2017-02-10T18:29:35.000");



		System.out.println("--- Process query with join and date shift");
		System.out.println("--- Get task with most late starttime");
		query = new JPAQuery(entityManager);
		qtr = QTaskRecord.taskRecord;
		
		QTaskRecord qtr2 = new QTaskRecord("neighbor");

		List<TaskRecord> recordList =
                query.select(qtr).from(qtr).leftJoin(qtr2)
                        .on(qtr.name.eq(qtr2.name)
								.and(qtr.startTime.lt(qtr2.startTime))
								.and(qtr.siteBean.id.eq(theSite.getId()))
						)
                        .where(qtr2.isNull(),qtr.siteBean.id.eq(theSite.getId()))
                        .fetch();

		for (TaskRecord rec: recordList) {
			System.out.println("Task name="+rec.getName()
					+", task time="+rec.getStartTime()
					+", task id="+rec.getId()
							);
		}
		
		
		query = new JPAQuery(entityManager);
		qtr = QTaskRecord.taskRecord;
		qtr2 = new QTaskRecord("neighbor");

       //List<TaskRecord>
        recordList = query.select(qtr).from(qtr)
	    .where(qtr.startTime.eq(
                JPAExpressions.select(qtr2.startTime.max()).from(qtr2))
        )
        .fetch();


//        recordList = query.from(qtr)
//                .where(qtr.startTime.eq(
//                        new JPASubQuery().from(qtr2).unique(qtr2.startTime.max())))
//                .list(qtr);

		for (TaskRecord rec: recordList) {
			System.out.println("Task name="+rec.getName()
					+", task time="+rec.getStartTime()
					+", task id="+rec.getId()
							);
		}
//

		System.out.println("--- Process query with inner join ");




//    	logger.info("Delete Site");
//    	siteRepository.delete(theSite);
//    	
    	
	}

	@Test
	@Transactional
	public void testJDBCT() throws Exception {

		JdbcTemplate  jdbcTemplate = new JdbcTemplate(dataSource);

		Site theSite = siteRepository.findOne("1");
		assertThat(theSite.getName()).isEqualTo(defSiteName);

		List<TaskRecord> recordList = jdbcTemplate.query(
				"SELECT tr.*  FROM task_records tr" +
				" INNER JOIN" +
				"    (SELECT name, MAX(startTime) AS MaxDateTime FROM task_records WHERE site = ? GROUP BY name) maxTasks " +
				" ON tr.name = maxTasks.name " +
				"    AND tr.startTime = maxTasks.MaxDateTime " +
				" WHERE tr.site = ?",
				new BeanPropertyRowMapper(TaskRecord.class), theSite.getId().toString(),theSite.getId().toString()
		);

		for (TaskRecord rec: recordList) {
			System.out.println("Task name="+rec.getName()
					+", task time="+rec.getStartTime()
					+", task id="+rec.getId()
			);
		}

		//		SELECT tt.*
//		FROM topten tt
//		INNER JOIN
//		(SELECT home, MAX(datetime) AS MaxDateTime
//		FROM topten
//		GROUP BY home) groupedtt
//		ON tt.home = groupedtt.home
//		AND tt.datetime = groupedtt.MaxDateTime
	}

	private void dumpDB() throws Exception {

		java.sql.Connection con = dataSource.getConnection();
		IDatabaseConnection connection = new DatabaseConnection(con);
		IDataSet fullDataSet = connection.createDataSet();
		FlatXmlDataSet.write(fullDataSet, new FileOutputStream("/tmp/full-dataset.xml"));

	}

	@AfterClass
	public static void afterClass() {

    	System.out.println("***After Class is invoked");
	}
    
}

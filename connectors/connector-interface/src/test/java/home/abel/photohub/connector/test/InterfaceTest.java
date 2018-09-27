package home.abel.photohub.connector.test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import java.io.File;
import java.sql.*;

import home.abel.photohub.connector.*;
import home.abel.photohub.connector.prototype.ConnectionKeyInt;
import home.abel.photohub.connector.prototype.KeyStoreInt;
import home.abel.photohub.connector.prototype.SiteConnectorInt;

import org.apache.commons.dbcp.BasicDataSource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;


public class InterfaceTest {
	public static final Logger logger = LoggerFactory.getLogger(InterfaceTest.class);

	public static DataSource dataSource = null;
	public static String dbName;

	@BeforeClass
	public static void  createDB() throws Exception{

		String tempDir = System.getProperty("java.io.tmpdir");

		BasicDataSource DS = new BasicDataSource();
		String dbURL = "jdbc:hsqldb:file:";
		dbName = tempDir + "/db/photohub";
		String dbUser = "photohub";
		String dbPw = "photohub";
		String dbDriverName = "org.hsqldb.jdbc.JDBCDriver";

		logger.debug("Create DB Connection: Driver="+ dbDriverName +
				", DB="+dbURL + dbName + ", Username=" +dbUser);

		DS.setUrl(dbURL + dbName);
		DS.setDriverClassName(dbDriverName);
		DS.setUsername(dbUser);
		DS.setPassword(dbPw);

		dataSource = DS;

		String sqlStr = "CREATE TABLE DATA_STORE (" +
				"  SITEID BIGINT," +
				"  ACCOUNT VARCHAR(255)," +
				"  TOKEN VARCHAR(255)," +
				"  DATA VARCHAR(1024)," +
				"  PRIMARY KEY (SITEID)" +
				");";

		Statement stmt = null;
		ResultSet rs = null;
		try {
			Connection conn = dataSource.getConnection();

			stmt = conn.createStatement();
			rs = stmt.executeQuery(sqlStr);

		}
		catch (SQLException ex) {

		}
		finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException sqlEx) { } // ignore
			}
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException sqlEx) { } // ignore
			}
		}

	}

	@AfterClass
	public static void  closeDB() throws Exception {

		logger.debug("[closeDB] AfterClass invoked.");

		Connection conn = dataSource.getConnection();


		Statement stmt = conn.createStatement();
		int rows = stmt.executeUpdate("DELETE FROM DATA_STORE");
		logger.debug("[closeDB] Remove records :  " + rows);



		conn.setAutoCommit(true);
		logger.debug("[closeDB] Shutdown DB.");
		stmt = conn.createStatement();
		stmt.execute("SHUTDOWN");
		conn.close();


		File dbFile = new File(dbName).getParentFile();
		logger.debug("[closeDB] Remove db file : " +  dbFile.getCanonicalPath());
		dbFile.delete();


	}


	@Test
	public void initFactory() {  	
		ConnectorsFactory factory   = new ConnectorsFactory();
		factory.setDataSource(dataSource);
		String hasType = null;
		try {
			factory.addConnectorClass(ConnectorImpl.class.getName());
			//factory.Init();
			for (String type: factory.getAvailableTypes()) {
				hasType = type;
				System.out.println("Factory has type '"+type+"'");
			}
			String connctorId = "1";
			SiteConnectorInt connector  = factory.getConnector(hasType, "abel", connctorId , "/tmp" , null);

			
			//  Test scanning
    	}
    	catch (Exception e) {
    		System.out.println("ERROR: " + e.getMessage() ) ;
    		e.printStackTrace();
    		
    	}

	}

	@Test
	public void keystoreTest() throws Exception {
		boolean thrown = false;
		KeyStoreFactory ksf = new KeyStoreFactory(dataSource);
		KeyStoreInt  keyStore =  ksf.getKeyStore("1");

		ConnectionKeyInt theKey = null;

		theKey = keyStore.getKey();
		assertThat(theKey).isNull();


		theKey  = new BaseConnKey();
		theKey.setKey("KEY");
		theKey.setData("DATA");

		keyStore.setKey(theKey);
		theKey = keyStore.getKey();
		assertThat(theKey.getData()).isEqualTo("DATA");
		assertThat(theKey.getKey()).isEqualTo("KEY");

		//   Reload key Store
		keyStore =  ksf.getKeyStore("1");
		theKey = keyStore.getKey();
		assertThat(theKey.getData()).isEqualTo("DATA");
		assertThat(theKey.getKey()).isEqualTo("KEY");

		keyStore.deleteKey();

		//   Reload key Store
		keyStore =  ksf.getKeyStore("1");
		theKey = keyStore.getKey();
		assertThat(theKey).isNull();
	}



}

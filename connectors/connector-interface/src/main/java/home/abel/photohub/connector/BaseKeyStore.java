package home.abel.photohub.connector;

import home.abel.photohub.connector.prototype.ConnectionKeyInt;
import home.abel.photohub.connector.prototype.KeyStoreInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


/**
 *
 *   Load, insert abd update key in database accessed thought DataSource.
 *   Expected that DataSource has table named has following definition:
 *
 *         CREATE TABLE DATA_STORE (
 *            SITEID BIGINT,
 *            TOKEN VARCHAR(255),
 *            DATA VARCHAR(1024),
 *            PRIMARY KEY (SITEID)
 *         );
 *
 */
public class BaseKeyStore implements KeyStoreInt {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected  DataSource dataSource = null;
    protected  String siteId = null;
    protected ConnectionKeyInt currentToken = null;


    public BaseKeyStore(String siteId, DataSource dataSource ) {
        this.siteId = siteId;
        this.dataSource = dataSource;
    }

    @Override
    public ConnectionKeyInt getKey() throws SQLException {

        Statement stmt = null;
        ResultSet rs = null;
        Connection conn = null;

        if (currentToken == null ) {
            try {
                String sqlStr = "SELECT TOKEN, DATA FROM DATA_STORE WHERE SITEID = " + siteId;
                logger.trace("[getKey] Execute sql = " +sqlStr);

                conn = dataSource.getConnection();
                //conn.setAutoCommit(true);

                stmt = conn.createStatement();
                rs = stmt.executeQuery(sqlStr);
                if (rs.next()) {
                    currentToken = new BaseConnKey();
                    currentToken.setData(rs.getString("DATA"));
                    currentToken.setKey(rs.getString("TOKEN"));
                }
//                else {
//                    logger.debug("[getKey] Token not found.");
//                }
            }
            finally {
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException sqlEx) { } // ignore
                }
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (SQLException sqlEx) { } // ignore
                }
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (SQLException sqlEx) { } // ignore
                }
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException sqlEx) { } // ignore
                }
            }
        }
        return currentToken;
    }


    @Override
    public void setKey(ConnectionKeyInt theKey) throws Exception {

        ResultSet rs = null;
        boolean found = false;

        try (Connection conn = dataSource.getConnection()) {
            //conn.setAutoCommit(true);

            String sqlStr = "SELECT TOKEN, DATA FROM DATA_STORE WHERE SITEID =" + siteId;
            logger.trace("[setKey] Execute sql = " +sqlStr);

            try(Statement stmt = conn.createStatement()) {
                found = !stmt.execute(sqlStr);
            }

            if (!found) {
                create(theKey,conn);
            } else {
                save(theKey,conn);
            }
        }
    }





    @Override
    public void deleteKey() throws SQLException {

        try (Connection conn = dataSource.getConnection()) {
            String SQLStr = "DELETE FROM DATA_STORE WHERE SITEID=" + siteId;
            executeSQL(SQLStr,conn);
        }
        currentToken = null;
    }


    protected void create(ConnectionKeyInt token, Connection conn) throws SQLException {
        String sqlStr = "INSERT INTO DATA_STORE (SITEID, TOKEN, DATA) VALUES (" +
                siteId + "," +
                "'" + token.getKey() + "'," +
                (token.getData() == null ? "NULL" : "'" + token.getData() + "'") +
                ") ";
        executeSQL(sqlStr,conn);
        currentToken = token;
    }

    protected void save(ConnectionKeyInt token, Connection conn) throws SQLException {
        String sqlStr = "UPDATE DATA_STORE SET " +
                "TOKEN = '" + token.getKey() + "', " +
                "DATA = " + (token.getData() == null ? "NULL" : "'" + token.getData() + "'") + " " +
                "WHERE SITEID=" + siteId;
        executeSQL(sqlStr,conn);
        currentToken = token;
    }


    protected int executeSQL(String sqlStr, Connection conn) throws SQLException {
        int rows = 0;
        logger.trace("[executeSQL] Execute sql = " + sqlStr);

        try (Statement stmt = conn.createStatement()) {
            rows = stmt.executeUpdate(sqlStr);
        }
        catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            throw ex;
        }
        logger.trace("[executeSQL] Processed rows  = " + rows);
        return rows;
    }

}

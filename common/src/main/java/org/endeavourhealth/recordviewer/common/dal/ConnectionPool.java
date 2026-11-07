package org.endeavourhealth.recordviewer.common.dal;

import com.fasterxml.jackson.databind.JsonNode;
import org.endeavourhealth.common.cache.GenericCache;
import org.endeavourhealth.common.config.ConfigManager;
import org.endeavourhealth.common.utility.MetricsHelper;
import org.endeavourhealth.core.database.dal.usermanager.caching.ProjectCache;
import org.endeavourhealth.core.database.rdbms.datasharingmanager.models.ProjectEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConnectionPool extends GenericCache<Connection> {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectionPool.class);
    private static final int VALID_TIMEOUT = 5;
    private static ConnectionPool instance = null;

    public static String configAppID;
    public static String configID;



    public static ConnectionPool getInstance() {
        if (instance == null)
            instance = new ConnectionPool();

        return instance;
    }

    @Override
    protected boolean isValid(Connection connection) {
        try {
            if (connection == null)
                return false;

            if (connection.isValid(VALID_TIMEOUT)) {
                connection.setAutoCommit(true);
                return true;
            }

            decSize();

            if (!connection.isClosed())
                connection.close();

            return false;
        } catch (SQLException e) {
            LOG.error("Error validating/cleaning up connection", e);
            return false;
        }
    }

    @Override
    protected Connection create() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            JsonNode json = ConfigManager.getConfigurationAsJson(configID);
            String url = json.get("url").asText();
            JsonNode credentials = json;
            if (null == json.get("username") && null != json.get("nested_config")){
                credentials = ConfigManager.getConfigurationAsJson(json.get("nested_config").asText());
            }
            String user = credentials.get("username").asText();
            String pass = credentials.get("password").asText();
            String driver = json.get("class") == null ? null : json.get("class").asText();

            if (driver != null && !driver.isEmpty())
                Class.forName(driver);

            Properties props = new Properties();

            props.setProperty("user", user);
            props.setProperty("password", pass);
            Connection connection = DriverManager.getConnection(url, props);    // NOSONAR

            incSize();
            return connection;
        } catch (Exception e) {
            LOG.error(e.getMessage());
            return null;
        }
    }

    @Override
    public Connection pop() {
        Connection conn = super.pop();
        incUse();
        return conn;
    }

    public Connection pop(ProjectEntity project) {
         configAppID = "global";
        configID = "record-viewer";
        if(null != project && !project.getConfigName().isEmpty() &&
                null != project.getConfigName()) {

            configID = "db_subscriber:" + project.getConfigName();
            Connection conn = super.pop();
            incUse();
            return conn;

        } else {
            return pop();
        }
    }


    @Override
    public void push(Connection conn) {
        decUse();
        super.push(conn);
    }

    private void incSize() {
        MetricsHelper.recordCounter("ConnectionPool.Size").inc();
    }

    private void decSize() {
        MetricsHelper.recordCounter("ConnectionPool.Size").dec();
    }

    private void incUse() {
        MetricsHelper.recordCounter("ConnectionPool.InUse").inc();
    }

    private void decUse() {
        MetricsHelper.recordCounter("ConnectionPool.InUse").dec();
    }

    private static String getDatabaseURLFromJson(String url) {

        try {
            int indexPrefix = url.indexOf("//");
            if (indexPrefix == -1) {
                LOG.error("Failed to find // in url " + url);

            } else {
                return url.substring( 5);
            }
            return null;

        } catch (Exception ex) {
            LOG.error("Failed to parse database url " + url, ex);
            return null;
        }
    }
}

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import javax.swing.JOptionPane;

/**
 * Enterprise Database Connection & Configuration Manager.
 * 
 * Supports explicit database selection (SQLite / MySQL) via 'database.properties'.
 * Strictly enforces zero-trust data safety: never silently switches databases
 * upon authentication or network failure to avoid exposing disparate datasets.
 */
public class javaconnect {

    private static final String CONFIG_FILE = "database.properties";
    private static String activeDbType = "UNKNOWN";

    public static String getActiveDbType() {
        return activeDbType;
    }

    /**
     * Obtains the primary database connection as explicitly configured in database.properties.
     */
    public static Connection connerDb() {
        Properties props = loadConfiguration();
        String dbType = props.getProperty("db.type", "sqlite").trim().toLowerCase();

        if ("mysql".equals(dbType)) {
            return connectMySQL(props);
        } else {
            return connectSQLite(props.getProperty("db.sqlite.file", "student_database.sqlite").trim());
        }
    }

    /**
     * Obtains an isolated test connection for automated unit/integration test suites.
     * Prevents testing operations from altering or risking production databases.
     */
    public static Connection getTestConnection(String testDbName) {
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + testDbName);
            DatabaseBootstrap.bootstrap(conn);
            return conn;
        } catch (Exception e) {
            if (conn != null) {
                try { conn.close(); } catch (Exception ignored) {}
            }
            System.err.println("[javaconnect] Error opening test database: " + e.getMessage());
            return null;
        }
    }

    public static Connection getTestConnection(File testDbFile) {
        if (testDbFile == null) return null;
        return getTestConnection(testDbFile.getAbsolutePath());
    }

    private static Connection connectMySQL(Properties props) {
        String host = props.getProperty("db.mysql.host", "localhost").trim();
        String port = props.getProperty("db.mysql.port", "3306").trim();
        String db = props.getProperty("db.mysql.database", "student_database").trim();
        String user = props.getProperty("db.mysql.username", "root").trim();
        String pass = props.getProperty("db.mysql.password", "");
        String useSSL = props.getProperty("db.mysql.useSSL", "false").trim();

        String url = String.format("jdbc:mysql://%s:%s/%s?zeroDateTimeBehavior=convertToNull&useSSL=%s",
                host, port, db, useSSL);

        Connection conn = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(url, user, pass);
            DatabaseBootstrap.bootstrap(conn);
            activeDbType = "MySQL (" + host + ":" + port + "/" + db + ")";
            System.out.println("[javaconnect] Connected successfully to configured MySQL database.");
            return conn;
        } catch (Exception e) {
            if (conn != null) {
                try { conn.close(); } catch (Exception ignored) {}
            }
            activeDbType = "FAILED";
            String errorMsg = String.format(
                "Unable to connect to or initialize the configured MySQL database on %s:%s/%s.\n" +
                "Error: %s\n\n" +
                "The system will NOT silently switch databases to prevent data corruption or exposure.\n" +
                "Please verify your MySQL server status and configuration in 'database.properties'.",
                host, port, db, e.getMessage()
            );
            System.err.println("[javaconnect] " + errorMsg);
            try {
                JOptionPane.showMessageDialog(null, errorMsg, "Database Connection Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ignored) {}
            return null;
        }
    }

    private static Connection connectSQLite(String fileName) {
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + fileName);
            DatabaseBootstrap.bootstrap(conn);
            activeDbType = "SQLite (" + fileName + ")";
            System.out.println("[javaconnect] Connected to SQLite database: " + fileName);
            return conn;
        } catch (Exception e) {
            if (conn != null) {
                try { conn.close(); } catch (Exception ignored) {}
            }
            activeDbType = "FAILED";
            String errorMsg = "Unable to connect to or bootstrap SQLite database '" + fileName + "': " + e.getMessage();
            System.err.println("[javaconnect] " + errorMsg);
            try {
                JOptionPane.showMessageDialog(null, errorMsg, "Database Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ignored) {}
            return null;
        }
    }

    private static Properties loadConfiguration() {
        Properties props = new Properties();
        File file = new File(CONFIG_FILE);

        if (file.exists()) {
            try (InputStream in = new FileInputStream(file)) {
                props.load(in);
                return props;
            } catch (Exception e) {
                System.err.println("[javaconnect] Warning: Could not read " + CONFIG_FILE + ": " + e.getMessage());
            }
        }

        // Set secure defaults and create the configuration file
        props.setProperty("db.type", "sqlite");
        props.setProperty("db.sqlite.file", "student_database.sqlite");
        props.setProperty("db.mysql.host", "localhost");
        props.setProperty("db.mysql.port", "3306");
        props.setProperty("db.mysql.database", "student_database");
        props.setProperty("db.mysql.username", "root");
        props.setProperty("db.mysql.password", "");
        props.setProperty("db.mysql.useSSL", "false");

        try (FileOutputStream out = new FileOutputStream(file)) {
            props.store(out, "School Management System Database Configuration");
        } catch (Exception e) {
            System.err.println("[javaconnect] Warning: Could not write default " + CONFIG_FILE + ": " + e.getMessage());
        }

        return props;
    }
}
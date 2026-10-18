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
        String host = getPropOrEnv(props, "db.mysql.host", "SMS_MYSQL_HOST", "localhost").trim();
        String portStr = getPropOrEnv(props, "db.mysql.port", "SMS_MYSQL_PORT", "3306").trim();
        String db = getPropOrEnv(props, "db.mysql.database", "SMS_MYSQL_DATABASE", "student_database").trim();
        String user = getPropOrEnv(props, "db.mysql.username", "SMS_MYSQL_USER", "").trim();
        String pass = getPropOrEnv(props, "db.mysql.password", "SMS_MYSQL_PASSWORD", "");
        String useSSL = getPropOrEnv(props, "db.mysql.useSSL", "SMS_MYSQL_USE_SSL", "true").trim();

        // Strict configuration validation
        if (host.isEmpty() || db.isEmpty()) {
            activeDbType = "FAILED";
            String err = "MySQL configuration error: Database host and database name must be specified.";
            System.err.println("[javaconnect] " + err);
            showError("Database Configuration Error", err);
            return null;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
            if (port < 1 || port > 65535) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            activeDbType = "FAILED";
            String err = "MySQL configuration error: Invalid port number '" + portStr + "'. Port must be between 1 and 65535.";
            System.err.println("[javaconnect] " + err);
            showError("Database Configuration Error", err);
            return null;
        }

        if (user.isEmpty()) {
            activeDbType = "FAILED";
            String err = "MySQL configuration error: Database username is missing.\n" +
                         "Please configure 'db.mysql.username' in database.properties or set environment variable 'SMS_MYSQL_USER'.";
            System.err.println("[javaconnect] " + err);
            showError("Database Authentication Error", err);
            return null;
        }

        // Forbid insecure root with blank password in network deployments
        if (user.equalsIgnoreCase("root") && pass.isEmpty()) {
            activeDbType = "FAILED";
            String err = "MySQL security violation: Connecting as 'root' with an empty password is prohibited.\n" +
                         "Please configure a dedicated, least-privilege user account with an authentic password.";
            System.err.println("[javaconnect] " + err);
            showError("Database Security Violation", err);
            return null;
        }

        String sslParams = "true".equalsIgnoreCase(useSSL) ? "useSSL=true&requireSSL=true" : "useSSL=false";
        String url = String.format("jdbc:mysql://%s:%d/%s?zeroDateTimeBehavior=convertToNull&%s",
                host, port, db, sslParams);

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
                "Unable to connect to or initialize the configured MySQL database on %s:%d/%s.\n" +
                "Error: %s\n\n" +
                "Zero-Trust Policy: The system will NOT silently switch to another database to prevent data exposure or corruption.\n" +
                "Please verify your MySQL server status and configuration in 'database.properties' or environment variables.",
                host, port, db, e.getMessage()
            );
            System.err.println("[javaconnect] " + errorMsg);
            showError("Database Connection Error", errorMsg);
            return null;
        }
    }

    private static void showError(String title, String message) {
        try {
            JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
        } catch (Exception ignored) {}
    }

    private static String getPropOrEnv(Properties props, String propKey, String envKey, String defaultVal) {
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.trim().isEmpty()) {
            return envVal.trim();
        }
        return props.getProperty(propKey, defaultVal);
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
            showError("Database Error", errorMsg);
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

        // Set secure, non-secret defaults and write configuration template if absent
        props.setProperty("db.type", "sqlite");
        props.setProperty("db.sqlite.file", "student_database.sqlite");
        props.setProperty("db.mysql.host", "localhost");
        props.setProperty("db.mysql.port", "3306");
        props.setProperty("db.mysql.database", "student_database");
        props.setProperty("db.mysql.username", "");
        props.setProperty("db.mysql.password", "");
        props.setProperty("db.mysql.useSSL", "true");

        try (FileOutputStream out = new FileOutputStream(file)) {
            props.store(out, "School Management System Database Configuration");
        } catch (Exception e) {
            System.err.println("[javaconnect] Warning: Could not write default " + CONFIG_FILE + ": " + e.getMessage());
        }

        return props;
    }
}
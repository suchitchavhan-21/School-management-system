import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import javax.swing.JOptionPane;

/**
 * Resilient Database Connection Manager.
 * Preserves the original 'connerDb()' signature for 100% backward compatibility.
 * 
 * Auto-detects local MySQL on port 3306; if unavailable, smoothly falls back
 * to embedded SQLite ('student_database.sqlite') using the bundled sqlite-jdbc driver.
 * Automatically runs DatabaseBootstrap to ensure all tables exist.
 */
public class javaconnect {

    private static String activeDbType = "UNKNOWN";

    public static String getActiveDbType() {
        return activeDbType;
    }

    public static Connection connerDb() {
        Connection conn = null;

        // Step 1: Check if MySQL is running on localhost:3306 with a fast socket probe (300ms)
        boolean mysqlReachable = isPortReachable("localhost", 3306, 300);

        if (mysqlReachable) {
            try {
                Class.forName("com.mysql.jdbc.Driver");
                conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/student_database?zeroDateTimeBehavior=convertToNull&useSSL=false",
                    "root",
                    ""
                );
                activeDbType = "MySQL";
                System.out.println("[javaconnect] Connected to MySQL database on localhost:3306.");
                DatabaseBootstrap.bootstrap(conn);
                return conn;
            } catch (Exception e) {
                System.out.println("[javaconnect] MySQL reachable on port 3306 but authentication or database failed: " + e.getMessage());
                System.out.println("[javaconnect] Falling back to embedded SQLite...");
            }
        } else {
            System.out.println("[javaconnect] Local MySQL server not active on port 3306.");
            System.out.println("[javaconnect] Activating embedded SQLite storage engine...");
        }

        // Step 2: Fallback to embedded SQLite
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:student_database.sqlite");
            activeDbType = "SQLite (Embedded)";
            System.out.println("[javaconnect] Connected to embedded SQLite database ('student_database.sqlite').");
            DatabaseBootstrap.bootstrap(conn);
            return conn;
        } catch (Exception e) {
            System.err.println("[javaconnect] Fatal database initialization error: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database Connection Problem: " + e.getMessage());
        }

        return null;
    }

    /**
     * Fast non-blocking socket test to check if a port is listening.
     */
    private static boolean isPortReachable(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
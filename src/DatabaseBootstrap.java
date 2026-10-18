import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Handles database bootstrapping and schema initialization.
 * Automatically verifies and creates required tables without hardcoding default production credentials or sample records.
 * Propagates initialization failures to prevent application from proceeding with incomplete schemas.
 */
public class DatabaseBootstrap {

    public static boolean initialize(Connection conn) throws SQLException {
        return bootstrap(conn);
    }

    public static boolean bootstrap(Connection conn) throws SQLException {
        if (conn == null || conn.isClosed()) {
            throw new SQLException("[DatabaseBootstrap] Cannot bootstrap schema: database connection is null or closed.");
        }

        System.out.println("[DatabaseBootstrap] Ensuring database tables exist...");

        boolean isMySQL = false;
        try {
            DatabaseMetaData meta = conn.getMetaData();
            if (meta != null && meta.getDatabaseProductName() != null) {
                isMySQL = meta.getDatabaseProductName().toLowerCase().contains("mysql");
            }
        } catch (Exception ignored) {}

        try (Statement stmt = conn.createStatement()) {
            // 1. pass table (user credentials)
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS pass (" +
                    "Username VARCHAR(50) NOT NULL PRIMARY KEY, " +
                    "Password VARCHAR(255) NOT NULL" +
                    ")");

            // 2. admin table (admin authorization master key / password)
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS admin (" +
                    "Password VARCHAR(255) NOT NULL" +
                    ")");

            // 3. student_info table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS student_info (" +
                    "Roll INTEGER PRIMARY KEY, " +
                    "First_name VARCHAR(50) NOT NULL, " +
                    "Last_name VARCHAR(50) NOT NULL, " +
                    "Class INTEGER NOT NULL, " +
                    "Year INTEGER NOT NULL, " +
                    "Birth_date VARCHAR(50) NOT NULL, " +
                    "Mobile_no VARCHAR(50) NOT NULL, " +
                    "Sex VARCHAR(20) NOT NULL, " +
                    "permanent VARCHAR(100) NOT NULL, " +
                    "Present VARCHAR(100) NOT NULL" +
                    ")");

            // 4. teacher_info table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS teacher_info (" +
                    "Teachers_id INTEGER PRIMARY KEY, " +
                    "First_name VARCHAR(50) NOT NULL, " +
                    "Last_name VARCHAR(50) NOT NULL, " +
                    "Position VARCHAR(50) NOT NULL, " +
                    "Birth_day VARCHAR(50) NOT NULL, " +
                    "Mobile_no VARCHAR(50) NOT NULL, " +
                    "Sex VARCHAR(20) NOT NULL, " +
                    "Permanent VARCHAR(100) NOT NULL, " +
                    "Present VARCHAR(100) NOT NULL" +
                    ")");

            // 5. employee_info table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS employee_info (" +
                    "Employee_id INTEGER PRIMARY KEY, " +
                    "First_name VARCHAR(50) NOT NULL, " +
                    "Last_name VARCHAR(50) NOT NULL, " +
                    "Position VARCHAR(50) NOT NULL, " +
                    "Birth_day VARCHAR(50) NOT NULL, " +
                    "Mobile_no VARCHAR(50) NOT NULL, " +
                    "Sex VARCHAR(20) NOT NULL, " +
                    "Permanent VARCHAR(100) NOT NULL, " +
                    "Present VARCHAR(100) NOT NULL" +
                    ")");

            // 6. exam_marks table (Academic Examination & Grading Feature)
            String autoIncClause = isMySQL ? "INT PRIMARY KEY AUTO_INCREMENT" : "INTEGER PRIMARY KEY AUTOINCREMENT";
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS exam_marks (" +
                    "Mark_id " + autoIncClause + ", " +
                    "Roll INTEGER NOT NULL, " +
                    "Student_name VARCHAR(100) NOT NULL, " +
                    "Class INTEGER NOT NULL, " +
                    "Exam_term VARCHAR(50) NOT NULL, " +
                    "Exam_year INTEGER NOT NULL, " +
                    "English REAL NOT NULL, " +
                    "Math REAL NOT NULL, " +
                    "Science REAL NOT NULL, " +
                    "Social_studies REAL NOT NULL, " +
                    "ICT REAL NOT NULL, " +
                    "Total_marks REAL NOT NULL, " +
                    "Percentage REAL NOT NULL, " +
                    "Grade VARCHAR(10) NOT NULL, " +
                    "GPA REAL NOT NULL, " +
                    "Remarks VARCHAR(50) NOT NULL, " +
                    "Created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE(Roll, Exam_term, Exam_year)" +
                    ")");

            // Rigorously verify that all required tables exist and can be queried
            String[] requiredTables = {"pass", "admin", "student_info", "teacher_info", "employee_info", "exam_marks"};
            for (String table : requiredTables) {
                try (Statement testStmt = conn.createStatement()) {
                    testStmt.execute("SELECT 1 FROM " + table + " LIMIT 1");
                } catch (SQLException e) {
                    throw new SQLException("[DatabaseBootstrap] Verification failed: table '" + table + "' is missing or inaccessible.", e);
                }
            }

            System.out.println("[DatabaseBootstrap] Database schema check completed successfully.");
            return true;
        } catch (SQLException e) {
            System.err.println("[DatabaseBootstrap] Critical error during bootstrap: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("[DatabaseBootstrap] Critical error during bootstrap: " + e.getMessage());
            throw new SQLException("[DatabaseBootstrap] Unexpected error during bootstrap: " + e.getMessage(), e);
        }
    }
}

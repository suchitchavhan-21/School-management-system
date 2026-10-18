import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;

/**
 * Handles database bootstrapping and schema initialization.
 * Automatically verifies and creates required tables without hardcoding default production credentials or sample records.
 */
public class DatabaseBootstrap {

    public static void initialize(Connection conn) {
        bootstrap(conn);
    }

    public static void bootstrap(Connection conn) {
        if (conn == null) {
            System.err.println("[DatabaseBootstrap] Connection is null. Skipping bootstrap.");
            return;
        }

        try (Statement stmt = conn.createStatement()) {
            System.out.println("[DatabaseBootstrap] Ensuring database tables exist...");

            boolean isMySQL = false;
            try {
                DatabaseMetaData meta = conn.getMetaData();
                if (meta != null && meta.getDatabaseProductName() != null) {
                    isMySQL = meta.getDatabaseProductName().toLowerCase().contains("mysql");
                }
            } catch (Exception ignored) {}

            // 1. pass table (user credentials)
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS pass (" +
                    "Username VARCHAR(50) NOT NULL PRIMARY KEY, " +
                    "Password VARCHAR(255) NOT NULL" +
                    ")");

            // 2. admin table (admin authorization PIN / password)
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

            System.out.println("[DatabaseBootstrap] Database schema check completed successfully.");

        } catch (Exception e) {
            System.err.println("[DatabaseBootstrap] Error during bootstrap: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

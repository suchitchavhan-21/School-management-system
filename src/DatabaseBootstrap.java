import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
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

            // 7. attendance table (Student Attendance Tracking Feature)
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS attendance (" +
                    "Attendance_id " + autoIncClause + ", " +
                    "Roll INTEGER NOT NULL, " +
                    "Student_name VARCHAR(100) NOT NULL, " +
                    "Class INTEGER NOT NULL, " +
                    "Attendance_date VARCHAR(20) NOT NULL, " +
                    "Status VARCHAR(20) NOT NULL, " +
                    "Remarks VARCHAR(255), " +
                    "Created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE(Roll, Attendance_date)" +
                    ")");

            // Migration check: Ensure existing MySQL installations have Remarks widened to VARCHAR(255)
            if (isMySQL) {
                int currentSize = getColumnSize(conn, "attendance", "Remarks");
                // Only execute ALTER TABLE if existing column is positively identified as narrower than 255
                // (e.g. legacy installations initialized with VARCHAR(100))
                if (currentSize > 0 && currentSize < 255) {
                    try {
                        stmt.executeUpdate("ALTER TABLE attendance MODIFY COLUMN Remarks VARCHAR(255) DEFAULT NULL");
                    } catch (SQLException alterEx) {
                        int postAlterSize = getColumnSize(conn, "attendance", "Remarks");
                        if (postAlterSize < 255) {
                            throw new SQLException("[DatabaseBootstrap] Migration failed: Unable to widen attendance.Remarks to VARCHAR(255): "
                                    + alterEx.getMessage(), alterEx);
                        }
                    }
                }

                // Strictly verify that attendance.Remarks supports at least 255 characters
                int verifiedSize = getColumnSize(conn, "attendance", "Remarks");
                if (verifiedSize > 0 && verifiedSize < 255) {
                    throw new SQLException("[DatabaseBootstrap] Migration verification failed: attendance.Remarks column size is "
                            + verifiedSize + ", but at least 255 characters are required.");
                }
            }

            // Rigorously verify that all required tables exist and can be queried
            String[] requiredTables = {"pass", "admin", "student_info", "teacher_info", "employee_info", "exam_marks", "attendance"};
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

    /**
     * Inspects column size in database metadata across case variations.
     * Returns the defined column size, or -1 if the column is not found or metadata is inaccessible.
     */
    public static int getColumnSize(Connection conn, String tableName, String columnName) {
        if (conn == null || tableName == null || columnName == null) return -1;
        try {
            DatabaseMetaData meta = conn.getMetaData();
            String catalog = conn.getCatalog();
            String[] tables = {tableName, tableName.toLowerCase(), tableName.toUpperCase()};
            String[] columns = {columnName, columnName.toLowerCase(), columnName.toUpperCase()};
            for (String t : tables) {
                for (String c : columns) {
                    try (ResultSet rs = meta.getColumns(catalog, null, t, c)) {
                        if (rs.next()) {
                            return rs.getInt("COLUMN_SIZE");
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return -1;
    }
}

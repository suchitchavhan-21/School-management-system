import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Handles database bootstrapping and schema initialization.
 * Automatically creates required tables and initial seed data if not present.
 */
public class DatabaseBootstrap {

    public static void bootstrap(Connection conn) {
        if (conn == null) {
            System.err.println("[DatabaseBootstrap] Connection is null. Skipping bootstrap.");
            return;
        }

        try (Statement stmt = conn.createStatement()) {
            System.out.println("[DatabaseBootstrap] Ensuring database tables exist...");

            // 1. pass table (user credentials)
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS pass (" +
                    "Username VARCHAR(50) NOT NULL, " +
                    "Password VARCHAR(50) NOT NULL" +
                    ")");

            // Seed default users if empty
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM pass")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.executeUpdate("INSERT INTO pass (Username, Password) VALUES ('school', 'school')");
                    stmt.executeUpdate("INSERT INTO pass (Username, Password) VALUES ('admin', 'admin')");
                    System.out.println("[DatabaseBootstrap] Seeded default credentials in 'pass' table.");
                }
            }

            // 2. admin table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS admin (" +
                    "Password VARCHAR(50) NOT NULL" +
                    ")");

            // Seed default admin password if empty
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM admin")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.executeUpdate("INSERT INTO admin (Password) VALUES ('123')");
                    System.out.println("[DatabaseBootstrap] Seeded default admin password in 'admin' table.");
                }
            }

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

            // Seed sample student if empty
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM student_info")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.executeUpdate("INSERT INTO student_info " +
                            "(Roll, First_name, Last_name, Class, Year, Birth_date, Mobile_no, Sex, permanent, Present) " +
                            "VALUES (15001, 'Subrata', 'Paul', 10, 2024, '2008-11-22', '01738061325', 'male', 'Nandigram', 'Bogra')");
                    stmt.executeUpdate("INSERT INTO student_info " +
                            "(Roll, First_name, Last_name, Class, Year, Birth_date, Mobile_no, Sex, permanent, Present) " +
                            "VALUES (15002, 'Uttam', 'Paul', 10, 2024, '2008-11-02', '01738820941', 'male', 'Nandigram', 'Bogra')");
                    System.out.println("[DatabaseBootstrap] Seeded sample students in 'student_info' table.");
                }
            }

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
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS exam_marks (" +
                    "Mark_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
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

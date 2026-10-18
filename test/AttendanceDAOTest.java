import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Isolated unit and integration test suite for AttendanceDAO and student attendance tracking.
 * Operates on unique temporary SQLite test databases and ensures full cleanup.
 */
public class AttendanceDAOTest {

    private int testsRun = 0;
    private int testsPassed = 0;
    private int testsFailed = 0;
    private int testsSkipped = 0;

    private File tempDbFile;

    public int getTestsRun() { return testsRun; }
    public int getTestsPassed() { return testsPassed; }
    public int getTestsFailed() { return testsFailed; }
    public int getTestsSkipped() { return testsSkipped; }

    private void assertTrue(String message, boolean condition) {
        testsRun++;
        if (condition) {
            testsPassed++;
            System.out.println("  [PASS] " + message);
        } else {
            testsFailed++;
            System.err.println("  [FAIL] " + message);
        }
    }

    private void assertEquals(String message, Object expected, Object actual) {
        testsRun++;
        if (expected == null && actual == null) {
            testsPassed++;
            System.out.println("  [PASS] " + message);
        } else if (expected != null && expected.equals(actual)) {
            testsPassed++;
            System.out.println("  [PASS] " + message);
        } else {
            testsFailed++;
            System.err.printf("  [FAIL] %s - Expected: %s, Got: %s\n", message, expected, actual);
        }
    }

    public boolean runAllTests() {
        System.out.println("==================================================");
        System.out.println("Running AttendanceDAOTest (Isolated Test DB)...");
        System.out.println("==================================================");

        try {
            tempDbFile = File.createTempFile("sms_attendance_test_", ".sqlite");
            tempDbFile.deleteOnExit();
        } catch (IOException e) {
            System.err.println("  [FAIL] Unable to create temp test database: " + e.getMessage());
            testsFailed++;
            return false;
        }

        Connection conn = javaconnect.getTestConnection(tempDbFile);
        if (conn == null) {
            System.err.println("  [FAIL] Could not connect to isolated test database.");
            testsFailed++;
            cleanup();
            return false;
        }

        try {
            testAttendanceOperations(conn);
        } catch (Exception e) {
            System.err.println("  [FAIL] Unexpected exception in AttendanceDAOTest: " + e.getMessage());
            e.printStackTrace();
            testsFailed++;
        } finally {
            try {
                conn.close();
            } catch (Exception ignored) {}
            cleanup();
        }

        System.out.printf("AttendanceDAOTest Summary: %d Passed, %d Failed, %d Skipped out of %d tests.\n\n",
                testsPassed, testsFailed, testsSkipped, testsRun);
        return testsFailed == 0 && testsSkipped == 0 && testsRun > 0;
    }

    private void cleanup() {
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    private void testAttendanceOperations(Connection conn) throws SQLException {
        // 1. Seed authoritative student in student_info
        String seedStudent = "INSERT INTO student_info (Roll, First_name, Last_name, Class, Year, Birth_date, Mobile_no, Sex, permanent, Present) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = conn.prepareStatement(seedStudent)) {
            pst.setInt(1, 201);
            pst.setString(2, "Daniel");
            pst.setString(3, "Miller");
            pst.setInt(4, 9);
            pst.setInt(5, 2026);
            pst.setString(6, "2009-03-22");
            pst.setString(7, "0987654321");
            pst.setString(8, "male");
            pst.setString(9, "742 Evergreen Terr");
            pst.setString(10, "742 Evergreen Terr");
            assertTrue("Seed student record in student_info", pst.executeUpdate() > 0);
        }

        // 2. Insert initial attendance record
        AttendanceDAO.AttendanceRecord rec1 = new AttendanceDAO.AttendanceRecord(
                201, "Daniel Miller", 9, "2026-10-01", "Present", "On time");
        boolean inserted = AttendanceDAO.insert(conn, rec1);
        assertTrue("Insert attendance record succeeds", inserted);

        // 3. Find record by Roll and Date
        AttendanceDAO.AttendanceRecord fetched = AttendanceDAO.findByRollAndDate(conn, 201, "2026-10-01");
        assertTrue("Fetched attendance record is not null", fetched != null);
        assertEquals("Roll matches", 201, fetched.getRoll());
        assertEquals("Student name matches", "Daniel Miller", fetched.getStudentName());
        assertEquals("Class matches", 9, fetched.getStudentClass());
        assertEquals("Date matches", "2026-10-01", fetched.getAttendanceDate());
        assertEquals("Status is Present", "Present", fetched.getStatus());
        assertEquals("Remarks match", "On time", fetched.getRemarks());

        // 4. Duplicate (Roll, Attendance_date) direct insert rejected by UNIQUE constraint
        boolean duplicateInsertFailed = false;
        try {
            AttendanceDAO.insert(conn, rec1);
        } catch (SQLException e) {
            duplicateInsertFailed = true;
        }
        assertTrue("Duplicate (Roll, Attendance_date) direct insert rejected by constraint", duplicateInsertFailed);

        // 5. saveOrUpdate updates existing record safely without constraint violation
        rec1.setStatus("Late");
        rec1.setRemarks("Bus delayed");
        boolean updated = AttendanceDAO.saveOrUpdate(conn, rec1);
        assertTrue("saveOrUpdate updates existing date record safely", updated);

        AttendanceDAO.AttendanceRecord updatedRec = AttendanceDAO.findByRollAndDate(conn, 201, "2026-10-01");
        assertEquals("Updated status is Late", "Late", updatedRec.getStatus());
        assertEquals("Updated remarks is Bus delayed", "Bus delayed", updatedRec.getRemarks());

        // 6. Seed multiple attendance days for summary analytics
        AttendanceDAO.insert(conn, new AttendanceDAO.AttendanceRecord(201, "Daniel Miller", 9, "2026-10-02", "Present", ""));
        AttendanceDAO.insert(conn, new AttendanceDAO.AttendanceRecord(201, "Daniel Miller", 9, "2026-10-03", "Present", ""));
        AttendanceDAO.insert(conn, new AttendanceDAO.AttendanceRecord(201, "Daniel Miller", 9, "2026-10-04", "Present", ""));
        AttendanceDAO.insert(conn, new AttendanceDAO.AttendanceRecord(201, "Daniel Miller", 9, "2026-10-05", "Absent", "Unexcused"));
        AttendanceDAO.insert(conn, new AttendanceDAO.AttendanceRecord(201, "Daniel Miller", 9, "2026-10-06", "Excused", "Doctor appointment"));

        // Days: 2026-10-01 (Late), 10-02 (Present), 10-03 (Present), 10-04 (Present), 10-05 (Absent), 10-06 (Excused)
        // Total: 6 days. Present: 3. Late: 1. Absent: 1. Excused: 1.
        // Overall % = (Present + Late) / Total * 100 = 4 / 6 * 100 = 66.67%
        AttendanceDAO.AttendanceSummary summary = AttendanceDAO.calculateSummary(conn, 201);
        assertEquals("Summary total tracked days is 6", 6, summary.getTotalDays());
        assertEquals("Summary present count is 3", 3, summary.getPresentDays());
        assertEquals("Summary late count is 1", 1, summary.getLateDays());
        assertEquals("Summary absent count is 1", 1, summary.getAbsentDays());
        assertEquals("Summary excused count is 1", 1, summary.getExcusedDays());
        assertEquals("Summary attendance percentage is 66.67%", 66.67, summary.getAttendancePercentage());

        // 7. Find by Roll returns all 6 records
        List<AttendanceDAO.AttendanceRecord> studentRecords = AttendanceDAO.findByRoll(conn, 201);
        assertEquals("findByRoll returns 6 records", 6, studentRecords.size());

        // 8. Filter by Class and Date
        List<AttendanceDAO.AttendanceRecord> classDateRecords = AttendanceDAO.findByClassAndDate(conn, 9, "2026-10-02");
        assertEquals("findByClassAndDate returns 1 record", 1, classDateRecords.size());

        // 9. FindAll returns non-empty list
        List<AttendanceDAO.AttendanceRecord> allRecords = AttendanceDAO.findAll(conn);
        assertEquals("findAll returns 6 total records", 6, allRecords.size());

        // 10. Delete record
        boolean deleted = AttendanceDAO.delete(conn, 201, "2026-10-06");
        assertTrue("Delete attendance record succeeds", deleted);
        assertTrue("Deleted record no longer exists", AttendanceDAO.findByRollAndDate(conn, 201, "2026-10-06") == null);
        assertEquals("Total records after delete is 5", 5, AttendanceDAO.findByRoll(conn, 201).size());
    }
}

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Integration Tests for ExamMarksDAO and Database Persistence.
 * Uses an isolated temporary test database to guarantee zero production pollution.
 */
public class ExamMarksDAOTest {

    private File tempDbFile;

    private int testsRun = 0;
    private int testsPassed = 0;
    private int testsFailed = 0;
    private int testsSkipped = 0;

    public static void main(String[] args) {
        ExamMarksDAOTest test = new ExamMarksDAOTest();
        boolean passed = test.runAllTests();
        System.exit(passed ? 0 : 1);
    }

    public boolean runAllTests() {
        System.out.println("==================================================");
        System.out.println("Running ExamMarksDAOTest (Isolated Test DB)...");
        System.out.println("==================================================");

        try {
            tempDbFile = File.createTempFile("sms_exam_marks_", ".sqlite");
            tempDbFile.deleteOnExit();
        } catch (Exception e) {
            System.err.println("  [FAIL] Unable to create unique temporary test database: " + e.getMessage());
            testsFailed++;
            return false;
        }

        Connection conn = javaconnect.getTestConnection(tempDbFile);

        if (conn == null) {
            System.err.println("  [FAIL] Unable to acquire isolated test database connection.");
            testsFailed++;
            cleanupTestDb();
            return false;
        }

        try {
            testStudentSeeding(conn);
            testCRUDOperations(conn);
            testDuplicateTermProtection(conn);
            testSearchAndFindAll(conn);
        } catch (Exception e) {
            System.err.println("  [FAIL] Unexpected exception during DAO test: " + e.getMessage());
            e.printStackTrace();
            testsFailed++;
        } finally {
            try {
                conn.close();
            } catch (Exception ignored) {}
            cleanupTestDb();
        }

        System.out.printf("ExamMarksDAOTest Summary: %d Passed, %d Failed, %d Skipped out of %d tests.\n\n",
                testsPassed, testsFailed, testsSkipped, testsRun);
        return testsFailed == 0 && testsSkipped == 0 && testsRun > 0;
    }

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

    private void cleanupTestDb() {
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    private void testStudentSeeding(Connection conn) throws SQLException {
        // Seed authoritative student in test database
        String sql = "INSERT INTO student_info (Roll, First_name, Last_name, Class, Year, Birth_date, Mobile_no, Sex, permanent, Present) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, 101);
            pst.setString(2, "Alice");
            pst.setString(3, "Smith");
            pst.setInt(4, 10);
            pst.setInt(5, 2026);
            pst.setString(6, "2008-01-15");
            pst.setString(7, "0123456789");
            pst.setString(8, "female");
            pst.setString(9, "Central St");
            pst.setString(10, "North Ave");
            int rows = pst.executeUpdate();
            assertTrue("Seed authoritative student record for test", rows == 1);
        }
    }

    private void testCRUDOperations(Connection conn) throws SQLException {
        int testRoll = 101;
        String testTerm = "Mid-Term";
        int testYear = 2026;

        // Clean any pre-existing test records
        ExamMarksDAO.delete(conn, testRoll, testTerm, testYear);

        // Prepare test mark
        GradeCalculator.GradeResult calc = GradeCalculator.calculate(85.0, 92.5, 78.0, 88.0, 95.0);
        ExamMarksDAO.ExamMark mark = new ExamMarksDAO.ExamMark(
                testRoll, "Alice Smith", 10, testTerm, testYear,
                85.0, 92.5, 78.0, 88.0, 95.0, calc
        );

        // 1. Insert
        boolean inserted = ExamMarksDAO.insert(conn, mark);
        assertTrue("Insert exam mark record", inserted);

        // 2. Read back & verify
        ExamMarksDAO.ExamMark fetched = ExamMarksDAO.findByRollTermYear(conn, testRoll, testTerm, testYear);
        assertTrue("Fetched mark is not null", fetched != null);
        if (fetched != null) {
            assertEquals("Fetched Roll matches", testRoll, fetched.roll);
            assertEquals("Fetched Student Name matches", "Alice Smith", fetched.studentName);
            assertEquals("Fetched Grade matches", "A+", fetched.grade);
            assertEquals("Fetched GPA matches", 5.0, fetched.gpa);
        }

        // 3. Update
        GradeCalculator.GradeResult updatedCalc = GradeCalculator.calculate(60.0, 65.0, 62.0, 68.0, 70.0);
        ExamMarksDAO.ExamMark updatedMark = new ExamMarksDAO.ExamMark(
                testRoll, "Alice Smith", 10, testTerm, testYear,
                60.0, 65.0, 62.0, 68.0, 70.0, updatedCalc
        );
        boolean updated = ExamMarksDAO.saveOrUpdate(conn, updatedMark);
        assertTrue("Save or update existing record", updated);

        ExamMarksDAO.ExamMark fetchedUpdated = ExamMarksDAO.findByRollTermYear(conn, testRoll, testTerm, testYear);
        assertTrue("Fetched updated mark is not null", fetchedUpdated != null);
        if (fetchedUpdated != null) {
            assertEquals("Updated Grade is A-", "A-", fetchedUpdated.grade);
            assertEquals("Updated GPA is 3.5", 3.5, fetchedUpdated.gpa);
        }

        // 4. Delete
        boolean deleted = ExamMarksDAO.delete(conn, testRoll, testTerm, testYear);
        assertTrue("Delete exam mark record", deleted);

        ExamMarksDAO.ExamMark deletedCheck = ExamMarksDAO.findByRollTermYear(conn, testRoll, testTerm, testYear);
        assertTrue("Deleted mark cannot be found", deletedCheck == null);
    }

    private void testDuplicateTermProtection(Connection conn) throws SQLException {
        int testRoll = 101;
        String testTerm = "Term-1";
        int testYear = 2026;

        ExamMarksDAO.delete(conn, testRoll, testTerm, testYear);

        GradeCalculator.GradeResult calc = GradeCalculator.calculate(80, 80, 80, 80, 80);
        ExamMarksDAO.ExamMark mark = new ExamMarksDAO.ExamMark(
                testRoll, "Alice Smith", 10, testTerm, testYear,
                80, 80, 80, 80, 80, calc
        );

        // Ensure record exists
        boolean firstInserted = ExamMarksDAO.insert(conn, mark);
        assertTrue("Initial insert of exam mark succeeds", firstInserted);

        // Direct second insert of duplicate must be rejected by database UNIQUE constraint
        boolean caughtUniqueViolation = false;
        try {
            ExamMarksDAO.insert(conn, mark);
        } catch (SQLException e) {
            caughtUniqueViolation = true;
        }
        assertTrue("Duplicate (Roll, Term, Year) direct insertion rejected by UNIQUE constraint", caughtUniqueViolation);

        // Calling saveOrUpdate handles duplicate gracefully by updating existing record
        calc = GradeCalculator.calculate(90, 90, 90, 90, 90);
        ExamMarksDAO.ExamMark updated = new ExamMarksDAO.ExamMark(
                testRoll, "Alice Smith", 10, testTerm, testYear,
                90, 90, 90, 90, 90, calc
        );
        boolean updatedSuccessfully = ExamMarksDAO.saveOrUpdate(conn, updated);
        assertTrue("Duplicate term safely updated via saveOrUpdate without constraint violation", updatedSuccessfully);
    }

    private void testSearchAndFindAll(Connection conn) throws SQLException {
        int testRoll = 101;
        String testTerm = "Final";
        int testYear = 2026;

        ExamMarksDAO.delete(conn, testRoll, testTerm, testYear);

        GradeCalculator.GradeResult calc = GradeCalculator.calculate(70, 75, 72, 74, 80);
        ExamMarksDAO.ExamMark mark = new ExamMarksDAO.ExamMark(
                testRoll, "Alice Smith", 10, testTerm, testYear,
                70, 75, 72, 74, 80, calc
        );
        ExamMarksDAO.insert(conn, mark);

        // Search by Roll
        List<ExamMarksDAO.ExamMark> byRoll = ExamMarksDAO.search(conn, String.valueOf(testRoll));
        assertTrue("Search by roll returns match", byRoll.size() >= 1);
        if (byRoll.size() >= 1) {
            assertEquals("Search by roll returns correct student", "Alice Smith", byRoll.get(0).studentName);
        }

        // Search by Name substring
        List<ExamMarksDAO.ExamMark> byName = ExamMarksDAO.search(conn, "Alice");
        assertTrue("Search by name substring returns match", byName.size() >= 1);

        // Find all
        List<ExamMarksDAO.ExamMark> all = ExamMarksDAO.findAll(conn);
        assertTrue("FindAll returns non-empty list", all.size() >= 1);

        // Cleanup
        ExamMarksDAO.delete(conn, testRoll, testTerm, testYear);
    }
}

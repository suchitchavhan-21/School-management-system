import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Integration Tests for ExamMarksDAO and Database Interaction.
 */
public class ExamMarksDAOTest {

    private int testsRun = 0;
    private int testsPassed = 0;

    public static void main(String[] args) {
        ExamMarksDAOTest test = new ExamMarksDAOTest();
        test.runAllTests();
    }

    public boolean runAllTests() {
        System.out.println("==================================================");
        System.out.println("Running ExamMarksDAOTest Suite...");
        System.out.println("==================================================");

        Connection conn = javaconnect.connerDb();
        if (conn == null) {
            System.err.println("  [FAIL] Unable to acquire database connection for testing.");
            return false;
        }

        try {
            DatabaseBootstrap.initialize(conn);
            testCRUDOperations(conn);
            testSearchAndFindAll(conn);
        } catch (Exception e) {
            System.err.println("  [FAIL] Unexpected exception during DAO test: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.printf("ExamMarksDAOTest Result: %d/%d tests passed.\n\n", testsPassed, testsRun);
        return testsRun == testsPassed;
    }

    private void assertTrue(String message, boolean condition) {
        testsRun++;
        if (condition) {
            testsPassed++;
            System.out.println("  [PASS] " + message);
        } else {
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
            System.err.printf("  [FAIL] %s - Expected: %s, Got: %s\n", message, expected, actual);
        }
    }

    private void testCRUDOperations(Connection conn) throws SQLException {
        int testRoll = 99991;
        String testTerm = "Mid-Term";
        int testYear = 2026;

        // Clean any pre-existing test records
        ExamMarksDAO.delete(conn, testRoll, testTerm, testYear);

        // Prepare test mark
        GradeCalculator.GradeResult calc = GradeCalculator.calculate(85.0, 92.5, 78.0, 88.0, 95.0);
        ExamMarksDAO.ExamMark mark = new ExamMarksDAO.ExamMark(
                testRoll, "Test Student Alpha", 10, testTerm, testYear,
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
            assertEquals("Fetched Student Name matches", "Test Student Alpha", fetched.studentName);
            assertEquals("Fetched Grade matches", "A+", fetched.grade);
            assertEquals("Fetched GPA matches", 5.0, fetched.gpa);
        }

        // 3. Update
        GradeCalculator.GradeResult updatedCalc = GradeCalculator.calculate(60.0, 65.0, 62.0, 68.0, 70.0);
        ExamMarksDAO.ExamMark updatedMark = new ExamMarksDAO.ExamMark(
                testRoll, "Test Student Alpha", 10, testTerm, testYear,
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

    private void testSearchAndFindAll(Connection conn) throws SQLException {
        int testRoll = 99992;
        String testTerm = "Final";
        int testYear = 2026;

        ExamMarksDAO.delete(conn, testRoll, testTerm, testYear);

        GradeCalculator.GradeResult calc = GradeCalculator.calculate(70, 75, 72, 74, 80);
        ExamMarksDAO.ExamMark mark = new ExamMarksDAO.ExamMark(
                testRoll, "UniqueSearchableStudentName", 9, testTerm, testYear,
                70, 75, 72, 74, 80, calc
        );
        ExamMarksDAO.insert(conn, mark);

        // Search by Roll
        List<ExamMarksDAO.ExamMark> byRoll = ExamMarksDAO.search(conn, String.valueOf(testRoll));
        assertTrue("Search by roll returns match", byRoll.size() >= 1);

        // Search by Name substring
        List<ExamMarksDAO.ExamMark> byName = ExamMarksDAO.search(conn, "UniqueSearchable");
        assertTrue("Search by name substring returns match", byName.size() >= 1);

        // Find all
        List<ExamMarksDAO.ExamMark> all = ExamMarksDAO.findAll(conn);
        assertTrue("FindAll returns non-empty list", all.size() >= 1);

        // Cleanup
        ExamMarksDAO.delete(conn, testRoll, testTerm, testYear);
    }
}

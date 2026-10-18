import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Opt-In Integration Test Suite for MySQL Enterprise Persistence with Zero-Trust Safety Guards.
 * 
 * Safely tests real MySQL database interactions including:
 * - Safety guard validation: refusal of production names and mandatory opt-in flags.
 * - MySQL dialect bootstrapping and schema creation.
 * - Attendance insert, update, and find operations.
 * - Duplicate key conflict detection (MySQL error code 1062 / ER_DUP_ENTRY / SQLState 23000).
 * - Race-safe saveOrUpdate execution in MySQL.
 * - Remarks boundary handling in VARCHAR(255).
 * - Targeted, isolated cleanup of only test-created records.
 * 
 * ZERO-TRUST SAFETY:
 * Requires explicit environment variable configuration AND an explicit opt-in flag
 * (SMS_MYSQL_ENABLE_DESTRUCTIVE_INTEGRATION_TESTS=true) to execute live tests against a disposable test DB.
 * Never modifies or connects to production data. If unconfigured or unreachable, safely marks
 * live validation as NOT RUN (ENVIRONMENTAL LIMITATION) without failing offline test suites.
 */
public class MySQLIntegrationTest {

    private int testsRun = 0;
    private int testsPassed = 0;
    private int testsFailed = 0;
    private int testsSkipped = 0;
    private boolean executed = false;

    public int getTestsRun() { return testsRun; }
    public int getTestsPassed() { return testsPassed; }
    public int getTestsFailed() { return testsFailed; }
    public int getTestsSkipped() { return testsSkipped; }
    public boolean wasExecuted() { return executed; }

    public static class SafetyCheckResult {
        public final boolean isSafe;
        public final String reason;

        public SafetyCheckResult(boolean isSafe, String reason) {
            this.isSafe = isSafe;
            this.reason = reason;
        }
    }

    /**
     * Strictly evaluates whether a database is safe for destructive integration testing.
     * Requires an explicit opt-in flag and a database name positively containing a disposable keyword
     * while strictly excluding any production-like indicators.
     */
    public static SafetyCheckResult evaluateDatabaseSafety(String dbName, String optInFlag) {
        if (optInFlag == null || !"true".equalsIgnoreCase(optInFlag.trim())) {
            return new SafetyCheckResult(false,
                "Missing mandatory opt-in safety flag: SMS_MYSQL_ENABLE_DESTRUCTIVE_INTEGRATION_TESTS must be explicitly set to 'true'.");
        }

        if (dbName == null || dbName.trim().isEmpty()) {
            return new SafetyCheckResult(false, "Database name cannot be null or empty.");
        }

        String lower = dbName.trim().toLowerCase();

        // 1. Explicit prohibition of production-like keywords
        String[] prohibitedTerms = {"prod", "production", "school", "live", "master", "release", "real", "deploy"};
        for (String term : prohibitedTerms) {
            if (lower.equals(term) || lower.startsWith(term + "_") || lower.endsWith("_" + term) || lower.contains("_" + term + "_")) {
                return new SafetyCheckResult(false,
                    "Database name '" + dbName + "' contains production indicator '" + term + "'. Execution strictly prohibited.");
            }
        }
        if (lower.equals("student_database") || lower.equals("students_database")) {
            return new SafetyCheckResult(false,
                "Database name '" + dbName + "' matches production default schema. Must use dedicated disposable test database.");
        }

        // 2. Mandatory presence of disposable/test keyword
        String[] requiredDisposableTerms = {"test", "disposable", "scratch", "ephemeral", "sandbox", "ci_temp"};
        boolean containsDisposableTerm = false;
        for (String term : requiredDisposableTerms) {
            if (lower.contains(term)) {
                containsDisposableTerm = true;
                break;
            }
        }

        if (!containsDisposableTerm) {
            return new SafetyCheckResult(false,
                "Database name '" + dbName + "' must explicitly include a disposable keyword (e.g. 'test', 'disposable', 'scratch', 'sandbox').");
        }

        return new SafetyCheckResult(true, "Database '" + dbName + "' is verified as a safe, disposable test database.");
    }

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

    private void assertFalse(String message, boolean condition) {
        assertTrue(message, !condition);
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
        System.out.println("Running MySQLIntegrationTest (Safety Guard & Suite)...");
        System.out.println("==================================================");

        // 1. Always execute Safety Guard Unit Tests (offline verifiable)
        runSafetyGuardTests();

        // 2. Check for Live MySQL Environment Configuration
        String host = getEnvOrProp("SMS_MYSQL_TEST_HOST", "sms.mysql.test.host");
        String port = getEnvOrProp("SMS_MYSQL_TEST_PORT", "sms.mysql.test.port");
        String db = getEnvOrProp("SMS_MYSQL_TEST_DATABASE", "sms.mysql.test.database");
        String user = getEnvOrProp("SMS_MYSQL_TEST_USER", "sms.mysql.test.user");
        String pass = getEnvOrProp("SMS_MYSQL_TEST_PASSWORD", "sms.mysql.test.password");
        String optIn = getEnvOrProp("SMS_MYSQL_ENABLE_DESTRUCTIVE_INTEGRATION_TESTS", "sms.mysql.enable.destructive.integration.tests");

        if (host == null || host.trim().isEmpty() || db == null || db.trim().isEmpty() || user == null || user.trim().isEmpty()) {
            System.out.println("  [NOT RUN] Live MySQL host/connection environment not configured.");
            System.out.println("  To run integration tests against a live MySQL instance, define:");
            System.out.println("    SMS_MYSQL_TEST_HOST (e.g. 'localhost')");
            System.out.println("    SMS_MYSQL_TEST_PORT (e.g. '3306')");
            System.out.println("    SMS_MYSQL_TEST_DATABASE (e.g. 'sms_disposable_test')");
            System.out.println("    SMS_MYSQL_TEST_USER (e.g. 'sms_tester')");
            System.out.println("    SMS_MYSQL_TEST_PASSWORD (e.g. 'password')");
            System.out.println("    SMS_MYSQL_ENABLE_DESTRUCTIVE_INTEGRATION_TESTS ('true')");
            System.out.println("  Status: NOT RUN (ENVIRONMENTAL LIMITATION).\n");
            return testsFailed == 0;
        }

        // 3. Evaluate safety of target database
        SafetyCheckResult safety = evaluateDatabaseSafety(db, optIn);
        if (!safety.isSafe) {
            System.err.println("  [REFUSED] MySQL Integration Test execution aborted for safety: " + safety.reason);
            testsFailed++;
            return false;
        }

        if (port == null || port.trim().isEmpty()) port = "3306";
        if (pass == null) pass = "";

        String url = String.format("jdbc:mysql://%s:%s/%s?zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true",
                host.trim(), port.trim(), db.trim());

        Connection conn = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(url, user.trim(), pass);
            executed = true;
            System.out.println("  [INFO] Connected to verified disposable MySQL test database: " + host + ":" + port + "/" + db);

            // Execute live test lifecycle
            runLiveMySQLTests(conn);

        } catch (ClassNotFoundException e) {
            System.err.println("  [FAIL] MySQL JDBC Driver not found: " + e.getMessage());
            testsFailed++;
            return false;
        } catch (SQLException e) {
            System.err.println("  [FAIL] MySQL connection error: " + e.getMessage());
            testsFailed++;
            return false;
        } catch (Exception e) {
            System.err.println("  [FAIL] Unexpected exception in MySQL integration tests: " + e.getMessage());
            e.printStackTrace();
            testsFailed++;
            return false;
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (Exception ignored) {}
            }
        }

        System.out.printf("MySQLIntegrationTest Summary: %d Passed, %d Failed, %d Skipped out of %d tests.\n\n",
                testsPassed, testsFailed, testsSkipped, testsRun);
        return testsFailed == 0;
    }

    private void runSafetyGuardTests() {
        System.out.println("--- Subsuite: Database Safety Guard Validation ---");
        // Refusal paths
        assertFalse("Safety guard rejects missing opt-in flag", evaluateDatabaseSafety("sms_disposable_test", null).isSafe);
        assertFalse("Safety guard rejects opt-in 'false'", evaluateDatabaseSafety("sms_disposable_test", "false").isSafe);
        assertFalse("Safety guard rejects empty opt-in", evaluateDatabaseSafety("sms_disposable_test", "").isSafe);
        assertFalse("Safety guard rejects null database name", evaluateDatabaseSafety(null, "true").isSafe);
        assertFalse("Safety guard rejects empty database name", evaluateDatabaseSafety("  ", "true").isSafe);
        assertFalse("Safety guard rejects 'production' database", evaluateDatabaseSafety("production", "true").isSafe);
        assertFalse("Safety guard rejects 'prod_sms' database", evaluateDatabaseSafety("prod_sms", "true").isSafe);
        assertFalse("Safety guard rejects 'school_system' database", evaluateDatabaseSafety("school_system", "true").isSafe);
        assertFalse("Safety guard rejects 'live_database' database", evaluateDatabaseSafety("live_database", "true").isSafe);
        assertFalse("Safety guard rejects default 'student_database'", evaluateDatabaseSafety("student_database", "true").isSafe);
        assertFalse("Safety guard rejects database without disposable term", evaluateDatabaseSafety("my_custom_db", "true").isSafe);
        // Prove that presence of 'test' or 'sandbox' alone is NOT treated as proof of safety
        assertFalse("Safety guard rejects 'prod_test' despite containing 'test'", evaluateDatabaseSafety("prod_test", "true").isSafe);
        assertFalse("Safety guard rejects 'school_sandbox' despite containing 'sandbox'", evaluateDatabaseSafety("school_sandbox", "true").isSafe);
        assertFalse("Safety guard rejects 'live_disposable' despite containing 'disposable'", evaluateDatabaseSafety("live_disposable", "true").isSafe);
        assertFalse("Safety guard rejects 'test' database when opt-in flag is missing", evaluateDatabaseSafety("test", null).isSafe);
        assertFalse("Safety guard rejects 'sandbox' database when opt-in flag is false", evaluateDatabaseSafety("sandbox", "false").isSafe);

        // Prove cleanup failure surfaces as false when connection is invalid
        assertFalse("Cleanup reports failure when connection is null", cleanupTestRecords(null, 999999, "2026-10-18", true, true));

        // Prove cleanup failure surfaces as false when connection throws SQLException (closed connection)
        try {
            java.io.File dummyFile = java.io.File.createTempFile("sms_cleanup_guard_", ".sqlite");
            dummyFile.deleteOnExit();
            Connection closedConn = javaconnect.getTestConnection(dummyFile);
            if (closedConn != null) {
                closedConn.close();
                assertFalse("Cleanup reports failure when connection throws SQLException", cleanupTestRecords(closedConn, 999999, "2026-10-18", true, true));
            }
            dummyFile.delete();
        } catch (Exception e) {
            System.err.println("  [WARN] Failed to execute closed connection cleanup guard test: " + e.getMessage());
        }

        // Acceptance paths
        assertTrue("Safety guard accepts 'sms_disposable_test' with opt-in", evaluateDatabaseSafety("sms_disposable_test", "true").isSafe);
        assertTrue("Safety guard accepts 'sms_test_sandbox' with opt-in", evaluateDatabaseSafety("sms_test_sandbox", "true").isSafe);
        assertTrue("Safety guard accepts 'ci_temp_scratch' with opt-in", evaluateDatabaseSafety("ci_temp_scratch", "true").isSafe);
        assertTrue("Safety guard accepts 'ephemeral_unit_test' with opt-in", evaluateDatabaseSafety("ephemeral_unit_test", "true").isSafe);
    }

    private void runLiveMySQLTests(Connection conn) throws SQLException {
        System.out.println("--- Subsuite: Live MySQL Operations & Resilience ---");

        // Positive evidence check: verify connected catalog is verified as a safe, disposable database
        String connectedDb = conn.getCatalog();
        if (connectedDb != null && !connectedDb.trim().isEmpty()) {
            String optInFlag = getEnvOrProp("SMS_MYSQL_ENABLE_DESTRUCTIVE_INTEGRATION_TESTS", "sms.mysql.enable.destructive.integration.tests");
            SafetyCheckResult connSafety = evaluateDatabaseSafety(connectedDb, optInFlag);
            if (!connSafety.isSafe) {
                throw new IllegalStateException("Safety abort: Connected database catalog '" + connectedDb + "' failed safety verification: " + connSafety.reason);
            }
        }

        // 1. Bootstrap schema in MySQL
        boolean bootstrapped = DatabaseBootstrap.bootstrap(conn);
        assertTrue("MySQL schema bootstrap succeeds idempotently", bootstrapped);

        // Dynamically generated unique roll number to prevent any possibility of colliding with pre-existing records
        int testRoll = 920000 + (int)(System.currentTimeMillis() % 70000);
        String testDate = "2026-10-18";

        boolean studentCreated = false;
        boolean attendanceCreated = false;

        try {
            // Verify roll number is strictly unoccupied before writing
            try (PreparedStatement checkPst = conn.prepareStatement("SELECT 1 FROM student_info WHERE Roll = ?")) {
                checkPst.setInt(1, testRoll);
                try (ResultSet rs = checkPst.executeQuery()) {
                    if (rs.next()) {
                        throw new IllegalStateException("Safety abort: Roll number " + testRoll + " already exists in student_info. Halting test.");
                    }
                }
            }

            // 2. Seed student in MySQL student_info
            String seedStudent = "INSERT INTO student_info (Roll, First_name, Last_name, Class, Year, Birth_date, Mobile_no, Sex, permanent, Present) " +
                                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pst = conn.prepareStatement(seedStudent)) {
                pst.setInt(1, testRoll);
                pst.setString(2, "MySQL");
                pst.setString(3, "Tester");
                pst.setInt(4, 10);
                pst.setInt(5, 2026);
                pst.setString(6, "2008-05-15");
                pst.setString(7, "0123456789");
                pst.setString(8, "male");
                pst.setString(9, "Database Lab");
                pst.setString(10, "Database Lab");
                boolean seeded = pst.executeUpdate() > 0;
                if (seeded) studentCreated = true;
                assertTrue("Seed unique student in MySQL student_info", seeded);
            }

            // 3. Normal Attendance Insert in MySQL
            AttendanceDAO.AttendanceRecord rec1 = new AttendanceDAO.AttendanceRecord(
                    testRoll, "MySQL Tester", 10, testDate, "Present", "MySQL live test");
            boolean inserted = AttendanceDAO.insert(conn, rec1);
            if (inserted) attendanceCreated = true;
            assertTrue("Normal attendance insert succeeds in MySQL", inserted);

            // 4. Duplicate insert triggers MySQL unique constraint (error code 1062)
            boolean duplicateRejected = false;
            try {
                AttendanceDAO.insert(conn, rec1);
            } catch (SQLException e) {
                duplicateRejected = AttendanceDAO.isUniqueConstraintViolation(e);
            }
            assertTrue("MySQL duplicate entry on (Roll, Attendance_date) recognized as unique violation", duplicateRejected);

            // 5. saveOrUpdate safely updates record in MySQL
            rec1.setStatus("Late");
            rec1.setRemarks("Updated in MySQL");
            boolean updated = AttendanceDAO.saveOrUpdate(conn, rec1);
            assertTrue("saveOrUpdate successfully updates existing record in MySQL", updated);

            AttendanceDAO.AttendanceRecord fetched = AttendanceDAO.findByRollAndDate(conn, testRoll, testDate);
            assertEquals("Updated status is Late in MySQL", "Late", fetched.getStatus());
            assertEquals("Updated remarks match in MySQL", "Updated in MySQL", fetched.getRemarks());

            // 6. Remarks boundary: Exactly 255 chars in MySQL VARCHAR(255)
            StringBuilder sb255 = new StringBuilder();
            for (int i = 0; i < 255; i++) sb255.append("M");
            rec1.setRemarks(sb255.toString());
            boolean remarks255Updated = AttendanceDAO.update(conn, rec1);
            assertTrue("Exactly 255 characters accepted in MySQL VARCHAR(255)", remarks255Updated);

            // 7. Remarks boundary: 256 chars rejected before sending to MySQL
            StringBuilder sb256 = new StringBuilder();
            for (int i = 0; i < 256; i++) sb256.append("X");
            boolean remarks256Rejected = false;
            try {
                rec1.setRemarks(sb256.toString());
                AttendanceDAO.update(conn, rec1);
            } catch (IllegalArgumentException e) {
                remarks256Rejected = e.getMessage().contains("exceed maximum allowed length");
            }
            assertTrue("Remarks with 256 characters rejected prior to MySQL execution", remarks256Rejected);

            // 8. Delete record in MySQL
            boolean deleted = AttendanceDAO.delete(conn, testRoll, testDate);
            if (deleted) attendanceCreated = false;
            assertTrue("Attendance record deleted successfully from MySQL", deleted);

        } finally {
            // Targeted cleanup strictly scoped to rows created by this specific run
            boolean cleaned = cleanupTestRecords(conn, testRoll, testDate, attendanceCreated, studentCreated);
            if (studentCreated || attendanceCreated) {
                assertTrue("Test cleanup completed without error", cleaned);
            }
        }
    }

    public boolean cleanupTestRecords(Connection conn, int roll, String date, boolean cleanAttendance, boolean cleanStudent) {
        if (conn == null) {
            return false;
        }

        boolean allClean = true;
        if (cleanAttendance) {
            try (PreparedStatement pst1 = conn.prepareStatement("DELETE FROM attendance WHERE Roll = ? AND Attendance_date = ?")) {
                if (pst1 != null) {
                    pst1.setInt(1, roll);
                    pst1.setString(2, date);
                    pst1.executeUpdate();
                }
            } catch (Exception ex) {
                allClean = false;
                System.err.println("  [FAIL] Cleanup failed to delete test attendance record for Roll " + roll + ": " + ex.getMessage());
            }
        }

        if (cleanStudent) {
            try (PreparedStatement pst2 = conn.prepareStatement("DELETE FROM student_info WHERE Roll = ?")) {
                if (pst2 != null) {
                    pst2.setInt(1, roll);
                    pst2.executeUpdate();
                }
            } catch (Exception ex) {
                allClean = false;
                System.err.println("  [FAIL] Cleanup failed to delete test student record for Roll " + roll + ": " + ex.getMessage());
            }
        }
        return allClean;
    }

    private String getEnvOrProp(String envKey, String propKey) {
        String val = System.getenv(envKey);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }
        val = System.getProperty(propKey);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }
        return null;
    }

    public static void main(String[] args) {
        MySQLIntegrationTest suite = new MySQLIntegrationTest();
        boolean passed = suite.runAllTests();
        if (!passed) {
            System.err.println(">>> MySQLIntegrationTest FAILED - EXITING WITH CODE 1 <<<");
            System.exit(1);
        } else {
            System.out.println(">>> MySQLIntegrationTest PASSED <<<");
            System.exit(0);
        }
    }
}

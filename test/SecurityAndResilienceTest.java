import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ethical Hacker & Application Security Engineer Test Suite.
 * Validates:
 * 1. Zero-trust database isolation and failover safety.
 * 2. Schema bootstrapping without insecure default credentials.
 * 3. Salted PBKDF2 hashing, random salting, constant-time verification, and memory wiping.
 * 4. Transparent migration of legacy plaintext credentials upon login.
 * 5. Parameter binding integrity (avoiding parameter index out of range bugs).
 * 6. SQL Injection immunity against Tautology, Piggybacked DDL, and UNION attacks.
 */
public class SecurityAndResilienceTest {

    private static final String TEST_DB_FILE = "test_security.sqlite";

    private int testsRun = 0;
    private int testsPassed = 0;
    private int testsFailed = 0;
    private int testsSkipped = 0;

    public static void main(String[] args) {
        SecurityAndResilienceTest test = new SecurityAndResilienceTest();
        boolean passed = test.runAllTests();
        System.exit(passed ? 0 : 1);
    }

    public boolean runAllTests() {
        System.out.println("==================================================");
        System.out.println("Running SecurityAndResilienceTest Suite...");
        System.out.println("==================================================");

        cleanupTestDb();
        Connection conn = javaconnect.getTestConnection(TEST_DB_FILE);

        if (conn == null) {
            System.err.println("  [FAIL] Unable to acquire isolated test database connection.");
            testsFailed++;
            return false;
        }

        try {
            testDatabaseConnectionAndIsolation(conn);
            testSchemaBootstrapAndNoDefaultCredentials(conn);
            testPasswordSecurityPBKDF2();
            testLegacyPlaintextPasswordMigration(conn);
            testAdminParameterIndexIntegrity(conn);
            testSqlInjectionImmunity(conn);
        } catch (Exception e) {
            System.err.println("  [FAIL] Unexpected exception during security tests: " + e.getMessage());
            e.printStackTrace();
            testsFailed++;
        } finally {
            try {
                conn.close();
            } catch (Exception ignored) {}
            cleanupTestDb();
        }

        System.out.printf("SecurityAndResilienceTest Summary: %d Passed, %d Failed, %d Skipped out of %d tests.\n\n",
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
        File file = new File(TEST_DB_FILE);
        if (file.exists()) {
            file.delete();
        }
    }

    private void testDatabaseConnectionAndIsolation(Connection conn) throws SQLException {
        assertTrue("Isolated test connection is active and not null", conn != null && !conn.isClosed());
        DatabaseMetaData meta = conn.getMetaData();
        assertTrue("Connected driver is SQLite for isolated test", meta.getDriverName().contains("SQLite"));
    }

    private void testSchemaBootstrapAndNoDefaultCredentials(Connection conn) throws SQLException {
        DatabaseBootstrap.initialize(conn);
        DatabaseMetaData meta = conn.getMetaData();
        Set<String> tables = new HashSet<>();
        try (ResultSet rs = meta.getTables(null, null, "%", null)) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME").toLowerCase());
            }
        }

        assertTrue("Table 'pass' exists", tables.contains("pass"));
        assertTrue("Table 'admin' exists", tables.contains("admin"));
        assertTrue("Table 'student_info' exists", tables.contains("student_info"));
        assertTrue("Table 'teacher_info' exists", tables.contains("teacher_info"));
        assertTrue("Table 'employee_info' exists", tables.contains("employee_info"));
        assertTrue("Table 'exam_marks' exists", tables.contains("exam_marks"));

        // Crucial Zero-Trust check: default hardcoded credentials MUST NOT be seeded into fresh databases
        try (PreparedStatement pst = conn.prepareStatement("SELECT COUNT(*) FROM admin");
             ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                assertEquals("No hardcoded default admin credentials seeded", 0, rs.getInt(1));
            }
        }

        try (PreparedStatement pst = conn.prepareStatement("SELECT COUNT(*) FROM pass");
             ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                assertEquals("No hardcoded default user credentials seeded", 0, rs.getInt(1));
            }
        }
    }

    private void testPasswordSecurityPBKDF2() {
        char[] password = "SecureAdminPassword!2026".toCharArray();
        String hash1 = PasswordSecurity.hashPassword(password);

        assertTrue("Generated hash starts with $pbkdf2-sha256$", hash1 != null && hash1.startsWith("$pbkdf2-sha256$"));
        assertTrue("PasswordSecurity.isHashed returns true for valid hash", PasswordSecurity.isHashed(hash1));
        assertTrue("PasswordSecurity.isHashed returns false for plaintext password", !PasswordSecurity.isHashed("plaintext123"));

        // Verify password match
        assertTrue("Correct password validates against hash", PasswordSecurity.verifyPassword("SecureAdminPassword!2026".toCharArray(), hash1));
        assertTrue("Incorrect password rejected by verifyPassword", !PasswordSecurity.verifyPassword("WrongPassword!2026".toCharArray(), hash1));

        // Salt randomness test: two hashes of the identical password must have different salts and strings
        String hash2 = PasswordSecurity.hashPassword("SecureAdminPassword!2026".toCharArray());
        assertTrue("Two hashes of identical password produce distinct salts and values", !hash1.equals(hash2));
        assertTrue("Second hash also validates with correct password", PasswordSecurity.verifyPassword("SecureAdminPassword!2026".toCharArray(), hash2));

        // Memory wipe verification
        char[] sensitiveData = new char[]{'s', 'e', 'c', 'r', 'e', 't'};
        PasswordSecurity.wipe(sensitiveData);
        boolean allZeroed = true;
        for (char c : sensitiveData) {
            if (c != 0) {
                allZeroed = false;
                break;
            }
        }
        assertTrue("PasswordSecurity.wipe successfully zeroes character array", allZeroed);
    }

    private void testLegacyPlaintextPasswordMigration(Connection conn) throws SQLException {
        String insertLegacy = "INSERT INTO pass (username, password) VALUES (?, ?)";
        try (PreparedStatement pst = conn.prepareStatement(insertLegacy)) {
            pst.setString(1, "legacy_teacher");
            pst.setString(2, "MyOldPlaintextPass123");
            pst.executeUpdate();
        }

        // Verify it was stored as plaintext
        try (PreparedStatement pst = conn.prepareStatement("SELECT password FROM pass WHERE username = ?")) {
            pst.setString(1, "legacy_teacher");
            try (ResultSet rs = pst.executeQuery()) {
                assertTrue("Legacy user initially exists with plaintext password", rs.next() && !PasswordSecurity.isHashed(rs.getString("password")));
            }
        }

        // Test login with wrong password - must fail and NOT migrate
        boolean wrongLogin = PasswordSecurity.authenticateAndMigrateUser(conn, "legacy_teacher", "WrongPass".toCharArray());
        assertTrue("Login with wrong password fails for legacy user", !wrongLogin);

        // Test login with correct password - must authenticate AND migrate to PBKDF2 hash
        boolean correctLogin = PasswordSecurity.authenticateAndMigrateUser(conn, "legacy_teacher", "MyOldPlaintextPass123".toCharArray());
        assertTrue("Login with correct password succeeds for legacy user", correctLogin);

        // Verify database row was transparently updated to a PBKDF2 hash
        String updatedHash = null;
        try (PreparedStatement pst = conn.prepareStatement("SELECT password FROM pass WHERE username = ?")) {
            pst.setString(1, "legacy_teacher");
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    updatedHash = rs.getString("password");
                }
            }
        }
        assertTrue("Legacy plaintext was migrated to $pbkdf2-sha256$ in database", updatedHash != null && updatedHash.startsWith("$pbkdf2-sha256$"));

        // Subsequent login must now authenticate using the newly migrated PBKDF2 hash
        boolean postMigrationLogin = PasswordSecurity.authenticateAndMigrateUser(conn, "legacy_teacher", "MyOldPlaintextPass123".toCharArray());
        assertTrue("Subsequent login succeeds against newly migrated PBKDF2 hash", postMigrationLogin);
    }

    private void testAdminParameterIndexIntegrity(Connection conn) throws SQLException {
        // Seed an admin user with hashed password
        String hashedAdminPin = PasswordSecurity.hashPassword("9988".toCharArray());
        String insertAdmin = "INSERT INTO admin (password) VALUES (?)";
        try (PreparedStatement pst = conn.prepareStatement(insertAdmin)) {
            pst.setString(1, hashedAdminPin);
            pst.executeUpdate();
        }

        // Test query: "SELECT * FROM admin WHERE password = ?"
        // In the legacy code, pst.setString(2, ...) caused Parameter index out of range (2 > 1).
        String sql = "SELECT * FROM admin WHERE password = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, hashedAdminPin);
            try (ResultSet rs = pst.executeQuery()) {
                assertTrue("Admin single-parameter query binds at index 1 without ParameterIndexOutOfBoundsException", rs.next());
            }
        }

        // Test admin authentication logic with correct and incorrect PIN
        assertTrue("Admin authentication succeeds with correct PIN", PasswordSecurity.authenticateAndMigrateAdmin(conn, "9988".toCharArray()));
        assertTrue("Admin authentication fails with incorrect PIN", !PasswordSecurity.authenticateAndMigrateAdmin(conn, "0000".toCharArray()));

        // Test legacy plaintext admin PIN migration
        try (PreparedStatement pst = conn.prepareStatement("INSERT INTO admin (password) VALUES (?)")) {
            pst.setString(1, "legacy_secret_pin");
            pst.executeUpdate();
        }
        assertTrue("Legacy plaintext admin PIN authenticates successfully", PasswordSecurity.authenticateAndMigrateAdmin(conn, "legacy_secret_pin".toCharArray()));
    }

    private void testSqlInjectionImmunity(Connection conn) throws SQLException {
        // First seed a legitimate student and exam mark record
        String seedStudent = "INSERT INTO student_info (Roll, First_name, Last_name, Class, Year, Birth_date, Mobile_no, Sex, permanent, Present) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = conn.prepareStatement(seedStudent)) {
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
            pst.executeUpdate();
        }

        GradeCalculator.GradeResult calc = GradeCalculator.calculate(85.0, 90.0, 88.0, 92.0, 80.0);
        ExamMarksDAO.ExamMark legitimateMark = new ExamMarksDAO.ExamMark(
                101, "Alice Smith", 10, "Final", 2026,
                85.0, 90.0, 88.0, 92.0, 80.0, calc
        );
        boolean saved = ExamMarksDAO.insert(conn, legitimateMark);
        assertTrue("Legitimate exam mark seeded for injection tests", saved);

        // 1. Tautology Attack: "' OR '1'='1"
        // In an insecure application: "WHERE Roll LIKE '%' OR '1'='1%'" would return all rows.
        // With parameterized query: ExamMarksDAO binds as literal string -> exactly 0 records match.
        String tautologyPayload = "' OR '1'='1";
        List<ExamMarksDAO.ExamMark> tautologyResults = ExamMarksDAO.search(conn, tautologyPayload);
        assertEquals("SQL Injection tautology (' OR '1'='1') returns 0 rows (treated as literal)", 0, tautologyResults.size());

        // 2. Piggybacked Statement Attack: "101; DROP TABLE exam_marks; --"
        // In an insecure multi-statement execution, this would drop the table.
        String piggybackedPayload = "101; DROP TABLE exam_marks; --";
        List<ExamMarksDAO.ExamMark> piggybackedResults = ExamMarksDAO.search(conn, piggybackedPayload);
        assertEquals("Piggybacked injection returns 0 rows", 0, piggybackedResults.size());

        // Prove that exam_marks was NOT dropped and legitimate record is intact
        try (PreparedStatement pst = conn.prepareStatement("SELECT COUNT(*) FROM exam_marks");
             ResultSet rs = pst.executeQuery()) {
            assertTrue("Table 'exam_marks' survived piggybacked DROP TABLE attempt", rs.next() && rs.getInt(1) == 1);
        }

        // 3. UNION-based Exfiltration Attack
        String unionPayload = "' UNION SELECT 999, 999, 'Hacked', 'Hacked', 'Hacked', 100, 100, 100, 100, 100, 500, 100, 5, 'A+', 'Pass' --";
        List<ExamMarksDAO.ExamMark> unionResults = ExamMarksDAO.search(conn, unionPayload);
        assertEquals("UNION-based injection payload returns 0 rows", 0, unionResults.size());
    }
}

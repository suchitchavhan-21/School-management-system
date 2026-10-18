import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ethical Hacker & Application Security Engineer Test Suite.
 * Validates:
 * 1. Safe test database isolation using unique temporary files and preserving pre-existing files.
 * 2. Strict schema bootstrapping, idempotence, table verification, and error propagation.
 * 3. Salted PBKDF2 hashing, format validation, iteration limits, PBEKeySpec clearing, and DoS resilience.
 * 4. Backward compatibility with older hashes (65,536 iterations) and transparent upgrade to 210,000 iterations.
 * 5. Strong password and administrator authorization key policy validation.
 * 6. Atomic, transactional first-run setup with guaranteed rollback on failure.
 * 7. Admin parameter binding integrity and authentication.
 * 8. SQL injection immunity against Tautology, Piggybacked DDL, and UNION attacks.
 */
public class SecurityAndResilienceTest {

    private File tempDbFile;

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

        try {
            tempDbFile = File.createTempFile("sms_test_sec_", ".sqlite");
            tempDbFile.deleteOnExit();
        } catch (IOException e) {
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
            testPreExistingFilePreservation();
            testDatabaseConnectionAndIsolation(conn);
            testSchemaBootstrapAndErrorPropagation(conn);
            testPasswordSecurityPBKDF2();
            testMalformedHashHandlingAndDoSImmunity();
            testOlderHashCompatibilityAndUpgrade(conn);
            testPasswordAndAdminKeyStrengthValidation();
            testTransactionalFirstRunSetupAndRollback(conn);
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
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    private void testPreExistingFilePreservation() throws IOException, SQLException {
        File sentinel = File.createTempFile("sms_sentinel_preserve_", ".txt");
        sentinel.deleteOnExit();
        String originalContent = "CRITICAL_USER_DATA_DO_NOT_DELETE";
        try (FileWriter writer = new FileWriter(sentinel)) {
            writer.write(originalContent);
        }

        // Verify file exists
        assertTrue("Pre-existing sentinel file exists prior to test operations", sentinel.exists());

        // Perform test database operations
        File anotherTemp = File.createTempFile("sms_dummy_test_", ".sqlite");
        Connection dummyConn = javaconnect.getTestConnection(anotherTemp);
        if (dummyConn != null) {
            dummyConn.close();
        }
        anotherTemp.delete();

        // Verify sentinel was untouched
        assertTrue("Pre-existing sentinel file survived test database lifecycle", sentinel.exists());
        String readContent = Files.readString(sentinel.toPath());
        assertEquals("Pre-existing file content is completely intact", originalContent, readContent);
        sentinel.delete();
    }

    private void testDatabaseConnectionAndIsolation(Connection conn) throws SQLException {
        assertTrue("Isolated test connection is active and not null", conn != null && !conn.isClosed());
        DatabaseMetaData meta = conn.getMetaData();
        assertTrue("Connected driver is SQLite for isolated test", meta.getDriverName().contains("SQLite"));
    }

    private void testSchemaBootstrapAndErrorPropagation(Connection conn) throws SQLException {
        // Idempotent repeated bootstrap
        boolean boot1 = DatabaseBootstrap.bootstrap(conn);
        assertTrue("Initial schema bootstrap returns true", boot1);

        boolean boot2 = DatabaseBootstrap.bootstrap(conn);
        assertTrue("Repeated schema bootstrap is idempotent and returns true", boot2);

        // Verify all 6 tables exist
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

        // Error propagation test on closed connection
        Connection closedConn = javaconnect.getTestConnection(tempDbFile);
        if (closedConn != null) {
            closedConn.close();
            boolean threwException = false;
            try {
                DatabaseBootstrap.bootstrap(closedConn);
            } catch (SQLException e) {
                threwException = true;
            }
            assertTrue("DatabaseBootstrap propagates SQLException when connection is closed or invalid", threwException);
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

        // Salt randomness test: two hashes of the identical password must have different salts and values
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

    private void testMalformedHashHandlingAndDoSImmunity() {
        char[] pwd = "TestPassword#2026".toCharArray();

        // 1. Null, empty, and truncated
        assertTrue("Null hash rejected", !PasswordSecurity.verifyPassword(pwd, null));
        assertTrue("Empty hash rejected", !PasswordSecurity.verifyPassword(pwd, ""));
        assertTrue("Truncated hash parts rejected", !PasswordSecurity.verifyPassword(pwd, "$pbkdf2-sha256$210000$abcd"));

        // 2. Invalid prefix
        assertTrue("Invalid algorithm prefix rejected", !PasswordSecurity.isValidHashFormat("$pbkdf1$210000$0123456789abcdef0123456789abcdef$0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"));

        // 3. Odd-length hex salt
        assertTrue("Odd-length salt hex rejected", !PasswordSecurity.isValidHashFormat("$pbkdf2-sha256$210000$abc$0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"));

        // 4. Invalid hex characters
        assertTrue("Invalid hex characters in salt rejected", !PasswordSecurity.isValidHashFormat("$pbkdf2-sha256$210000$ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ$0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"));

        // 5. Unreasonable / extreme iteration counts (DoS prevention)
        // Below minimum (< 10000)
        assertTrue("Iterations below minimum rejected", !PasswordSecurity.isValidHashFormat("$pbkdf2-sha256$100$0123456789abcdef0123456789abcdef$0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"));
        // Extreme / Excessive (> 2000000)
        assertTrue("Extreme iterations (DoS attempt) rejected immediately without CPU hang",
                !PasswordSecurity.isValidHashFormat("$pbkdf2-sha256$999999999$0123456789abcdef0123456789abcdef$0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"));
        // Non-numeric iterations
        assertTrue("Non-numeric iterations rejected", !PasswordSecurity.isValidHashFormat("$pbkdf2-sha256$notanumber$0123456789abcdef0123456789abcdef$0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"));

        // 6. Salt and key length boundaries
        // Salt < 16 bytes (32 hex chars)
        assertTrue("Salt smaller than 16 bytes rejected", !PasswordSecurity.isValidHashFormat("$pbkdf2-sha256$210000$0123456789abcdef$0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"));
        // Key < 32 bytes (64 hex chars)
        assertTrue("Key smaller than 32 bytes rejected", !PasswordSecurity.isValidHashFormat("$pbkdf2-sha256$210000$0123456789abcdef0123456789abcdef$0123456789abcdef"));

        // 7. Verify malformed hashes do not crash verifyPassword
        assertTrue("Malformed hash safely returns false from verifyPassword", !PasswordSecurity.verifyPassword(pwd, "$pbkdf2-sha256$999999999$bad$bad"));
    }

    private void testOlderHashCompatibilityAndUpgrade(Connection conn) throws SQLException {
        // Construct an older valid hash with 65,536 iterations (valid, but lower than current 210,000)
        // Format: $pbkdf2-sha256$65536$<16 bytes salt>$<32 bytes hash>
        String saltHex = "00112233445566778899aabbccddeeff";
        byte[] salt = new byte[]{0,17,34,51,68,85,102,119,-120,-103,-86,-69,-52,-35,-18,-1};
        char[] oldPassword = "OlderPassword#2025".toCharArray();

        // Calculate expected hash for 65536 iterations
        javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(oldPassword, salt, 65536, 256);
        byte[] keyBytes;
        try {
            keyBytes = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            spec.clearPassword();
        }

        StringBuilder keyHex = new StringBuilder();
        for (byte b : keyBytes) keyHex.append(String.format("%02x", b));
        String olderHash = "$pbkdf2-sha256$65536$" + saltHex + "$" + keyHex.toString();

        // 1. Verify compatibility: verifyPassword must accept older hash
        assertTrue("verifyPassword validates older 65,536-iteration hash successfully", PasswordSecurity.verifyPassword(oldPassword, olderHash));

        // 2. needsRehash must identify that this hash should be upgraded to CURRENT_ITERATIONS (210,000)
        assertTrue("needsRehash recognizes older hash with 65,536 iterations needs upgrade", PasswordSecurity.needsRehash(olderHash));

        // 3. Insert user with older hash
        try (PreparedStatement pst = conn.prepareStatement("INSERT INTO pass (Username, Password) VALUES (?, ?)")) {
            pst.setString(1, "older_account_user");
            pst.setString(2, olderHash);
            pst.executeUpdate();
        }

        // 4. Authenticate and migrate: must authenticate and upgrade in-place
        boolean migrated = PasswordSecurity.authenticateAndMigrateUser(conn, "older_account_user", oldPassword);
        assertTrue("authenticateAndMigrateUser succeeds for older hash account", migrated);

        // 5. Verify database record now has CURRENT_ITERATIONS (210,000)
        String newHash = null;
        try (PreparedStatement pst = conn.prepareStatement("SELECT Password FROM pass WHERE Username = ?")) {
            pst.setString(1, "older_account_user");
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) newHash = rs.getString("Password");
            }
        }
        assertTrue("Upgraded hash starts with $pbkdf2-sha256$210000$", newHash != null && newHash.startsWith("$pbkdf2-sha256$210000$"));
        assertTrue("Subsequent login verifies against upgraded hash", PasswordSecurity.verifyPassword(oldPassword, newHash));
    }

    private void testPasswordAndAdminKeyStrengthValidation() {
        // Password strength policy checks
        assertTrue("Rejects password < 8 chars", !PasswordSecurity.validatePasswordStrength("Short1!".toCharArray()).valid);
        assertTrue("Rejects password without uppercase", !PasswordSecurity.validatePasswordStrength("nouppercase123!".toCharArray()).valid);
        assertTrue("Rejects password without lowercase", !PasswordSecurity.validatePasswordStrength("NOLOWERCASE123!".toCharArray()).valid);
        assertTrue("Rejects password without digit/symbol", !PasswordSecurity.validatePasswordStrength("NoDigitsOrSymbolsHere".toCharArray()).valid);
        assertTrue("Rejects password containing whitespace", !PasswordSecurity.validatePasswordStrength("Password With Spaces 123!".toCharArray()).valid);
        assertTrue("Accepts strong password meeting all criteria", PasswordSecurity.validatePasswordStrength("StrongAdmin#2026".toCharArray()).valid);

        // Admin authorization key policy checks
        assertTrue("Rejects admin key < 8 chars", !PasswordSecurity.validateAdminKeyStrength("Key1!".toCharArray()).valid);
        assertTrue("Rejects admin key without digits or symbols", !PasswordSecurity.validateAdminKeyStrength("LettersOnlyHere".toCharArray()).valid);
        assertTrue("Rejects admin key containing whitespace", !PasswordSecurity.validateAdminKeyStrength("Admin Key 123".toCharArray()).valid);
        assertTrue("Accepts strong admin authorization key", PasswordSecurity.validateAdminKeyStrength("AdminKey#2026".toCharArray()).valid);
    }

    private void testTransactionalFirstRunSetupAndRollback(Connection conn) throws SQLException {
        // Clear pass and admin for test
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM pass");
            stmt.executeUpdate("DELETE FROM admin");
        }

        assertTrue("isSetupRequired is true when database has no users", PasswordSecurity.isSetupRequired(conn));

        // 1. Successful transactional setup
        char[] adminPwd = "MasterPassword#2026".toCharArray();
        char[] adminKey = "MasterKey#2026".toCharArray();
        boolean success = PasswordSecurity.initializeFirstRunCredentials(conn, "superadmin", adminPwd, adminKey);
        assertTrue("initializeFirstRunCredentials completes successfully", success);
        assertTrue("isSetupRequired is false after complete initialization", !PasswordSecurity.isSetupRequired(conn));

        // Verify both tables have exactly 1 record
        try (Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM pass")) {
                assertTrue("pass table has 1 record", rs.next() && rs.getInt(1) == 1);
            }
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM admin")) {
                assertTrue("admin table has 1 record", rs.next() && rs.getInt(1) == 1);
            }
        }

        // 2. Deliberately forced failure & transaction rollback test:
        // Drop admin table temporarily to simulate failure on step 2 of setup
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP TABLE admin");
        }

        boolean threwRollbackException = false;
        try {
            PasswordSecurity.initializeFirstRunCredentials(conn, "failed_user", adminPwd, adminKey);
        } catch (SQLException e) {
            threwRollbackException = true;
        }
        assertTrue("Forced second-step failure throws SQLException", threwRollbackException);

        // Verify that 'failed_user' was NOT committed to 'pass' (transaction rolled back completely!)
        try (PreparedStatement pst = conn.prepareStatement("SELECT COUNT(*) FROM pass WHERE Username = ?")) {
            pst.setString(1, "failed_user");
            try (ResultSet rs = pst.executeQuery()) {
                assertEquals("No partial record committed to pass table after rollback", 0, rs.next() ? rs.getInt(1) : -1);
            }
        }

        // Restore admin table and verify retry works cleanly
        DatabaseBootstrap.bootstrap(conn);
        boolean retrySuccess = PasswordSecurity.initializeFirstRunCredentials(conn, "superadmin", adminPwd, adminKey);
        assertTrue("Retry of first-run setup succeeds after schema recovery", retrySuccess);
    }

    private void testAdminParameterIndexIntegrity(Connection conn) throws SQLException {
        String hashedAdminPin = PasswordSecurity.hashPassword("MasterKey#2026".toCharArray());
        try (PreparedStatement pst = conn.prepareStatement("INSERT INTO admin (password) VALUES (?)")) {
            pst.setString(1, hashedAdminPin);
            pst.executeUpdate();
        }

        String sql = "SELECT * FROM admin WHERE password = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, hashedAdminPin);
            try (ResultSet rs = pst.executeQuery()) {
                assertTrue("Admin single-parameter query binds at index 1 without ParameterIndexOutOfBoundsException", rs.next());
            }
        }

        assertTrue("Admin authentication succeeds with correct PIN", PasswordSecurity.authenticateAndMigrateAdmin(conn, "MasterKey#2026".toCharArray()));
        assertTrue("Admin authentication fails with incorrect PIN", !PasswordSecurity.authenticateAndMigrateAdmin(conn, "WrongKey#0000".toCharArray()));
    }

    private void testSqlInjectionImmunity(Connection conn) throws SQLException {
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
        String tautologyPayload = "' OR '1'='1";
        List<ExamMarksDAO.ExamMark> tautologyResults = ExamMarksDAO.search(conn, tautologyPayload);
        assertEquals("SQL Injection tautology (' OR '1'='1') returns 0 rows (treated as literal)", 0, tautologyResults.size());

        // 2. Piggybacked Statement Attack: "101; DROP TABLE exam_marks; --"
        String piggybackedPayload = "101; DROP TABLE exam_marks; --";
        List<ExamMarksDAO.ExamMark> piggybackedResults = ExamMarksDAO.search(conn, piggybackedPayload);
        assertEquals("Piggybacked injection returns 0 rows", 0, piggybackedResults.size());

        // Prove that exam_marks was NOT dropped
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

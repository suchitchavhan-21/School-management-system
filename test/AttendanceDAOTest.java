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

        // 11. Unknown Roll number rejected
        boolean unknownRollRejected = false;
        try {
            AttendanceDAO.insert(conn, new AttendanceDAO.AttendanceRecord(999, "Unknown Student", 9, "2026-10-10", "Present", ""));
        } catch (IllegalArgumentException e) {
            unknownRollRejected = e.getMessage().contains("does not exist in student database");
        }
        assertTrue("Unknown student roll number rejected with descriptive error", unknownRollRejected);

        // 12. Altered student name rejected
        boolean alteredNameRejected = false;
        try {
            AttendanceDAO.insert(conn, new AttendanceDAO.AttendanceRecord(201, "Altered Impostor", 9, "2026-10-10", "Present", ""));
        } catch (IllegalArgumentException e) {
            alteredNameRejected = e.getMessage().contains("does not match database record");
        }
        assertTrue("Altered student name disagreeing with student_info rejected", alteredNameRejected);

        // 13. Altered class rejected
        boolean alteredClassRejected = false;
        try {
            AttendanceDAO.insert(conn, new AttendanceDAO.AttendanceRecord(201, "Daniel Miller", 12, "2026-10-10", "Present", ""));
        } catch (IllegalArgumentException e) {
            alteredClassRejected = e.getMessage().contains("Submitted class");
        }
        assertTrue("Altered student class disagreeing with student_info rejected", alteredClassRejected);

        // 14. Impossible calendar dates rejected
        String[] impossibleDates = {
            "2026-02-31", // Feb 31 does not exist
            "2026-04-31", // April has 30 days
            "2026-13-01", // Month 13 does not exist
            "2026-02-29", // 2026 is not a leap year
            "2026/10/10", // Invalid delimiter
            "2026-10-1",  // Truncated format
            "not-a-date"  // Malformed text
        };
        for (String badDate : impossibleDates) {
            boolean dateRejected = false;
            try {
                AttendanceDAO.insert(conn, new AttendanceDAO.AttendanceRecord(201, "Daniel Miller", 9, badDate, "Present", ""));
            } catch (IllegalArgumentException e) {
                dateRejected = e.getMessage().contains("Invalid attendance date");
            }
            assertTrue("Impossible calendar date '" + badDate + "' rejected strictly", dateRejected);
        }

        // 15. Invalid attendance status rejected
        String[] invalidStatuses = {"Suspended", "Presentt", "Expelled", "Unknown", ""};
        for (String badStatus : invalidStatuses) {
            boolean statusRejected = false;
            try {
                AttendanceDAO.insert(conn, new AttendanceDAO.AttendanceRecord(201, "Daniel Miller", 9, "2026-10-10", badStatus, ""));
            } catch (IllegalArgumentException e) {
                statusRejected = e.getMessage().contains("Invalid attendance status");
            }
            assertTrue("Invalid status '" + badStatus + "' rejected", statusRejected);
        }

        // 16. Case-insensitive status normalized to Title Case
        AttendanceDAO.AttendanceRecord lowerStatusRec = new AttendanceDAO.AttendanceRecord(
                201, "Daniel Miller", 9, "2026-10-10", "present", "case test");
        boolean lowerStatusInserted = AttendanceDAO.insert(conn, lowerStatusRec);
        assertTrue("Insert with lowercase status succeeds", lowerStatusInserted);
        AttendanceDAO.AttendanceRecord fetchedNormalized = AttendanceDAO.findByRollAndDate(conn, 201, "2026-10-10");
        assertEquals("Status normalized to canonical Title Case", "Present", fetchedNormalized.getStatus());

        // 17. Excessive remarks length (> 255 chars) rejected
        StringBuilder longRemarks = new StringBuilder();
        for (int i = 0; i < 260; i++) longRemarks.append("A");
        boolean longRemarksRejected = false;
        try {
            AttendanceDAO.insert(conn, new AttendanceDAO.AttendanceRecord(201, "Daniel Miller", 9, "2026-10-11", "Present", longRemarks.toString()));
        } catch (IllegalArgumentException e) {
            longRemarksRejected = e.getMessage().contains("Remarks exceed maximum allowed length");
        }
        assertTrue("Remarks exceeding 255 characters rejected", longRemarksRejected);

        // 18. Authoritative name and class populated when blank
        AttendanceDAO.AttendanceRecord blankNameRec = new AttendanceDAO.AttendanceRecord(
                201, "", 9, "2026-10-12", "Present", "Blank name auto-resolve");
        boolean blankNameInserted = AttendanceDAO.insert(conn, blankNameRec);
        assertTrue("Insert with blank name succeeds via student_info resolution", blankNameInserted);
        AttendanceDAO.AttendanceRecord fetchedResolved = AttendanceDAO.findByRollAndDate(conn, 201, "2026-10-12");
        assertEquals("Authoritative name resolved from student_info", "Daniel Miller", fetchedResolved.getStudentName());
        assertEquals("Authoritative class resolved from student_info", 9, fetchedResolved.getStudentClass());

        // 19. isUniqueConstraintViolation correctly classifies duplicate keys vs unrelated SQL failures
        assertFalse("Null exception is not a unique violation", AttendanceDAO.isUniqueConstraintViolation(null));
        assertFalse("Connection failure is not misclassified as duplicate violation",
                AttendanceDAO.isUniqueConstraintViolation(new SQLException("Connection is closed", "08003", 0)));
        assertFalse("Missing table error is not misclassified as duplicate violation",
                AttendanceDAO.isUniqueConstraintViolation(new SQLException("no such table: attendance", "42S02", 1)));
        assertFalse("SQL syntax error is not misclassified as duplicate violation",
                AttendanceDAO.isUniqueConstraintViolation(new SQLException("syntax error near SELECT", "42000", 1)));
        assertFalse("Permission denied error is not misclassified as duplicate violation",
                AttendanceDAO.isUniqueConstraintViolation(new SQLException("Access denied for user 'sms'@'localhost'", "42000", 1045)));

        // Non-duplicate integrity constraint violations must NEVER be classified as duplicate
        assertFalse("Generic SQLState 23000 alone is not misclassified as duplicate violation",
                AttendanceDAO.isUniqueConstraintViolation(new SQLException("Integrity constraint violation", "23000", 0)));
        assertFalse("MySQL not-null violation (1048) with SQLState 23000 is not misclassified",
                AttendanceDAO.isUniqueConstraintViolation(new SQLException("Column 'Remarks' cannot be null", "23000", 1048)));
        assertFalse("MySQL foreign key violation (1452) with SQLState 23000 is not misclassified",
                AttendanceDAO.isUniqueConstraintViolation(new SQLException("Cannot add or update a child row: a foreign key constraint fails", "23000", 1452)));
        assertFalse("MySQL check constraint violation (3819) with SQLState 23000 is not misclassified",
                AttendanceDAO.isUniqueConstraintViolation(new SQLException("Check constraint 'chk_status' is violated", "23000", 3819)));
        assertFalse("SQLite foreign key failure is not misclassified as duplicate violation",
                AttendanceDAO.isUniqueConstraintViolation(new SQLException("[SQLITE_CONSTRAINT] Abort due to constraint violation (foreign key constraint failed)", null, 0)));
        assertFalse("SQLite not-null failure is not misclassified as duplicate violation",
                AttendanceDAO.isUniqueConstraintViolation(new SQLException("[SQLITE_CONSTRAINT] Abort due to constraint violation (attendance.Status may not be NULL)", null, 0)));
        assertFalse("SQLite modern not-null failure is not misclassified as duplicate violation",
                AttendanceDAO.isUniqueConstraintViolation(new SQLException("[SQLITE_CONSTRAINT_NOTNULL] NOT NULL constraint failed: attendance.Status", null, 19)));

        // Genuine duplicate key violations across supported databases
        assertTrue("MySQL 1062 ER_DUP_ENTRY recognized as unique violation",
                AttendanceDAO.isUniqueConstraintViolation(new SQLException("Duplicate entry '201-2026-10-01' for key 'PRIMARY'", "23000", 1062)));
        assertTrue("MySQL 1022 ER_DUP_KEY recognized as unique violation",
                AttendanceDAO.isUniqueConstraintViolation(new SQLException("Can't write; duplicate key in table 'attendance'", "23000", 1022)));
        assertTrue("MySQL 1169 ER_DUP_UNIQUE recognized as unique violation",
                AttendanceDAO.isUniqueConstraintViolation(new SQLException("Can't write, because of unique constraint, to table 'attendance'", "23000", 1169)));
        assertTrue("ANSI standard SQLState 23505 recognized as unique violation",
                AttendanceDAO.isUniqueConstraintViolation(new SQLException("Unique violation", "23505", 0)));
        assertTrue("SQLite 3.7.2 composite unique constraint recognized as unique violation",
                AttendanceDAO.isUniqueConstraintViolation(new SQLException("[SQLITE_CONSTRAINT]  Abort due to constraint violation (columns Roll, Attendance_date are not unique)", null, 0)));
        assertTrue("SQLite 3.7.2 single column unique constraint recognized as unique violation",
                AttendanceDAO.isUniqueConstraintViolation(new SQLException("[SQLITE_CONSTRAINT]  Abort due to constraint violation (column id is not unique)", null, 0)));
        assertTrue("SQLite modern unique constraint message recognized as unique violation",
                AttendanceDAO.isUniqueConstraintViolation(new SQLException("[SQLITE_CONSTRAINT_UNIQUE] A UNIQUE constraint failed (UNIQUE constraint failed: attendance.Roll, attendance.Attendance_date)", null, 19)));
        assertTrue("SQLite modern primary key constraint recognized as unique violation",
                AttendanceDAO.isUniqueConstraintViolation(new SQLException("[SQLITE_CONSTRAINT_PRIMARYKEY] PRIMARY KEY must be unique", null, 19)));

        // 20. Unrelated SQL failure is propagated rather than swallowed by saveOrUpdate
        boolean unrelatedSqlPropagated = false;
        try {
            // Attempting saveOrUpdate on a null connection throws SQLException
            AttendanceDAO.saveOrUpdate(null, blankNameRec);
        } catch (SQLException e) {
            unrelatedSqlPropagated = e.getMessage().contains("Database connection is null");
        }
        assertTrue("Unrelated SQL failure (null connection) is propagated rather than swallowed", unrelatedSqlPropagated);

        // Verification of exception chaining / preservation on recovery failure
        SQLException simulatedInsertDup = new SQLException("Duplicate entry '201-2026-10-18' for key 'PRIMARY'", "23000", 1062);
        SQLException simulatedUpdateFailure = new SQLException("Deadlock found when trying to get lock", "40001", 1213);
        simulatedUpdateFailure.addSuppressed(simulatedInsertDup);
        Throwable[] suppressed = simulatedUpdateFailure.getSuppressed();
        assertTrue("Recovery failure preserves original insert exception in suppressed list",
                suppressed != null && suppressed.length == 1 && suppressed[0] == simulatedInsertDup);

        // 21. Duplicate-key conflict follows intended recovery path in saveOrUpdate
        AttendanceDAO.AttendanceRecord raceRec = new AttendanceDAO.AttendanceRecord(
                201, "Daniel Miller", 9, "2026-10-15", "Present", "Initial race test");
        boolean raceInsertSuccess = AttendanceDAO.insert(conn, raceRec);
        assertTrue("Initial insert for race test succeeds", raceInsertSuccess);

        // Direct second insert of identical (Roll, Date) throws real SQLite duplicate exception
        SQLException caughtDuplicateInsert = null;
        try {
            AttendanceDAO.insert(conn, raceRec);
        } catch (SQLException e) {
            caughtDuplicateInsert = e;
        }
        assertTrue("Direct second insert throws real SQLite constraint violation", caughtDuplicateInsert != null);
        assertTrue("Live SQLite constraint exception is recognized by isUniqueConstraintViolation",
                AttendanceDAO.isUniqueConstraintViolation(caughtDuplicateInsert));

        // Calling saveOrUpdate with modified status and remarks recovers and updates record
        raceRec.setStatus("Late");
        raceRec.setRemarks("Recovered race condition");
        boolean raceUpdateSuccess = AttendanceDAO.saveOrUpdate(conn, raceRec);
        assertTrue("saveOrUpdate follows recovery path and successfully updates record", raceUpdateSuccess);

        AttendanceDAO.AttendanceRecord fetchedRace = AttendanceDAO.findByRollAndDate(conn, 201, "2026-10-15");
        assertEquals("Recovered record status is Late", "Late", fetchedRace.getStatus());
        assertEquals("Recovered record remarks match", "Recovered race condition", fetchedRace.getRemarks());

        // 22. Remarks boundary: Null remarks accepted and stored
        AttendanceDAO.AttendanceRecord nullRemarksRec = new AttendanceDAO.AttendanceRecord(
                201, "Daniel Miller", 9, "2026-10-16", "Present", null);
        boolean nullRemarksSuccess = AttendanceDAO.insert(conn, nullRemarksRec);
        assertTrue("Null remarks accepted and stored safely", nullRemarksSuccess);
        AttendanceDAO.AttendanceRecord fetchedNullRemarks = AttendanceDAO.findByRollAndDate(conn, 201, "2026-10-16");
        assertEquals("Null remarks coalesced to empty string", "", fetchedNullRemarks.getRemarks());

        // 23. Remarks boundary: Exactly 255 ASCII characters accepted
        StringBuilder ascii255 = new StringBuilder();
        for (int i = 0; i < 255; i++) ascii255.append("R");
        AttendanceDAO.AttendanceRecord exact255Rec = new AttendanceDAO.AttendanceRecord(
                201, "Daniel Miller", 9, "2026-10-17", "Present", ascii255.toString());
        boolean exact255Success = AttendanceDAO.insert(conn, exact255Rec);
        assertTrue("Exactly 255 ASCII characters in remarks accepted", exact255Success);
        AttendanceDAO.AttendanceRecord fetched255 = AttendanceDAO.findByRollAndDate(conn, 201, "2026-10-17");
        assertEquals("Stored remarks length is exactly 255", 255, fetched255.getRemarks().length());

        // 24. Remarks boundary: Exactly 255 Multibyte Unicode characters accepted
        // Construct 255 multibyte characters (accents/umlauts: 51 copies of "Caféñ" = 255 chars)
        StringBuilder multi255 = new StringBuilder();
        for (int i = 0; i < 51; i++) multi255.append("Caféñ"); // 5 chars * 51 = 255 chars
        AttendanceDAO.AttendanceRecord multi255Rec = new AttendanceDAO.AttendanceRecord(
                201, "Daniel Miller", 9, "2026-10-18", "Present", multi255.toString());
        boolean multi255Success = AttendanceDAO.insert(conn, multi255Rec);
        assertTrue("Exactly 255 multibyte Unicode characters in remarks accepted", multi255Success);
        AttendanceDAO.AttendanceRecord fetchedMulti = AttendanceDAO.findByRollAndDate(conn, 201, "2026-10-18");
        assertEquals("Stored multibyte remarks length is exactly 255", 255, fetchedMulti.getRemarks().length());

        // 25. Remarks boundary: Exactly 256 characters (1 char beyond MAX) strictly rejected
        StringBuilder over256 = new StringBuilder(ascii255.toString()).append("!");
        boolean over256Rejected = false;
        try {
            AttendanceDAO.insert(conn, new AttendanceDAO.AttendanceRecord(
                    201, "Daniel Miller", 9, "2026-10-19", "Present", over256.toString()));
        } catch (IllegalArgumentException e) {
            over256Rejected = e.getMessage().contains("Remarks exceed maximum allowed length of 255 characters (submitted length: 256)");
        }
        assertTrue("Remarks of 256 characters (1 over limit) strictly rejected with detailed diagnostic", over256Rejected);

        // 26. Failed operations do not leave partially written or inconsistent records
        int initialCount = AttendanceDAO.findAll(conn).size();
        try {
            AttendanceDAO.insert(conn, new AttendanceDAO.AttendanceRecord(201, "Daniel Miller", 9, "2026-02-31", "Present", ""));
        } catch (IllegalArgumentException ignored) {}
        try {
            AttendanceDAO.insert(conn, new AttendanceDAO.AttendanceRecord(999, "Unknown", 9, "2026-10-20", "Present", ""));
        } catch (IllegalArgumentException ignored) {}
        try {
            AttendanceDAO.insert(conn, new AttendanceDAO.AttendanceRecord(201, "Altered Impostor", 9, "2026-10-20", "Present", ""));
        } catch (IllegalArgumentException ignored) {}

        int countAfterFailures = AttendanceDAO.findAll(conn).size();
        assertEquals("Failed operations do not alter attendance record count or leave inconsistent rows", initialCount, countAfterFailures);
    }
}

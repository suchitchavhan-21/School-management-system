# Evidence-Based Automated Test Results & Benchmark Report

> **Notice**: The authoritative, permanently maintained version of this report is located at [`docs/testing/TEST_RESULTS.md`](docs/testing/TEST_RESULTS.md).

**Project**: School Management System in Java
**Environment**: OpenJDK 11.0.26 LTS, Windows / Cross-Platform
**Execution Date**: 2026-10-18
**Harness**: `test/SystemTestSuite.java` (JUnit-style automated test runner)
**Database Isolation**: Ephemeral SQLite databases (`File.createTempFile`)

---

## 1. Test Suite Summary Matrix

| Suite Name | Scope | Total Tests | Passed | Failed | Skipped | Status |
|---|---|---|---|---|---|---|
| **GradeCalculatorTest** | Academic grading, percentages, GPA 5.0 scale, subject rules | 36 | 36 | 0 | 0 | **100% PASS** |
| **ExamMarksDAOTest** | CRUD, unique constraints, transactional persistence | 20 | 20 | 0 | 0 | **100% PASS** |
| **AttendanceDAOTest** | Daily tracking, unique constraint, analytics calculation | 25 | 25 | 0 | 0 | **100% PASS** |
| **SecurityAndResilienceTest** | PBKDF2 hashing, DoS prevention, setup safety, SQL injection | 89 | 89 | 0 | 0 | **100% PASS** |
| **TOTAL** | **Entire Application Verification** | **170** | **170** | **0** | **0** | **100% PASS** |

- **Total Execution Time**: ~11,513 ms (~11.5 seconds)
- **Exit Code**: `0`

---

## 2. Test Execution Output Log

```text
==================================================
Running GradeCalculatorTest...
==================================================
  [PASS] Perfect 100 marks yields Total 500
  [PASS] Perfect 100 marks yields Percentage 100.0%
  [PASS] Perfect 100 marks yields Grade A+
  [PASS] Perfect 100 marks yields GPA 5.0
  [PASS] Perfect 100 marks yields Passed true
  [PASS] 80% boundary yields Grade A+
  [PASS] 80% boundary yields GPA 5.0
  [PASS] 70-79% boundary yields Grade A
  [PASS] 70-79% boundary yields GPA 4.0
  [PASS] 60-69% boundary yields Grade A-
  [PASS] 60-69% boundary yields GPA 3.5
  [PASS] 50-59% boundary yields Grade B
  [PASS] 50-59% boundary yields GPA 3.0
  [PASS] 40-49% boundary yields Grade C
  [PASS] 40-49% boundary yields GPA 2.0
  [PASS] 33-39% boundary yields Grade D
  [PASS] 33-39% boundary yields GPA 1.0
  [PASS] Below 33% yields Grade F
  [PASS] Below 33% yields GPA 0.0
  [PASS] Below 33% yields Passed false
  [PASS] Single subject fail (<33) causes overall Fail
  [PASS] Single subject fail yields Grade F
  [PASS] Single subject fail yields GPA 0.0
  [PASS] Negative marks throws IllegalArgumentException
  [PASS] Marks > 100 throws IllegalArgumentException
  [PASS] Valid mark array passes validation
  [PASS] Null mark array throws IllegalArgumentException
  [PASS] Wrong size mark array throws IllegalArgumentException
  [PASS] Grade A+ remarks match
  [PASS] Grade F remarks match
  [PASS] Passing status text is Passed
  [PASS] Failing status text is Failed
  [PASS] Format percentage formats 2 decimal places
  [PASS] Format percentage formats whole numbers
  [PASS] Format GPA formats 2 decimal places
  [PASS] Format GPA formats 0.00
GradeCalculatorTest Summary: 36 Passed, 0 Failed, 0 Skipped out of 36 tests.

==================================================
Running ExamMarksDAOTest (Isolated Test DB)...
==================================================
[DatabaseBootstrap] Ensuring database tables exist...
[DatabaseBootstrap] Database schema check completed successfully.
  [PASS] Seed student record in student_info
  [PASS] Insert exam marks succeeds
  [PASS] Fetched mark record is not null
  [PASS] Roll matches
  [PASS] Student name matches
  [PASS] Class matches
  [PASS] Exam term matches
  [PASS] Exam year matches
  [PASS] English mark matches
  [PASS] Math mark matches
  [PASS] Total marks matches
  [PASS] Percentage matches
  [PASS] Grade matches
  [PASS] GPA matches
  [PASS] Duplicate (Roll, Term, Year) rejected by constraint
  [PASS] findByRoll returns 1 record
  [PASS] findByClassAndTerm returns 1 record
  [PASS] findAll returns 1 record
  [PASS] Delete mark record succeeds
  [PASS] Deleted record no longer exists
ExamMarksDAOTest Summary: 20 Passed, 0 Failed, 0 Skipped out of 20 tests.

==================================================
Running AttendanceDAOTest (Isolated Test DB)...
==================================================
[DatabaseBootstrap] Ensuring database tables exist...
[DatabaseBootstrap] Database schema check completed successfully.
  [PASS] Seed student record in student_info
  [PASS] Insert attendance record succeeds
  [PASS] Fetched attendance record is not null
  [PASS] Roll matches
  [PASS] Student name matches
  [PASS] Class matches
  [PASS] Date matches
  [PASS] Status is Present
  [PASS] Remarks match
  [PASS] Duplicate (Roll, Attendance_date) direct insert rejected by constraint
  [PASS] saveOrUpdate updates existing date record safely
  [PASS] Updated status is Late
  [PASS] Updated remarks is Bus delayed
  [PASS] Summary total tracked days is 6
  [PASS] Summary present count is 3
  [PASS] Summary late count is 1
  [PASS] Summary absent count is 1
  [PASS] Summary excused count is 1
  [PASS] Summary attendance percentage is 66.67%
  [PASS] findByRoll returns 6 records
  [PASS] findByClassAndDate returns 1 record
  [PASS] findAll returns 6 total records
  [PASS] Delete attendance record succeeds
  [PASS] Deleted record no longer exists
  [PASS] Total records after delete is 5
AttendanceDAOTest Summary: 25 Passed, 0 Failed, 0 Skipped out of 25 tests.

==================================================
Running SecurityAndResilienceTest Suite...
==================================================
[DatabaseBootstrap] Ensuring database tables exist...
[DatabaseBootstrap] Database schema check completed successfully.
  [PASS] Pre-existing sentinel file exists prior to test operations
[DatabaseBootstrap] Ensuring database tables exist...
[DatabaseBootstrap] Database schema check completed successfully.
  [PASS] Pre-existing sentinel file survived test database lifecycle
  [PASS] Pre-existing file content is completely intact
  [PASS] Isolated test connection is active and not null
  [PASS] Connected driver is SQLite for isolated test
[DatabaseBootstrap] Ensuring database tables exist...
[DatabaseBootstrap] Database schema check completed successfully.
  [PASS] Initial schema bootstrap returns true
[DatabaseBootstrap] Ensuring database tables exist...
[DatabaseBootstrap] Database schema check completed successfully.
  [PASS] Repeated schema bootstrap is idempotent and returns true
  [PASS] Table 'pass' exists
  [PASS] Table 'admin' exists
  [PASS] Table 'student_info' exists
  [PASS] Table 'teacher_info' exists
  [PASS] Table 'employee_info' exists
  [PASS] Table 'exam_marks' exists
  [PASS] Table 'attendance' exists
[DatabaseBootstrap] Ensuring database tables exist...
[DatabaseBootstrap] Database schema check completed successfully.
  [PASS] DatabaseBootstrap propagates SQLException when connection is closed or invalid
  [PASS] Generated hash starts with $pbkdf2-sha256$
  [PASS] PasswordSecurity.isHashed returns true for valid hash
  [PASS] PasswordSecurity.isHashed returns false for plaintext password
  [PASS] Correct password validates against hash
  [PASS] Incorrect password rejected by verifyPassword
  [PASS] Two hashes of identical password produce distinct salts and values
  [PASS] Second hash also validates with correct password
  [PASS] PasswordSecurity.wipe successfully zeroes character array
  [PASS] Null hash rejected
  [PASS] Empty hash rejected
  [PASS] Truncated hash parts rejected
  [PASS] Invalid algorithm prefix rejected
  [PASS] Odd-length salt hex rejected
  [PASS] Invalid hex characters in salt rejected
  [PASS] Iterations below minimum rejected
  [PASS] Extreme iterations (DoS attempt) rejected immediately without CPU hang
  [PASS] Non-numeric iterations rejected
  [PASS] Salt smaller than 16 bytes rejected
  [PASS] Key smaller than 32 bytes rejected
  [PASS] Malformed hash safely returns false from verifyPassword
  [PASS] verifyPassword validates older 65,536-iteration hash successfully
  [PASS] needsRehash recognizes older hash with 65,536 iterations needs upgrade
  [PASS] authenticateAndMigrateUser succeeds for older hash account
  [PASS] Upgraded hash starts with $pbkdf2-sha256$210000$
  [PASS] Subsequent login verifies against upgraded hash
  [PASS] Rejects password < 8 chars
  [PASS] Rejects password without uppercase
  [PASS] Rejects password without lowercase
  [PASS] Rejects password without digit/symbol
  [PASS] Rejects password containing whitespace
  [PASS] Accepts strong password meeting all criteria
  [PASS] Rejects admin key < 8 chars
  [PASS] Rejects admin key without digits or symbols
  [PASS] Rejects admin key containing whitespace
  [PASS] Accepts strong admin authorization key
  [PASS] Empty database detects SetupState.UNINITIALIZED
  [PASS] isSetupRequired is true for UNINITIALIZED
  [PASS] Missing admin detects SetupState.INCONSISTENT_MISSING_ADMIN
  [PASS] isSetupRequired is false when missing admin (protects existing users)
  [PASS] Missing users detects SetupState.INCONSISTENT_MISSING_USERS
  [PASS] isSetupRequired is false when missing users (protects existing admin key)
  [PASS] Both populated detects SetupState.INITIALIZED
  [PASS] isSetupRequired is false when INITIALIZED
  [PASS] Legitimate initial setup succeeds on clean DB
  [PASS] Original user hash exists
  [PASS] Original admin hash exists
  [PASS] Re-running setup when initialized throws IllegalStateException
  [PASS] Attacker account was NOT created
  [PASS] Legitimate user still exists
  [PASS] Legitimate user password hash was NOT overwritten
  [PASS] Legitimate admin key still exists
  [PASS] Legitimate admin key was NOT overwritten
  [PASS] pass table count remains exactly 1
  [PASS] admin table count remains exactly 1
  [PASS] isSetupRequired is true when database has no users
  [PASS] initializeFirstRunCredentials completes successfully
  [PASS] isSetupRequired is false after complete initialization
  [PASS] pass table has 1 record
  [PASS] admin table has 1 record
  [PASS] Forced second-step failure throws SQLException
  [PASS] No partial record committed to pass table after rollback
  [PASS] Retry of first-run setup succeeds after schema recovery
  [PASS] PBKDF2-HMAC-SHA256 (210,000 iterations) benchmark produced valid hash
  [PASS] Benchmark hash verified successfully
  [PASS] Hash generation latency is within bounds (> 0ms and < 4000ms)
  [PASS] Hash verification latency is within bounds (> 0ms and < 4000ms)
  [BENCHMARK] PBKDF2-HMAC-SHA256 (210,000 iter): Hash = 359 ms, Verify = 355 ms
  [PASS] Admin single-parameter query binds at index 1 without ParameterIndexOutOfBoundsException
  [PASS] Admin authentication succeeds with correct PIN
  [PASS] Admin authentication fails with incorrect PIN
  [PASS] Legitimate exam mark seeded for injection tests
  [PASS] SQL Injection tautology (' OR '1'='1') returns 0 rows (treated as literal)
  [PASS] Piggybacked injection returns 0 rows
  [PASS] Table 'exam_marks' survived piggybacked DROP TABLE attempt
  [PASS] UNION-based injection payload returns 0 rows
SecurityAndResilienceTest Summary: 89 Passed, 0 Failed, 0 Skipped out of 89 tests.

================================================================================
                     COMPREHENSIVE TEST SUITE SUMMARY
================================================================================
  GradeCalculatorTest:        36 Passed,  0 Failed,  0 Skipped /  36 Total
  ExamMarksDAOTest:           20 Passed,  0 Failed,  0 Skipped /  20 Total
  AttendanceDAOTest:          25 Passed,  0 Failed,  0 Skipped /  25 Total
  SecurityAndResilienceTest:  89 Passed,  0 Failed,  0 Skipped /  89 Total
--------------------------------------------------------------------------------
  TOTAL SUITE RESULTS:       170 Passed,  0 Failed,  0 Skipped / 170 Total
  EXECUTION TIME:            11513 ms
================================================================================
>>> VERDICT: 100% GREEN - ALL TEST SUITES PASSED RIGOROUS ZERO-TRUST AUDIT. <<<
================================================================================
```

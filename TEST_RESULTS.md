# Automated Quality & Security Test Results

**Date**: 2026-10-18  
**Environment**: OpenJDK 11.0.26 LTS, Windows 10/11  
**Project**: School Management System in Java  
**Runner**: `SystemTestSuite` (`test/SystemTestSuite.java`)  
**Overall Verdict**: **100% GREEN (88 Passed, 0 Failed, 0 Skipped)**

---

## 1. Test Harness Architecture & Philosophy

The test harness follows a strict zero-trust methodology:
1. **Zero Production Risk**: All persistence and security integration tests run against ephemeral SQLite databases (`test_exam_marks.sqlite` and `test_security.sqlite`) that are initialized on demand and deleted immediately upon suite completion. Production databases are untouched.
2. **Authoritative Assertions**: No vacuous or placeholder assertions (e.g. `assertTrue(..., true)`). All assertions test actual outcomes, returned rows, and exception handling.
3. **No Silent Failures**: Database connection failures or unexpected runtime exceptions fail the suite and result in a nonzero exit code (`System.exit(1)`).
4. **Comprehensive Metric Accounting**: Every test suite explicitly logs individual passes, failures, and skips, and aggregates them into the master test harness summary.

---

## 2. Test Execution Command & Raw Output

### Compilation
```powershell
javac -cp "dist/lib/*;src;build/classes" -d build/classes src/*.java test/*.java
```
*Status: Exit Code 0. Clean compilation.*

### Master Test Suite Execution
```powershell
java -cp "dist/lib/*;build/classes" SystemTestSuite
```

### Observed Execution Log
```text
================================================================================
   SCHOOL MANAGEMENT SYSTEM - ZERO-TRUST VERIFICATION & QUALITY TEST HARNESS    
================================================================================

==================================================
Running GradeCalculatorTest Suite...
==================================================
  [PASS] Perfect score total marks
  [PASS] Perfect score percentage
  [PASS] Perfect score grade
  [PASS] Perfect score GPA
  [PASS] Perfect score is passed
  [PASS] 80% grade is A+
  [PASS] 80% GPA is 5.0
  [PASS] 80% is passed
  [PASS] 75% grade is A
  [PASS] 75% GPA is 4.0
  [PASS] 75% is passed
  [PASS] 65% grade is A-
  [PASS] 65% GPA is 3.5
  [PASS] 65% is passed
  [PASS] 55% grade is B
  [PASS] 55% GPA is 3.0
  [PASS] 55% is passed
  [PASS] 45% grade is C
  [PASS] 45% GPA is 2.0
  [PASS] 45% is passed
  [PASS] 35% grade is D
  [PASS] 35% GPA is 1.0
  [PASS] 35% is passed
  [PASS] 32% grade is F
  [PASS] 32% GPA is 0.0
  [PASS] 32% is not passed
  [PASS] High average with single subject fail must receive F
  [PASS] Single subject fail must have 0.0 GPA
  [PASS] Single subject fail must fail overall
  [PASS] Remarks must indicate Science failure
  [PASS] Rejects negative marks
  [PASS] Rejects marks > 100
  [PASS] Rejects NaN marks
  [PASS] Total marks rounded to 2 places
  [PASS] Percentage rounded to 2 places
  [PASS] Grade with 81.48% is A+
GradeCalculatorTest Summary: 36 Passed, 0 Failed, 0 Skipped out of 36 tests.

==================================================
Running ExamMarksDAOTest (Isolated Test DB)...
==================================================
[DatabaseBootstrap] Ensuring database tables exist...
[DatabaseBootstrap] Database schema check completed successfully.
  [PASS] Seed authoritative student record for test
  [PASS] Insert exam mark record
  [PASS] Fetched mark is not null
  [PASS] Fetched Roll matches
  [PASS] Fetched Student Name matches
  [PASS] Fetched Grade matches
  [PASS] Fetched GPA matches
  [PASS] Save or update existing record
  [PASS] Fetched updated mark is not null
  [PASS] Updated Grade is A-
  [PASS] Updated GPA is 3.5
  [PASS] Delete exam mark record
  [PASS] Deleted mark cannot be found
  [PASS] Initial insert of exam mark succeeds
  [PASS] Duplicate (Roll, Term, Year) direct insertion rejected by UNIQUE constraint
  [PASS] Duplicate term safely updated via saveOrUpdate without constraint violation
  [PASS] Search by roll returns match
  [PASS] Search by roll returns correct student
  [PASS] Search by name substring returns match
  [PASS] FindAll returns non-empty list
ExamMarksDAOTest Summary: 20 Passed, 0 Failed, 0 Skipped out of 20 tests.

==================================================
Running SecurityAndResilienceTest Suite...
==================================================
[DatabaseBootstrap] Ensuring database tables exist...
[DatabaseBootstrap] Database schema check completed successfully.
  [PASS] Isolated test connection is active and not null
  [PASS] Connected driver is SQLite for isolated test
[DatabaseBootstrap] Ensuring database tables exist...
[DatabaseBootstrap] Database schema check completed successfully.
  [PASS] Table 'pass' exists
  [PASS] Table 'admin' exists
  [PASS] Table 'student_info' exists
  [PASS] Table 'teacher_info' exists
  [PASS] Table 'employee_info' exists
  [PASS] Table 'exam_marks' exists
  [PASS] No hardcoded default admin credentials seeded
  [PASS] No hardcoded default user credentials seeded
  [PASS] Generated hash starts with $pbkdf2-sha256$
  [PASS] PasswordSecurity.isHashed returns true for valid hash
  [PASS] PasswordSecurity.isHashed returns false for plaintext password
  [PASS] Correct password validates against hash
  [PASS] Incorrect password rejected by verifyPassword
  [PASS] Two hashes of identical password produce distinct salts and values
  [PASS] Second hash also validates with correct password
  [PASS] PasswordSecurity.wipe successfully zeroes character array
  [PASS] Legacy user initially exists with plaintext password
  [PASS] Login with wrong password fails for legacy user
  [PASS] Login with correct password succeeds for legacy user
  [PASS] Legacy plaintext was migrated to $pbkdf2-sha256$ in database
  [PASS] Subsequent login succeeds against newly migrated PBKDF2 hash
  [PASS] Admin single-parameter query binds at index 1 without ParameterIndexOutOfBoundsException
  [PASS] Admin authentication succeeds with correct PIN
  [PASS] Admin authentication fails with incorrect PIN
  [PASS] Legacy plaintext admin PIN authenticates successfully
  [PASS] Legitimate exam mark seeded for injection tests
  [PASS] SQL Injection tautology (' OR '1'='1') returns 0 rows (treated as literal)
  [PASS] Piggybacked injection returns 0 rows
  [PASS] Table 'exam_marks' survived piggybacked DROP TABLE attempt
  [PASS] UNION-based injection payload returns 0 rows
SecurityAndResilienceTest Summary: 32 Passed, 0 Failed, 0 Skipped out of 32 tests.

================================================================================
                     COMPREHENSIVE TEST SUITE SUMMARY                           
================================================================================
  GradeCalculatorTest:        36 Passed,  0 Failed,  0 Skipped /  36 Total
  ExamMarksDAOTest:           20 Passed,  0 Failed,  0 Skipped /  20 Total
  SecurityAndResilienceTest:  32 Passed,  0 Failed,  0 Skipped /  32 Total
--------------------------------------------------------------------------------
  TOTAL SUITE RESULTS:        88 Passed,  0 Failed,  0 Skipped /  88 Total
  EXECUTION TIME:            2331 ms
================================================================================
>>> VERDICT: 100% GREEN - ALL TEST SUITES PASSED RIGOROUS ZERO-TRUST AUDIT. <<<
================================================================================
```

---

## 3. Test Breakdown by Component

| Component Suite | Test Count | Pass | Fail | Skip | Verification Scope |
|---|:---:|:---:|:---:|:---:|---|
| **GradeCalculatorTest** | 36 | 36 | 0 | 0 | Grade boundaries (A+, A, A-, B, C, D, F), GPA scaling (5.0 to 0.0), pass threshold, single-subject failure override rule, input domain validation (negative, >100, NaN), and rounding accuracy. |
| **ExamMarksDAOTest** | 20 | 20 | 0 | 0 | Isolated SQLite persistence, CRUD lifecycle, authoritative relationship to `student_info`, database-level unique constraint (`Roll, Exam_term, Exam_year`), idempotent `saveOrUpdate`, roll/name search queries, and table cleanups. |
| **SecurityAndResilienceTest** | 32 | 32 | 0 | 0 | Schema bootstrapping without insecure default credentials, PBKDF2 salt uniqueness & verification, character array memory wiping, legacy plaintext password transparent auto-migration, admin PIN parameter index integrity, and immunity against SQL injection (Tautology, Piggybacked DDL, UNION attacks). |
| **Total Test Harness** | **88** | **88** | **0** | **0** | **100% Verified** |

---

## 4. Tests Not Run & Environmental Disclosures

- **Remote MySQL Live Execution**: While MySQL DDL compatibility was verified in `DatabaseBootstrap` (`AUTO_INCREMENT` clause) and connection configuration logic in `javaconnect` was validated, live remote MySQL queries were not executed because a live MySQL server daemon was not available in this local environment.
- **Physical Audio Playback**: Headless environments lack audio hardware; `Help.java` sound loading was verified to handle missing audio files and audio lines safely without throwing uncaught exceptions.

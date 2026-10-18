# Zero-Trust Code Audit Register: School Management System

> **Notice**: The authoritative, permanently maintained version of this register is located at [`docs/audit/ZERO_TRUST_AUDIT_REGISTER.md`](docs/audit/ZERO_TRUST_AUDIT_REGISTER.md).

**Repository**: `https://github.com/suchitchavhan-21/School-management-system.git`
**Base Commit**: `010085f768dff5e2be40974c1b96678a24349f90`
**Audit Verification Date**: 2026-10-18
**Auditor Roles**: Senior Java Engineer, Application Security Engineer, Database Architect, QA Lead, Release Engineer

---

## 1. Verified Baseline Audit Findings

| Issue ID | File & Line(s) | Category | Severity | Confidence | Root Cause & Evidence | Proposed Remediation |
|---|---|---|---|---|---|---|
| **SEC-01** | `src/Login_frame.java` | Security | **CRITICAL** | **CONFIRMED** | Query `select * from pass where Username=? and Password=?` checked passwords in plaintext; credentials stored unhashed. | Implement salted PBKDF2 hashing via `PasswordSecurity.java`, with automatic migration of legacy credentials on successful login. |
| **SEC-02** | `src/Login_frame.java`, `src/Admin.java` | Security | **HIGH** | **CONFIRMED** | Use of deprecated `JPasswordField.getText()` retained plain passwords in heap memory. | Switch to `JPasswordField.getPassword()`, wipe character array with `PasswordSecurity.wipe(pwd)`. |
| **SEC-03** | `src/DatabaseBootstrap.java` | Security | **CRITICAL** | **CONFIRMED** | Hardcoded production credentials (`school`/`school`, `admin`/`admin`, `123`) were automatically inserted on startup. | Remove hardcoded credentials. Implement non-destructive first-run administrator setup. |
| **SEC-04** | `src/Admin.java` | Reliability / UX | **CRITICAL** | **CONFIRMED** | `setDefaultCloseOperation(EXIT_ON_CLOSE)` terminated entire JVM when the Admin dialog was closed. | Change default close operation to `DISPOSE_ON_CLOSE`. |
| **SEC-05** | `Students_Infromation_System.java`, `Teachers_Information_System.java`, `Employees_Infromation_System.java` | Authorization | **HIGH** | **CONFIRMED** | Admin password check was commented out in Student deletions and absent in Teacher and Employee delete handlers. | Enforce `Admin.verifyAdmin(Component, Connection)` check prior to executing record deletion. |
| **SEC-06** | `login crendentials.txt` | Security | **HIGH** | **CONFIRMED** | Plaintext production credentials originally stored in tracked text file in repository. | Replaced plaintext credentials with zero-trust initial setup security guidance. |
| **SEC-07** | `src/PasswordSecurity.java` | Security / DoS | **HIGH** | **CONFIRMED** | Unbounded iteration counts and unvalidated stored hash format could trigger CPU exhaustion or unhandled exceptions. | Added `isValidHashFormat` pre-computation check and bounded iterations [10,000, 2,000,000]. |
| **SEC-08** | `src/FirstRunSetupDialog.java`, `PasswordSecurity.java` | Data Integrity | **HIGH** | **CONFIRMED** | First-run setup created user and admin in separate statements without transactional rollback on partial failure. | Implemented `initializeFirstRunCredentials` with atomic transaction commit and rollback on failure. |
| **SEC-09** | `src/FirstRunSetupDialog.java` | Security | **HIGH** | **CONFIRMED** | Four-character minimum and short PIN permitted weak administrator credentials. | Enforced strong password and master key policy (minimum 8 characters, mixed case, numbers/symbols). |
| **SEC-10** | `src/PasswordSecurity.java` | Memory Security | **MEDIUM** | **CONFIRMED** | Legacy password check created immutable String on heap; `PBEKeySpec` was not cleared in finally block. | Implemented `constantTimeCharEquals` and invoked `spec.clearPassword()` in finally block. |
| **SEC-11** | `src/PasswordSecurity.java`, `FirstRunSetupDialog.java` | Data Integrity / Auth | **CRITICAL** | **CONFIRMED** | `initializeFirstRunCredentials` executed `DELETE FROM pass` and `DELETE FROM admin` before inserting accounts, risking deletion of existing users. | Added `SetupState` enum with strict check for `UNINITIALIZED`. Prohibited deleting existing accounts or admin keys. Added safe diagnostic warnings for inconsistent states. |
| **DB-01** | `src/javaconnect.java` | Reliability / Integrity | **CRITICAL** | **CONFIRMED** | Silent automatic fallback from MySQL to SQLite upon connection or auth failure exposes disparate datasets without operator awareness. | Introduce `database.properties` for explicit configuration. Never silently switch databases on failure; fail safely with an error. |
| **DB-02** | `src/javaconnect.java` | Security | **MEDIUM** | **CONFIRMED** | Hardcoded MySQL connection details (`root`, empty password, `localhost:3306`, `useSSL=false`). | Make database parameters configurable via external `database.properties`. |
| **DB-03** | `src/DatabaseBootstrap.java` | Data Integrity | **MEDIUM** | **CONFIRMED** | Seed data automatically inserted dummy student records (`Subrata Paul`, `Uttam Paul`) into production table. | Removed automatic insertion of sample students. |
| **DB-04** | `src/DatabaseBootstrap.java` | Portability | **HIGH** | **CONFIRMED** | DDL syntax `Mark_id INTEGER PRIMARY KEY AUTOINCREMENT` is invalid in MySQL (`AUTO_INCREMENT` required). | Detect database product type and apply dialect-specific DDL. |
| **DB-05** | `Students_Infromation_System.java`, `Teachers_Information_System.java`, `Employees_Infromation_System.java` | Resource Leak | **HIGH** | **CONFIRMED** | `PreparedStatement` and `ResultSet` objects not closed in `try-with-resources` or `finally` blocks in save and delete methods. | Refactor all JDBC calls to `try-with-resources`. |
| **DB-06** | `src/Help.java`, `src/Students_and_Teachers_Management.java` | Resource Leak | **MEDIUM** | **CONFIRMED** | `javaconnect.connerDb()` called on frame initialization but connection is never used in either class. | Removed unused database connection fields and initialization calls. |
| **DB-07** | `src/DatabaseBootstrap.java` | Reliability | **HIGH** | **CONFIRMED** | Schema bootstrap swallowed exceptions and returned void, allowing app to proceed with incomplete tables. | Propagated `SQLException`, verified table queries, and closed failed connections. |
| **DB-08** | `src/javaconnect.java` | Security | **HIGH** | **CONFIRMED** | MySQL connection generated defaults with `root` username, blank password, and disabled SSL. | Prohibited `root` with blank password. Added 12-factor environment variable support (`SMS_MYSQL_USER`, `SMS_MYSQL_PASSWORD`). Enforced SSL transport encryption option. Provided `database.properties.template`. |
| **FUNC-01** | `src/Employees_Infromation_System.java` | Code Quality | **LOW** | **CONFIRMED** | Employee ID field and buttons named `txt_teacher_ID`, `cmd_tdelete`, `cmd_tsave` due to copy-paste. | Rename identifiers and form variables to reflect `employee`. |
| **FUNC-02** | `src/All_Teachers_data_details.java`, `src/All_Employees_Data_Details.java` | Code Quality | **LOW** | **CONFIRMED** | JTable variable copy-pasted as `Table_student` instead of teacher/employee. | Correct component variable names. |
| **FUNC-03** | `Students_Infromation_System.java`, `Teachers_Information_System.java`, `Employees_Infromation_System.java` | Correctness | **MEDIUM** | **CONFIRMED** | Delete handlers display "Data Deleted Successfully" even if 0 rows matched the ID. | Check rows affected (`executeUpdate() > 0`) before confirming deletion. |
| **FUNC-04** | `Students_Infromation_System.java`, `Teachers_Information_System.java`, `Employees_Infromation_System.java` | Correctness | **HIGH** | **CONFIRMED** | No input validation before insert/update: empty ID, negative ID, non-numeric values cause uncaught database errors. | Add validation helper to verify non-empty required fields and valid numeric IDs. |
| **GRD-01** | `src/Student_Grades_System.java` | Data Integrity | **HIGH** | **CONFIRMED** | `saveRecord()` originally trusted user-editable text fields without validating student existence in `student_info`. | Validate student exists in `student_info` and retrieve authoritative name and class before saving marks. |
| **FEAT-01** | Repository-wide | Feature Completeness | **HIGH** | **CONFIRMED** | System lacked daily student attendance tracking and attendance rate reporting. | Implemented `attendance` schema, `AttendanceDAO.java`, and `Student_Attendance_System.java` GUI with summary analytics and 25 unit/integration tests. |
| **BLD-01** | `nbproject/project.properties` | Build Portability | **HIGH** | **CONFIRMED** | Hardcoded `D:\` absolute developer paths for bundled JAR dependencies. | Replace absolute paths with project-relative references to `dist/lib/`. |
| **TST-01** | `test/SecurityAndResilienceTest.java` | Test Quality | **HIGH** | **CONFIRMED** | Unconditional assertions `assertTrue(..., true)` mask security test outcomes. | Implement real assertions checking SQL injection payload results and schema stability. |
| **TST-02** | `test/SecurityAndResilienceTest.java` | Test Quality | **HIGH** | **CONFIRMED** | `if (conn == null) return;` skipped test silently without failing. | Removed silent early returns; asserted connection non-null. |
| **TST-03** | `test/ExamMarksDAOTest.java`, `test/SecurityAndResilienceTest.java` | Test Isolation | **HIGH** | **CONFIRMED** | Integration tests originally executed against the live application database `student_database.sqlite`. | Provide dedicated isolated test database cleaned before/after tests. |
| **TST-04** | `test/SystemTestSuite.java` | Test Reporting | **MEDIUM** | **CONFIRMED** | Test runner did not separate Passed, Failed, and Skipped test metrics. | Added full accounting of passed, failed, and skipped tests with nonzero exit code on failure. |
| **TST-05** | `test/ExamMarksDAOTest.java`, `test/SecurityAndResilienceTest.java` | Test Safety | **HIGH** | **CONFIRMED** | Test runner deleted hardcoded filenames, risking deletion of pre-existing user files. | Switched to unique `File.createTempFile` with safe connection closing and verified pre-existing file preservation. |

---

## 2. Remediation and Verification Summary

All 29 verified findings have been completely remediated and validated:

| Category | Issues Remediated | Key Changes Applied | Verification Status |
|---|---|---|---|
| **Security & Authentication** | SEC-01, SEC-02, SEC-03, SEC-06, SEC-07, SEC-09, SEC-10, SEC-11 | Salted PBKDF2-HMAC-SHA256 (210,000 iterations), format pre-validation, iteration limits [10k, 2M], `spec.clearPassword()` in finally, `constantTimeCharEquals`, strong policy (min 8 chars), SetupState detection, non-destructive initialization, zero-trust account protection. | **VERIFIED (100% Passed)** |
| **Authorization & Control** | SEC-04, SEC-05 | Changed `Admin.java` close operation to `DISPOSE_ON_CLOSE`. Added `Admin.verifyAdmin(Component, Connection)` and enforced administrator privilege verification on record deletion in Students, Teachers, Employees, Grades, and Attendance modules. | **VERIFIED (100% Passed)** |
| **Database & Resilience** | DB-01, DB-02, DB-03, DB-04, DB-07, DB-08, SEC-08 | Replaced silent MySQL->SQLite fallback with `database.properties`. Fails safely on database connection error. Removed dummy seed records. Dialect-aware DDL. Propagated `DatabaseBootstrap` SQLExceptions. Atomic transactional first-run setup. Prohibited root with blank password. Environment variable configuration. | **VERIFIED (100% Passed)** |
| **Resource Management** | DB-05, DB-06 | Converted all `PreparedStatement` and `ResultSet` usages to try-with-resources. Eliminated unused `javaconnect.connerDb()` leaks in `Help.java` and `Students_and_Teachers_Management.java`. | **VERIFIED (100% Passed)** |
| **Data Integrity & Validation** | FUNC-03, FUNC-04, GRD-01 | Added strict input validation for student roll, teacher ID, and employee ID (non-empty, positive integers). Enforced authoritative existence check and retrieval of official name/class in `Student_Grades_System.saveRecord()`. | **VERIFIED (100% Passed)** |
| **Feature Engineering** | FEAT-01 | Implemented Student Attendance Tracking feature (`attendance` table, `AttendanceDAO.java`, `Student_Attendance_System.java`) with daily unique constraints, status options, summary calculation, and admin authorization. | **VERIFIED (100% Passed)** |
| **Build & Test Portability** | BLD-01, TST-01, TST-02, TST-03, TST-04, TST-05 | Replaced absolute `D:\` paths with portable relative paths in `nbproject/project.properties`. Upgraded test suite with unique temporary databases (`File.createTempFile`), genuine assertions, pre-existing file preservation test, and structured pass/fail accounting. | **VERIFIED (100% Passed)** |

### Final Automated Test Results:
- `GradeCalculatorTest`: 36 Passed, 0 Failed, 0 Skipped (36 Total)
- `ExamMarksDAOTest`: 20 Passed, 0 Failed, 0 Skipped (20 Total)
- `AttendanceDAOTest`: 25 Passed, 0 Failed, 0 Skipped (25 Total)
- `SecurityAndResilienceTest`: 89 Passed, 0 Failed, 0 Skipped (89 Total)
- **Total Suite Execution**: **170 Passed, 0 Failed, 0 Skipped** (Exit Code: 0)

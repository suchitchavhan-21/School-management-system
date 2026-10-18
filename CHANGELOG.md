# Changelog

All notable changes to the **School Management System** are documented in this file.

---

## [Unreleased] - 2026-10-18

### Fixed & Hardened
- **Duplicate-Key & Exception Handling**: Refactored `AttendanceDAO.saveOrUpdate` and `ExamMarksDAO.saveOrUpdate` to selectively intercept vendor-specific and SQLState unique constraint violations (`23505`, MySQL `1062`/`1022`/`1169`, SQLite `are not unique`/`is not unique`/`unique constraint failed`), attach suppressed exceptions via `addSuppressed()`, and strictly propagate unrelated database errors (connection drops, missing tables, syntax errors, permission failures). Generic SQLState `23000` alone is strictly excluded from duplicate classification to prevent misclassifying foreign-key and not-null violations. Added connection null-safety checks across `ExamMarksDAO`.
- **Attendance Remarks Alignment**: Standardized `Remarks` column length to 255 characters across Java validation (`MAX_REMARKS_LENGTH = 255`), MySQL DDL (`Database/schema/schema-mysql.sql`), runtime auto-migration (`DatabaseBootstrap.java`), and migration script (`Database/migrations/002_widen_attendance_remarks.sql`).

### Added
- **Opt-In Live MySQL Integration Test Suite**: Added `test/MySQLIntegrationTest.java` running against real MySQL servers when `SMS_MYSQL_TEST_HOST` is configured, or reporting `NOT RUN (ENVIRONMENTAL LIMITATION)` without failing offline CI.
- **Boundary & Resilience Regression Tests**: Expanded test suite to 232 automated tests with 100% pass verification, testing non-duplicate constraint rejections (foreign-key, not-null, check constraints), duplicate race recoveries, exception suppression chaining, exact 255-character ASCII and multibyte UTF-8 strings, 256-character rejection diagnostics, and atomic transaction preservation.

### Architecture & Repository Organization
- **Production-Grade Directory Structure**: Reorganized repository with clear separation of concerns without breaking NetBeans GUI Builder `.form` pairing:
  - `docs/`: Centralized audit, security, architecture, and testing documentation.
  - `Database/schema/`: Authoritative SQLite and MySQL DDL definitions.
  - `Database/migrations/`: Schema evolution and versioning policies.
  - `config/`: Configuration templates and 12-factor environment variable guidance.
  - `scripts/`: PowerShell automation scripts (`build.ps1`, `test.ps1`, `run.ps1`, `verify-repository.ps1`).
  - `.github/workflows/ci.yml`: GitHub Actions automated CI matrix testing on Ubuntu and Windows.
- **Repository Hygiene**: Added `.gitattributes` for consistent LF/CRLF line endings and binary attributes for JARs and images.
- **Standard Community Health Files**: Added `CONTRIBUTING.md`, `SECURITY.md`, and comprehensive `README.md`.

---

## [2.1.0] - 2026-10-18

### Added
- **Student Daily Attendance Management (`src/AttendanceDAO.java`, `Student_Attendance_System.java`)**:
  - Implemented attendance tracking table with database-level `UNIQUE(Roll, Attendance_date)` constraint.
  - Integrated student existence validation against `student_info`.
  - Added attendance rate calculation (Total Days, Present, Late, Absent, Excused, Attendance %).
  - Filter and search by student roll, class, and date.
  - Administrator PIN authorization requirement on attendance record deletions.
  - 25 automated unit/integration tests in `test/AttendanceDAOTest.java`.

### Security & Hardening
- **PBKDF2 Work Factor & Bounds**: Upgraded to 210,000 iterations (OWASP guidance). Added `isValidHashFormat` pre-computation validation to prevent DoS via extreme iterations.
- **Transactional First-Run Setup**: Enforced atomic user and admin key initialization with rollback and guaranteed non-destructive `SetupState` checks.
- **Admin Parameter Index Fix**: Resolved 1-parameter index crash in `Admin.java`. Changed window close operation to `DISPOSE_ON_CLOSE`.
- **Pre-existing Data Preservation**: Upgraded test suite to use unique temporary SQLite files via `File.createTempFile` and added assertions verifying pre-existing files are untouched.

---

## [2.0.0] - 2026-10-18

### Added
- **Academic Examination & Grade Report Card System (`src/GradeCalculator.java`, `Student_Grades_System.java`, `ExamMarksDAO.java`)**:
  - Academic grading calculation with letter grades (`A+` to `F`), GPA scale (5.0), and pass/fail rules.
  - Report card generation and student search.
  - 36 unit tests for `GradeCalculator` and 20 integration tests for `ExamMarksDAO`.
- **Database Resilience**: Configurable database connection supporting SQLite embedded and MySQL enterprise network (`javaconnect.java`).
- **Zero-Trust Master Test Suite**: `test/SystemTestSuite.java` unifying all test harnesses.

### Fixed
- **SQL Injection Remediation**: Replaced string-concatenated SQL queries across all modules with parameterized `PreparedStatement`.
- **Resource Leaks**: Wrapped all database statements and result sets in try-with-resources.
- **Hardcoded Credentials**: Removed plaintext passwords and default credentials.

# School Management System (Java Swing & JDBC)

An enterprise-hardened School Management System built in Java Swing, backed by robust JDBC persistence (SQLite / MySQL) with automated examination grading, daily attendance tracking, and zero-trust security controls.

---

## 1. Key Modules & Features

- **Authentication & Zero-Trust Security**:
  - Salted **PBKDF2-HMAC-SHA256** password hashing with 210,000 iterations (OWASP compliant), cryptographically secure random salts, bounded iterations/salt/key sizes to prevent DoS attacks, and heap memory zero-wiping (`PasswordSecurity.java`).
  - **Non-Destructive First-Run Setup**: Enforces atomic, transactional credential setup strictly on uninitialized databases (`SetupState.UNINITIALIZED`), strictly refusing to overwrite existing accounts or administrator credentials (`FirstRunSetupDialog.java`).
  - **Inconsistent Database State Detection**: Distinguishes uninitialized databases from missing admin keys or missing user accounts, displaying safe diagnostic alerts rather than silently wiping data.
  - Transparent in-place migration of older hashes upon successful authentication.
  - Mandatory administrator PIN verification for destructive record deletions (`Admin.verifyAdmin`).
- **Student Examination & Grade Report Card Management**:
  - Academic grading engine with standard boundary rules, GPA mapping (5.0 to 0.0), and single-subject failure protection (`GradeCalculator.java`).
  - Authoritative validation against enrolled students (`student_info`) to ensure records match official student names and classes.
  - Unique constraint protection preventing duplicate term entries.
  - Printable examination report card generation (`Student_Grades_System.java`).
- **Student Daily Attendance Tracking Management**:
  - Daily attendance tracking with status options (`Present`, `Absent`, `Late`, `Excused`) and remarks (`Student_Attendance_System.java`, `AttendanceDAO.java`).
  - Authoritative student lookup from `student_info`.
  - Unique daily constraint `(Roll, Attendance_date)` ensuring exactly one attendance record per student per day with safe `saveOrUpdate`.
  - Comprehensive attendance summary analytics (Total Days, Days Present, Days Absent, Days Late, Days Excused, Overall Percentage).
  - Search and filter by Roll, Class, and Date.
  - Protected deletion requiring Administrator PIN verification.
- **Student, Teacher & Employee Management**:
  - Full CRUD operations with strict input validation (positive integer IDs, non-empty fields).
  - Search by ID, name, and attributes.
  - Dedicated database overview tables with tabular data viewing.
- **Database Architecture & Resilience**:
  - Configurable database backend via `database.properties` (SQLite embedded by default; MySQL supported).
  - 12-Factor environment variable overrides supported (`SMS_DB_TYPE`, `SMS_MYSQL_HOST`, `SMS_MYSQL_PORT`, `SMS_MYSQL_DATABASE`, `SMS_MYSQL_USER`, `SMS_MYSQL_PASSWORD`, `SMS_MYSQL_USE_SSL`).
  - Strict security validation: MySQL requires dedicated user credentials and prohibits running with `root` and a blank password.
  - Dialect-aware schema bootstrapping with zero hardcoded default credentials (`DatabaseBootstrap.java`).
  - Fail-safe connection handling without silent database switching.
  - 100% parameterized queries via `PreparedStatement` with `try-with-resources`.

---

## 2. Prerequisites

- **Java Development Kit (JDK)**: OpenJDK 11 or higher (tested on OpenJDK 11.0.26 LTS).
- **Operating System**: Windows, Linux, or macOS.
- **IDE (Optional)**: Apache NetBeans 12+ (standard Ant project structure).

---

## 3. Build & Execution Instructions

### A. Compile from Command Line
```powershell
javac -cp "dist/lib/*;src;build/classes" -d build/classes src/*.java test/*.java
```

### B. Run the Master Automated Test Suite
```powershell
java -cp "dist/lib/*;build/classes" SystemTestSuite
```
*Executes all 170 automated unit, persistence, attendance, and security resilience tests with 100% pass verification.*

### C. Launch the Application
```powershell
java -cp "dist/lib/*;build/classes" Login_frame
```

---

## 4. Configuration (`database.properties` & Environment Variables)

The database connection is configured via `database.properties` or environment variables:

```properties
# Database Engine Selection: 'sqlite' or 'mysql'
db.type=sqlite
db.sqlite.file=student_database.sqlite

# MySQL Settings (Active when db.type=mysql)
# Environment variables SMS_MYSQL_USER and SMS_MYSQL_PASSWORD can be used instead of storing secrets here.
db.mysql.host=localhost
db.mysql.port=3306
db.mysql.database=student_database
db.mysql.username=
db.mysql.password=
db.mysql.useSSL=true
```

For production MySQL deployments, refer to [`database.properties.template`](file:///C:/School%20Management%20system%20in%20java/database.properties.template).

---

## 5. Security & Verification Documentation

For comprehensive technical reports and audit logs, consult:
- [`SECURITY_REMEDIATION.md`](file:///C:/School%20Management%20system%20in%20java/SECURITY_REMEDIATION.md): Detailed cryptographic specifications and security architecture.
- [`TEST_RESULTS.md`](file:///C:/School%20Management%20system%20in%20java/TEST_RESULTS.md): Full automated test suite breakdown and execution logs (170 tests).
- [`ZERO_TRUST_AUDIT_REGISTER.md`](file:///C:/School%20Management%20system%20in%20java/ZERO_TRUST_AUDIT_REGISTER.md): File-by-file audit findings and remediation status.
- [`FINAL_COMPLETION_REPORT.md`](file:///C:/School%20Management%20system%20in%20java/FINAL_COMPLETION_REPORT.md): Executive summary and verification delivery details.

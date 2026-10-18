# School Management System (Java Swing & JDBC)

An enterprise-hardened School Management System built in Java Swing, backed by robust JDBC persistence (SQLite / MySQL) with automated examination grading and zero-trust security controls.

---

## 1. Key Modules & Features

- **Authentication & Zero-Trust Security**:
  - Salted **PBKDF2-HMAC-SHA256** password hashing with cryptographically secure random salts and memory zero-wiping (`PasswordSecurity.java`).
  - Automatic, seamless migration of legacy plaintext accounts without operator lockout.
  - Interactive First-Run Administrator Setup dialog (`FirstRunSetupDialog.java`).
  - Administrator PIN authorization required for record deletions (`Admin.verifyAdmin`).
- **Student Examination & Grade Report Card Management**:
  - Complete academic grading engine with standard boundary rules, GPA mapping (5.0 to 0.0), and single-subject failure protection (`GradeCalculator.java`).
  - Authoritative validation against enrolled students (`student_info`) to ensure records match official student names and classes.
  - Unique constraint protection preventing duplicate term entries.
  - Printable examination report card generation (`Student_Grades_System.java`).
- **Student, Teacher & Employee Management**:
  - Full CRUD operations with strict input validation (positive integer IDs, non-empty fields).
  - Search by ID, name, and attributes.
  - Dedicated database overview tables with tabular data viewing.
- **Database Architecture & Resilience**:
  - Configurable database backend via `database.properties` (SQLite embedded by default; MySQL supported).
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
*Executes all 88 automated unit, persistence, and security resilience tests with 100% pass verification.*

### C. Launch the Application
```powershell
java -cp "dist/lib/*;build/classes" Login_frame
```
*Or execute the pre-built JAR:*
```powershell
java -jar dist/School_Managment_System.jar
```

---

## 4. Configuration (`database.properties`)

The application database connection is managed via `database.properties` in the project root:

```properties
# Database Engine Selection: 'sqlite' or 'mysql'
db.type=sqlite
db.sqlite.file=student_database.sqlite

# MySQL Settings (Active when db.type=mysql)
db.mysql.host=localhost
db.mysql.port=3306
db.mysql.database=student_database
db.mysql.username=root
db.mysql.password=
db.mysql.useSSL=false
```

---

## 5. Security & Verification Documentation

For comprehensive technical reports and audit logs, consult:
- [`SECURITY_REMEDIATION.md`](file:///C:/School%20Management%20system%20in%20java/SECURITY_REMEDIATION.md): Detailed cryptographic specifications and security architecture.
- [`TEST_RESULTS.md`](file:///C:/School%20Management%20system%20in%20java/TEST_RESULTS.md): Full automated test suite breakdown and execution logs.
- [`ZERO_TRUST_AUDIT_REGISTER.md`](file:///C:/School%20Management%20system%20in%20java/ZERO_TRUST_AUDIT_REGISTER.md): File-by-file audit findings and remediation status.
- [`FINAL_COMPLETION_REPORT.md`](file:///C:/School%20Management%20system%20in%20java/FINAL_COMPLETION_REPORT.md): Executive summary and verification delivery details.

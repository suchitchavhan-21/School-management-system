# School Management System (Java Swing & JDBC)

An enterprise-hardened School Management System built in Java Swing, backed by robust JDBC persistence (SQLite / MySQL) with automated examination grading, daily attendance tracking, production-grade repository organization, CI/CD, and zero-trust security controls.

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
  - Dual-engine support: Standalone embedded SQLite (default) and enterprise network MySQL.
  - 12-Factor environment variable overrides supported (`SMS_DB_TYPE`, `SMS_MYSQL_HOST`, `SMS_MYSQL_PORT`, `SMS_MYSQL_DATABASE`, `SMS_MYSQL_USER`, `SMS_MYSQL_PASSWORD`, `SMS_MYSQL_USE_SSL`).
  - Strict security validation: MySQL requires dedicated user credentials and prohibits running with `root` and a blank password.
  - Dialect-aware schema bootstrapping with zero hardcoded default credentials (`DatabaseBootstrap.java`).
  - Fail-safe connection handling without silent database switching.
  - 100% parameterized queries via `PreparedStatement` with `try-with-resources`.

---

## 2. Production-Grade Repository Organization

```text
School Management system in java/
├── .github/workflows/         # CI/CD pipelines (GitHub Actions Java 11 matrix)
├── config/                    # Configuration templates and 12-factor guidelines
├── Database/
│   ├── migrations/            # Schema migration guidance and evolution policies
│   ├── schema/                # Production DDL for SQLite and MySQL
│   └── student_database.sql   # Reference baseline SQL dump
├── dist/lib/                  # Bundled runtime libraries (JDBC drivers, UI bindings)
├── docs/
│   ├── architecture/          # Repository structure & architectural specifications
│   ├── audit/                 # Baseline verification, audit registers, final reports
│   ├── security/              # Cryptographic architecture & remediation guides
│   └── testing/               # Evidence-based test logs & benchmark reports
├── help center/               # Graphical user guide & documentation
├── nbproject/                 # NetBeans Ant configuration & metadata
├── scripts/                   # Automation scripts (build.ps1, test.ps1, run.ps1, verify-repository.ps1)
├── src/                       # Application source code & NetBeans GUI Builder .form files
├── test/                      # Comprehensive zero-trust test suite (232 tests)
├── .gitattributes             # Line ending normalization & binary attributes
├── .gitignore                 # Build and secret ignore rules
├── CHANGELOG.md               # Version history and milestone documentation
├── CONTRIBUTING.md            # Contributor guidelines and coding standards
├── README.md                  # This file
└── SECURITY.md                # Enterprise security policy & disclosure procedures
```

---

## 3. Quickstart & Build Instructions

### Prerequisites
- **Java Development Kit (JDK)**: OpenJDK 11 LTS or higher (tested on OpenJDK 11.0.26 LTS).
- **PowerShell** (Windows) or standard Bash shell (Linux/macOS).
- **IDE (Optional)**: Apache NetBeans, IntelliJ IDEA, Eclipse, or VS Code.

### A. Build the Application
```powershell
# Using PowerShell automation script:
.\scripts\build.ps1

# Or directly using javac:
javac -encoding UTF-8 -cp "dist/lib/*;src" -d build/classes src/*.java test/*.java
```

### B. Execute the Automated Test Suite
```powershell
# Using PowerShell automation script:
.\scripts\test.ps1

# Or directly using java:
java -cp "dist/lib/*;build/classes" SystemTestSuite
```
*Executes all 232 automated unit, integration, and security resilience tests with 100% pass verification.*

### C. Verify Repository Integrity
```powershell
# Comprehensive zero-trust validation:
.\scripts\verify-repository.ps1
```

### D. Launch the Application
```powershell
# Using PowerShell automation script:
.\scripts\run.ps1

# Or directly using java:
java -cp "dist/lib/*;build/classes" Login_frame
```

---

## 4. Configuration (`config/database.properties.template`)

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

For full details on configuring network databases, refer to [`config/README.md`](config/README.md) and [`config/database.properties.template`](config/database.properties.template).

---

## 5. Technical Documentation Links

- **Architecture**: [`docs/architecture/REPOSITORY_STRUCTURE.md`](docs/architecture/REPOSITORY_STRUCTURE.md)
- **Baseline Audit**: [`docs/audit/BASELINE.md`](docs/audit/BASELINE.md)
- **Audit Register**: [`docs/audit/ZERO_TRUST_AUDIT_REGISTER.md`](docs/audit/ZERO_TRUST_AUDIT_REGISTER.md)
- **Completion Report**: [`docs/audit/FINAL_COMPLETION_REPORT.md`](docs/audit/FINAL_COMPLETION_REPORT.md)
- **Security Architecture**: [`docs/security/SECURITY_REMEDIATION.md`](docs/security/SECURITY_REMEDIATION.md)
- **Test Evidence & Benchmarks**: [`docs/testing/TEST_RESULTS.md`](docs/testing/TEST_RESULTS.md)
- **Database DDL**: [`Database/schema/schema-sqlite.sql`](Database/schema/schema-sqlite.sql) & [`Database/schema/schema-mysql.sql`](Database/schema/schema-mysql.sql)
- **Contributing**: [`CONTRIBUTING.md`](CONTRIBUTING.md)
- **Security Policy**: [`SECURITY.md`](SECURITY.md)
- **Changelog**: [`CHANGELOG.md`](CHANGELOG.md)

# Production-Grade Repository Architecture & Structure

**Repository**: `https://github.com/suchitchavhan-21/School-management-system.git`
**Application**: Java Swing Desktop Application / JDBC Persistence (SQLite & MySQL)
**Build Platform**: OpenJDK 11 LTS / NetBeans Ant & JDK CLI Tools

---

## 1. Architectural Strategy & Design Principles

The repository is structured to satisfy two critical, co-equal requirements:
1. **Production-Grade Enterprise Maintainability**: Clear separation of concerns between source code, test suites, architecture/audit documentation, database schemas, environment configuration, automation scripts, and continuous integration.
2. **NetBeans GUI Builder & Ant Backward Compatibility**: NetBeans visual designer generates paired `.java` and `.form` files whose class names, package definitions, and asset references are tightly coupled to `nbproject/project.properties` and `build.xml`. An uncoordinated deep directory refactoring breaks the visual form designer and legacy Ant targets.

To achieve both goals, the project uses an **incremental, non-destructive enterprise organization**:
- Core Java source code (`src/`) and test suites (`test/`) remain at the standard NetBeans root positions.
- All non-code artifacts are categorized into top-level functional directories:
  - `docs/`: Centralized documentation library (audit, security, architecture, testing).
  - `Database/`: Schema definitions (`Database/schema/`) and migrations (`Database/migrations/`).
  - `config/`: Safe environment configuration templates and guidelines (`config/`).
  - `scripts/`: Cross-platform PowerShell automation scripts for building, testing, and verifying (`scripts/`).
  - `.github/`: Continuous Integration workflow for automated builds and testing (`.github/workflows/`).

---

## 2. Directory Layout & Manifest

```text
School Management system in java/
│
├── .github/
│   └── workflows/
│       └── ci.yml                     # Continuous Integration workflow (GitHub Actions)
│
├── config/
│   ├── README.md                      # Configuration documentation & 12-factor secret guidelines
│   └── database.properties.template   # Version-controlled connection template
│
├── Database/
│   ├── migrations/
│   │   └── README.md                  # Schema evolution, migration policies & versioning rules
│   ├── schema/
│   │   ├── schema-mysql.sql           # Production MySQL DDL schema (InnoDB, UTF-8, AUTO_INCREMENT)
│   │   └── schema-sqlite.sql          # Embedded SQLite DDL schema (AUTOINCREMENT, constraints)
│   └── student_database.sql           # Reference legacy SQL export (historical baseline)
│
├── dist/
│   └── lib/                           # Bundled runtime JDBC and UI dependencies
│       ├── beansbinding-1.2.1.jar     # Swing desktop data-binding library
│       ├── com.mysql.jdbc_5.1.5.jar   # MySQL Connector/J driver
│       ├── jcalendar-1.4.jar          # Swing calendar date-picker components
│       ├── rs2xml.jar                 # ResultSet to TableModel adapter
│       └── sqlite-jdbc-3.7.2.jar      # SQLite JDBC driver
│
├── docs/
│   ├── architecture/
│   │   └── REPOSITORY_STRUCTURE.md    # This architectural organization specification
│   ├── audit/
│   │   ├── BASELINE.md                # Real repository environment & baseline verification
│   │   ├── FINAL_COMPLETION_REPORT.md # Master executive zero-trust completion report
│   │   └── ZERO_TRUST_AUDIT_REGISTER.md# Complete itemized defect & vulnerability register
│   ├── security/
│   │   └── SECURITY_REMEDIATION.md    # Comprehensive cryptographic & authorization specification
│   └── testing/
│       └── TEST_RESULTS.md            # Evidence-based automated test results & benchmarks
│
├── help center/
│   ├── images/                        # Help center graphical assets
│   └── index.html                     # Embedded offline user documentation & guide
│
├── nbproject/
│   ├── build-impl.xml                 # NetBeans Ant build implementation script
│   ├── genfiles.properties            # NetBeans build generation metadata
│   ├── project.properties             # NetBeans classpath, source paths, and target definitions
│   └── project.xml                    # NetBeans project configuration descriptor
│
├── scripts/
│   ├── build.ps1                      # Automated build script (compiles source, bundles resources)
│   ├── run.ps1                        # Automated application launcher script
│   ├── test.ps1                       # Automated test execution script
│   └── verify-repository.ps1          # Comprehensive zero-trust end-to-end repository validator
│
├── src/                               # Application source code & NetBeans GUI forms
│   ├── Admin.form / Admin.java        # Administrator authentication & authorization dialog
│   ├── AttendanceDAO.java             # Data Access Object for student attendance records
│   ├── DatabaseBootstrap.java         # Automatic schema verification & table bootstrap
│   ├── ExamMarksDAO.java              # Data Access Object for student examination marks
│   ├── FirstRunSetupDialog.java       # First-run administrator initialization dialog
│   ├── GradeCalculator.java           # Academic grading, GPA calculation & business rules
│   ├── javaconnect.java               # Enterprise JDBC connection manager (SQLite/MySQL)
│   ├── Login_frame.form / .java       # Application entry point & authentication screen
│   ├── PasswordSecurity.java          # Salted PBKDF2-HMAC-SHA256 password engine
│   ├── Student_Attendance_System.java # Student Attendance Tracking GUI & analytics
│   ├── Student_Grades_System.java     # Examination Marks & Report Card Generation GUI
│   ├── Students_and_Teachers_Management.form / .java # Main application dashboard
│   ├── Students_Infromation_System.form / .java      # Student CRUD management system
│   ├── Teachers_Information_System.form / .java      # Teacher CRUD management system
│   ├── Employees_Infromation_System.form / .java     # Employee CRUD management system
│   ├── All_Students_Data_Details.form / .java        # Tabular student record viewer
│   ├── All_Teachers_data_details.form / .java        # Tabular teacher record viewer
│   ├── All_Employees_Data_Details.form / .java       # Tabular employee record viewer
│   ├── My_Profile.form / .java        # Institutional developer & system profile
│   ├── Help.form / .java              # In-app help viewer & documentation launcher
│   └── [image assets]                 # Icons, backgrounds, and graphical resources
│
├── test/                              # Automated zero-trust test suite
│   ├── AttendanceDAOTest.java         # 25 unit/integration tests for AttendanceDAO
│   ├── ExamMarksDAOTest.java          # 20 unit/integration tests for ExamMarksDAO
│   ├── GradeCalculatorTest.java       # 36 unit tests for academic grading & GPA rules
│   ├── SecurityAndResilienceTest.java # 89 security, injection, PBKDF2 & resilience tests
│   └── SystemTestSuite.java           # Master test runner (170 total tests, 100% green)
│
├── .gitattributes                     # Line-ending normalization & binary attributes
├── .gitignore                         # Build output, local database & secret exclusions
├── CHANGELOG.md                       # Comprehensive version & release changelog
├── CONTRIBUTING.md                    # Zero-trust developer guide & coding conventions
├── database.properties                # Local active database configuration (SQLite default)
├── database.properties.template       # Root template link for quick access
├── login crendentials.txt             # User-facing security setup notice
├── manifest.mf                        # JAR manifest specification
├── README.md                          # Primary project documentation & quickstart
└── SECURITY.md                        # Enterprise security policy & vulnerability disclosure
```

---

## 3. Technology Stack & Key Subsystems

### A. Presentation & Application Layer
- **Java Swing (JFC)**: Heavy-client desktop graphical interface designed with NetBeans GUI Builder.
- **Event Dispatch Thread (EDT) Safety**: Long-running database operations and dialogs cleanly interact with Swing via `SwingUtilities.invokeLater()`.
- **Authorization Barrier**: High-risk operations (such as record deletion across all entities) are protected by `Admin.verifyAdmin(Component, Connection)`, requiring administrator PIN authorization before executing database mutations.

### B. Business Logic & Calculation Engine
- **GradeCalculator**: Encapsulates institutional grading policies (Marks -> Percentage -> Letter Grade `A+` through `F` -> GPA on a 5.0 scale). Incorporates mandatory subject-passing rules.
- **Attendance Analytics**: Summarizes daily attendance records into aggregate metrics: Total Days, Present, Late, Absent, Excused, and Attendance Percentage.

### C. Persistence & Database Layer
- **Dual-Engine Architecture (`javaconnect.java`)**: Supports standalone embedded SQLite (zero setup) or network MySQL (enterprise multi-user).
- **Zero-Trust Connection Policy**: Database errors fail fast and visibly; the system strictly refuses to silently switch databases to prevent cross-dataset contamination.
- **Schema Bootstrap (`DatabaseBootstrap.java`)**: Automatically checks and provisions all 7 required tables with dialect-aware DDL (`pass`, `admin`, `student_info`, `teacher_info`, `employee_info`, `exam_marks`, `attendance`).
- **SQL Injection Immunity**: 100% of database interactions across all DAOs and frames use parameterized `PreparedStatement` queries with try-with-resources.

### D. Security & Cryptography Engine
- **PBKDF2-HMAC-SHA256 (`PasswordSecurity.java`)**: 210,000 iterations, 16-byte random salt, 256-bit derived key length.
- **Format Validation**: Pre-computation bounds checking [10k, 2M iterations] prevents DoS via malformed hashes.
- **Memory Hygiene**: In-memory password buffers are explicitly cleared (`wipe(char[])`, `PBEKeySpec.clearPassword()`).
- **Account Preservation**: Setup is strictly permitted only on uninitialized databases; existing accounts cannot be overwritten or deleted.

# School Management System - Repository Intelligence & Feature Engineering Report

## 1. Repository Identity & Current Git State
- **Workspace Location**: `C:\School Management system in java`
- **Git Repository Status**: Not a Git repository (`fatal: not a git repository`). Unversioned standalone project extracted from NetBeans IDE.
- **Java Platform**: OpenJDK 11.0.26 LTS (verified `javac 11.0.26`, `java 11.0.26`, `jar 11.0.26`).
- **Build System**: NetBeans Ant (`build.xml`, `nbproject/`). Standalone Ant/Maven/Gradle not in PATH; compilation and packaging execute cleanly via JDK CLI (`javac -cp "dist/lib/*;src" -d build/classes src/*.java`).
- **Bundled Libraries (`dist/lib/`)**:
  - `beansbinding-1.2.1.jar`
  - `com.mysql.jdbc_5.1.5.jar`
  - `jcalendar-1.4.jar`
  - `rs2xml.jar`
  - `sqlite-jdbc-3.7.2.jar`

---

## 2. Project Architecture & Execution Flow
- **Pattern**: Fat-client Java Swing desktop application (NetBeans `.form` UI files).
- **Execution Flow**:
  1. `Login_frame` authenticates against table `pass`.
  2. `Students_and_Teachers_Management` acts as main navigation hub.
  3. `Students_Infromation_System`, `Teachers_Information_System`, `Employees_Infromation_System` provide CRUD forms.
  4. `All_*_Data_Details` provide JTable grid views using `rs2xml DbUtils`.
- **Database Layer**: Direct JDBC connection in `javaconnect.java` targeting `localhost:3306/student_database` (MySQL).

---

## 3. Prioritized Issue Register

| ID | Severity | Finding | Root Cause |
|---|---|---|---|
| **SEC-01** | **Critical** | SQL Injection in Record Updates | `Students_Infromation_System#L1247`, `Teachers_Information_System#L655`, `Employees_Infromation_System#L687` concatenate raw user strings into SQL. |
| **REL-01** | **Critical** | Single-Point-of-Failure Database Connection | `javaconnect.java` hardcodes `localhost:3306/student_database`. Fails with NPE if MySQL service is inactive. |
| **BUG-01** | **High** | Admin Verification Runtime Crash | `Admin.java#L107` calls `pst.setString(2, ...)` on a query with only 1 parameter (`SQLException: Parameter index out of range`). |
| **BUG-02** | **Medium** | Premature "No Data Found" False Alarm | Search executes 10 sequential queries; first query immediately pops up error dialog if `Roll` is not matched. |
| **PERF-01**| **Medium** | Unbounded Clock Thread Leak & EDT Violation | Frames spawn unmanaged `Thread` with `sleep(1000)` updating UI off the EDT and never terminating on window close. |
| **RES-01** | **Medium** | Unclosed JDBC Statements & ResultSets | `PreparedStatement` and `ResultSet` are not closed in `finally` or try-with-resources. |
| **UX-01**  | **Low** | Broken File Paths & Dead Links | `Help.java` hardcodes non-existent audio path `C:\Users\Subrata_CSE\Desktop\applause.wav` and dead Dropbox URL. |

---

## 4. Selected High-Value Feature: Student Examination & Grade Report Card Management

### Why this feature:
A school management system without academic grades, marks, and report cards is incomplete. Teachers and administrators need to record term exam marks, compute totals/percentages/letter grades/GPA, and print or export official report cards.

### Architectural Layers:
1. **Resilient Database Layer (`javaconnect.java` & `DatabaseBootstrap.java`)**:
   - Supports both MySQL and embedded SQLite fallback (`dist/lib/sqlite-jdbc-3.7.2.jar`).
   - Automatically bootstraps tables and seed data if missing.
   - 100% parameterized queries.
2. **Data Access Layer (`ExamMarksDAO.java`)**:
   - Manages CRUD operations and unique student term records (`Roll`, `Class`, `Exam_term`, `Exam_year`).
3. **Domain Business Logic (`GradeCalculator.java`)**:
   - Computes Total Marks, Percentage, Letter Grade (A+, A, B, C, D, F), GPA (5.0 scale), and Pass/Fail status.
4. **Presentation Layer (`Student_Grades_System.java`)**:
   - Swing GUI for entering subject scores (English, Math, Science, Social Studies, ICT), calculating live results, searching student records, viewing data tables, and generating printable/exportable report cards.
5. **CLI & Automated Verification Harness (`RunSystem.java`, `TestGradeSystem.java`)**:
   - Provides instant console output, automated testing, and execution directly from CMD.

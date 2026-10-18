# Database Migrations & Schema Evolution Guide

**Project**: School Management System in Java
**Supported Databases**: SQLite 3.x (Embedded standalone) and MySQL 5.7+ / 8.0+ (Enterprise network)

---

## 1. Schema Evolution Philosophy

The School Management System utilizes a **dual-layer schema management architecture**:
1. **Dynamic Runtime Bootstrapping (`src/DatabaseBootstrap.java`)**:
   - The application inspects connected database metadata at startup via `DatabaseBootstrap.bootstrap(Connection)`.
   - Executes idempotent `CREATE TABLE IF NOT EXISTS` statements adapted dynamically to the active database dialect (e.g. `AUTOINCREMENT` in SQLite vs `AUTO_INCREMENT` in MySQL).
   - Verifies the availability and queryability of all 7 mandatory tables before allowing user interaction:
     - `pass`
     - `admin`
     - `student_info`
     - `teacher_info`
     - `employee_info`
     - `exam_marks`
     - `attendance`
2. **Deterministic Version-Controlled DDL Scripts (`Database/schema/`)**:
   - Standalone reference scripts (`schema-sqlite.sql` and `schema-mysql.sql`) provide authoritative schemas for DBAs, CI/CD automated provisioning, and fresh server deployment.

---

## 2. Migration Guidelines for Existing Databases

When upgrading an existing database from legacy versions:

### A. Password Hash Length Migration
- **Legacy Schema**: Table `pass` originally defined `Password VARCHAR(20)` for plaintext strings.
- **Remediation**:
  - In SQLite: Column type affinities are dynamic, so strings > 20 characters store without truncation.
  - In MySQL: Run the following migration if upgrading from an older dump:
    ```sql
    ALTER TABLE pass MODIFY Password VARCHAR(255) NOT NULL;
    ALTER TABLE admin MODIFY Password VARCHAR(255) NOT NULL;
    ```

### B. New Tables Provisioning
- The `exam_marks` and `attendance` tables are automatically provisioned by `DatabaseBootstrap.java` on application launch without requiring manual DBA intervention.
- For manual provisioning on enterprise MySQL instances:
  ```bash
  mysql -u sms_user -p student_database < Database/schema/schema-mysql.sql
  ```

### C. Zero-Data-Loss Rule
- **Never drop or truncate tables** during migration.
- `DatabaseBootstrap.java` and `PasswordSecurity.java` adhere strictly to zero-trust data preservation: existing student, teacher, employee, exam, and attendance records are never overwritten during bootstrapping or setup.

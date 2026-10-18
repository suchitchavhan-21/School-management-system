# Application Security Remediation & Hardening Guide

**Project**: School Management System in Java  
**Security Standard**: Zero-Trust, NIST SP 800-63B Password Guidelines, OWASP Top 10 Remediation  
**Status**: All Critical and High-Severity Security Defects Remediated & Verified  

---

## 1. Vulnerabilities Remediated

| Vulnerability | Initial State | Remediated State | Risk Reduction |
|---|---|---|---|
| **Plaintext Password Storage** | Credentials stored unencrypted in `pass` and `admin` tables. | Salted PBKDF2-HMAC-SHA256 (65,536 iterations, 16-byte random salt). | **CRITICAL -> RESOLVED** |
| **Insecure Heap Memory Retention** | Passwords read as immutable `String` via `JPasswordField.getText()`. | Retrieved as mutable `char[]` via `getPassword()` and wiped with `\0` immediately after use. | **HIGH -> RESOLVED** |
| **Default Production Credentials** | Hardcoded accounts (`school`/`school`, `admin`/`123`) automatically seeded into database on launch. | Removed hardcoded credentials. Implemented modal `FirstRunSetupDialog.java` requiring administrator configuration. | **CRITICAL -> RESOLVED** |
| **Authorization Bypass on Deletions** | Admin authorization was disabled/commented out; any user could delete students, teachers, and employees. | Mandatory modal `Admin.verifyAdmin(Component, Connection)` check enforced on all delete operations. | **HIGH -> RESOLVED** |
| **Silent Database Failover Exposure** | Failure to connect to MySQL silently defaulted to local SQLite without operator knowledge. | Removed silent fallback; system fails safely with explicit diagnostics and reads strictly from `database.properties`. | **CRITICAL -> RESOLVED** |
| **SQL Injection Risks** | Potential string concatenation in grade searches or parameters. | 100% of database access parameterized via `PreparedStatement`; verified resilient against Tautology, DDL, and UNION payloads. | **HIGH -> RESOLVED** |
| **Unclosed JDBC Resources** | `PreparedStatement` and `ResultSet` objects leaked on multiple CRUD operations. | Refactored all queries to `try-with-resources` blocks. | **HIGH -> RESOLVED** |

---

## 2. Technical Architecture & Cryptographic Design

### A. Salted PBKDF2 Password Hashing (`src/PasswordSecurity.java`)
- **Algorithm**: `PBKDF2WithHmacSHA256` via Java standard cryptographic library (`javax.crypto.SecretKeyFactory`).
- **Iteration Count**: 65,536 iterations (NIST SP 800-63B compliant).
- **Salt**: 16 bytes (128 bits) generated per-password via `java.security.SecureRandom`.
- **Hash Storage Format**:
  `$pbkdf2-sha256$<iterations>$<salt_hex>$<hash_hex>`
- **Constant-Time Verification**: All password checks execute via `MessageDigest.isEqual(...)` to eliminate timing side-channel attacks.
- **Memory Hygiene**:
  ```java
  public static void wipe(char[] password) {
      if (password != null) {
          Arrays.fill(password, '\0');
      }
  }
  ```

### B. Transparent Legacy Account Migration
To ensure zero disruption and avoid locking existing staff out of the system:
1. When a user logs in, `PasswordSecurity.authenticateAndMigrateUser(...)` inspects the stored password format.
2. If the stored password does not start with `$pbkdf2-sha256$`, it is recognized as a legacy password.
3. The entered password is compared in constant time against the legacy value.
4. Upon successful authentication, the system immediately hashes the password with PBKDF2 and updates the database record.
5. All subsequent logins are validated against the modern PBKDF2 hash.

### C. First-Run Administrator Bootstrap (`src/FirstRunSetupDialog.java`)
- When the system launches, `Login_frame` checks if any user accounts exist in the `pass` table.
- If the table is empty (e.g. fresh installation), a modal dialog appears requiring:
  - Administrator Username
  - Master Password (with confirmation matching)
  - Administrator Authorization PIN
- Credentials are immediately hashed with PBKDF2 and written to the database.

### D. Explicit Database Configuration (`database.properties`)
The application no longer hardcodes credentials or database engines. Configuration is centralized in `database.properties`:
```properties
db.type=sqlite
db.sqlite.file=student_database.sqlite

# MySQL Configuration (used when db.type=mysql)
db.mysql.host=localhost
db.mysql.port=3306
db.mysql.database=student_database
db.mysql.username=root
db.mysql.password=
db.mysql.useSSL=false
```

---

## 3. Operational Security & Migration Procedures

### Initial Setup Procedure
1. Configure `database.properties` with the desired engine (`sqlite` or `mysql`).
2. Run the application (`java -jar dist/School_Managment_System.jar` or NetBeans / CLI).
3. If this is a new installation, complete the **First-Run Administrator Setup** dialog.
4. Securely store your Administrator PIN; it is required for all destructive operations (e.g., deleting students, teachers, employees, or exam records).

### Credential Rotation
- **User Password Change**: Users can update their passwords through administrative tools or upon logging in.
- **Administrator PIN Rotation**: The administrator PIN can be updated directly via the `Admin.java` administrative interface.

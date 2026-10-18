# Application Security Remediation & Hardening Guide

**Project**: School Management System in Java  
**Security Standard**: Zero-Trust, NIST SP 800-63B Guidelines, OWASP Password Storage Standards  
**Status**: All Critical and High-Severity Security Defects Remediated & Verified  

---

## 1. Vulnerabilities Remediated

| Vulnerability | Initial State | Remediated State | Risk Reduction |
|---|---|---|---|
| **Plaintext Password Storage** | Credentials stored unencrypted in `pass` and `admin` tables. | Salted PBKDF2-HMAC-SHA256 (210,000 iterations in compliance with OWASP guidance, 16-byte random salt). | **CRITICAL -> RESOLVED** |
| **Malformed Hash DoS & CPU Exhaustion** | Incomplete/malformed hash strings could trigger excessive computation or unhandled crashes. | Strict pre-computation format validation; iteration counts strictly bounded [10,000, 2,000,000]; salt/key lengths strictly bounded. | **HIGH -> RESOLVED** |
| **Insecure Heap Memory Retention** | Passwords read as immutable `String` via `JPasswordField.getText()` and converted via `new String(password)`. | Retrieved as mutable `char[]` via `getPassword()`; verified via `constantTimeCharEquals` without `String` allocation; wiped with `\0` immediately after use. `PBEKeySpec.clearPassword()` invoked in `finally` blocks. | **HIGH -> RESOLVED** |
| **Default Production Credentials & Weak Setup** | Hardcoded accounts (`school`/`school`, `admin`/`123`) automatically seeded; 4-char passwords permitted. | Removed hardcoded credentials. Implemented modal `FirstRunSetupDialog.java` enforcing strong policy (min 8 chars, uppercase, lowercase, numbers/symbols). | **CRITICAL -> RESOLVED** |
| **Non-Transactional First-Run Setup** | User and admin credential registration occurred in independent operations; partial failure left orphaned state. | Atomic transactional initialization (`PasswordSecurity.initializeFirstRunCredentials`) with automatic rollback on failure and clean retry support. | **HIGH -> RESOLVED** |
| **Swallowed Bootstrap Schema Errors** | `DatabaseBootstrap` caught and printed exceptions, allowing application to run against broken schemas. | Bootstrap failures now strictly propagate `SQLException`; connection safely closed on initialization failure. | **HIGH -> RESOLVED** |
| **Authorization Bypass on Deletions** | Admin authorization was disabled/commented out; any user could delete students, teachers, and employees. | Mandatory modal `Admin.verifyAdmin(Component, Connection)` check enforced on all delete operations. | **HIGH -> RESOLVED** |
| **Silent Database Failover Exposure** | Failure to connect to MySQL silently defaulted to local SQLite without operator knowledge. | Removed silent fallback; system fails safely with explicit diagnostics and reads strictly from `database.properties`. | **CRITICAL -> RESOLVED** |
| **SQL Injection Risks** | Potential string concatenation in grade searches or parameters. | 100% of database access parameterized via `PreparedStatement`; verified resilient against Tautology, DDL, and UNION payloads. | **HIGH -> RESOLVED** |
| **Unclosed JDBC Resources & Unsafe Test Cleanups** | `PreparedStatement` and `ResultSet` objects leaked; test runners deleted hardcoded filenames. | All queries in `try-with-resources`; test databases use unique temporary files (`File.createTempFile`) preserving all pre-existing files. | **HIGH -> RESOLVED** |

---

## 2. Technical Architecture & Cryptographic Design

### A. Salted PBKDF2 Password Hashing (`src/PasswordSecurity.java`)
- **Algorithm**: `PBKDF2WithHmacSHA256` via Java standard cryptographic library (`javax.crypto.SecretKeyFactory`).
- **Iteration Count**: 210,000 iterations (OWASP guidance).
- **Bounds Enforcement**:
  - `MIN_ITERATIONS`: 10,000
  - `MAX_ITERATIONS`: 2,000,000 (prevents algorithmic complexity / DoS attacks via stored hash manipulation)
  - `MIN_SALT_BYTES`: 16 (128 bits)
  - `MAX_SALT_BYTES`: 64
  - `MIN_KEY_BYTES`: 32 (256 bits)
  - `MAX_KEY_BYTES`: 64
- **Format Validation**: `isValidHashFormat(storedHash)` validates `$pbkdf2-sha256$<iterations>$<salt_hex>$<hash_hex>` before performing any key derivation.
- **Constant-Time Verification**: `constantTimeCharEquals(char[] a, String b)` compares characters in constant time without allocating immutable `String` objects on the Java heap.
- **Spec Clearing**:
  ```java
  PBEKeySpec spec = null;
  try {
      spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
      return skf.generateSecret(spec).getEncoded();
  } finally {
      if (spec != null) {
          spec.clearPassword();
      }
  }
  ```

### B. Backward-Compatible Hash Migration
To ensure zero disruption for existing users:
1. Hashes previously created with 65,536 iterations authenticate seamlessly (`verifyPassword` allows valid iterations between 10,000 and 2,000,000).
2. `needsRehash(storedHash)` returns `true` if `iterations < CURRENT_ITERATIONS` (210,000) or if the account is in legacy plaintext format.
3. Upon successful login, the credential is automatically upgraded in-place to 210,000 iterations and written to the database.

### C. Transactional First-Run Setup (`src/FirstRunSetupDialog.java`)
- Requires both administrator user credentials and a master authorization key.
- Strong password policy enforced: minimum 8 characters, no whitespace, mix of uppercase, lowercase, and numeric/special characters.
- Initial credentials saved via a single logical transaction:
  ```java
  conn.setAutoCommit(false);
  // insert user
  // insert admin key
  conn.commit();
  ```
- Any failure triggers `conn.rollback()`, ensuring zero partial state remains in the database.
- Character arrays are wiped on all exit paths using `PasswordSecurity.wipe(char[])`.

### D. Safe Database Bootstrapping (`src/DatabaseBootstrap.java`)
- Creates all 6 required tables (`pass`, `admin`, `student_info`, `teacher_info`, `employee_info`, `exam_marks`).
- Validates each table with `SELECT 1 FROM <table> LIMIT 1`.
- Any error immediately throws `SQLException` to the startup handler, closing connections and displaying diagnostic error dialogs.

---

## 3. Operational Security & Migration Procedures

### Initial Setup Procedure
1. Configure `database.properties` with the desired engine (`sqlite` or `mysql`).
2. Run the application (`java -jar dist/School_Managment_System.jar` or NetBeans / CLI).
3. If this is a new installation, complete the **First-Run Administrator Setup** dialog.
4. Securely store your Administrator Authorization Key; it is required for all destructive operations (e.g., deleting students, teachers, employees, or exam records).

### Credential Rotation
- **User Password Change**: Users can update their passwords through administrative tools or upon logging in.
- **Administrator Key Rotation**: The administrator key can be updated directly via the `Admin.java` administrative interface.

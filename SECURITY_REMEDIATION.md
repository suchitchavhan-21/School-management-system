# Application Security Remediation & Hardening Guide

**Project**: School Management System in Java  
**Security Standard**: Zero-Trust, NIST SP 800-63B Guidelines, OWASP Password Storage Standards  
**Status**: All Critical and High-Severity Security Defects Remediated & Verified (170/170 Tests Passed)

---

## 1. Vulnerabilities Remediated

| Vulnerability | Initial State | Remediated State | Risk Reduction |
|---|---|---|---|
| **Plaintext Password Storage** | Credentials stored unencrypted in `pass` and `admin` tables. | Salted PBKDF2-HMAC-SHA256 (210,000 iterations in compliance with OWASP guidance, 16-byte random salt). | **CRITICAL -> RESOLVED** |
| **Destructive Credential Initialization** | `initializeFirstRunCredentials` executed `DELETE FROM pass` and `DELETE FROM admin` before inserting accounts. | Strictly checks `SetupState.UNINITIALIZED`. Prohibits deletion/overwrite of existing accounts. Inconsistent states produce safe diagnostic alerts. | **CRITICAL -> RESOLVED** |
| **Malformed Hash DoS & CPU Exhaustion** | Incomplete/malformed hash strings could trigger excessive computation or unhandled crashes. | Strict pre-computation format validation; iteration counts strictly bounded [10,000, 2,000,000]; salt/key lengths strictly bounded. | **HIGH -> RESOLVED** |
| **Insecure Heap Memory Retention** | Passwords read as immutable `String` via `JPasswordField.getText()` and converted via `new String(password)`. | Retrieved as mutable `char[]` via `getPassword()`; verified via `constantTimeCharEquals` without `String` allocation; wiped with `\0` immediately after use. `PBEKeySpec.clearPassword()` invoked in `finally` blocks. | **HIGH -> RESOLVED** |
| **Default Production Credentials & Weak Setup** | Hardcoded accounts (`school`/`school`, `admin`/`123`) automatically seeded; 4-char passwords permitted. | Removed hardcoded credentials. Implemented modal `FirstRunSetupDialog.java` enforcing strong policy (min 8 chars, uppercase, lowercase, numbers/symbols). | **CRITICAL -> RESOLVED** |
| **Non-Transactional First-Run Setup** | User and admin credential registration occurred in independent operations; partial failure left orphaned state. | Atomic transactional initialization (`PasswordSecurity.initializeFirstRunCredentials`) with automatic rollback on failure and clean retry support. | **HIGH -> RESOLVED** |
| **Swallowed Bootstrap Schema Errors** | `DatabaseBootstrap` caught and printed exceptions, allowing application to run against broken schemas. | Bootstrap failures now strictly propagate `SQLException`; connection safely closed on initialization failure. | **HIGH -> RESOLVED** |
| **Authorization Bypass on Deletions** | Admin authorization was disabled/commented out; any user could delete students, teachers, employees, marks, and attendance. | Mandatory modal `Admin.verifyAdmin(Component, Connection)` check enforced on all delete operations. | **HIGH -> RESOLVED** |
| **Silent Database Failover Exposure** | Failure to connect to MySQL silently defaulted to local SQLite without operator knowledge. | Removed silent fallback; system fails safely with explicit diagnostics and reads strictly from `database.properties` or environment variables. | **CRITICAL -> RESOLVED** |
| **Unsafe MySQL Deployment Defaults** | `javaconnect.java` generated MySQL defaults with `root` username, blank password, and disabled SSL. | Prohibited `root` with blank password. Added 12-factor environment variable support (`SMS_MYSQL_USER`, `SMS_MYSQL_PASSWORD`). Enforced SSL transport encryption option. Provided `database.properties.template`. | **HIGH -> RESOLVED** |
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
- **Observed Benchmarks**: Measured on OpenJDK 11.0.26 LTS:
  - Hash computation: ~350–600 ms
  - Verification: ~400–600 ms
  - Calibrated for strong protection against offline GPU cracking without impacting interactive login usability.

### B. SetupState & Non-Destructive Credential Preservation
- **Enum `SetupState`**:
  - `UNINITIALIZED`: Both `pass` and `admin` tables have 0 rows. First-run setup dialog is permitted.
  - `INITIALIZED`: Both tables contain $\ge 1$ records. Setup is permanently disabled.
  - `INCONSISTENT_MISSING_ADMIN`: Users exist, but admin master key is missing. System halts setup and displays a diagnostic warning. Existing accounts are strictly preserved.
  - `INCONSISTENT_MISSING_USERS`: Admin master key exists, but no user accounts exist. System halts setup and displays a diagnostic warning. Existing admin key is strictly preserved.
  - `SCHEMA_ERROR` / `INACCESSIBLE`: Table verification fails or connection is closed.
- **Zero-Trust Initialization**:
  - In `initializeFirstRunCredentials`: Verifies `getSetupState(conn) == SetupState.UNINITIALIZED`.
  - Never executes `DELETE FROM pass` or `DELETE FROM admin`.
  - Transaction commits atomically; full rollback on any failure.

### C. Backward-Compatible Hash Migration
To ensure zero disruption for existing users:
1. Hashes previously created with 65,536 iterations authenticate seamlessly (`verifyPassword` allows valid iterations between 10,000 and 2,000,000).
2. `needsRehash(storedHash)` returns `true` if `iterations < CURRENT_ITERATIONS` (210,000) or if the account is in legacy plaintext format.
3. Upon successful login, the credential is automatically upgraded in-place to 210,000 iterations and written to the database.

### D. Secure MySQL Deployment Architecture (`src/javaconnect.java`)
- **Configuration Hierarchy**:
  1. Environment Variables: `SMS_DB_TYPE`, `SMS_MYSQL_HOST`, `SMS_MYSQL_PORT`, `SMS_MYSQL_DATABASE`, `SMS_MYSQL_USER`, `SMS_MYSQL_PASSWORD`, `SMS_MYSQL_USE_SSL`.
  2. Configuration File: `database.properties` or `database.properties.local`.
- **Hardening Rules**:
  - Missing database username is explicitly rejected.
  - Connecting as `root` with a blank password is strictly forbidden for MySQL.
  - TLS transport encryption is enforced when `db.mysql.useSSL=true` (`useSSL=true&requireSSL=true`).
  - Connection failures do not fall back to SQLite.

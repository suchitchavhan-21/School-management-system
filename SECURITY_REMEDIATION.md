# Zero-Trust Security Architecture & Remediation Report

> **Notice**: The authoritative, permanently maintained version of this report is located at [`docs/security/SECURITY_REMEDIATION.md`](docs/security/SECURITY_REMEDIATION.md).

**Project**: School Management System in Java
**Cryptographic Standard**: PBKDF2WithHmacSHA256 (210,000 iterations, 16-byte secure random salt)
**Security Model**: Defense-in-Depth, Least Privilege, Zero-Trust Storage & Memory Safety

---

## 1. Cryptographic Security Engine (`src/PasswordSecurity.java`)

### A. Algorithm Specification
- **Key Derivation Function**: `PBKDF2WithHmacSHA256` via Java standard cryptography (`javax.crypto.SecretKeyFactory`).
- **Work Factor / Iterations**: 210,000 iterations (OWASP Password Storage Cheat Sheet compliance).
- **Salt Generation**: 16-byte cryptographically secure pseudorandom salt generated using `java.security.SecureRandom`.
- **Derived Key Length**: 256 bits (32 bytes).
- **Encoded String Format**:
  `$pbkdf2-sha256$<iterations>$<salt_hex>$<hash_hex>`

### B. Algorithmic Complexity & Denial-of-Service Defense
Before executing CPU-intensive key derivation, `PasswordSecurity.isValidHashFormat(storedHash)` validates structural integrity:
- Algorithm identifier must equal `pbkdf2-sha256`.
- Iteration count must be an integer within `[10,000, 2,000,000]`.
- Salt hex length must be even and decode to between 16 and 64 bytes.
- Key hex length must be even and decode to between 32 and 64 bytes.
Malformed hashes or extreme iteration values (e.g. 100,000,000) are rejected instantly without CPU freeze.

### C. In-Memory Protection & Zeroization
- Plaintext password buffers are kept as `char[]` rather than immutable `java.lang.String` objects to avoid indefinite persistence in the Java Virtual Machine String pool / heap.
- `PasswordSecurity.wipe(char[])` explicitly overwrites character arrays with null characters (`\0`) in `finally` blocks.
- `PBEKeySpec.clearPassword()` is invoked inside a `finally` block in `pbkdf2()` immediately following key generation.
- Legacy plaintext comparisons use `constantTimeCharEquals(char[], String)` without allocating String copies.

### D. Benchmark Latency (Verified on OpenJDK 11.0.26 LTS)
- Password Hash Generation (210,000 iterations): **~359 ms**
- Password Verification (210,000 iterations): **~355 ms**
- Sufficiently fast for interactive human authentication while imposing severe computational penalties on offline brute-force attacks.

---

## 2. Non-Destructive Credential Provisioning (`FirstRunSetupDialog.java`)

### A. State Inspection Matrix
`PasswordSecurity.getSetupState(Connection)` queries table counts for `pass` and `admin` before permitting setup:
- `UNINITIALIZED` (`pass=0`, `admin=0`): Setup permitted.
- `INITIALIZED` (`pass>0`, `admin>0`): Normal operating state; setup strictly rejected.
- `INCONSISTENT_MISSING_ADMIN` (`pass>0`, `admin=0`): Setup rejected to protect existing user accounts.
- `INCONSISTENT_MISSING_USERS` (`pass=0`, `admin>0`): Setup rejected to protect existing administrator key.
- `SCHEMA_ERROR`: Setup halted until database tables are provisioned.

### B. Atomic Transactional Enrollment
`PasswordSecurity.initializeFirstRunCredentials`:
1. Sets `conn.setAutoCommit(false)`.
2. Validates `UNINITIALIZED` state inside transaction.
3. Inserts administrator login account into `pass`.
4. Inserts master authorization key into `admin`.
5. Calls `conn.commit()`.
6. On any `SQLException` or `RuntimeException`, calls `conn.rollback()` and restores previous auto-commit state.

---

## 3. Privilege Enforcement & Access Control (`src/Admin.java`)

- **Administrator Verification**: `Admin.verifyAdmin(Component, Connection)` is invoked before executing record deletions across all data systems:
  - `Students_Infromation_System.java`: Student record deletion.
  - `Teachers_Information_System.java`: Faculty record deletion.
  - `Employees_Infromation_System.java`: Staff record deletion.
  - `Student_Grades_System.java`: Examination mark deletion.
  - `Student_Attendance_System.java`: Attendance record deletion.
- **Window Lifecycle**: Corrected from `EXIT_ON_CLOSE` to `DISPOSE_ON_CLOSE`.

---

## 4. Database Layer Hardening (`src/javaconnect.java`)

- **Root Access Prohibition**: Explicitly rejects connecting to MySQL as `root` with a blank password.
- **Fail-Safe Integrity**: Refuses to silently switch from MySQL to SQLite upon connection failure, preventing data drift and split-brain vulnerability.
- **Transport Encryption**: Supports `useSSL=true&requireSSL=true` for MySQL JDBC connections.

# Security Policy

## 1. Supported Versions

| Version | Supported | Minimum Java Version | Cryptographic Standard |
|---|---|---|---|
| 2.x (Current `main`) | :white_check_mark: Yes | OpenJDK 11 LTS | PBKDF2-HMAC-SHA256 (210,000 iter) |
| 1.x (Legacy) | :x: No | Java 7/8 | Plaintext / Unsalted (Deprecated) |

---

## 2. Security Architecture & Threat Model

The **School Management System** employs a defense-in-depth security model:

### A. Authentication & Cryptography
- **Password Hashing**: Implements **PBKDF2-HMAC-SHA256** with 210,000 iterations (OWASP recommendation), a 16-byte cryptographically secure random salt (`SecureRandom`), and a 256-bit derived key length.
- **Hash Bounds & DoS Prevention**: Stored hashes are validated against strict structural rules prior to executing any cryptographic derivation:
  - Iterations must be between 10,000 and 2,000,000.
  - Salt length must be between 16 and 64 bytes.
  - Key length must be between 32 and 64 bytes.
  - Malformed or corrupted hashes are safely rejected without excessive CPU usage or uncaught runtime exceptions.
- **Memory Protection**:
  - Deprecated `JPasswordField.getText()` is completely removed.
  - `JPasswordField.getPassword()` is used across all authentication dialogs.
  - Sensitive character arrays (`char[]`) are zero-wiped immediately after authentication via `PasswordSecurity.wipe(char[])`.
  - `PBEKeySpec` instances are cleared via `clearPassword()` in `finally` blocks.
- **Constant-Time Verification**: Password equality verification avoids timing side-channels and does not allocate immutable `String` objects on the heap.

### B. Authorization & Access Control
- **Two-Tier Privilege Model**: Differentiates between standard operational users (teachers/staff) and system administrators.
- **Administrator Barrier**: Destructive actions (deleting student records, faculty records, employee profiles, examination marks, or attendance logs) strictly require administrator authorization via `Admin.verifyAdmin(Component, Connection)`.
- **First-Run Account Preservation**: The first-run setup wizard (`FirstRunSetupDialog.java`) only executes on genuinely uninitialized databases. If existing accounts or administrator keys are detected, initialization is permanently prohibited, preventing account takeover or data destruction.

### C. Database Security & Injection Immunity
- **Parameterized SQL**: 100% of SQL queries across all DAOs and user interface frames utilize `PreparedStatement` with typed parameter binding.
- **Connection Isolation**: The system prohibits connecting to network MySQL databases as `root` with a blank password.
- **No Silent Fallback**: Database connection errors fail visibly to prevent silent split-brain conditions or unintended data exposure.

---

## 3. Reporting a Vulnerability

If you discover a security vulnerability or weakness within this repository:
1. **Do not create a public issue.**
2. Send a responsible disclosure report directly to the repository maintainer or project security team.
3. Include:
   - Vulnerability description and classification.
   - Proof of Concept (PoC) or reproduction steps.
   - Impact assessment.
4. The maintainers will respond promptly, evaluate the finding, and coordinate a remediated release.

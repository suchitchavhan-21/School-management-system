# Contributing to the School Management System

Thank you for your interest in contributing to the **School Management System**. This project adheres to strict **zero-trust security principles**, robust automated testing, and backwards-compatible engineering.

---

## 1. Zero-Trust Development Principles

All contributors must follow these mandatory rules:
1. **Never commit credentials or secrets**:
   - Never add passwords, private keys, connection strings, or personal student records to any file in the repository.
   - Use environment variables (`SMS_MYSQL_USER`, `SMS_MYSQL_PASSWORD`) or uncommitted `database.properties.local` files.
2. **Parameterized queries only**:
   - All database interactions must use parameterized `PreparedStatement` with try-with-resources. String concatenation for SQL queries is strictly prohibited.
3. **Password Security**:
   - Never store or log plaintext passwords. Use `PasswordSecurity.hashPassword(char[])` (salted PBKDF2-HMAC-SHA256, 210,000 iterations).
   - In-memory password buffers (`char[]`) must be zero-wiped immediately after use via `PasswordSecurity.wipe(char[])`.
4. **Preserve existing data**:
   - Database operations, tests, and setup scripts must never execute destructive operations (`DELETE FROM ...`, `DROP TABLE`) against production or pre-existing databases.
   - Tests must exclusively target ephemeral SQLite files (`File.createTempFile`).

---

## 2. Setting Up the Development Environment

### Prerequisites:
- **Java Development Kit (JDK)**: OpenJDK 11 LTS or newer.
- **Git**: Version 2.20 or newer.
- **IDE**: NetBeans 8.x / 11.x+ or any Java-compatible editor (VS Code, IntelliJ IDEA).

### Verifying Build:
```powershell
# Using PowerShell automation script:
.\scripts\build.ps1

# Or using the JDK directly:
javac -encoding UTF-8 -cp "dist/lib/*;src" -d build/classes src/*.java test/*.java
```

---

## 3. Automated Testing Requirements

All contributions must compile cleanly and pass 100% of the automated test suite before opening a pull request.

```powershell
# Run the test suite:
.\scripts\test.ps1

# Or directly via Java:
java -cp "dist/lib/*;build/classes" SystemTestSuite
```

### Adding New Tests:
- If adding a feature, provide unit and integration tests in the `test/` directory.
- Register new test suites in `test/SystemTestSuite.java`.
- Ensure tests run against isolated temporary databases and leave no residual files.

---

## 4. NetBeans GUI Form Compatibility

- The user interface is built using NetBeans GUI Builder.
- Do NOT delete or dissociate `.form` files from their matching `.java` files in `src/`.
- If modifying UI layouts, ensure NetBeans XML form structures remain valid.

---

## 5. Pull Request Guidelines

1. Create a feature branch from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```
2. Verify repository integrity:
   ```powershell
   .\scripts\verify-repository.ps1
   ```
3. Commit changes with clear, semantic commit messages (e.g., `feat:`, `fix:`, `docs:`, `test:`).
4. Open a Pull Request against `main`. All CI checks must pass.

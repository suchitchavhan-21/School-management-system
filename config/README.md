# Configuration & Secret Management Guide

**Project**: School Management System in Java
**Security Standard**: Zero-Trust / 12-Factor Application Configuration

---

## 1. Overview

The School Management System decouples runtime configuration and secrets from the codebase using a flexible dual-tier model:
1. **Property Files**: `database.properties` (or `database.properties.local`).
2. **Environment Variables**: Operating system environment variables conforming to 12-factor application standards.

---

## 2. Configuration Precedence & Resolution Order

When connecting to the database, `javaconnect.java` resolves configuration values using the following precedence (highest priority first):
1. **Environment Variables**: Overrides any file-based setting.
2. **`database.properties`**: Local configuration file in the working directory.
3. **Built-in Safe Defaults**: Embedded SQLite database (`student_database.sqlite`).

---

## 3. Supported Environment Variables

| Variable | Description | Default | Example |
|---|---|---|---|
| `SMS_DB_TYPE` | Active database engine (`sqlite` or `mysql`) | `sqlite` | `mysql` |
| `SMS_MYSQL_HOST` | MySQL hostname or IP address | `localhost` | `db.school.edu` |
| `SMS_MYSQL_PORT` | MySQL listening port (1 - 65535) | `3306` | `3306` |
| `SMS_MYSQL_DATABASE` | MySQL database schema name | `student_database` | `school_production` |
| `SMS_MYSQL_USER` | MySQL database username (least privilege) | *(empty)* | `sms_user` |
| `SMS_MYSQL_PASSWORD` | MySQL database user password | *(empty)* | `P@ssw0rd#2026!` |
| `SMS_MYSQL_USE_SSL` | Enforce SSL/TLS transport encryption | `true` | `true` |

---

## 4. Zero-Trust Security Policies

1. **No Root with Blank Password**:
   - `javaconnect.java` explicitly forbids connecting to MySQL as `root` with a blank or empty password. If attempted, the application halts immediately with a `Database Security Violation` message.
2. **No Silent Fallback**:
   - If MySQL is configured but fails to connect or bootstrap, the system **will NOT** silently fall back to SQLite. Silent fallbacks risk data divergence, data leakage, and silent split-brain conditions.
3. **Credential Exclusion**:
   - `database.properties.local` and `*.local.properties` are ignored by `.gitignore`. Never commit live database credentials to Git.

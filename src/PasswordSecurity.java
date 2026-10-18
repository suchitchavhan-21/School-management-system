import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Enterprise Password Security & Authentication Engine.
 * 
 * Implements salted PBKDF2 with HMAC-SHA256 (210,000 iterations in compliance with OWASP guidelines)
 * using standard Java cryptographic libraries.
 * 
 * Features:
 * - Strict hash format validation before performing expensive computation.
 * - Enforcement of iteration bounds (10,000 to 2,000,000) and salt/key size limits to prevent DoS.
 * - Constant-time verification without allocating immutable plaintext password Strings.
 * - Secure clearing of PBEKeySpec in finally blocks.
 * - Transparent upgrade of legacy plaintext and older lower-iteration hashes.
 * - Transactional first-run credential provisioning with atomic commit/rollback.
 * - Memory zero-wiping for sensitive character arrays.
 */
public class PasswordSecurity {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    public static final int CURRENT_ITERATIONS = 210000;
    public static final int MIN_ITERATIONS = 10000;
    public static final int MAX_ITERATIONS = 2000000;
    public static final int MIN_SALT_BYTES = 16;
    public static final int MAX_SALT_BYTES = 64;
    public static final int MIN_KEY_BYTES = 32;
    public static final int MAX_KEY_BYTES = 64;

    private static final int DEFAULT_SALT_BYTES = 16;
    private static final int DEFAULT_KEY_LENGTH_BITS = 256;
    public static final String PREFIX = "$pbkdf2-sha256$";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static class ValidationResult {
        public final boolean valid;
        public final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
    }

    /**
     * Hashes a password using PBKDF2WithHmacSHA256 and a cryptographically secure random salt.
     */
    public static String hashPassword(char[] password) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }

        byte[] salt = new byte[DEFAULT_SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);

        byte[] hash = pbkdf2(password, salt, CURRENT_ITERATIONS, DEFAULT_KEY_LENGTH_BITS);
        try {
            return PREFIX + CURRENT_ITERATIONS + "$" + toHex(salt) + "$" + toHex(hash);
        } finally {
            Arrays.fill(hash, (byte) 0);
        }
    }

    /**
     * Validates whether a stored credential string matches the expected PBKDF2 format
     * and stays within safe iteration and size boundaries before running CPU-intensive operations.
     */
    public static boolean isValidHashFormat(String storedHash) {
        if (storedHash == null || !storedHash.startsWith(PREFIX)) {
            return false;
        }
        String[] parts = storedHash.split("\\$", -1);
        if (parts.length != 5 || !parts[0].isEmpty() || !parts[1].equals("pbkdf2-sha256")) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[2]);
            if (iterations < MIN_ITERATIONS || iterations > MAX_ITERATIONS) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }

        String saltHex = parts[3];
        if (saltHex.isEmpty() || saltHex.length() % 2 != 0) {
            return false;
        }
        int saltBytes = saltHex.length() / 2;
        if (saltBytes < MIN_SALT_BYTES || saltBytes > MAX_SALT_BYTES || !isHex(saltHex)) {
            return false;
        }

        String hashHex = parts[4];
        if (hashHex.isEmpty() || hashHex.length() % 2 != 0) {
            return false;
        }
        int keyBytes = hashHex.length() / 2;
        if (keyBytes < MIN_KEY_BYTES || keyBytes > MAX_KEY_BYTES || !isHex(hashHex)) {
            return false;
        }

        return true;
    }

    /**
     * Verifies a password against a stored hash or legacy plaintext record.
     * Uses constant-time comparison to prevent timing attacks.
     * Validates hash format before performing any expensive derivation.
     */
    public static boolean verifyPassword(char[] password, String storedHash) {
        if (password == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }

        if (storedHash.startsWith(PREFIX)) {
            if (!isValidHashFormat(storedHash)) {
                return false; // Safely reject malformed hash without expensive computation
            }

            String[] parts = storedHash.split("\\$", -1);
            try {
                int iterations = Integer.parseInt(parts[2]);
                byte[] salt = fromHex(parts[3]);
                byte[] expectedHash = fromHex(parts[4]);

                if (salt == null || expectedHash == null) {
                    return false;
                }

                byte[] actualHash = pbkdf2(password, salt, iterations, expectedHash.length * 8);
                try {
                    return MessageDigest.isEqual(expectedHash, actualHash);
                } finally {
                    Arrays.fill(actualHash, (byte) 0);
                    Arrays.fill(expectedHash, (byte) 0);
                }
            } catch (Exception e) {
                return false;
            }
        } else {
            // Constant-time check for legacy plaintext without creating an immutable String
            return constantTimeCharEquals(password, storedHash);
        }
    }

    /**
     * Checks if the stored hash requires re-hashing due to being legacy plaintext
     * or having an iteration count lower than CURRENT_ITERATIONS.
     */
    public static boolean needsRehash(String storedHash) {
        if (storedHash == null || !storedHash.startsWith(PREFIX)) {
            return true;
        }
        if (!isValidHashFormat(storedHash)) {
            return true;
        }
        String[] parts = storedHash.split("\\$", -1);
        try {
            int iterations = Integer.parseInt(parts[2]);
            return iterations < CURRENT_ITERATIONS;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    /**
     * Checks if the stored credential is a valid PBKDF2 hash.
     */
    public static boolean isHashed(String storedHash) {
        return storedHash != null && storedHash.startsWith(PREFIX) && isValidHashFormat(storedHash);
    }

    /**
     * Checks if the stored credential is in legacy plaintext format requiring migration.
     */
    public static boolean isLegacyPlaintext(String storedHash) {
        return storedHash != null && !storedHash.startsWith(PREFIX);
    }

    /**
     * Authenticates a user from the 'pass' table and transparently upgrades legacy plaintext
     * passwords or older lower-iteration hashes to the current PBKDF2 standard.
     */
    public static boolean authenticateAndMigrateUser(Connection conn, String username, char[] password) {
        if (conn == null || username == null || password == null) {
            return false;
        }

        String selectSql = "SELECT Password FROM pass WHERE Username = ?";
        try (PreparedStatement pst = conn.prepareStatement(selectSql)) {
            pst.setString(1, username);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("Password");
                    if (verifyPassword(password, storedHash)) {
                        if (needsRehash(storedHash)) {
                            upgradeUserPassword(conn, username, password);
                        }
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[PasswordSecurity] User authentication query failed: " + e.getMessage());
        }
        return false;
    }

    /**
     * Upgrades a user password to PBKDF2 hash.
     */
    public static boolean upgradeUserPassword(Connection conn, String username, char[] password) {
        String updateSql = "UPDATE pass SET Password = ? WHERE Username = ?";
        try (PreparedStatement pst = conn.prepareStatement(updateSql)) {
            pst.setString(1, hashPassword(password));
            pst.setString(2, username);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[PasswordSecurity] Failed to upgrade user password: " + e.getMessage());
            return false;
        }
    }

    /**
     * Authenticates an administrator and upgrades legacy admin credentials or older hashes.
     */
    public static boolean authenticateAndMigrateAdmin(Connection conn, char[] password) {
        if (conn == null || password == null) {
            return false;
        }

        String selectSql = "SELECT Password FROM admin";
        try (PreparedStatement pst = conn.prepareStatement(selectSql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                String storedHash = rs.getString("Password");
                if (verifyPassword(password, storedHash)) {
                    if (needsRehash(storedHash)) {
                        upgradeAdminPassword(conn, storedHash, password);
                    }
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("[PasswordSecurity] Admin authentication query failed: " + e.getMessage());
        }
        return false;
    }

    /**
     * Upgrades an admin password to PBKDF2 hash.
     */
    public static boolean upgradeAdminPassword(Connection conn, String oldStored, char[] password) {
        String updateSql = "UPDATE admin SET Password = ? WHERE Password = ?";
        try (PreparedStatement pst = conn.prepareStatement(updateSql)) {
            pst.setString(1, hashPassword(password));
            pst.setString(2, oldStored);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[PasswordSecurity] Failed to upgrade admin password: " + e.getMessage());
            return false;
        }
    }

    /**
     * Sets or registers a new user with a hashed password.
     */
    public static boolean registerUser(Connection conn, String username, char[] password) throws SQLException {
        String sql = "INSERT INTO pass (Username, Password) VALUES (?, ?)";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, username);
            pst.setString(2, hashPassword(password));
            return pst.executeUpdate() > 0;
        }
    }

    /**
     * Sets or registers a new admin password with a hashed password.
     */
    public static boolean setAdminPassword(Connection conn, char[] password) throws SQLException {
        try (PreparedStatement delPst = conn.prepareStatement("DELETE FROM admin")) {
            delPst.executeUpdate();
        }
        String sql = "INSERT INTO admin (Password) VALUES (?)";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, hashPassword(password));
            return pst.executeUpdate() > 0;
        }
    }

    /**
     * Atomically initializes both initial user and administrator credentials as a single transaction.
     * Ensures atomic commit and full rollback on failure, preserving connection auto-commit state.
     */
    public static boolean initializeFirstRunCredentials(Connection conn, String username, char[] userPassword, char[] adminKey) throws SQLException {
        if (conn == null || username == null || username.trim().isEmpty() || userPassword == null || adminKey == null) {
            return false;
        }

        boolean originalAutoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);

            // Clean any partial setup records to ensure clean retry
            try (PreparedStatement delPass = conn.prepareStatement("DELETE FROM pass WHERE Username = ?");
                 PreparedStatement delAdmin = conn.prepareStatement("DELETE FROM admin")) {
                delPass.setString(1, username.trim());
                delPass.executeUpdate();
                delAdmin.executeUpdate();
            }

            // Insert initial user
            int userRows;
            String sqlPass = "INSERT INTO pass (Username, Password) VALUES (?, ?)";
            try (PreparedStatement pst = conn.prepareStatement(sqlPass)) {
                pst.setString(1, username.trim());
                pst.setString(2, hashPassword(userPassword));
                userRows = pst.executeUpdate();
            }

            if (userRows <= 0) {
                conn.rollback();
                return false;
            }

            // Insert administrator authorization key
            int adminRows;
            String sqlAdmin = "INSERT INTO admin (Password) VALUES (?)";
            try (PreparedStatement pst = conn.prepareStatement(sqlAdmin)) {
                pst.setString(1, hashPassword(adminKey));
                adminRows = pst.executeUpdate();
            }

            if (adminRows <= 0) {
                conn.rollback();
                return false;
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                System.err.println("[PasswordSecurity] Rollback failed: " + rollbackEx.getMessage());
            }
            throw e;
        } finally {
            try {
                conn.setAutoCommit(originalAutoCommit);
            } catch (SQLException ignored) {}
        }
    }

    /**
     * Checks if initial first-run setup is required (either user or admin credentials missing).
     */
    public static boolean isSetupRequired(Connection conn) {
        if (conn == null) return false;
        try {
            int passCount = 0;
            int adminCount = 0;
            try (PreparedStatement pst = conn.prepareStatement("SELECT COUNT(*) FROM pass");
                 ResultSet rs = pst.executeQuery()) {
                if (rs.next()) passCount = rs.getInt(1);
            }
            try (PreparedStatement pst = conn.prepareStatement("SELECT COUNT(*) FROM admin");
                 ResultSet rs = pst.executeQuery()) {
                if (rs.next()) adminCount = rs.getInt(1);
            }
            return passCount == 0 || adminCount == 0;
        } catch (SQLException e) {
            System.err.println("[PasswordSecurity] Error checking setup requirement: " + e.getMessage());
            return false;
        }
    }

    /**
     * Validates password strength policy:
     * - Minimum 8 characters.
     * - No whitespace characters.
     * - Mix of uppercase, lowercase, and numeric or special characters.
     */
    public static ValidationResult validatePasswordStrength(char[] password) {
        if (password == null || password.length < 8) {
            return new ValidationResult(false, "Password must be at least 8 characters long.");
        }
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigitOrSpecial = false;

        for (char c : password) {
            if (Character.isWhitespace(c)) {
                return new ValidationResult(false, "Password cannot contain whitespace characters.");
            }
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else hasDigitOrSpecial = true;
        }

        if (!hasUpper || !hasLower || !hasDigitOrSpecial) {
            return new ValidationResult(false,
                "Password must contain a mix of uppercase letters, lowercase letters, and digits or special symbols.");
        }

        return new ValidationResult(true, "Password meets security policy.");
    }

    /**
     * Validates administrator authorization key strength policy:
     * - Minimum 8 characters.
     * - No whitespace characters.
     * - Must contain both letters and digits/special characters.
     */
    public static ValidationResult validateAdminKeyStrength(char[] key) {
        if (key == null || key.length < 8) {
            return new ValidationResult(false, "Administrator Authorization Key must be at least 8 characters long.");
        }
        boolean hasLetter = false;
        boolean hasDigitOrSpecial = false;

        for (char c : key) {
            if (Character.isWhitespace(c)) {
                return new ValidationResult(false, "Administrator Authorization Key cannot contain whitespace characters.");
            }
            if (Character.isLetter(c)) hasLetter = true;
            else hasDigitOrSpecial = true;
        }

        if (!hasLetter || !hasDigitOrSpecial) {
            return new ValidationResult(false,
                "Administrator Authorization Key must contain both letters and digits or special characters.");
        }

        return new ValidationResult(true, "Administrator Authorization Key meets security policy.");
    }

    /**
     * Securely clears sensitive character arrays from memory.
     */
    public static void wipe(char[] chars) {
        if (chars != null) {
            Arrays.fill(chars, '\0');
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        PBEKeySpec spec = null;
        try {
            spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("PBKDF2 calculation error: " + e.getMessage(), e);
        } finally {
            if (spec != null) {
                spec.clearPassword();
            }
        }
    }

    private static boolean constantTimeCharEquals(char[] a, String b) {
        if (a == null || b == null) {
            return false;
        }
        int aLen = a.length;
        int bLen = b.length();
        int result = aLen ^ bLen;
        int minLen = Math.min(aLen, bLen);
        for (int i = 0; i < minLen; i++) {
            result |= a[i] ^ b.charAt(i);
        }
        return result == 0;
    }

    private static boolean isHex(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean isHexDigit = (c >= '0' && c <= '9') ||
                                 (c >= 'a' && c <= 'f') ||
                                 (c >= 'A' && c <= 'F');
            if (!isHexDigit) {
                return false;
            }
        }
        return true;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] fromHex(String hex) {
        if (hex == null || hex.isEmpty() || (hex.length() % 2 != 0)) {
            return null;
        }
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int h = Character.digit(hex.charAt(i), 16);
            int l = Character.digit(hex.charAt(i + 1), 16);
            if (h == -1 || l == -1) {
                return null;
            }
            data[i / 2] = (byte) ((h << 4) + l);
        }
        return data;
    }
}

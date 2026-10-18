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
 * Implements salted PBKDF2 with HMAC-SHA256 (65,536 iterations) using Java Standard Library.
 * Provides transparent backward-compatible migration of legacy plaintext credentials,
 * constant-time hash verification, and secure memory wiping of character arrays.
 */
public class PasswordSecurity {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int SALT_BYTES = 16;
    private static final int KEY_LENGTH_BITS = 256;
    private static final String PREFIX = "$pbkdf2-sha256$";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Hashes a password using PBKDF2WithHmacSHA256 and a cryptographically secure random salt.
     */
    public static String hashPassword(char[] password) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }

        byte[] salt = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);

        byte[] hash = pbkdf2(password, salt, ITERATIONS, KEY_LENGTH_BITS);

        return PREFIX + ITERATIONS + "$" + toHex(salt) + "$" + toHex(hash);
    }

    /**
     * Verifies a password against a stored hash or legacy plaintext record.
     * Uses constant-time comparison to prevent timing attacks.
     */
    public static boolean verifyPassword(char[] password, String storedHash) {
        if (password == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }

        if (storedHash.startsWith(PREFIX)) {
            String[] parts = storedHash.split("\\$");
            // format: "" [0], "pbkdf2-sha256" [1], "<iterations>" [2], "<salt>" [3], "<hash>" [4]
            if (parts.length != 5) {
                return false;
            }

            try {
                int iterations = Integer.parseInt(parts[2]);
                byte[] salt = fromHex(parts[3]);
                byte[] expectedHash = fromHex(parts[4]);

                byte[] actualHash = pbkdf2(password, salt, iterations, expectedHash.length * 8);

                return MessageDigest.isEqual(expectedHash, actualHash);
            } catch (Exception e) {
                return false;
            }
        } else {
            // Constant-time check for legacy plaintext migration
            byte[] pwdBytes = new String(password).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] storedBytes = storedHash.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            return MessageDigest.isEqual(pwdBytes, storedBytes);
        }
    }

    /**
     * Checks if the stored credential is a valid PBKDF2 hash.
     */
    public static boolean isHashed(String storedHash) {
        return storedHash != null && storedHash.startsWith(PREFIX);
    }

    /**
     * Checks if the stored credential is in legacy plaintext format requiring migration.
     */
    public static boolean isLegacyPlaintext(String storedHash) {
        return storedHash != null && !storedHash.startsWith(PREFIX);
    }

    /**
     * Authenticates a user from the 'pass' table and transparently upgrades legacy plaintext passwords to PBKDF2.
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
                        // Transparently migrate legacy plaintext to PBKDF2
                        if (isLegacyPlaintext(storedHash)) {
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
     * Authenticates an administrator and upgrades legacy admin password if necessary.
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
                    if (isLegacyPlaintext(storedHash)) {
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
        // Clear old admin passwords and insert new hashed password
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
     * Securely clears sensitive character arrays from memory.
     */
    public static void wipe(char[] chars) {
        if (chars != null) {
            Arrays.fill(chars, '\0');
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("PBKDF2 calculation error: " + e.getMessage(), e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] fromHex(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}

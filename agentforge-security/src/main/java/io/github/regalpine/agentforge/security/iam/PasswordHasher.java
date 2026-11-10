package io.github.regalpine.agentforge.security.iam;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/**
 * Password hashing utility using SHA-256 with salt.
 * <p>
 * This implementation uses only JDK built-in primitives
 * (no external dependencies). The hash format is:
 * <pre>
 *   {salt-base64}:{hash-base64}
 * </pre>
 * <p>
 * For production deployments with higher security requirements,
 * consider replacing with a dedicated KDF (e.g. bcrypt, argon2)
 * via an external dependency.
 */
public final class PasswordHasher {

    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16;
    private static final SecureRandom RANDOM =
        new SecureRandom();

    private PasswordHasher() {
    }

    /**
     * Hash a plaintext password with a random salt.
     *
     * @param password the plaintext password
     * @return the combined salt+hash string
     */
    public static String hash(String password) {
        Objects.requireNonNull(password, "password cannot be null");

        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);

        byte[] hash = computeHash(password, salt);

        String saltB64 = Base64.getEncoder()
            .encodeToString(salt);
        String hashB64 = Base64.getEncoder()
            .encodeToString(hash);

        return saltB64 + ":" + hashB64;
    }

    /**
     * Verify a plaintext password against a stored hash.
     *
     * @param password the plaintext password to check
     * @param storedHash the stored hash string (salt:hash)
     * @return true if the password matches
     */
    public static boolean verify(
        String password, String storedHash
    ) {
        if (password == null || storedHash == null) {
            return false;
        }

        int separatorIndex = storedHash.indexOf(':');
        if (separatorIndex < 0) {
            return false;
        }

        String saltB64 = storedHash.substring(
            0, separatorIndex
        );
        String expectedHashB64 = storedHash.substring(
            separatorIndex + 1
        );

        byte[] salt;
        try {
            salt = Base64.getDecoder().decode(saltB64);
        } catch (IllegalArgumentException e) {
            return false;
        }

        byte[] actualHash = computeHash(password, salt);
        String actualHashB64 = Base64.getEncoder()
            .encodeToString(actualHash);

        return constantTimeEquals(
            expectedHashB64, actualHashB64
        );
    }

    private static byte[] computeHash(
        String password, byte[] salt
    ) {
        try {
            MessageDigest digest =
                MessageDigest.getInstance(ALGORITHM);
            digest.update(salt);
            return digest.digest(
                password.getBytes(StandardCharsets.UTF_8)
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                "SHA-256 not available", e
            );
        }
    }

    private static boolean constantTimeEquals(
        String a, String b
    ) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}

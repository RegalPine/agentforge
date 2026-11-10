package io.github.regalpine.agentforge.observability;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Production data protection.
 * AF-ENHANCE-06 §10: ensures sensitive
 * data (prompts, tool inputs) is not
 * logged in full in production.
 *
 * <p>Default policy:
 * <ul>
 *   <li>Prompt content → hash only</li>
 *   <li>Tool input → hash only</li>
 *   <li>Model output → truncated</li>
 * </ul>
 */
public final class DataProtection {

    private final boolean enabled;
    private final int maxOutputLength;

    /**
     * Create with defaults (enabled,
     * max 256 chars).
     */
    public DataProtection() {
        this(true, 256);
    }

    /**
     * Create with custom settings.
     */
    public DataProtection(
        boolean enabled,
        int maxOutputLength
    ) {
        this.enabled = enabled;
        this.maxOutputLength = maxOutputLength;
    }

    /**
     * Protect a prompt for logging.
     * Returns hash if enabled, original
     * if disabled.
     */
    public String protectPrompt(String prompt) {
        if (!enabled || prompt == null) {
            return prompt;
        }
        return sha256(prompt);
    }

    /**
     * Protect tool input for logging.
     */
    public String protectToolInput(
        String input
    ) {
        if (!enabled || input == null) {
            return input;
        }
        return sha256(input);
    }

    /**
     * Protect model output for logging.
     * Truncates to max length.
     */
    public String protectOutput(String output) {
        if (!enabled || output == null) {
            return output;
        }
        if (output.length() <= maxOutputLength) {
            return output;
        }
        return output.substring(
            0, maxOutputLength
        ) + "...[truncated]";
    }

    /**
     * Protect attributes map.
     */
    public Map<String, String> protectAttributes(
        Map<String, String> attrs
    ) {
        if (!enabled || attrs == null) {
            return attrs;
        }

        var result =
            new java.util.HashMap<String, String>();

        for (var e : attrs.entrySet()) {
            String key = e.getKey();
            String val = e.getValue();

            if (key.contains("prompt")
                || key.contains("input")
            ) {
                result.put(key, sha256(val));
            } else if (key.contains("output")) {
                result.put(
                    key, protectOutput(val)
                );
            } else {
                result.put(key, val);
            }
        }

        return Map.copyOf(result);
    }

    /**
     * Check if protection is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get max output length.
     */
    public int maxOutputLength() {
        return maxOutputLength;
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest =
                MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                input.getBytes(StandardCharsets.UTF_8)
            );
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(
                    String.format("%02x", b)
                );
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available
            throw new RuntimeException(e);
        }
    }
}

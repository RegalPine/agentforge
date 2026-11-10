package io.github.regalpine.agentforge.security;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Redacts sensitive information from strings (logs, events, audit records).
 * <p>
 * Default redaction patterns cover common secret types:
 * Authorization headers, API keys, access tokens, passwords, private keys, etc.
 * <p>
 * Thread-safe and immutable.
 */
public final class SecretRedactor {

    private static final String REDACTED = "[REDACTED]";

    private static final List<Pattern> DEFAULT_PATTERNS = List.of(
        // Bearer tokens
        Pattern.compile("(Bearer\\s+)\\S+", Pattern.CASE_INSENSITIVE),
        // API-Key headers
        Pattern.compile("((?i)API[-_]?Key[:\\s]+)\\S+"),
        // Authorization header values
        Pattern.compile("((?i)Authorization[:\\s]+)\\S+"),
        // Common secret field patterns in JSON-like content
        Pattern.compile("((?i)(password|passwd|secret|token|api_key|apikey|access_token|private_key|refresh_token)[\"']?\\s*[:=]\\s*[\"']?)\\S+"),
        // Private key blocks
        Pattern.compile("(-----BEGIN\\s+(RSA\\s+)?PRIVATE KEY-----)[\\s\\S]*?(-----END\\s+(RSA\\s+)?PRIVATE KEY-----)")
    );

    private final List<Pattern> patterns;
    private final String replacement;

    public SecretRedactor() {
        this(DEFAULT_PATTERNS, REDACTED);
    }

    public SecretRedactor(List<Pattern> patterns, String replacement) {
        this.patterns = List.copyOf(patterns);
        this.replacement = Objects.requireNonNull(replacement);
    }

    /**
     * Redact sensitive information from the input string.
     *
     * @param input the input to redact
     * @return the redacted string
     */
    public String redact(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String result = input;
        for (Pattern pattern : patterns) {
            result = pattern.matcher(result).replaceAll("$1" + replacement);
        }
        return result;
    }

    /**
     * Redact values in a map (for metadata/audit records).
     *
     * @param map the map to redact
     * @return a new map with redacted values
     */
    public Map<String, Object> redactMap(Map<String, Object> map) {
        if (map == null) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (isSensitiveKey(key) && entry.getValue() instanceof String value) {
                result.put(entry.getKey(), replacement);
            } else if (entry.getValue() instanceof String value) {
                result.put(entry.getKey(), redact(value));
            } else {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return Map.copyOf(result);
    }

    private static final Set<String> SENSITIVE_KEYS = Set.of(
        "password", "passwd", "secret", "token", "api_key", "apikey",
        "access_token", "refresh_token", "private_key", "authorization",
        "bearer", "credential", "api-key"
    );

    private boolean isSensitiveKey(String lowerKey) {
        for (String sensitive : SENSITIVE_KEYS) {
            if (lowerKey.contains(sensitive)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a string likely contains a secret.
     */
    public boolean containsSensitiveData(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        for (Pattern pattern : patterns) {
            if (pattern.matcher(input).find()) {
                return true;
            }
        }
        return false;
    }
}

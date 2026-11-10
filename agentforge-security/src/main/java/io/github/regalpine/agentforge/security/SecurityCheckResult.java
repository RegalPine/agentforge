package io.github.regalpine.agentforge.security;

/**
 * Result of a single security check.
 */
public record SecurityCheckResult(
    boolean allowed,
    String reasonCode,
    String message
) {

    public static SecurityCheckResult allow() {
        return new SecurityCheckResult(true, null, null);
    }

    public static SecurityCheckResult deny(String reasonCode, String message) {
        return new SecurityCheckResult(false, reasonCode, message);
    }
}

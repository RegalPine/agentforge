package io.github.regalpine.agentforge.security;

/**
 * Result of the security execution chain.
 */
public record SecurityChainResult(
    boolean allowed,
    String reasonCode,
    String message
) {

    public static SecurityChainResult allow() {
        return new SecurityChainResult(true, null, null);
    }

    public static SecurityChainResult deny(String reasonCode, String message) {
        return new SecurityChainResult(false, reasonCode, message);
    }

    public boolean isDenied() {
        return !allowed;
    }
}

package io.github.regalpine.agentforge.memory;

/**
 * Memory policy decision.
 * AE-08 §17-18: Memory Policy.
 */
public record MemoryDecision(
    boolean allowed,
    String reason,
    MemoryDecisionType type
) {

    public enum MemoryDecisionType {
        ALLOW,
        DENY,
        DENY_REQUIRES_APPROVAL,
        DENY_REDACT
    }

    public static MemoryDecision allow() {
        return new MemoryDecision(
            true, "", MemoryDecisionType.ALLOW
        );
    }

    public static MemoryDecision allow(
        String reason
    ) {
        return new MemoryDecision(
            true, reason,
            MemoryDecisionType.ALLOW
        );
    }

    public static MemoryDecision deny(
        String reason
    ) {
        return new MemoryDecision(
            false, reason,
            MemoryDecisionType.DENY
        );
    }

    public static MemoryDecision denyRedact(
        String reason
    ) {
        return new MemoryDecision(
            false, reason,
            MemoryDecisionType.DENY_REDACT
        );
    }

    public static MemoryDecision denyApproval(
        String reason
    ) {
        return new MemoryDecision(
            false, reason,
            MemoryDecisionType.DENY_REQUIRES_APPROVAL
        );
    }

    public boolean isDenied() {
        return !allowed;
    }
}

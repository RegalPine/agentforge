package io.github.regalpine.agentforge.security;

/**
 * Trust level for instructions in the Agent execution context.
 * <p>
 * Implements the Instruction Hierarchy as defined in AF-SEC-01 §25:
 * <pre>
 * SYSTEM > DEVELOPER > POLICY > USER > TOOL > KNOWLEDGE > MEMORY > EXTERNAL
 * </pre>
 * Higher trust instructions cannot be overridden by lower trust instructions.
 * This is the primary defense against Prompt Injection.
 */
public enum InstructionTrustLevel {

    /** System instructions — highest trust, set by the platform. */
    SYSTEM(7),

    /** Developer-defined instructions. */
    DEVELOPER(6),

    /** Policy-injected constraints. */
    POLICY(5),

    /** User-provided instructions. */
    USER(4),

    /** Tool result content. */
    TOOL(3),

    /** Knowledge/RAG retrieved content. */
    KNOWLEDGE(2),

    /** Memory retrieved content. */
    MEMORY(1),

    /** External untrusted content — lowest trust. */
    EXTERNAL(0);

    private final int level;

    InstructionTrustLevel(int level) {
        this.level = level;
    }

    public int level() {
        return level;
    }

    /**
     * Check if this trust level is higher than another.
     */
    public boolean isHigherThan(InstructionTrustLevel other) {
        return this.level > other.level;
    }

    /**
     * Check if this trust level is at least as high as another.
     */
    public boolean isAtLeast(InstructionTrustLevel other) {
        return this.level >= other.level;
    }

    /**
     * Check if content at this trust level can override content at another level.
     */
    public boolean canOverride(InstructionTrustLevel target) {
        return this.level > target.level;
    }
}

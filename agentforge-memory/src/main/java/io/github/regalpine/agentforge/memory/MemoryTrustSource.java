package io.github.regalpine.agentforge.memory;

/**
 * Memory trust source.
 * AF-ENHANCE-06 §65: Memory Trust.
 *
 * <p>Determines how much trust
 * is placed in a memory based on
 * its origin. Context Assembly can
 * use this to control whether a
 * source enters high-priority
 * context.
 */
public enum MemoryTrustSource {

    SYSTEM,

    GOVERNED_KNOWLEDGE,

    VERIFIED_TOOL,

    USER,

    AGENT,

    MODEL_GENERATED,

    UNKNOWN;

    /**
     * Whether this source can
     * be used as system instruction.
     */
    public boolean canBeSystemInstruction() {
        return this == SYSTEM
            || this == GOVERNED_KNOWLEDGE;
    }

    /**
     * Whether this source has
     * high trust.
     */
    public boolean isHighTrust() {
        return this == SYSTEM
            || this == GOVERNED_KNOWLEDGE
            || this == VERIFIED_TOOL
            || this == USER;
    }

    /**
     * Whether model-generated
     * memory can bypass policy.
     * Answer: never.
     */
    public boolean requiresPolicyCheck() {
        return this == MODEL_GENERATED
            || this == AGENT
            || this == UNKNOWN;
    }
}

package io.github.regalpine.agentforge.memory;

/**
 * Memory source / provenance.
 * AE-08 §10: Memory Source.
 *
 * <p>Distinguishes between user-explicit
 * statements and agent-inferred conclusions.
 * Critical for confidence and policy decisions.
 */
public enum MemorySource {

    USER_EXPLICIT,

    AGENT_INFERRED,

    TOOL_RESULT,

    CAPABILITY_RESULT,

    SYSTEM,

    IMPORTED
}

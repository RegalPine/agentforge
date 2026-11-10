package io.github.regalpine.agentforge.memory;

/**
 * Confidence basis for memory.
 * AE-08 §11: Memory Confidence.
 */
public enum ConfidenceBasis {

    USER_EXPLICIT_STATEMENT,

    AGENT_INFERENCE,

    TOOL_EVIDENCE,

    STATISTICAL,

    MANUAL
}

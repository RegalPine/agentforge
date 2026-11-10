package io.github.regalpine.agentforge.memory;

/**
 * Context segment type.
 * AE-08 §62: Context Segment Type.
 */
public enum ContextSegmentType {

    SYSTEM,

    INSTRUCTION,

    USER_INPUT,

    STATE,

    OBSERVATION,

    MEMORY,

    CAPABILITY,

    TOOL_RESULT,

    KNOWLEDGE
}

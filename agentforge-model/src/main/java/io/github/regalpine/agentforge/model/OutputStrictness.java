package io.github.regalpine.agentforge.model;

/**
 * Strictness level for structured output.
 */
public enum OutputStrictness {
    /**
     * Strict validation - reject any non-conforming output.
     */
    STRICT,

    /**
     * Lenient validation - attempt to repair non-conforming output.
     */
    LENIENT,

    /**
     * No validation - return raw output.
     */
    NONE
}

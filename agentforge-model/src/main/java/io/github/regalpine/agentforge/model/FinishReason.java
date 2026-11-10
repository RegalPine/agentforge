package io.github.regalpine.agentforge.model;

/**
 * Reason why the model finished generating.
 */
public enum FinishReason {
    /**
     * Model completed normally.
     */
    STOP,

    /**
     * Model reached maximum token limit.
     */
    LENGTH,

    /**
     * Model requested tool calls.
     */
    TOOL_CALLS,

    /**
     * Content was filtered.
     */
    CONTENT_FILTER,

    /**
     * Unknown reason.
     */
    UNKNOWN
}

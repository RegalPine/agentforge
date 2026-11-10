package io.github.regalpine.agentforge.a2a;

/**
 * A2A standard error codes.
 * AF-ENHANCE-10 §52: Failure.
 */
public enum A2AErrorCode {

    A2A_AGENT_NOT_FOUND,

    A2A_AGENT_UNAVAILABLE,

    A2A_AUTH_FAILED,

    A2A_PERMISSION_DENIED,

    A2A_TASK_REJECTED,

    A2A_TASK_TIMEOUT,

    A2A_TASK_FAILED,

    A2A_TASK_CANCELLED,

    A2A_PROTOCOL_ERROR,

    A2A_INVALID_MESSAGE,

    A2A_ARTIFACT_INVALID,

    A2A_STREAM_FAILED,

    A2A_RECURSION_DETECTED;

    /**
     * Parse an error string into
     * the closest matching code.
     */
    public static A2AErrorCode fromString(
        String error
    ) {
        if (error == null || error.isBlank()) {
            return A2A_PROTOCOL_ERROR;
        }

        String lower = error.toLowerCase();

        if (lower.contains("not found")
            && lower.contains("agent")
        ) {
            return A2A_AGENT_NOT_FOUND;
        }
        if (lower.contains("unavailable")
        ) {
            return A2A_AGENT_UNAVAILABLE;
        }
        if (lower.contains("auth")
            && lower.contains("fail")
        ) {
            return A2A_AUTH_FAILED;
        }
        if (lower.contains("permission")
            || lower.contains("denied")
        ) {
            return A2A_PERMISSION_DENIED;
        }
        if (lower.contains("timeout")
        ) {
            return A2A_TASK_TIMEOUT;
        }
        if (lower.contains("cancel")
        ) {
            return A2A_TASK_CANCELLED;
        }
        if (lower.contains("recursion")
            || lower.contains("loop")
        ) {
            return A2A_RECURSION_DETECTED;
        }
        if (lower.contains("stream")
        ) {
            return A2A_STREAM_FAILED;
        }

        return A2A_PROTOCOL_ERROR;
    }
}

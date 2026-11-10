package io.github.regalpine.agentforge.provider.mcp;

import io.github.regalpine.agentforge.capability.CapabilityErrorType;

/**
 * MCP error → CapabilityErrorType mapper.
 * AE-10 §49: MCP Error Mapping.
 */
public final class McpErrorMapper {

    private McpErrorMapper() {}

    /**
     * Map an MCP error string to the
     * appropriate CapabilityErrorType.
     */
    public static CapabilityErrorType mapError(
        String error
    ) {
        if (error == null || error.isBlank()) {
            return CapabilityErrorType.UNKNOWN;
        }

        String lower = error.toLowerCase();

        if (lower.contains("timeout")
            || lower.contains("timed out")
        ) {
            return CapabilityErrorType.TIMEOUT;
        }

        if (lower.contains("connection")
            || lower.contains("connect")
            || lower.contains("refused")
            || lower.contains("reset")
        ) {
            return CapabilityErrorType.CONNECTION_ERROR;
        }

        if (lower.contains("auth")
            && lower.contains("denied")
        ) {
            return CapabilityErrorType.AUTHORIZATION_ERROR;
        }

        if (lower.contains("unauthorized")
            || lower.contains("401")
        ) {
            return CapabilityErrorType.AUTHENTICATION_ERROR;
        }

        if (lower.contains("forbidden")
            || lower.contains("403")
        ) {
            return CapabilityErrorType.AUTHORIZATION_ERROR;
        }

        if (lower.contains("invalid")
            && lower.contains("argument")
        ) {
            return CapabilityErrorType.INVALID_ARGUMENT;
        }

        if (lower.contains("rate limit")
            || lower.contains("429")
            || lower.contains("too many")
        ) {
            return CapabilityErrorType.RATE_LIMITED;
        }

        if (lower.contains("protocol")
            || lower.contains("parse")
            || lower.contains("schema")
        ) {
            return CapabilityErrorType.PROTOCOL_ERROR;
        }

        return CapabilityErrorType.REMOTE_ERROR;
    }

    /**
     * Determine if the error is retryable
     * based on its type.
     */
    public static boolean isRetryable(
        CapabilityErrorType type
    ) {
        return switch (type) {
            case CONNECTION_ERROR,
                 TIMEOUT,
                 RATE_LIMITED,
                 REMOTE_ERROR -> true;
            default -> false;
        };
    }
}

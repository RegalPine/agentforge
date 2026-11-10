package io.github.regalpine.agentforge.provider.mcp;

/**
 * MCP standard error codes.
 * AF-ENHANCE-10 §20: Error Mapping.
 */
public enum McpErrorCode {

    MCP_SERVER_NOT_FOUND,

    MCP_SERVER_UNAVAILABLE,

    MCP_CONNECTION_FAILED,

    MCP_TOOL_NOT_FOUND,

    MCP_INVALID_ARGUMENT,

    MCP_PERMISSION_DENIED,

    MCP_TIMEOUT,

    MCP_PROTOCOL_ERROR,

    MCP_TOOL_FAILED;

    /**
     * Parse an error string into
     * the closest matching code.
     */
    public static McpErrorCode fromString(
        String error
    ) {
        if (error == null || error.isBlank()) {
            return MCP_PROTOCOL_ERROR;
        }

        String lower = error.toLowerCase();

        if (lower.contains("not found")
            && lower.contains("server")
        ) {
            return MCP_SERVER_NOT_FOUND;
        }
        if (lower.contains("unavailable")
            || lower.contains("server_unavailable")
        ) {
            return MCP_SERVER_UNAVAILABLE;
        }
        if (lower.contains("connection")
            || lower.contains("connect")
        ) {
            return MCP_CONNECTION_FAILED;
        }
        if (lower.contains("tool")
            && lower.contains("not found")
        ) {
            return MCP_TOOL_NOT_FOUND;
        }
        if (lower.contains("invalid")
            && lower.contains("argument")
        ) {
            return MCP_INVALID_ARGUMENT;
        }
        if (lower.contains("permission")
            || lower.contains("denied")
        ) {
            return MCP_PERMISSION_DENIED;
        }
        if (lower.contains("timeout")
            || lower.contains("timed out")
        ) {
            return MCP_TIMEOUT;
        }
        if (lower.contains("protocol")
            || lower.contains("parse")
        ) {
            return MCP_PROTOCOL_ERROR;
        }
        if (lower.contains("failed")
            || lower.contains("error")
        ) {
            return MCP_TOOL_FAILED;
        }

        return MCP_PROTOCOL_ERROR;
    }
}

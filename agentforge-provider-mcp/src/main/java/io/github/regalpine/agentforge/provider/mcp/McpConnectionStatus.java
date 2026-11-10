package io.github.regalpine.agentforge.provider.mcp;

/**
 * MCP connection lifecycle status.
 * AE-10 §10: Connection Lifecycle.
 */
public enum McpConnectionStatus {

    CREATED,

    CONNECTING,

    CONNECTED,

    READY,

    FAILED,

    DISCONNECTED,

    RECONNECTING
}

package io.github.regalpine.agentforge.provider.mcp;

/**
 * MCP server lifecycle states.
 * AF-ENHANCE-10 §21: Lifecycle.
 */
public enum McpLifecycle {

    REGISTERED,

    CONNECTING,

    CONNECTED,

    DISCOVERING,

    READY,

    DEGRADED,

    DISABLED,

    REVOKED;

    /**
     * Check if transition to target
     * state is allowed.
     */
    public boolean canTransitionTo(
        McpLifecycle target
    ) {
        return switch (this) {
            case REGISTERED ->
                target == CONNECTING
                    || target == DISABLED
                    || target == REVOKED;
            case CONNECTING ->
                target == CONNECTED
                    || target == DISABLED
                    || target == REVOKED;
            case CONNECTED ->
                target == DISCOVERING
                    || target == DEGRADED
                    || target == DISABLED
                    || target == REVOKED;
            case DISCOVERING ->
                target == READY
                    || target == DEGRADED
                    || target == DISABLED;
            case READY ->
                target == DEGRADED
                    || target == DISABLED
                    || target == REVOKED;
            case DEGRADED ->
                target == READY
                    || target == DISABLED
                    || target == REVOKED;
            case DISABLED ->
                target == REVOKED
                    || target == CONNECTING;
            case REVOKED ->
                false;
        };
    }
}

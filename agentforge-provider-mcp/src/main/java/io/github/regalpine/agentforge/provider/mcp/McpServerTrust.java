package io.github.regalpine.agentforge.provider.mcp;

/**
 * MCP server trust level.
 * AF-ENHANCE-10 §12: Server Trust.
 *
 * <p>V1.0 default: UNTRUSTED.
 * Registration does not grant
 * automatic access to tenant
 * data, credentials, workspace,
 * memory, or internal network.
 */
public enum McpServerTrust {

    TRUSTED,

    VERIFIED,

    UNTRUSTED,

    REVOKED
}

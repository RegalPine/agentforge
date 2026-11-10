package io.github.regalpine.agentforge.a2a;

/**
 * External agent trust level.
 * AF-ENHANCE-10 §46: Agent Trust.
 *
 * <p>Untrusted agents cannot access
 * internal memory, workspace,
 * credentials, cross-tenant data,
 * or internal tools.
 */
public enum ExternalAgentTrust {

    TRUSTED,

    VERIFIED,

    UNTRUSTED,

    REVOKED
}

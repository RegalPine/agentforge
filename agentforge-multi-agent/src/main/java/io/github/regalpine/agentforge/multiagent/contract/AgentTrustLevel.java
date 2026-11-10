package io.github.regalpine.agentforge.multiagent.contract;

/**
 * AgentTrustLevel — AE-11 §53.
 *
 * Governance trust attribute for an agent.
 * Does NOT replace Policy, does NOT auto-authorize,
 * does NOT replace Capability Permission.
 */
public enum AgentTrustLevel {

    UNTRUSTED,
    STANDARD,
    VERIFIED,
    GOVERNED
}

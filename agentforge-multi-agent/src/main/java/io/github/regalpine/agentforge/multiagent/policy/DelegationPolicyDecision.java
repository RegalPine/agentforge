package io.github.regalpine.agentforge.multiagent.policy;

/**
 * DelegationPolicyDecision — AE-11 §22.
 *
 * The outcome of a delegation authorization check.
 */
public enum DelegationPolicyDecision {

    ALLOW,
    DENY,
    REQUIRE_APPROVAL
}

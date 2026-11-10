package io.github.regalpine.agentforge.multiagent.contract;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

/**
 * AgentExecutionPolicy — AE-11 §5, §20, §23, §54.
 *
 * Defines execution constraints for an agent participating
 * in multi-agent collaboration: timeout, max depth, max
 * children, allowed delegation targets, budget limits.
 */
public record AgentExecutionPolicy(

    Duration timeout,

    int maxDelegationDepth,

    int maxChildren,

    Set<String> allowedDelegationTargets,

    long maxTokens,

    int maxModelCalls,

    int maxToolCalls,

    int maxChildExecutions
) {

    public AgentExecutionPolicy {
        timeout = timeout == null
            ? Duration.ofSeconds(60) : timeout;

        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException(
                "Timeout must be positive");
        }

        if (maxDelegationDepth < 0) {
            throw new IllegalArgumentException(
                "maxDelegationDepth must be >= 0");
        }

        if (maxChildren < 0) {
            throw new IllegalArgumentException(
                "maxChildren must be >= 0");
        }

        allowedDelegationTargets = allowedDelegationTargets == null
            ? Set.of()
            : Set.copyOf(allowedDelegationTargets);
    }

    /**
     * Permissive default policy.
     */
    public static AgentExecutionPolicy defaults() {
        return new AgentExecutionPolicy(
            Duration.ofSeconds(60),
            5,
            10,
            Set.of(),
            100_000,
            50,
            100,
            10
        );
    }

    /**
     * Strict policy for testing.
     */
    public static AgentExecutionPolicy strict() {
        return new AgentExecutionPolicy(
            Duration.ofSeconds(10),
            2,
            3,
            Set.of(),
            10_000,
            5,
            10,
            3
        );
    }

    /**
     * Check if a target agentId is allowed.
     * Empty set means all targets allowed.
     */
    public boolean isDelegationTargetAllowed(String targetAgentId) {
        Objects.requireNonNull(targetAgentId);
        if (allowedDelegationTargets.isEmpty()) {
            return true;
        }
        return allowedDelegationTargets.contains(targetAgentId);
    }
}

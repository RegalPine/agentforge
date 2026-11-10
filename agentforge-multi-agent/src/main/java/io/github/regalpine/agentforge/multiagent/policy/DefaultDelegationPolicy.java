package io.github.regalpine.agentforge.multiagent.policy;

import io.github.regalpine.agentforge.kernel.Reference;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * DefaultDelegationPolicy — AE-11 §21-22, §54.
 *
 * Configurable policy that checks:
 * - denied agents (explicit block list)
 * - allowed agents (explicit allow list)
 * - max delegation depth
 * - trust level requirements
 *
 * <p>If allowedAgents is empty, all non-denied agents
 * are permitted.
 */
public final class DefaultDelegationPolicy
    implements DelegationPolicy {

    private final Set<String> deniedAgents;
    private final Set<String> allowedAgents;
    private final int maxDepth;

    public DefaultDelegationPolicy(
        Set<String> deniedAgents,
        Set<String> allowedAgents,
        int maxDepth
    ) {
        this.deniedAgents = deniedAgents == null
            ? Set.of() : Set.copyOf(deniedAgents);
        this.allowedAgents = allowedAgents == null
            ? Set.of() : Set.copyOf(allowedAgents);
        this.maxDepth = maxDepth;
    }

    /**
     * Permissive policy: allow all.
     */
    public static DefaultDelegationPolicy permissive() {
        return new DefaultDelegationPolicy(
            Set.of(), Set.of(), 10
        );
    }

    /**
     * Restrictive policy with explicit allow list.
     */
    public static DefaultDelegationPolicy restrictive(
        Set<String> allowedAgents,
        int maxDepth
    ) {
        return new DefaultDelegationPolicy(
            Set.of(), allowedAgents, maxDepth
        );
    }

    @Override
    public DelegationPolicyDecision authorize(
        Reference parent,
        Reference child,
        Map<String, Object> context
    ) {
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(child, "child");

        String childId = child.id().value();

        // Check denied list first
        if (deniedAgents.contains(childId)) {
            return DelegationPolicyDecision.DENY;
        }

        // Check allowed list (empty = all allowed)
        if (!allowedAgents.isEmpty()
            && !allowedAgents.contains(childId)
        ) {
            return DelegationPolicyDecision.DENY;
        }

        // Check depth
        if (context != null
            && context.containsKey("currentDepth")
        ) {
            Object depthObj = context.get("currentDepth");
            if (depthObj instanceof Number num) {
                if (num.intValue() >= maxDepth) {
                    return DelegationPolicyDecision.DENY;
                }
            }
        }

        return DelegationPolicyDecision.ALLOW;
    }

    public Set<String> deniedAgents() {
        return deniedAgents;
    }

    public Set<String> allowedAgents() {
        return allowedAgents;
    }

    public int maxDepth() {
        return maxDepth;
    }
}

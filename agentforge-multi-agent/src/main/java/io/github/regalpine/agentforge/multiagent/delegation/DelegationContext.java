package io.github.regalpine.agentforge.multiagent.delegation;


import io.github.regalpine.agentforge.multiagent.budget.DelegationBudget;
import io.github.regalpine.agentforge.multiagent.policy.DelegationPolicy;
import io.github.regalpine.agentforge.multiagent.projection.ContextProjection;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * DelegationContext — AE-11 §59.
 *
 * Context for a delegation from parent to child agent.
 * Carries execution tree identity, budget, policy,
 * projection, and the delegation stack for circular
 * detection.
 */
public record DelegationContext(

    String rootExecutionId,

    String parentExecutionId,

    String parentAgentId,

    int delegationDepth,

    DelegationBudget budget,

    DelegationPolicy policy,

    ContextProjection projection,

    List<String> delegationStack,

    Map<String, Object> parentContext,

    String idempotencyKey
) {

    public DelegationContext {
        Objects.requireNonNull(
            rootExecutionId, "rootExecutionId");
        Objects.requireNonNull(
            parentExecutionId, "parentExecutionId");

        delegationStack = delegationStack == null
            ? List.of()
            : List.copyOf(delegationStack);

        parentContext = parentContext == null
            ? Map.of()
            : Map.copyOf(parentContext);
    }

    /**
     * Create a root delegation context.
     */
    public static DelegationContext root(
        String rootExecutionId,
        String parentAgentId,
        DelegationBudget budget,
        DelegationPolicy policy,
        ContextProjection projection
    ) {
        return new DelegationContext(
            rootExecutionId,
            rootExecutionId,
            parentAgentId,
            0,
            budget,
            policy,
            projection,
            List.of(parentAgentId),
            Map.of(),
            null
        );
    }

    /**
     * Derive a child context for delegation.
     */
    public DelegationContext deriveChild(
        String childExecutionId,
        String childAgentId,
        DelegationBudget childBudget
    ) {
        var newStack = new java.util.ArrayList<>(
            delegationStack);
        newStack.add(childAgentId);

        String idemKey = rootExecutionId + "/"
            + parentExecutionId + "/"
            + childAgentId;

        return new DelegationContext(
            rootExecutionId,
            childExecutionId,
            childAgentId,
            delegationDepth + 1,
            childBudget,
            policy,
            projection,
            List.copyOf(newStack),
            parentContext,
            idemKey
        );
    }
}

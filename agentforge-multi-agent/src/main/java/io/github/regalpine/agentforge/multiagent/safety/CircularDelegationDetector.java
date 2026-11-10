package io.github.regalpine.agentforge.multiagent.safety;

import java.util.ArrayList;
import java.util.List;

/**
 * CircularDelegationDetector — AE-11 §24.
 *
 * Detects circular delegation patterns like:
 * A → B → A, or A → B → C → A.
 *
 * <p>Maintains a delegation stack per execution chain.
 * If the target agent already exists in the stack,
 * delegation is rejected.
 */
public final class CircularDelegationDetector {

    /**
     * Check if delegating to targetAgentId would create
     * a circular delegation.
     *
     * @param delegationStack current agent execution stack
     * @param targetAgentId   the agent to delegate to
     * @return safety result
     */
    public DelegationSafetyResult check(
        List<String> delegationStack,
        String targetAgentId
    ) {
        if (delegationStack == null
            || delegationStack.isEmpty()
        ) {
            return DelegationSafetyResult.ok(0, 0);
        }

        if (delegationStack.contains(targetAgentId)) {
            List<String> chain = new ArrayList<>(
                delegationStack);
            chain.add(targetAgentId);
            return DelegationSafetyResult.circularDetected(
                String.join(" → ", chain)
            );
        }

        return DelegationSafetyResult.ok(
            delegationStack.size(), 0
        );
    }
}

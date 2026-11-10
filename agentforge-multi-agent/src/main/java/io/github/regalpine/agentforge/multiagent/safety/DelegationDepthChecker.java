package io.github.regalpine.agentforge.multiagent.safety;

/**
 * DelegationDepthChecker — AE-11 §23.
 *
 * Checks that delegation depth does not exceed
 * the configured maximum. Default max depth: 5.
 *
 * <p>After reaching the limit, delegation is denied
 * with POLICY_DENIED or BUDGET_EXCEEDED.
 */
public final class DelegationDepthChecker {

    private final int maxDepth;

    public DelegationDepthChecker(int maxDepth) {
        if (maxDepth < 0) {
            throw new IllegalArgumentException(
                "maxDepth must be >= 0");
        }
        this.maxDepth = maxDepth;
    }

    public DelegationDepthChecker() {
        this(5);
    }

    /**
     * Check if the current depth is within limits.
     *
     * @param currentDepth current delegation depth
     * @return safety result
     */
    public DelegationSafetyResult check(int currentDepth) {
        if (currentDepth >= maxDepth) {
            return DelegationSafetyResult.depthExceeded(
                currentDepth, maxDepth
            );
        }
        return DelegationSafetyResult.ok(
            currentDepth, maxDepth
        );
    }

    public int maxDepth() {
        return maxDepth;
    }
}

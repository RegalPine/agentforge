package io.github.regalpine.agentforge.multiagent.safety;

/**
 * DelegationSafetyResult — AE-11 §23-24.
 *
 * Result of a delegation safety check (depth + circular).
 */
public record DelegationSafetyResult(

    boolean safe,

    String reason,

    int currentDepth,

    int maxDepth
) {

    public DelegationSafetyResult {
        if (reason == null) reason = "";
    }

    public static DelegationSafetyResult ok(
        int currentDepth,
        int maxDepth
    ) {
        return new DelegationSafetyResult(
            true, "", currentDepth, maxDepth
        );
    }

    public static DelegationSafetyResult depthExceeded(
        int currentDepth,
        int maxDepth
    ) {
        return new DelegationSafetyResult(
            false,
            "Delegation depth exceeded: "
                + currentDepth + " >= " + maxDepth,
            currentDepth,
            maxDepth
        );
    }

    public static DelegationSafetyResult circularDetected(
        String agentChain
    ) {
        return new DelegationSafetyResult(
            false,
            "Circular delegation detected: "
                + agentChain,
            -1, -1
        );
    }
}

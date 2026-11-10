package io.github.regalpine.agentforge.model;

import java.util.Objects;

/**
 * Policy for repairing non-conforming output.
 */
public record RepairPolicy(
    int maxRepairAttempts,
    RepairStrategy strategy
) {

    public RepairPolicy {
        if (maxRepairAttempts < 0) {
            throw new IllegalArgumentException("maxRepairAttempts cannot be negative");
        }
        Objects.requireNonNull(strategy, "strategy");
    }

    /**
     * Create a repair policy.
     *
     * @param maxRepairAttempts maximum number of repair attempts
     * @param strategy the repair strategy
     * @return a new RepairPolicy
     */
    public static RepairPolicy of(int maxRepairAttempts, RepairStrategy strategy) {
        return new RepairPolicy(maxRepairAttempts, strategy);
    }

    /**
     * Default repair policy with 3 attempts.
     *
     * @return a default RepairPolicy
     */
    public static RepairPolicy defaults() {
        return new RepairPolicy(3, RepairStrategy.RETRY_WITH_FEEDBACK);
    }
}

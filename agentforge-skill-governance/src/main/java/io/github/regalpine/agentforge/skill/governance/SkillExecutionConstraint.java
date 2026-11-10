package io.github.regalpine.agentforge.skill.governance;

import java.time.Duration;
import java.util.Objects;

/**
 * Skill 执行约束.
 * AF-SKILL-03 §8: SkillExecutionConstraint.
 * AF-AD-15 §8.2: Execution Constraint.
 *
 * <p>Defines runtime constraints for
 * Skill execution: timeout, budget,
 * retry policy, side effect allowance.
 *
 * <p>Immutable.
 */
public record SkillExecutionConstraint(
    Duration timeout,
    long maxRetries,
    long tokenBudget,
    long costBudgetMicros,
    boolean allowsSideEffects
) {

    public SkillExecutionConstraint {
        Objects.requireNonNull(timeout, "timeout");

        if (timeout.isNegative()
            || timeout.isZero()
        ) {
            throw new IllegalArgumentException(
                "timeout must be positive"
            );
        }

        if (maxRetries < 0) {
            throw new IllegalArgumentException(
                "maxRetries cannot be negative"
            );
        }

        if (tokenBudget < 0) {
            throw new IllegalArgumentException(
                "tokenBudget cannot be negative"
            );
        }

        if (costBudgetMicros < 0) {
            throw new IllegalArgumentException(
                "costBudgetMicros cannot "
                    + "be negative"
            );
        }
    }

    /**
     * Default constraints.
     */
    public static SkillExecutionConstraint defaults() {
        return new SkillExecutionConstraint(
            Duration.ofSeconds(30),
            0,
            10_000L,
            1_000_000L,
            false
        );
    }
}

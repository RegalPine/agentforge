package io.github.regalpine.agentforge.agent;

import io.github.regalpine.agentforge.kernel.Reference;

import java.time.Duration;
import java.util.Objects;

/**
 * Runtime metrics snapshot for an Agent.
 *
 * <p>Captures invocation count, failure
 * count, and latency percentiles (p50/p99).
 *
 * <p>Immutable record.
 */
public record AgentMetrics(
    Reference reference,
    long invocations,
    long failures,
    Duration p50,
    Duration p99
) {

    public AgentMetrics {
        Objects.requireNonNull(reference, "reference");
        if (invocations < 0) {
            throw new IllegalArgumentException(
                "invocations must be >= 0"
            );
        }
        if (failures < 0) {
            throw new IllegalArgumentException(
                "failures must be >= 0"
            );
        }
        p50 = p50 != null ? p50 : Duration.ZERO;
        p99 = p99 != null ? p99 : Duration.ZERO;
    }

    /**
     * Create a zero-value metrics snapshot.
     */
    public static AgentMetrics empty(
        Reference reference
    ) {
        return new AgentMetrics(
            reference, 0L, 0L,
            Duration.ZERO, Duration.ZERO
        );
    }

    /**
     * Compute the success rate (0.0 - 1.0).
     */
    public double successRate() {
        if (invocations == 0) return 1.0;
        return (double) (invocations - failures)
            / (double) invocations;
    }
}

package io.github.regalpine.agentforge.observability.slo;

import java.util.Map;
import java.util.Objects;

/**
 * SLO definition.
 * AF-ENHANCE-06 §11: defines a
 * service level objective.
 */
public record SloDefinition(
    String sloId,
    SloMetric metric,
    double threshold,
    SloOperator operator,
    String description
) {

    public SloDefinition {
        Objects.requireNonNull(sloId);
        Objects.requireNonNull(metric);
        Objects.requireNonNull(operator);
    }

    /**
     * Create availability SLO.
     */
    public static SloDefinition availability(
        String id,
        double minAvailability
    ) {
        return new SloDefinition(
            id, SloMetric.AVAILABILITY,
            minAvailability,
            SloOperator.GREATER_OR_EQUAL,
            "Availability >= "
            + (minAvailability * 100) + "%"
        );
    }

    /**
     * Create latency SLO.
     */
    public static SloDefinition latency(
        String id,
        double maxLatencyMs
    ) {
        return new SloDefinition(
            id, SloMetric.LATENCY_P99,
            maxLatencyMs,
            SloOperator.LESS_OR_EQUAL,
            "P99 latency <= "
            + maxLatencyMs + "ms"
        );
    }

    /**
     * Create error rate SLO.
     */
    public static SloDefinition errorRate(
        String id,
        double maxErrorRate
    ) {
        return new SloDefinition(
            id, SloMetric.ERROR_RATE,
            maxErrorRate,
            SloOperator.LESS_OR_EQUAL,
            "Error rate <= "
            + (maxErrorRate * 100) + "%"
        );
    }
}

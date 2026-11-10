package io.github.regalpine.agentforge.observability.slo;

import java.util.Map;
import java.util.Objects;

/**
 * SLO evaluation result.
 */
public record SloResult(
    String sloId,
    SloMetric metric,
    double actualValue,
    double threshold,
    boolean satisfied,
    String message
) {

    public SloResult {
        Objects.requireNonNull(sloId);
        Objects.requireNonNull(metric);
    }

    /**
     * Create a passing result.
     */
    public static SloResult pass(
        String sloId,
        SloMetric metric,
        double actual,
        double threshold
    ) {
        return new SloResult(
            sloId, metric,
            actual, threshold,
            true,
            "SLO satisfied: "
            + actual + " vs " + threshold
        );
    }

    /**
     * Create a violation result.
     */
    public static SloResult violation(
        String sloId,
        SloMetric metric,
        double actual,
        double threshold
    ) {
        return new SloResult(
            sloId, metric,
            actual, threshold,
            false,
            "SLO violated: "
            + actual + " vs " + threshold
        );
    }
}

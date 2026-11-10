package io.github.regalpine.agentforge.observability.slo;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SLO evaluator.
 * AF-ENHANCE-06 §11: evaluates SLOs
 * against current metrics.
 */
public final class SloEvaluator {

    private final List<SloDefinition> definitions =
        new CopyOnWriteArrayList<>();

    private final List<SloResult> lastResults =
        new CopyOnWriteArrayList<>();

    /**
     * Register an SLO definition.
     */
    public SloEvaluator addSlo(
        SloDefinition slo
    ) {
        definitions.add(
            Objects.requireNonNull(slo)
        );
        return this;
    }

    /**
     * Evaluate all SLOs against current
     * metric values.
     */
    public List<SloResult> evaluate(
        Map<SloMetric, Double> currentValues
    ) {
        List<SloResult> results = new ArrayList<>();

        for (SloDefinition slo : definitions) {
            Double actual = currentValues.get(
                slo.metric()
            );

            if (actual == null) {
                results.add(new SloResult(
                    slo.sloId(), slo.metric(),
                    0.0, slo.threshold(),
                    false,
                    "No data for "
                    + slo.metric()
                ));
                continue;
            }

            boolean satisfied = switch (
                slo.operator()
            ) {
                case GREATER_OR_EQUAL ->
                    actual >= slo.threshold();
                case LESS_OR_EQUAL ->
                    actual <= slo.threshold();
                case EQUAL ->
                    Math.abs(actual - slo.threshold())
                    < 0.0001;
            };

            if (satisfied) {
                results.add(SloResult.pass(
                    slo.sloId(), slo.metric(),
                    actual, slo.threshold()
                ));
            } else {
                results.add(SloResult.violation(
                    slo.sloId(), slo.metric(),
                    actual, slo.threshold()
                ));
            }
        }

        lastResults.clear();
        lastResults.addAll(results);
        return List.copyOf(results);
    }

    /**
     * Get last evaluation results.
     */
    public List<SloResult> lastResults() {
        return List.copyOf(lastResults);
    }

    /**
     * Check if all SLOs are satisfied.
     */
    public boolean allSatisfied() {
        return lastResults.stream()
            .allMatch(SloResult::satisfied);
    }

    /**
     * Get count of violations.
     */
    public long violationCount() {
        return lastResults.stream()
            .filter(r -> !r.satisfied())
            .count();
    }

    /**
     * Get registered SLO count.
     */
    public int sloCount() {
        return definitions.size();
    }
}

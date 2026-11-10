package io.github.regalpine.agentforge.evaluation.model;

import java.util.Objects;
import java.util.Optional;

/**
 * ExpectedOutcome — AE-12 §7.
 *
 * Defines what the evaluation expects. Supports five outcome modes:
 * EXACT, STRUCTURAL, SEMANTIC, BEHAVIORAL, POLICY, CUSTOM.
 */
public final class ExpectedOutcome {

    private final OutcomeMode mode;
    private final Object expectedValue;
    private final double threshold;

    public ExpectedOutcome(OutcomeMode mode, Object expectedValue, double threshold) {
        this.mode = Objects.requireNonNull(mode, "mode");
        this.expectedValue = expectedValue;
        this.threshold = threshold;
    }

    public static ExpectedOutcome exact(Object value) {
        return new ExpectedOutcome(OutcomeMode.EXACT, value, 1.0);
    }

    public static ExpectedOutcome structural(Object schema, double threshold) {
        return new ExpectedOutcome(OutcomeMode.STRUCTURAL, schema, threshold);
    }

    public static ExpectedOutcome semantic(Object reference, double threshold) {
        return new ExpectedOutcome(OutcomeMode.SEMANTIC, reference, threshold);
    }

    public static ExpectedOutcome behavioral(Object expectedBehavior) {
        return new ExpectedOutcome(OutcomeMode.BEHAVIORAL, expectedBehavior, 1.0);
    }

    public static ExpectedOutcome policy(PolicyEffect expectedEffect) {
        return new ExpectedOutcome(OutcomeMode.POLICY, expectedEffect, 1.0);
    }

    public OutcomeMode mode() {
        return mode;
    }

    public Optional<Object> expectedValue() {
        return Optional.ofNullable(expectedValue);
    }

    public double threshold() {
        return threshold;
    }

    @Override
    public String toString() {
        return "ExpectedOutcome{mode=" + mode + ", threshold=" + threshold + "}";
    }
}

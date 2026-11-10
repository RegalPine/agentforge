package io.github.regalpine.agentforge.evaluation.model;

import java.util.Objects;

/**
 * Score — AE-12 §16.
 *
 * A score from a single evaluation run. Scores are ephemeral — they belong
 * to one evaluation, not a permanent quality rating.
 */
public final class Score {

    private final double value;
    private final double threshold;
    private final boolean passed;

    public Score(double value, double threshold) {
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException("score value must be in [0, 1]: " + value);
        }
        this.value = value;
        this.threshold = threshold;
        this.passed = value >= threshold;
    }

    public static Score passing(double value) {
        return new Score(value, 0.0);
    }

    public double value() {
        return value;
    }

    public double threshold() {
        return threshold;
    }

    public boolean passed() {
        return passed;
    }

    @Override
    public String toString() {
        return "Score{value=" + value + ", threshold=" + threshold + ", passed=" + passed + "}";
    }
}

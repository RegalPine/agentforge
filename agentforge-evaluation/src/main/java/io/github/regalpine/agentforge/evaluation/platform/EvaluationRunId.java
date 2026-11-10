package io.github.regalpine.agentforge.evaluation.platform;

import java.util.Objects;

/**
 * Evaluation run identity.
 * AF-ENHANCE-12 §35: RunId.
 */
public record EvaluationRunId(
    String value
) {

    public EvaluationRunId {
        Objects.requireNonNull(
            value, "value"
        );
    }

    @Override
    public String toString() {
        return value;
    }
}

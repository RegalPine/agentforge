package io.github.regalpine.agentforge.evaluation.platform;

import java.util.Map;
import java.util.Objects;

/**
 * Evaluation evidence.
 * AF-ENHANCE-12 §40: Evidence.
 *
 * <p>Score + Evidence together
 * compose Evaluation Result.
 */
public record EvaluationEvidence(
    String type,
    String source,
    String reference,
    Object value
) {

    public EvaluationEvidence {
        Objects.requireNonNull(
            type, "type"
        );

        source = source == null
            ? "" : source;
        reference = reference == null
            ? "" : reference;
    }

    /**
     * Create evidence.
     */
    public static EvaluationEvidence of(
        String type,
        Object value
    ) {
        return new EvaluationEvidence(
            type, "", "", value
        );
    }
}

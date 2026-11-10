package io.github.regalpine.agentforge.evaluation.platform;

import java.util.Map;
import java.util.Objects;

/**
 * Evaluation variant.
 * AF-ENHANCE-12 §53: Variant.
 *
 * <p>Represents a version of an
 * agent, skill, model, or policy
 * being evaluated.
 */
public record EvaluationVariant(
    String variantId,
    String agentReference,
    String version,
    Map<String, Object> configuration
) {

    public EvaluationVariant {
        Objects.requireNonNull(
            variantId, "variantId"
        );
        Objects.requireNonNull(
            agentReference, "agentReference"
        );

        version = version == null
            ? "1.0.0" : version;
        configuration = configuration == null
            ? Map.of() : Map.copyOf(configuration);
    }

    /**
     * Create a variant.
     */
    public static EvaluationVariant of(
        String variantId,
        String agentReference
    ) {
        return new EvaluationVariant(
            variantId, agentReference,
            "1.0.0", Map.of()
        );
    }
}

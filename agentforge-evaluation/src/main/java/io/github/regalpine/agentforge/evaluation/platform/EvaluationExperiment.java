package io.github.regalpine.agentforge.evaluation.platform;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Evaluation experiment.
 * AF-ENHANCE-12 §51-52: Experiment.
 *
 * <p>A controlled evaluation
 * comparison experiment between
 * variants.
 */
public record EvaluationExperiment(
    String experimentId,
    String datasetId,
    List<EvaluationVariant> variants,
    List<String> evaluatorIds,
    EvaluationRunStatus status,
    List<EvaluationRun> runs,
    Map<String, Object> metadata
) {

    public EvaluationExperiment {
        Objects.requireNonNull(
            experimentId, "experimentId"
        );
        Objects.requireNonNull(
            datasetId, "datasetId"
        );

        variants = variants == null
            ? List.of() : List.copyOf(variants);
        evaluatorIds = evaluatorIds == null
            ? List.of() : List.copyOf(evaluatorIds);
        status = status == null
            ? EvaluationRunStatus.CREATED : status;
        runs = runs == null
            ? List.of() : List.copyOf(runs);
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create an experiment.
     */
    public static EvaluationExperiment of(
        String experimentId,
        String datasetId,
        List<EvaluationVariant> variants
    ) {
        return new EvaluationExperiment(
            experimentId, datasetId,
            variants, List.of(),
            EvaluationRunStatus.CREATED,
            List.of(), Map.of()
        );
    }
}

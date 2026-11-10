package io.github.regalpine.agentforge.evaluation.platform;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Evaluation run.
 * AF-ENHANCE-12 §34: EvaluationRun.
 *
 * <p>Represents a complete
 * evaluation execution with
 * dataset, agent, results.
 */
public record EvaluationRun(
    EvaluationRunId runId,
    String datasetId,
    String agentId,
    String agentVersion,
    EvaluationRunStatus status,
    EvaluationEnvironment environment,
    List<EvaluationCaseResult> results,
    Map<String, Object> metadata
) {

    public EvaluationRun {
        Objects.requireNonNull(
            runId, "runId"
        );
        Objects.requireNonNull(
            status, "status"
        );

        datasetId = datasetId == null
            ? "" : datasetId;
        agentId = agentId == null
            ? "" : agentId;
        agentVersion = agentVersion == null
            ? "" : agentVersion;
        environment = environment == null
            ? EvaluationEnvironment.defaults()
            : environment;
        results = results == null
            ? List.of() : List.copyOf(results);
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Check if the run passed.
     */
    public boolean isPassed() {
        return status == EvaluationRunStatus.COMPLETED;
    }

    /**
     * Check if the run is in a
     * terminal state.
     */
    public boolean isTerminal() {
        return status == EvaluationRunStatus.COMPLETED
            || status == EvaluationRunStatus.FAILED
            || status == EvaluationRunStatus.CANCELLED;
    }
}

package io.github.regalpine.agentforge.evaluation.platform;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Evaluation case result.
 * AF-ENHANCE-12 §38: EvaluationResult.
 */
public record EvaluationCaseResult(
    String caseId,
    String evaluatorId,
    EvaluationCaseStatus status,
    double score,
    List<EvaluationEvidence> evidence,
    Map<String, Object> metadata
) {

    public EvaluationCaseResult {
        Objects.requireNonNull(
            caseId, "caseId"
        );
        Objects.requireNonNull(
            status, "status"
        );

        evaluatorId = evaluatorId == null
            ? "" : evaluatorId;
        evidence = evidence == null
            ? List.of() : List.copyOf(evidence);
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }
}

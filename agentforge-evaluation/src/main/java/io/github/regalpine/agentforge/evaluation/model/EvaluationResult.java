package io.github.regalpine.agentforge.evaluation.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * EvaluationResult — AE-12 §15.
 *
 * The result of evaluating a single EvaluationCase.
 */
public final class EvaluationResult {

    private final String caseId;
    private final EvaluationStatus status;
    private final List<CriterionResult> criteria;
    private final Score score;
    private final List<EvaluationFinding> findings;

    public EvaluationResult(
            String caseId,
            EvaluationStatus status,
            List<CriterionResult> criteria,
            Score score,
            List<EvaluationFinding> findings) {
        this.caseId = Objects.requireNonNull(caseId, "caseId");
        this.status = Objects.requireNonNull(status, "status");
        this.criteria = List.copyOf(Objects.requireNonNull(criteria, "criteria"));
        this.score = Objects.requireNonNull(score, "score");
        this.findings = List.copyOf(Objects.requireNonNull(findings, "findings"));
    }

    public String caseId() {
        return caseId;
    }

    public EvaluationStatus status() {
        return status;
    }

    public List<CriterionResult> criteria() {
        return criteria;
    }

    public Score score() {
        return score;
    }

    public List<EvaluationFinding> findings() {
        return findings;
    }

    public Optional<CriterionResult> findCriterion(String criterionId) {
        return criteria.stream().filter(c -> c.criterionId().equals(criterionId)).findFirst();
    }

    @Override
    public String toString() {
        return "EvaluationResult{caseId='" + caseId + "', status=" + status + ", score=" + score + "}";
    }
}

package io.github.regalpine.agentforge.evaluation.model;

import java.util.Objects;

/**
 * CriterionResult — the result of evaluating a single criterion.
 */
public final class CriterionResult {

    private final String criterionId;
    private final CriterionStatus status;
    private final Score score;
    private final String detail;

    public CriterionResult(String criterionId, CriterionStatus status, Score score, String detail) {
        this.criterionId = Objects.requireNonNull(criterionId, "criterionId");
        this.status = Objects.requireNonNull(status, "status");
        this.score = Objects.requireNonNull(score, "score");
        this.detail = Objects.requireNonNull(detail, "detail");
    }

    public String criterionId() {
        return criterionId;
    }

    public CriterionStatus status() {
        return status;
    }

    public Score score() {
        return score;
    }

    public String detail() {
        return detail;
    }

    @Override
    public String toString() {
        return "CriterionResult{criterionId='" + criterionId + "', status=" + status + ", score=" + score + "}";
    }
}

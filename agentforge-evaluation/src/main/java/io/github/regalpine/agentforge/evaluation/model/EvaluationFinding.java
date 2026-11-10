package io.github.regalpine.agentforge.evaluation.model;

import java.util.Objects;

/**
 * EvaluationFinding — AE-12 §15.
 *
 * An observation produced during evaluation, with optional severity.
 */
public final class EvaluationFinding {

    private final String findingId;
    private final FindingSeverity severity;
    private final String message;
    private final String criterionId;

    public EvaluationFinding(String findingId, FindingSeverity severity, String message, String criterionId) {
        this.findingId = Objects.requireNonNull(findingId, "findingId");
        this.severity = Objects.requireNonNull(severity, "severity");
        this.message = Objects.requireNonNull(message, "message");
        this.criterionId = Objects.requireNonNull(criterionId, "criterionId");
    }

    public String findingId() {
        return findingId;
    }

    public FindingSeverity severity() {
        return severity;
    }

    public String message() {
        return message;
    }

    public String criterionId() {
        return criterionId;
    }

    @Override
    public String toString() {
        return "EvaluationFinding{id='" + findingId + "', severity=" + severity + ", message='" + message + "'}";
    }
}

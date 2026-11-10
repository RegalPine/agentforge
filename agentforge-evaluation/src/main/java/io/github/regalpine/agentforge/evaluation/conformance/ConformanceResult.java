package io.github.regalpine.agentforge.evaluation.conformance;

import java.util.Objects;
import java.util.Optional;

/**
 * ConformanceResult — the result of a single conformance test.
 */
public final class ConformanceResult {

    private final String caseId;
    private final ConformanceStatus status;
    private final String detail;

    public ConformanceResult(String caseId, ConformanceStatus status, String detail) {
        this.caseId = Objects.requireNonNull(caseId, "caseId");
        this.status = Objects.requireNonNull(status, "status");
        this.detail = Objects.requireNonNull(detail, "detail");
    }

    public static ConformanceResult pass(String caseId) {
        return new ConformanceResult(caseId, ConformanceStatus.PASS, "Conformance verified");
    }

    public static ConformanceResult fail(String caseId, String reason) {
        return new ConformanceResult(caseId, ConformanceStatus.FAIL, reason);
    }

    public String caseId() {
        return caseId;
    }

    public ConformanceStatus status() {
        return status;
    }

    public String detail() {
        return detail;
    }

    public enum ConformanceStatus {
        PASS,
        FAIL,
        SKIP
    }

    @Override
    public String toString() {
        return "ConformanceResult{caseId='" + caseId + "', status=" + status + "}";
    }
}

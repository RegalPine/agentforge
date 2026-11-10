package io.github.regalpine.agentforge.evaluation.report;

import java.util.List;
import java.util.Objects;

/**
 * RegressionReport — AE-12 §52.
 *
 * Compares two evaluation reports to detect regressions.
 */
public final class RegressionReport {

    private final EvaluationReport baseline;
    private final EvaluationReport current;
    private final List<RegressionFinding> findings;
    private final boolean hasRegression;

    public RegressionReport(
            EvaluationReport baseline,
            EvaluationReport current,
            List<RegressionFinding> findings,
            boolean hasRegression) {
        this.baseline = Objects.requireNonNull(baseline, "baseline");
        this.current = Objects.requireNonNull(current, "current");
        this.findings = List.copyOf(Objects.requireNonNull(findings, "findings"));
        this.hasRegression = hasRegression;
    }

    public EvaluationReport baseline() {
        return baseline;
    }

    public EvaluationReport current() {
        return current;
    }

    public List<RegressionFinding> findings() {
        return findings;
    }

    public boolean hasRegression() {
        return hasRegression;
    }

    public double scoreDelta() {
        return current.averageScore() - baseline.averageScore();
    }

    public record RegressionFinding(
            String caseId,
            double baselineScore,
            double currentScore,
            RegressionType type,
            String description) {}

    public enum RegressionType {
        SCORE_DECREASED,
        STATUS_REGRESSED,
        NEW_FAILURE,
        NEW_CASE_ADDED
    }

    @Override
    public String toString() {
        return "RegressionReport{baseline=" + baseline.reportId()
                + ", current=" + current.reportId()
                + ", hasRegression=" + hasRegression
                + ", delta=" + String.format("%+.3f", scoreDelta()) + "}";
    }
}

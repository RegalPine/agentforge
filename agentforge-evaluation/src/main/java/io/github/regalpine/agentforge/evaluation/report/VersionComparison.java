package io.github.regalpine.agentforge.evaluation.report;

import io.github.regalpine.agentforge.evaluation.model.EvaluationResult;
import io.github.regalpine.agentforge.evaluation.model.EvaluationStatus;

import java.util.*;

/**
 * VersionComparison — AE-12 §53.
 *
 * Compares evaluation results between two versions of an agent/graph.
 */
public final class VersionComparison {

    private final String entityName;
    private final String baselineVersion;
    private final String currentVersion;
    private final EvaluationReport baselineReport;
    private final EvaluationReport currentReport;
    private final List<VersionDelta> deltas;

    public VersionComparison(
            String entityName,
            String baselineVersion,
            String currentVersion,
            EvaluationReport baselineReport,
            EvaluationReport currentReport,
            List<VersionDelta> deltas) {
        this.entityName = Objects.requireNonNull(entityName, "entityName");
        this.baselineVersion = Objects.requireNonNull(baselineVersion, "baselineVersion");
        this.currentVersion = Objects.requireNonNull(currentVersion, "currentVersion");
        this.baselineReport = Objects.requireNonNull(baselineReport, "baselineReport");
        this.currentReport = Objects.requireNonNull(currentReport, "currentReport");
        this.deltas = List.copyOf(Objects.requireNonNull(deltas, "deltas"));
    }

    public String entityName() {
        return entityName;
    }

    public String baselineVersion() {
        return baselineVersion;
    }

    public String currentVersion() {
        return currentVersion;
    }

    public EvaluationReport baselineReport() {
        return baselineReport;
    }

    public EvaluationReport currentReport() {
        return currentReport;
    }

    public List<VersionDelta> deltas() {
        return deltas;
    }

    public boolean improved() {
        return currentReport.averageScore() > baselineReport.averageScore();
    }

    public boolean regressed() {
        return currentReport.averageScore() < baselineReport.averageScore();
    }

    public record VersionDelta(
            String caseId,
            double baselineScore,
            double currentScore,
            DeltaType type) {

        public double delta() {
            return currentScore - baselineScore;
        }
    }

    public enum DeltaType {
        IMPROVED,
        REGRESSED,
        UNCHANGED,
        NEW_CASE,
        REMOVED_CASE
    }

    @Override
    public String toString() {
        return "VersionComparison{entity='" + entityName
                + "', " + baselineVersion + " → " + currentVersion
                + ", improved=" + improved() + ", regressed=" + regressed() + "}";
    }
}

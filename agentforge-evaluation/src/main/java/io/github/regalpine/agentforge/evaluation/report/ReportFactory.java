package io.github.regalpine.agentforge.evaluation.report;

import io.github.regalpine.agentforge.evaluation.model.EvaluationResult;
import io.github.regalpine.agentforge.evaluation.model.EvaluationStatus;

import java.util.*;

/**
 * ReportFactory — creates reports from evaluation results.
 */
public final class ReportFactory {

    private ReportFactory() {}

    /**
     * Creates an EvaluationReport from results.
     */
    public static EvaluationReport createReport(
            String reportId,
            String datasetId,
            String datasetVersion,
            List<EvaluationResult> results,
            Map<String, String> environmentFingerprint) {
        return new EvaluationReport(
                reportId,
                java.time.Instant.now(),
                datasetId,
                datasetVersion,
                results,
                environmentFingerprint);
    }

    /**
     * Creates a RegressionReport by comparing baseline and current reports.
     */
    public static RegressionReport compareRegression(
            EvaluationReport baseline,
            EvaluationReport current,
            double regressionThreshold) {

        List<RegressionReport.RegressionFinding> findings = new ArrayList<>();
        boolean hasRegression = false;

        // Build a map of baseline results by caseId
        Map<String, EvaluationResult> baselineMap = new HashMap<>();
        for (EvaluationResult r : baseline.results()) {
            baselineMap.put(r.caseId(), r);
        }

        // Check each current result against baseline
        for (EvaluationResult currentResult : current.results()) {
            EvaluationResult baselineResult = baselineMap.get(currentResult.caseId());
            if (baselineResult == null) {
                findings.add(new RegressionReport.RegressionFinding(
                        currentResult.caseId(),
                        0.0,
                        currentResult.score().value(),
                        RegressionReport.RegressionType.NEW_CASE_ADDED,
                        "New case added in current run"));
                continue;
            }

            double delta = currentResult.score().value() - baselineResult.score().value();
            if (currentResult.status() == EvaluationStatus.FAIL && baselineResult.status() == EvaluationStatus.PASS) {
                findings.add(new RegressionReport.RegressionFinding(
                        currentResult.caseId(),
                        baselineResult.score().value(),
                        currentResult.score().value(),
                        RegressionReport.RegressionType.NEW_FAILURE,
                        "Case regressed from PASS to FAIL"));
                hasRegression = true;
            } else if (delta < -regressionThreshold) {
                findings.add(new RegressionReport.RegressionFinding(
                        currentResult.caseId(),
                        baselineResult.score().value(),
                        currentResult.score().value(),
                        RegressionReport.RegressionType.SCORE_DECREASED,
                        "Score decreased by " + String.format("%.3f", -delta)));
                hasRegression = true;
            }
        }

        return new RegressionReport(baseline, current, findings, hasRegression);
    }

    /**
     * Creates a VersionComparison between two versions.
     */
    public static VersionComparison compareVersions(
            String entityName,
            String baselineVersion,
            String currentVersion,
            EvaluationReport baselineReport,
            EvaluationReport currentReport) {

        List<VersionComparison.VersionDelta> deltas = new ArrayList<>();

        Map<String, EvaluationResult> baselineMap = new HashMap<>();
        for (EvaluationResult r : baselineReport.results()) {
            baselineMap.put(r.caseId(), r);
        }
        Map<String, EvaluationResult> currentMap = new HashMap<>();
        for (EvaluationResult r : currentReport.results()) {
            currentMap.put(r.caseId(), r);
        }

        // Cases in both versions
        for (EvaluationResult currentResult : currentReport.results()) {
            EvaluationResult baselineResult = baselineMap.get(currentResult.caseId());
            if (baselineResult != null) {
                double delta = currentResult.score().value() - baselineResult.score().value();
                VersionComparison.DeltaType type;
                if (Math.abs(delta) < 0.001) {
                    type = VersionComparison.DeltaType.UNCHANGED;
                } else if (delta > 0) {
                    type = VersionComparison.DeltaType.IMPROVED;
                } else {
                    type = VersionComparison.DeltaType.REGRESSED;
                }
                deltas.add(new VersionComparison.VersionDelta(
                        currentResult.caseId(),
                        baselineResult.score().value(),
                        currentResult.score().value(),
                        type));
            } else {
                deltas.add(new VersionComparison.VersionDelta(
                        currentResult.caseId(),
                        0.0,
                        currentResult.score().value(),
                        VersionComparison.DeltaType.NEW_CASE));
            }
        }

        // Cases only in baseline (removed)
        for (EvaluationResult baselineResult : baselineReport.results()) {
            if (!currentMap.containsKey(baselineResult.caseId())) {
                deltas.add(new VersionComparison.VersionDelta(
                        baselineResult.caseId(),
                        baselineResult.score().value(),
                        0.0,
                        VersionComparison.DeltaType.REMOVED_CASE));
            }
        }

        return new VersionComparison(
                entityName, baselineVersion, currentVersion,
                baselineReport, currentReport, deltas);
    }
}

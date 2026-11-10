package io.github.regalpine.agentforge.evaluation.report;

import io.github.regalpine.agentforge.evaluation.model.EvaluationResult;

import java.time.Instant;
import java.util.*;

/**
 * EvaluationReport — AE-12 §51.
 *
 * Aggregates results from multiple evaluation cases into a report.
 */
public final class EvaluationReport {

    private final String reportId;
    private final Instant timestamp;
    private final String datasetId;
    private final String datasetVersion;
    private final List<EvaluationResult> results;
    private final Map<String, String> environmentFingerprint;

    public EvaluationReport(
            String reportId,
            Instant timestamp,
            String datasetId,
            String datasetVersion,
            List<EvaluationResult> results,
            Map<String, String> environmentFingerprint) {
        this.reportId = Objects.requireNonNull(reportId, "reportId");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
        this.datasetId = Objects.requireNonNull(datasetId, "datasetId");
        this.datasetVersion = Objects.requireNonNull(datasetVersion, "datasetVersion");
        this.results = List.copyOf(Objects.requireNonNull(results, "results"));
        this.environmentFingerprint = Map.copyOf(Objects.requireNonNull(environmentFingerprint, "environmentFingerprint"));
    }

    public String reportId() {
        return reportId;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public String datasetId() {
        return datasetId;
    }

    public String datasetVersion() {
        return datasetVersion;
    }

    public List<EvaluationResult> results() {
        return results;
    }

    public Map<String, String> environmentFingerprint() {
        return environmentFingerprint;
    }

    public int totalCases() {
        return results.size();
    }

    public long passedCases() {
        return results.stream().filter(r -> r.status() == io.github.regalpine.agentforge.evaluation.model.EvaluationStatus.PASS).count();
    }

    public long failedCases() {
        return results.stream().filter(r -> r.status() == io.github.regalpine.agentforge.evaluation.model.EvaluationStatus.FAIL).count();
    }

    public long errorCases() {
        return results.stream().filter(r -> r.status() == io.github.regalpine.agentforge.evaluation.model.EvaluationStatus.ERROR).count();
    }

    public double averageScore() {
        if (results.isEmpty()) return 0.0;
        return results.stream().mapToDouble(r -> r.score().value()).average().orElse(0.0);
    }

    public boolean allPassed() {
        return results.stream().allMatch(r -> r.status() == io.github.regalpine.agentforge.evaluation.model.EvaluationStatus.PASS);
    }

    @Override
    public String toString() {
        return "EvaluationReport{id='" + reportId + "', total=" + totalCases()
                + ", passed=" + passedCases() + ", failed=" + failedCases()
                + ", avgScore=" + String.format("%.3f", averageScore()) + "}";
    }
}

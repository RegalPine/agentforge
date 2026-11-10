package io.github.regalpine.agentforge.evaluation.conformance;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ConformanceReport — AE-12 §59-60.
 *
 * Aggregates results from all conformance test cases.
 */
public final class ConformanceReport {

    private final String reportId;
    private final Instant timestamp;
    private final List<ConformanceResult> results;
    private final Map<String, String> environmentFingerprint;

    public ConformanceReport(
            String reportId,
            Instant timestamp,
            List<ConformanceResult> results,
            Map<String, String> environmentFingerprint) {
        this.reportId = Objects.requireNonNull(reportId, "reportId");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
        this.results = List.copyOf(Objects.requireNonNull(results, "results"));
        this.environmentFingerprint = Map.copyOf(Objects.requireNonNull(environmentFingerprint, "environmentFingerprint"));
    }

    public String reportId() {
        return reportId;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public List<ConformanceResult> results() {
        return results;
    }

    public Map<String, String> environmentFingerprint() {
        return environmentFingerprint;
    }

    public int totalCases() {
        return results.size();
    }

    public long passedCases() {
        return results.stream().filter(r -> r.status() == ConformanceResult.ConformanceStatus.PASS).count();
    }

    public long failedCases() {
        return results.stream().filter(r -> r.status() == ConformanceResult.ConformanceStatus.FAIL).count();
    }

    public boolean isConformant() {
        return results.stream().allMatch(r -> r.status() == ConformanceResult.ConformanceStatus.PASS);
    }

    @Override
    public String toString() {
        return "ConformanceReport{id='" + reportId + "', total=" + totalCases()
                + ", passed=" + passedCases() + ", failed=" + failedCases()
                + ", conformant=" + isConformant() + "}";
    }
}

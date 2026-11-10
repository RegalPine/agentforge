package io.github.regalpine.agentforge.benchmark;

import java.time.Instant;
import java.util.*;

/**
 * Benchmark report aggregating multiple
 * results with environment fingerprint.
 * AF-ENHANCE-07 §4: complete report.
 */
public record BenchmarkReport(
    String reportId,
    Instant timestamp,
    EnvironmentFingerprint fingerprint,
    List<BenchmarkResult> results,
    Map<String, String> metadata
) {

    public BenchmarkReport {
        Objects.requireNonNull(reportId);
        Objects.requireNonNull(fingerprint);
        timestamp = timestamp == null
            ? Instant.now() : timestamp;
        results = results == null
            ? List.of() : List.copyOf(results);
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Results by category.
     */
    public Map<String, List<BenchmarkResult>> byCategory() {
        Map<String, List<BenchmarkResult>> map =
            new LinkedHashMap<>();
        for (BenchmarkResult r : results) {
            map.computeIfAbsent(
                r.category(), k -> new ArrayList<>()
            ).add(r);
        }
        return Map.copyOf(map);
    }

    /**
     * Total result count.
     */
    public int size() {
        return results.size();
    }

    /**
     * Find a result by ID.
     */
    public Optional<BenchmarkResult> find(String id) {
        return results.stream()
            .filter(r -> r.benchmarkId().equals(id))
            .findFirst();
    }

    /**
     * Format as multi-line report.
     */
    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Benchmark Report: ")
          .append(reportId).append(" ===\n");
        sb.append(fingerprint.report());
        sb.append("\n--- Results (")
          .append(results.size()).append(") ---\n");
        for (BenchmarkResult r : results) {
            sb.append(r.summary()).append('\n');
        }
        return sb.toString();
    }
}

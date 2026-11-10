package io.github.regalpine.agentforge.hardening;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TCK conformance report.
 * AF-RELEASE-02 §8: aggregates all
 * test cases across domains and levels.
 */
public record TckReport(
    String reportId,
    Instant timestamp,
    List<TckCase> cases,
    Map<String, String> metadata
) {

    public TckReport {
        Objects.requireNonNull(reportId);
        timestamp = timestamp == null
            ? Instant.now() : timestamp;
        cases = cases == null
            ? List.of() : List.copyOf(cases);
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Cases by domain.
     */
    public Map<String, List<TckCase>> byDomain() {
        return cases.stream().collect(
            Collectors.groupingBy(
                TckCase::domain,
                LinkedHashMap::new,
                Collectors.toList()
            )
        );
    }

    /**
     * Cases by level.
     */
    public Map<TestLevel, List<TckCase>> byLevel() {
        return cases.stream().collect(
            Collectors.groupingBy(
                TckCase::level,
                LinkedHashMap::new,
                Collectors.toList()
            )
        );
    }

    /**
     * Cases by result.
     */
    public Map<TckResult, List<TckCase>> byResult() {
        return cases.stream().collect(
            Collectors.groupingBy(
                TckCase::result,
                LinkedHashMap::new,
                Collectors.toList()
            )
        );
    }

    /**
     * Total case count.
     */
    public int totalCases() {
        return cases.size();
    }

    /**
     * Count of passed cases.
     */
    public int passCount() {
        return (int) cases.stream()
            .filter(TckCase::isPassing)
            .count();
    }

    /**
     * Count of failed cases.
     */
    public int failCount() {
        return (int) cases.stream()
            .filter(c -> c.result() == TckResult.FAIL)
            .count();
    }

    /**
     * Count of blocked cases.
     */
    public int blockedCount() {
        return (int) cases.stream()
            .filter(c -> c.result() == TckResult.BLOCKED)
            .count();
    }

    /**
     * Pass rate (0.0 to 1.0).
     */
    public double passRate() {
        if (cases.isEmpty()) return 0.0;
        return (double) passCount() / cases.size();
    }

    /**
     * Whether all cases passed.
     */
    public boolean allPassed() {
        return failCount() == 0
            && blockedCount() == 0;
    }

    /**
     * Format as a summary string.
     */
    public String summary() {
        var sb = new StringBuilder();
        sb.append("=== TCK Report: ")
            .append(reportId).append(" ===\n");
        sb.append("Total: ").append(totalCases())
            .append(" | PASS: ").append(passCount())
            .append(" | FAIL: ").append(failCount())
            .append(" | BLOCKED: ").append(blockedCount())
            .append("\n");
        sb.append(String.format(
            "Pass Rate: %.1f%%\n",
            passRate() * 100
        ));

        for (var entry : byDomain().entrySet()) {
            long domPass = entry.getValue().stream()
                .filter(TckCase::isPassing).count();
            sb.append("  ").append(entry.getKey())
                .append(": ").append(domPass)
                .append("/").append(entry.getValue().size())
                .append("\n");
        }
        return sb.toString();
    }
}

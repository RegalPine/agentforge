package io.github.regalpine.agentforge.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BE-09: Performance regression gate.
 *
 * <p>Defines latency/throughput thresholds for core paths.
 * When integrated with CI, benchmark results exceeding thresholds
 * cause the build to fail.
 *
 * <p>Usage in benchmark tests:
 * <pre>{@code
 * PerformanceGate gate = PerformanceGate.defaults();
 * gate.assertThreshold("graph-compile", result, 50_000_000); // P99 < 50ms
 * gate.assertAllPassed(); // throws if any threshold exceeded
 * }</pre>
 */
public final class PerformanceGate {

    /** Threshold entry: benchmarkId → max P99 nanos. */
    private final Map<String, Long> thresholds = new ConcurrentHashMap<>();
    private final List<ThresholdViolation> violations = new ArrayList<>();

    /**
     * Register a P99 latency threshold (in nanoseconds) for a benchmark.
     */
    public PerformanceGate threshold(String benchmarkId, long maxP99Nanos) {
        thresholds.put(benchmarkId, maxP99Nanos);
        return this;
    }

    /**
     * Assert that a benchmark result meets the registered threshold.
     * If no threshold is registered for the benchmarkId, the check is skipped.
     */
    public PerformanceGate assertThreshold(String benchmarkId, BenchmarkResult result) {
        Long maxP99 = thresholds.get(benchmarkId);
        if (maxP99 == null) {
            return this; // no threshold registered
        }
        if (result.p99Nanos() > maxP99) {
            violations.add(new ThresholdViolation(
                benchmarkId,
                maxP99,
                (long) result.p99Nanos(),
                result.category()
            ));
        }
        return this;
    }

    /**
     * Assert with explicit threshold value (convenience).
     */
    public PerformanceGate assertThreshold(String benchmarkId, BenchmarkResult result, long maxP99Nanos) {
        threshold(benchmarkId, maxP99Nanos);
        return assertThreshold(benchmarkId, result);
    }

    /**
     * Get all violations found so far.
     */
    public List<ThresholdViolation> getViolations() {
        return List.copyOf(violations);
    }

    /**
     * Assert that all thresholds passed. Throws if any violations exist.
     */
    public void assertAllPassed() {
        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder("Performance regression detected!\n");
            for (ThresholdViolation v : violations) {
                sb.append("  - ").append(v.benchmarkId)
                    .append(" (").append(v.category).append("): P99=")
                    .append(v.actualP99Nanos / 1_000_000).append("ms > threshold=")
                    .append(v.maxP99Nanos / 1_000_000).append("ms\n");
            }
            throw new PerformanceRegressionException(sb.toString(), violations);
        }
    }

    /**
     * Default thresholds for core paths.
     */
    public static PerformanceGate defaults() {
        return new PerformanceGate()
            // Graph compilation P99 < 50ms
            .threshold("graph-compile", 50_000_000L)
            // Agent loop decision P99 < 5ms
            .threshold("agent-loop-decision", 5_000_000L)
            // LLM call round-trip P99 < 2000ms
            .threshold("llm-call", 2_000_000_000L)
            // Execution start P99 < 100ms
            .threshold("execution-start", 100_000_000L);
    }

    /**
     * A single threshold violation.
     */
    public record ThresholdViolation(
        String benchmarkId,
        long maxP99Nanos,
        long actualP99Nanos,
        String category
    ) {
    }

    /**
     * Exception thrown when performance regression is detected.
     */
    public static class PerformanceRegressionException extends RuntimeException {
        private final List<ThresholdViolation> violations;

        public PerformanceRegressionException(String message, List<ThresholdViolation> violations) {
            super(message);
            this.violations = violations;
        }

        public List<ThresholdViolation> violations() {
            return violations;
        }
    }
}

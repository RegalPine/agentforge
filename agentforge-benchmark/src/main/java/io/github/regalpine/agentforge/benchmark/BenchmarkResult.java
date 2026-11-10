package io.github.regalpine.agentforge.benchmark;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Benchmark result for a single run.
 * AF-ENHANCE-07 §4: captures throughput,
 * latency, and resource metrics.
 */
public record BenchmarkResult(
    String benchmarkId,
    String category,
    Instant timestamp,
    long iterations,
    long totalNanos,
    long minNanos,
    long maxNanos,
    double avgNanos,
    double p50Nanos,
    double p95Nanos,
    double p99Nanos,
    double throughputPerSec,
    Map<String, String> metadata
) {

    public BenchmarkResult {
        Objects.requireNonNull(benchmarkId);
        Objects.requireNonNull(category);
        timestamp = timestamp == null
            ? Instant.now() : timestamp;
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Total duration in milliseconds.
     */
    public long totalMs() {
        return totalNanos / 1_000_000;
    }

    /**
     * Average latency in microseconds.
     */
    public double avgMicros() {
        return avgNanos / 1000.0;
    }

    /**
     * P95 latency in microseconds.
     */
    public double p95Micros() {
        return p95Nanos / 1000.0;
    }

    /**
     * Format a summary line.
     */
    public String summary() {
        return String.format(
            "[%s] %s: %d iter, %d ms, "
            + "avg=%.1f µs, p50=%.1f µs, "
            + "p95=%.1f µs, p99=%.1f µs, "
            + "throughput=%.0f ops/s",
            category, benchmarkId,
            iterations, totalMs(),
            avgMicros(), p50Nanos / 1000.0,
            p95Micros(), p99Nanos / 1000.0,
            throughputPerSec
        );
    }
}

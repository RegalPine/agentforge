package io.github.regalpine.agentforge.benchmark;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.IntFunction;

/**
 * Unified benchmark runner.
 * AF-ENHANCE-07 §4: runs benchmarks,
 * collects results, computes latency
 * percentiles, and generates reports.
 */
public final class BenchmarkRunner {

    private final String reportId;
    private final EnvironmentFingerprint fingerprint;
    private final List<BenchmarkResult> results =
        new CopyOnWriteArrayList<>();
    private final Map<String, String> metadata =
        new LinkedHashMap<>();

    /**
     * Create a runner with auto-captured
     * environment fingerprint.
     */
    public BenchmarkRunner(String reportId) {
        this.reportId = Objects.requireNonNull(reportId);
        this.fingerprint = EnvironmentFingerprint.capture();
    }

    /**
     * Create with a custom fingerprint.
     */
    public BenchmarkRunner(
        String reportId,
        EnvironmentFingerprint fingerprint
    ) {
        this.reportId = Objects.requireNonNull(reportId);
        this.fingerprint =
            Objects.requireNonNull(fingerprint);
    }

    /**
     * Add metadata to the report.
     */
    public BenchmarkRunner metadata(
        String key, String value
    ) {
        metadata.put(key, value);
        return this;
    }

    /**
     * Run a benchmark and collect results.
     *
     * @param id         benchmark identifier
     * @param category   benchmark category
     * @param iterations number of iterations
     * @param action     the action to benchmark
     * @return the computed result
     */
    public BenchmarkResult run(
        String id,
        String category,
        int iterations,
        Runnable action
    ) {
        long[] samples = new long[iterations];
        long totalStart = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            action.run();
            samples[i] = System.nanoTime() - start;
        }

        long totalNanos = System.nanoTime() - totalStart;
        return computeResult(
            id, category, iterations,
            totalNanos, samples, Map.of()
        );
    }

    /**
     * Run a benchmark with metadata.
     */
    public BenchmarkResult run(
        String id,
        String category,
        int iterations,
        Runnable action,
        Map<String, String> meta
    ) {
        long[] samples = new long[iterations];
        long totalStart = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            action.run();
            samples[i] = System.nanoTime() - start;
        }

        long totalNanos = System.nanoTime() - totalStart;
        return computeResult(
            id, category, iterations,
            totalNanos, samples, meta
        );
    }

    /**
     * Run a parameterised benchmark.
     */
    public BenchmarkResult runParam(
        String id,
        String category,
        int iterations,
        IntFunction<?> action
    ) {
        long[] samples = new long[iterations];
        long totalStart = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            action.apply(i);
            samples[i] = System.nanoTime() - start;
        }

        long totalNanos = System.nanoTime() - totalStart;
        return computeResult(
            id, category, iterations,
            totalNanos, samples, Map.of()
        );
    }

    /**
     * Add a pre-computed result.
     */
    public BenchmarkRunner addResult(
        BenchmarkResult result
    ) {
        results.add(
            Objects.requireNonNull(result)
        );
        return this;
    }

    /**
     * Generate the report.
     */
    public BenchmarkReport report() {
        return new BenchmarkReport(
            reportId,
            null,
            fingerprint,
            List.copyOf(results),
            Map.copyOf(metadata)
        );
    }

    /**
     * Get all collected results.
     */
    public List<BenchmarkResult> results() {
        return List.copyOf(results);
    }

    /**
     * Get the environment fingerprint.
     */
    public EnvironmentFingerprint fingerprint() {
        return fingerprint;
    }

    /* --- internal --- */

    private BenchmarkResult computeResult(
        String id,
        String category,
        int iterations,
        long totalNanos,
        long[] samples,
        Map<String, String> meta
    ) {
        Arrays.sort(samples);

        long min = samples[0];
        long max = samples[samples.length - 1];
        double avg = average(samples);
        double p50 = percentile(samples, 50);
        double p95 = percentile(samples, 95);
        double p99 = percentile(samples, 99);
        double tps = iterations * 1_000_000_000.0
            / totalNanos;

        BenchmarkResult result = new BenchmarkResult(
            id, category, null,
            iterations, totalNanos,
            min, max, avg,
            p50, p95, p99,
            tps, meta
        );

        results.add(result);
        return result;
    }

    private static double average(long[] arr) {
        long sum = 0;
        for (long v : arr) {
            sum += v;
        }
        return (double) sum / arr.length;
    }

    private static double percentile(
        long[] sorted, int pct
    ) {
        if (sorted.length == 1) {
            return sorted[0];
        }
        double rank = (pct / 100.0)
            * (sorted.length - 1);
        int lower = (int) Math.floor(rank);
        int upper = Math.min(
            lower + 1, sorted.length - 1
        );
        double frac = rank - lower;
        return sorted[lower]
            + frac * (sorted[upper] - sorted[lower]);
    }
}

package io.github.regalpine.agentforge.benchmark;

import java.util.*;

/**
 * Checkpoint frequency strategy analyser.
 * AF-ENHANCE-07 §13: compares four
 * checkpoint strategies across durability,
 * latency, storage and recovery time.
 *
 * <p>Strategies:
 * <ul>
 *   <li>EVERY_NODE — checkpoint after each node</li>
 *   <li>EVERY_N_NODES — checkpoint every N nodes</li>
 *   <li>CRITICAL_ONLY — only at critical nodes</li>
 *   <li>PERIODIC — every T milliseconds</li>
 * </ul>
 */
public final class CheckpointStrategyAnalyzer {

    private CheckpointStrategyAnalyzer() {}

    /**
     * Strategy type.
     */
    public enum Strategy {
        EVERY_NODE,
        EVERY_N_NODES,
        CRITICAL_ONLY,
        PERIODIC
    }

    /**
     * Analysis result for one strategy.
     */
    public record StrategyResult(
        Strategy strategy,
        int nodeCount,
        int checkpointCount,
        double durabilityScore,
        double latencyOverheadMs,
        long storageBytes,
        double estimatedRecoveryMs
    ) {
        /**
         * Overall efficiency score
         * (higher = better).
         */
        public double efficiencyScore() {
            return durabilityScore
                / (1.0 + latencyOverheadMs)
                / (1.0 + storageBytes / 1024.0);
        }
    }

    /**
     * Analyse all four strategies for a
     * graph with the given node count.
     *
     * @param nodeCount       total nodes
     * @param criticalNodes   indices of
     *                        critical nodes
     * @param avgNodeMs       average node
     *                        execution ms
     * @param checkpointSize  bytes per
     *                        checkpoint
     * @param intervalN       interval for
     *                        EVERY_N_NODES
     * @param periodMs        period for
     *                        PERIODIC
     * @return results for all strategies
     */
    public static List<StrategyResult> analyse(
        int nodeCount,
        Set<Integer> criticalNodes,
        double avgNodeMs,
        long checkpointSize,
        int intervalN,
        long periodMs
    ) {
        List<StrategyResult> results =
            new ArrayList<>();

        // EVERY_NODE
        int cpEvery = nodeCount;
        results.add(new StrategyResult(
            Strategy.EVERY_NODE, nodeCount,
            cpEvery,
            1.0,
            cpEvery * 0.5,
            cpEvery * checkpointSize,
            avgNodeMs
        ));

        // EVERY_N_NODES
        int n = Math.max(1, intervalN);
        int cpN = Math.max(1, nodeCount / n);
        results.add(new StrategyResult(
            Strategy.EVERY_N_NODES, nodeCount,
            cpN,
            1.0 / n,
            cpN * 0.5,
            cpN * checkpointSize,
            avgNodeMs * n / 2.0
        ));

        // CRITICAL_ONLY
        int cpCrit = Math.max(
            1, criticalNodes.size()
        );
        results.add(new StrategyResult(
            Strategy.CRITICAL_ONLY, nodeCount,
            cpCrit,
            (double) cpCrit / nodeCount,
            cpCrit * 0.5,
            cpCrit * checkpointSize,
            avgNodeMs * nodeCount / 2.0
        ));

        // PERIODIC
        long totalMs = (long) (nodeCount * avgNodeMs);
        int cpPeriodic = Math.max(
            1, (int) (totalMs / Math.max(1, periodMs))
        );
        results.add(new StrategyResult(
            Strategy.PERIODIC, nodeCount,
            cpPeriodic,
            (double) cpPeriodic / nodeCount,
            cpPeriodic * 0.5,
            cpPeriodic * checkpointSize,
            periodMs / 2.0
        ));

        return results;
    }

    /**
     * Select the best strategy by
     * efficiency score.
     */
    public static StrategyResult best(
        List<StrategyResult> results
    ) {
        return results.stream()
            .max(Comparator.comparingDouble(
                StrategyResult::efficiencyScore
            ))
            .orElseThrow();
    }

    /**
     * Format analysis as a report.
     */
    public static String report(
        List<StrategyResult> results
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Checkpoint Strategy Analysis ===\n");
        sb.append(String.format(
            "%-18s %5s %5s %8s %10s %10s %10s%n",
            "Strategy", "Nodes", "CPs",
            "Durabil.", "Latency", "Storage", "Recovery"
        ));
        for (StrategyResult r : results) {
            sb.append(String.format(
                "%-18s %5d %5d %8.2f %8.1fms %8d B %8.1fms%n",
                r.strategy(), r.nodeCount(),
                r.checkpointCount(),
                r.durabilityScore(),
                r.latencyOverheadMs(),
                r.storageBytes(),
                r.estimatedRecoveryMs()
            ));
        }
        StrategyResult best = best(results);
        sb.append("Best: ").append(best.strategy())
          .append(" (efficiency=")
          .append(String.format("%.4f",
              best.efficiencyScore()))
          .append(")\n");
        return sb.toString();
    }
}

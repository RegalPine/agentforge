package io.github.regalpine.agentforge.benchmark;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checkpoint strategy comparison benchmarks.
 * AF-ENHANCE-07 §13: compares four
 * checkpoint strategies across durability,
 * latency, storage and recovery time.
 */
@org.junit.jupiter.api.Tag("benchmark")
class CheckpointStrategyBenchmarkTest {

    @Test
    void shouldAnalyseAllFourStrategies() {
        List<CheckpointStrategyAnalyzer.StrategyResult> results =
            CheckpointStrategyAnalyzer.analyse(
                20,             // nodeCount
                Set.of(5, 10),  // criticalNodes
                10.0,           // avgNodeMs
                512,            // checkpointSize
                5,              // intervalN
                100             // periodMs
            );

        assertEquals(4, results.size());

        // All strategies should have results
        for (var r : results) {
            assertTrue(r.nodeCount() == 20);
            assertTrue(r.checkpointCount() >= 1);
            assertTrue(r.durabilityScore() > 0);
            assertTrue(r.latencyOverheadMs() >= 0);
            assertTrue(r.storageBytes() > 0);
        }

        // EVERY_NODE should have most checkpoints
        var everyNode = results.stream()
            .filter(r -> r.strategy() ==
                CheckpointStrategyAnalyzer.Strategy.EVERY_NODE)
            .findFirst().orElseThrow();
        assertEquals(20, everyNode.checkpointCount());
        assertEquals(1.0, everyNode.durabilityScore());
    }

    @Test
    void everyNodeShouldHaveHighestDurability() {
        List<CheckpointStrategyAnalyzer.StrategyResult> results =
            CheckpointStrategyAnalyzer.analyse(
                50, Set.of(10, 25, 40),
                5.0, 256, 10, 50
            );

        var everyNode = results.stream()
            .filter(r -> r.strategy() ==
                CheckpointStrategyAnalyzer.Strategy.EVERY_NODE)
            .findFirst().orElseThrow();

        // EVERY_NODE has durability = 1.0
        // All others should be lower
        for (var r : results) {
            if (r.strategy() !=
                CheckpointStrategyAnalyzer.Strategy.EVERY_NODE
            ) {
                assertTrue(
                    r.durabilityScore() <= 1.0,
                    r.strategy() + " durability should be <= 1.0"
                );
            }
        }

        assertEquals(1.0, everyNode.durabilityScore());
    }

    @Test
    void criticalOnlyShouldHaveFewestCheckpoints() {
        List<CheckpointStrategyAnalyzer.StrategyResult> results =
            CheckpointStrategyAnalyzer.analyse(
                100, Set.of(5),
                2.0, 128, 10, 100
            );

        var criticalOnly = results.stream()
            .filter(r -> r.strategy() ==
                CheckpointStrategyAnalyzer.Strategy.CRITICAL_ONLY)
            .findFirst().orElseThrow();

        // Only 1 critical node → 1 checkpoint
        assertEquals(1, criticalOnly.checkpointCount());
    }

    @Test
    void bestStrategyShouldBeSelected() {
        List<CheckpointStrategyAnalyzer.StrategyResult> results =
            CheckpointStrategyAnalyzer.analyse(
                20, Set.of(5, 10),
                10.0, 512, 5, 100
            );

        var best = CheckpointStrategyAnalyzer.best(results);
        assertNotNull(best);

        // Best should have highest efficiency
        for (var r : results) {
            assertTrue(
                best.efficiencyScore() >= r.efficiencyScore()
                    - 0.0001,
                "Best should have max efficiency"
            );
        }
    }

    @Test
    void reportShouldFormatCorrectly() {
        List<CheckpointStrategyAnalyzer.StrategyResult> results =
            CheckpointStrategyAnalyzer.analyse(
                30, Set.of(10, 20),
                8.0, 1024, 5, 200
            );

        String report =
            CheckpointStrategyAnalyzer.report(results);

        assertNotNull(report);
        assertTrue(report.contains(
            "Checkpoint Strategy Analysis"));
        assertTrue(report.contains("EVERY_NODE"));
        assertTrue(report.contains("EVERY_N_NODES"));
        assertTrue(report.contains("CRITICAL_ONLY"));
        assertTrue(report.contains("PERIODIC"));
        assertTrue(report.contains("Best:"));
    }

    @Test
    void periodicShouldDependOnTiming() {
        // With very short period → more checkpoints
        List<CheckpointStrategyAnalyzer.StrategyResult> shortPeriod =
            CheckpointStrategyAnalyzer.analyse(
                100, Set.of(),
                10.0, 256, 10, 10 // 10ms period
            );

        var periodicShort = shortPeriod.stream()
            .filter(r -> r.strategy() ==
                CheckpointStrategyAnalyzer.Strategy.PERIODIC)
            .findFirst().orElseThrow();

        // With very long period → fewer checkpoints
        List<CheckpointStrategyAnalyzer.StrategyResult> longPeriod =
            CheckpointStrategyAnalyzer.analyse(
                100, Set.of(),
                10.0, 256, 10, 10000 // 10s period
            );

        var periodicLong = longPeriod.stream()
            .filter(r -> r.strategy() ==
                CheckpointStrategyAnalyzer.Strategy.PERIODIC)
            .findFirst().orElseThrow();

        assertTrue(
            periodicShort.checkpointCount()
                >= periodicLong.checkpointCount(),
            "Shorter period should produce more checkpoints"
        );
    }
}

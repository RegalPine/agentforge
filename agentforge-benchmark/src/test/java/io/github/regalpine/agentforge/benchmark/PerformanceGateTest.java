package io.github.regalpine.agentforge.benchmark;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BE-09: Tests for PerformanceGate threshold enforcement.
 */
class PerformanceGateTest {

    private BenchmarkResult result(String id, double p99Nanos) {
        return new BenchmarkResult(id, "test", Instant.now(), 100, 1_000_000, 100, 200_000, p99Nanos, p99Nanos, p99Nanos, p99Nanos, 1000, Map.of());
    }

    @Test
    void passesWhenUnderThreshold() {
        PerformanceGate gate = new PerformanceGate()
            .threshold("fast-op", 50_000_000L); // 50ms

        BenchmarkResult r = result("fast-op", 30_000_000); // 30ms
        gate.assertThreshold("fast-op", r);

        assertTrue(gate.getViolations().isEmpty());
        assertDoesNotThrow(gate::assertAllPassed);
    }

    @Test
    void failsWhenOverThreshold() {
        PerformanceGate gate = new PerformanceGate()
            .threshold("slow-op", 10_000_000L); // 10ms

        BenchmarkResult r = result("slow-op", 20_000_000); // 20ms
        gate.assertThreshold("slow-op", r);

        assertEquals(1, gate.getViolations().size());
        assertThrows(PerformanceGate.PerformanceRegressionException.class, gate::assertAllPassed);
    }

    @Test
    void skipsUnregisteredBenchmarks() {
        PerformanceGate gate = new PerformanceGate();
        BenchmarkResult r = result("unknown", 999_000_000);
        gate.assertThreshold("unknown", r);

        assertTrue(gate.getViolations().isEmpty());
    }

    @Test
    void defaultThresholdsAreReasonable() {
        PerformanceGate gate = PerformanceGate.defaults();

        // All default benchmarks should pass with fast results
        gate.assertThreshold("graph-compile", result("graph-compile", 10_000_000));
        gate.assertThreshold("agent-loop-decision", result("agent-loop-decision", 1_000_000));
        gate.assertThreshold("llm-call", result("llm-call", 500_000_000));
        gate.assertThreshold("execution-start", result("execution-start", 20_000_000));

        assertDoesNotThrow(gate::assertAllPassed);
    }

    @Test
    void multipleViolationsCollected() {
        PerformanceGate gate = new PerformanceGate()
            .threshold("op-a", 10_000_000L)
            .threshold("op-b", 5_000_000L);

        gate.assertThreshold("op-a", result("op-a", 20_000_000));
        gate.assertThreshold("op-b", result("op-b", 10_000_000));

        assertEquals(2, gate.getViolations().size());
    }

    @Test
    void convenienceMethodRegistersAndAsserts() {
        PerformanceGate gate = new PerformanceGate();
        gate.assertThreshold("my-op", result("my-op", 100_000_000), 50_000_000L);

        assertEquals(1, gate.getViolations().size());
        assertEquals("my-op", gate.getViolations().get(0).benchmarkId());
    }
}

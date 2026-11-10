package io.github.regalpine.agentforge.benchmark;

import io.github.regalpine.agentforge.capability.CapabilityDefinition;
import io.github.regalpine.agentforge.compiler.CompilationContext;
import io.github.regalpine.agentforge.compiler.CompilationResult;
import io.github.regalpine.agentforge.compiler.CompilerOptions;
import io.github.regalpine.agentforge.compiler.DefaultGraphCompiler;
import io.github.regalpine.agentforge.graph.GraphBuilder;
import io.github.regalpine.agentforge.graph.GraphDefinition;
import io.github.regalpine.agentforge.graph.ir.ExecutionPlan;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.compiler.RegistrySnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@org.junit.jupiter.api.Tag("benchmark")
class CompilerBenchmarkTest {

    private DefaultGraphCompiler compiler;
    private CapabilityDefinition capability;
    private RegistrySnapshot snapshot;

    @BeforeEach
    void setUp() {
        compiler = new DefaultGraphCompiler();

        capability = new CapabilityDefinition(
            new EntityId("benchmark.cap"),
            new Version("1.0.0"),
            "Benchmark Cap",
            () -> "object",
            () -> "object",
            null
        );

        Reference capRef = new Reference(
            new EntityId("benchmark.cap"),
            new Version("1.0.0")
        );

        snapshot = new RegistrySnapshot(
            Map.of(),
            Map.of(capRef, capability),
            Map.of()
        );
    }

    @Test
    void shouldCompileSimpleGraphWithinTimeBudget() {
        GraphDefinition graph = GraphBuilder
            .graph("bench-simple", "1.0.0", "Simple Bench")
            .start("start")
            .capability("cap", capability)
            .end("end")
            .connect("start", "cap")
            .connect("cap", "end")
            .build();

        CompilationContext context = new CompilationContext(
            snapshot, CompilerOptions.defaults()
        );

        long start = System.nanoTime();
        int iterations = 1000;

        for (int i = 0; i < iterations; i++) {
            CompilationResult result = compiler.compile(graph, context);
            assertInstanceOf(CompilationResult.Success.class, result);
        }

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        double avgMicros = (double) elapsedMs * 1000.0 / iterations;

        System.out.printf(
            "[BENCH] Simple graph compile: %d iterations in %d ms (%.1f µs/iter)%n",
            iterations, elapsedMs, avgMicros
        );

        assertTrue(elapsedMs < 30_000,
            "1000 compilations should complete within 30s, took " + elapsedMs + "ms");
    }

    @Test
    void shouldCompileMediumGraphWithinTimeBudget() {
        GraphBuilder builder = GraphBuilder
            .graph("bench-medium", "1.0.0", "Medium Bench")
            .start("start");

        for (int i = 0; i < 10; i++) {
            builder.capability("cap-" + i, capability);
        }

        builder.end("end");
        builder.connect("start", "cap-0");

        for (int i = 0; i < 9; i++) {
            builder.connect("cap-" + i, "cap-" + (i + 1));
        }

        builder.connect("cap-9", "end");

        GraphDefinition graph = builder.build();

        CompilationContext context = new CompilationContext(
            snapshot, CompilerOptions.defaults()
        );

        long start = System.nanoTime();
        int iterations = 200;

        for (int i = 0; i < iterations; i++) {
            CompilationResult result = compiler.compile(graph, context);
            assertInstanceOf(CompilationResult.Success.class, result);
        }

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        double avgMicros = (double) elapsedMs * 1000.0 / iterations;

        System.out.printf(
            "[BENCH] Medium graph (12 nodes) compile: %d iterations in %d ms (%.1f µs/iter)%n",
            iterations, elapsedMs, avgMicros
        );

        assertTrue(elapsedMs < 30_000,
            "200 compilations should complete within 30s");
    }

    @Test
    void shouldMeasurePlanFingerprintStability() {
        GraphDefinition graph = GraphBuilder
            .graph("bench-fp", "1.0.0", "FP Bench")
            .start("start")
            .capability("cap", capability)
            .end("end")
            .connect("start", "cap")
            .connect("cap", "end")
            .build();

        CompilationContext context = new CompilationContext(
            snapshot, CompilerOptions.defaults()
        );

        String firstFingerprint = null;

        for (int i = 0; i < 100; i++) {
            CompilationResult result = compiler.compile(graph, context);
            CompilationResult.Success success = (CompilationResult.Success) result;
            ExecutionPlan plan = success.executionPlan();

            if (firstFingerprint == null) {
                firstFingerprint = plan.fingerprint();
            } else {
                assertEquals(firstFingerprint, plan.fingerprint(),
                    "Fingerprint must be deterministic across compilations");
            }
        }

        assertNotNull(firstFingerprint);
        assertFalse(firstFingerprint.isEmpty());
    }
}

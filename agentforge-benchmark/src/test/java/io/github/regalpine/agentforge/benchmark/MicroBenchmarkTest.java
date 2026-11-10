package io.github.regalpine.agentforge.benchmark;

import io.github.regalpine.agentforge.event.ExecutionState;
import io.github.regalpine.agentforge.event.ExecutionStatus;
import io.github.regalpine.agentforge.event.ExecutionId;

import io.github.regalpine.agentforge.capability.CapabilityDefinition;
import io.github.regalpine.agentforge.compiler.CompilationContext;
import io.github.regalpine.agentforge.compiler.CompilationResult;
import io.github.regalpine.agentforge.compiler.CompilerOptions;
import io.github.regalpine.agentforge.compiler.DefaultGraphCompiler;
import io.github.regalpine.agentforge.compiler.RegistrySnapshot;
import io.github.regalpine.agentforge.graph.GraphBuilder;
import io.github.regalpine.agentforge.graph.GraphDefinition;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.runtime.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Micro benchmarks.
 * AF-ENHANCE-07 §5: measures fine-grained
 * operations — state access, event creation,
 * schema validation, capability resolution,
 * routing, serialisation.
 */
@org.junit.jupiter.api.Tag("benchmark")
class MicroBenchmarkTest {

    private BenchmarkRunner runner;
    private InMemoryStateManager stateManager;
    private CapabilityDefinition capability;
    private RegistrySnapshot snapshot;
    private DefaultGraphCompiler compiler;

    @BeforeEach
    void setUp() {
        runner = new BenchmarkRunner("micro");
        stateManager = new InMemoryStateManager();

        capability = new CapabilityDefinition(
            new EntityId("micro.cap"),
            new Version("1.0.0"),
            "Micro Cap",
            () -> "object",
            () -> "object",
            null
        );

        Reference capRef = new Reference(
            new EntityId("micro.cap"),
            new Version("1.0.0")
        );

        snapshot = new RegistrySnapshot(
            Map.of(),
            Map.of(capRef, capability),
            Map.of()
        );

        compiler = new DefaultGraphCompiler();
    }

    @Test
    void stateAccessBenchmark() {
        // Pre-populate states
        for (int i = 0; i < 100; i++) {
            stateManager.create(new ExecutionState(
                new ExecutionId("exec-" + i),
                ExecutionStatus.READY,
                new EntityId("start"),
                Map.of(), Set.of(), Set.of(), 0
            ));
        }

        BenchmarkResult result = runner.run(
            "state-access",
            "MICRO",
            10_000,
            () -> {
                for (int i = 0; i < 100; i++) {
                    stateManager.get(
                        new ExecutionId("exec-" + i)
                    );
                }
            }
        );

        System.out.println(result.summary());
        assertTrue(result.iterations() == 10_000);
        assertTrue(result.throughputPerSec() > 0);
    }

    @Test
    void stateCreationBenchmark() {
        BenchmarkResult result = runner.runParam(
            "state-create",
            "MICRO",
            1_000,
            (i) -> {
                ExecutionId id = new ExecutionId(
                    "bench-" + i
                );
                return new ExecutionState(
                    id, ExecutionStatus.READY,
                    new EntityId("start"),
                    Map.of(), Set.of(), Set.of(), 0
                );
            }
        );

        System.out.println(result.summary());
        assertEquals(1_000, result.iterations());
    }

    @Test
    void capabilityResolutionBenchmark() {
        Reference capRef = new Reference(
            new EntityId("micro.cap"),
            new Version("1.0.0")
        );

        BenchmarkResult result = runner.run(
            "cap-resolution",
            "MICRO",
            100_000,
            () -> {
                CapabilityDefinition resolved =
                    snapshot.capabilities().get(capRef);
                assertNotNull(resolved);
            }
        );

        System.out.println(result.summary());
        assertTrue(result.throughputPerSec() > 10_000);
    }

    @Test
    void graphCompilationBenchmark() {
        GraphDefinition graph = GraphBuilder
            .graph("micro-compile", "1.0.0", "Micro")
            .start("start")
            .capability("cap", capability)
            .end("end")
            .connect("start", "cap")
            .connect("cap", "end")
            .build();

        CompilationContext ctx = new CompilationContext(
            snapshot, CompilerOptions.defaults()
        );

        BenchmarkResult result = runner.run(
            "graph-compile-micro",
            "MICRO",
            5_000,
            () -> {
                CompilationResult r =
                    compiler.compile(graph, ctx);
                assertInstanceOf(
                    CompilationResult.Success.class, r
                );
            }
        );

        System.out.println(result.summary());
        assertTrue(result.iterations() == 5_000);
    }

    @Test
    void entityIdCreationBenchmark() {
        BenchmarkResult result = runner.run(
            "entity-id-create",
            "MICRO",
            100_000,
            () -> new EntityId("bench-id")
        );

        System.out.println(result.summary());
        assertTrue(result.throughputPerSec() > 100_000);
    }

    @Test
    void versionParseBenchmark() {
        BenchmarkResult result = runner.run(
            "version-parse",
            "MICRO",
            50_000,
            () -> new Version("1.2.3")
        );

        System.out.println(result.summary());
        assertTrue(result.iterations() == 50_000);
    }

    @Test
    void reportShouldContainAllResults() {
        // Run a quick benchmark
        runner.run("quick", "MICRO", 100, () -> {});

        BenchmarkReport report = runner.report();
        assertEquals(1, report.size());
        assertTrue(report.find("quick").isPresent());
        assertNotNull(report.fingerprint());
        assertFalse(report.fingerprint().stableKey().isEmpty());
    }
}

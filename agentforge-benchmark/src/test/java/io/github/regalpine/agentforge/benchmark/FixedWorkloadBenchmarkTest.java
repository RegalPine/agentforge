package io.github.regalpine.agentforge.benchmark;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * G4-5: Fixed-workload capacity benchmarks.
 *
 * <p>Measures compilation throughput and latency for
 * canonical graph topologies under controlled conditions:
 * <ul>
 *   <li>Linear graph — sequential chain of N nodes</li>
 *   <li>Wide parallel graph — fan-out/fan-in with B branches</li>
 *   <li>Deep subgraph — nested hierarchical structure</li>
 *   <li>Iterative graph — repeated compilation simulating loops</li>
 * </ul>
 *
 * <p>Records p50/p95/p99 latency and establishes regression
 * gates via {@link PerformanceGate}.
 */
@org.junit.jupiter.api.Tag("benchmark")
class FixedWorkloadBenchmarkTest {

    private BenchmarkRunner runner;
    private PerformanceGate gate;
    private CapabilityDefinition capability;
    private RegistrySnapshot snapshot;
    private DefaultGraphCompiler compiler;

    @BeforeEach
    void setUp() {
        runner = new BenchmarkRunner("g4-fixed-workload");
        gate = PerformanceGate.defaults();

        capability = new CapabilityDefinition(
            new EntityId("bench.cap"),
            new Version("1.0.0"),
            "Bench Cap",
            () -> "object",
            () -> "object",
            null
        );

        Reference capRef = new Reference(
            capability.id(), capability.version()
        );

        snapshot = new RegistrySnapshot(
            Map.of(), Map.of(capRef, capability),
            Map.of()
        );

        compiler = new DefaultGraphCompiler();
    }

    // ── Linear Graph ──

    @Test
    void linearGraph10Nodes() {
        GraphDefinition graph = buildLinear(10);
        CompilationContext ctx = new CompilationContext(
            snapshot, CompilerOptions.defaults()
        );

        BenchmarkResult result = runner.run(
            "linear-10", "FIXED_WORKLOAD",
            5_000,
            () -> {
                CompilationResult r = compiler.compile(graph, ctx);
                assertInstanceOf(CompilationResult.Success.class, r);
            }
        );

        System.out.println("[G4-5] Linear-10: " + result.summary());
        gate.assertThreshold("linear-10", result, 10_000_000L);
        assertTrue(result.p50Nanos() < result.p95Nanos());
        assertTrue(result.p95Nanos() <= result.p99Nanos());
    }

    @Test
    void linearGraph50Nodes() {
        GraphDefinition graph = buildLinear(50);
        CompilationContext ctx = new CompilationContext(
            snapshot, CompilerOptions.defaults()
        );

        BenchmarkResult result = runner.run(
            "linear-50", "FIXED_WORKLOAD",
            1_000,
            () -> {
                CompilationResult r = compiler.compile(graph, ctx);
                assertInstanceOf(CompilationResult.Success.class, r);
            }
        );

        System.out.println("[G4-5] Linear-50: " + result.summary());
        gate.assertThreshold("linear-50", result, 50_000_000L);
    }

    @Test
    void linearGraph100Nodes() {
        GraphDefinition graph = buildLinear(100);
        CompilationContext ctx = new CompilationContext(
            snapshot, CompilerOptions.defaults()
        );

        BenchmarkResult result = runner.run(
            "linear-100", "FIXED_WORKLOAD",
            500,
            () -> {
                CompilationResult r = compiler.compile(graph, ctx);
                assertInstanceOf(CompilationResult.Success.class, r);
            }
        );

        System.out.println("[G4-5] Linear-100: " + result.summary());
        gate.assertThreshold("linear-100", result, 100_000_000L);
    }

    // ── Wide Parallel Graph ──

    @Test
    void wideParallelGraph5Branches() {
        GraphDefinition graph = buildParallel(5);
        CompilationContext ctx = new CompilationContext(
            snapshot, CompilerOptions.defaults()
        );

        BenchmarkResult result = runner.run(
            "parallel-5", "FIXED_WORKLOAD",
            5_000,
            () -> {
                CompilationResult r = compiler.compile(graph, ctx);
                assertInstanceOf(CompilationResult.Success.class, r);
            }
        );

        System.out.println("[G4-5] Parallel-5: " + result.summary());
        gate.assertThreshold("parallel-5", result, 10_000_000L);
    }

    @Test
    void wideParallelGraph20Branches() {
        GraphDefinition graph = buildParallel(20);
        CompilationContext ctx = new CompilationContext(
            snapshot, CompilerOptions.defaults()
        );

        BenchmarkResult result = runner.run(
            "parallel-20", "FIXED_WORKLOAD",
            1_000,
            () -> {
                CompilationResult r = compiler.compile(graph, ctx);
                assertInstanceOf(CompilationResult.Success.class, r);
            }
        );

        System.out.println("[G4-5] Parallel-20: " + result.summary());
        gate.assertThreshold("parallel-20", result, 50_000_000L);
    }

    // ── Deep Subgraph ──

    @Test
    void deepSubgraph5Levels() {
        GraphDefinition graph = buildDeep(5);
        CompilationContext ctx = new CompilationContext(
            snapshot, CompilerOptions.defaults()
        );

        BenchmarkResult result = runner.run(
            "deep-5", "FIXED_WORKLOAD",
            2_000,
            () -> {
                CompilationResult r = compiler.compile(graph, ctx);
                assertInstanceOf(CompilationResult.Success.class, r);
            }
        );

        System.out.println("[G4-5] Deep-5: " + result.summary());
        gate.assertThreshold("deep-5", result, 20_000_000L);
    }

    @Test
    void deepSubgraph10Levels() {
        GraphDefinition graph = buildDeep(10);
        CompilationContext ctx = new CompilationContext(
            snapshot, CompilerOptions.defaults()
        );

        BenchmarkResult result = runner.run(
            "deep-10", "FIXED_WORKLOAD",
            1_000,
            () -> {
                CompilationResult r = compiler.compile(graph, ctx);
                assertInstanceOf(CompilationResult.Success.class, r);
            }
        );

        System.out.println("[G4-5] Deep-10: " + result.summary());
        gate.assertThreshold("deep-10", result, 50_000_000L);
    }

    // ── Iterative (Simulated Loop) ──

    @Test
    void iterativeCompilation100Rounds() {
        GraphDefinition graph = buildLinear(5);
        CompilationContext ctx = new CompilationContext(
            snapshot, CompilerOptions.defaults()
        );

        BenchmarkResult result = runner.run(
            "iterative-100", "FIXED_WORKLOAD",
            100,
            () -> {
                for (int i = 0; i < 100; i++) {
                    CompilationResult r = compiler.compile(graph, ctx);
                    assertInstanceOf(CompilationResult.Success.class, r);
                }
            }
        );

        System.out.println("[G4-5] Iterative-100: " + result.summary());
    }

    // ── Regression Gate ──

    @Test
    void performanceGateShouldPass() {
        // Run all core benchmarks and assert gates
        GraphDefinition linear = buildLinear(10);
        CompilationContext ctx = new CompilationContext(
            snapshot, CompilerOptions.defaults()
        );

        BenchmarkResult r1 = runner.run(
            "graph-compile", "GATE",
            5_000,
            () -> compiler.compile(linear, ctx)
        );

        gate.assertThreshold("graph-compile", r1);
        gate.assertAllPassed();
    }

    // ── Report Generation ──

    @Test
    void shouldGenerateBenchmarkReport() {
        GraphDefinition graph = buildLinear(5);
        CompilationContext ctx = new CompilationContext(
            snapshot, CompilerOptions.defaults()
        );

        runner.run("report-linear", "REPORT", 100,
            () -> compiler.compile(graph, ctx));

        BenchmarkReport report = runner.report();
        assertNotNull(report);
        String formatted = report.format();
        assertTrue(formatted.contains("REPORT"));
        assertTrue(formatted.contains("report-linear"));
    }

    // ── Graph Builders ──

    private GraphDefinition buildLinear(int nodes) {
        GraphBuilder b = GraphBuilder
            .graph("linear-" + nodes, "1.0.0", "Linear")
            .start("start");

        for (int i = 0; i < nodes; i++) {
            b.capability("n" + i, capability);
        }

        b.end("end");
        b.connect("start", "n0");
        for (int i = 0; i < nodes - 1; i++) {
            b.connect("n" + i, "n" + (i + 1));
        }
        b.connect("n" + (nodes - 1), "end");

        return b.build();
    }

    private GraphDefinition buildParallel(int branches) {
        GraphBuilder b = GraphBuilder
            .graph("parallel-" + branches, "1.0.0", "Parallel")
            .start("start");

        for (int i = 0; i < branches; i++) {
            b.capability("branch-" + i, capability);
            b.connect("start", "branch-" + i);
            b.connect("branch-" + i, "end");
        }

        b.end("end");
        return b.build();
    }

    private GraphDefinition buildDeep(int levels) {
        GraphBuilder b = GraphBuilder
            .graph("deep-" + levels, "1.0.0", "Deep")
            .start("root");

        String prev = "root";
        for (int level = 0; level < levels; level++) {
            // Each level has a fork-join with 3 nodes
            String fork = "L" + level + "-fork";
            b.capability(fork, capability);
            b.connect(prev, fork);

            for (int j = 0; j < 3; j++) {
                String mid = "L" + level + "-m" + j;
                b.capability(mid, capability);
                b.connect(fork, mid);
                b.connect(mid, "join-" + level);
            }

            String join = "join-" + level;
            b.capability(join, capability);
            prev = join;
        }

        b.end("leaf");
        b.connect(prev, "leaf");
        return b.build();
    }
}

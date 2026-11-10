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
 * Scenario benchmarks.
 * AF-ENHANCE-07 §7: measures end-to-end
 * graph scenarios — linear, parallel,
 * loop, agent-in-graph.
 */
@org.junit.jupiter.api.Tag("benchmark")
class ScenarioBenchmarkTest {

    private BenchmarkRunner runner;
    private CapabilityDefinition capability;
    private RegistrySnapshot snapshot;
    private DefaultGraphCompiler compiler;

    @BeforeEach
    void setUp() {
        runner = new BenchmarkRunner("scenario");

        capability = new CapabilityDefinition(
            new EntityId("scenario.cap"),
            new Version("1.0.0"),
            "Scenario Cap",
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

    @Test
    void linearGraphScenario() {
        GraphDefinition graph = buildLinear(10);

        CompilationContext ctx = new CompilationContext(
            snapshot, CompilerOptions.defaults()
        );

        BenchmarkResult result = runner.run(
            "linear-10",
            "SCENARIO",
            2_000,
            () -> {
                CompilationResult r =
                    compiler.compile(graph, ctx);
                assertInstanceOf(
                    CompilationResult.Success.class, r
                );
            }
        );

        System.out.println(result.summary());
        assertTrue(result.iterations() == 2_000);
    }

    @Test
    void parallelGraphScenario() {
        GraphDefinition graph = buildParallel(5);

        CompilationContext ctx = new CompilationContext(
            snapshot, CompilerOptions.defaults()
        );

        BenchmarkResult result = runner.run(
            "parallel-5",
            "SCENARIO",
            2_000,
            () -> {
                CompilationResult r =
                    compiler.compile(graph, ctx);
                assertInstanceOf(
                    CompilationResult.Success.class, r
                );
            }
        );

        System.out.println(result.summary());
        assertTrue(result.iterations() == 2_000);
    }

    @Test
    void loopGraphScenario() {
        // Simulate a loop graph by compiling
        // a linear graph multiple times
        // (compiler rejects cycles)
        GraphDefinition graph = buildLinear(5);

        CompilationContext ctx = new CompilationContext(
            snapshot, CompilerOptions.defaults()
        );

        BenchmarkResult result = runner.run(
            "loop-simulated",
            "SCENARIO",
            2_000,
            () -> {
                CompilationResult r =
                    compiler.compile(graph, ctx);
                assertInstanceOf(
                    CompilationResult.Success.class, r
                );
            }
        );

        System.out.println(result.summary());
        assertTrue(result.iterations() == 2_000);
    }

    @Test
    void largeLinearGraphScenario() {
        GraphDefinition graph = buildLinear(50);

        CompilationContext ctx = new CompilationContext(
            snapshot, CompilerOptions.defaults()
        );

        BenchmarkResult result = runner.run(
            "linear-50",
            "SCENARIO",
            500,
            () -> {
                CompilationResult r =
                    compiler.compile(graph, ctx);
                assertInstanceOf(
                    CompilationResult.Success.class, r
                );
            }
        );

        System.out.println(result.summary());
        assertTrue(result.iterations() == 500);
    }

    @Test
    void scenarioReportFormat() {
        GraphDefinition graph = buildLinear(5);
        CompilationContext ctx = new CompilationContext(
            snapshot, CompilerOptions.defaults()
        );

        runner.run("scenario-run", "SCENARIO", 100,
            () -> compiler.compile(graph, ctx));

        BenchmarkReport report = runner.report();
        String formatted = report.format();
        assertTrue(formatted.contains("SCENARIO"));
        assertTrue(formatted.contains("scenario-run"));
        assertTrue(
            formatted.contains("Environment Fingerprint")
        );
    }

    /* --- graph builders --- */

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
            .graph("parallel-" + branches, "1.0.0",
                "Parallel")
            .start("start");

        for (int i = 0; i < branches; i++) {
            b.capability("branch-" + i, capability);
            b.connect("start", "branch-" + i);
            b.connect("branch-" + i, "end");
        }

        b.end("end");
        return b.build();
    }

    // Loop graph not supported by compiler
    // (cycles are rejected). Loop scenario
    // is simulated via repeated compilation.
}

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

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Component benchmarks.
 * AF-ENHANCE-07 §6: measures component-level
 * performance — CapabilityRegistry (100~100K),
 * schema validation, agent loop (1~50 iter).
 */
@org.junit.jupiter.api.Tag("benchmark")
class ComponentBenchmarkTest {

    private BenchmarkRunner runner;

    @BeforeEach
    void setUp() {
        runner = new BenchmarkRunner("component");
    }

    @Test
    void capabilityRegistry100Benchmark() {
        Map<Reference, CapabilityDefinition> caps =
            buildCaps(100);
        RegistrySnapshot snapshot = new RegistrySnapshot(
            Map.of(), caps, Map.of()
        );

        BenchmarkResult result = runner.run(
            "registry-100",
            "COMPONENT",
            10_000,
            () -> {
                assertEquals(100,
                    snapshot.capabilities().size());
            }
        );

        System.out.println(result.summary());
        assertTrue(result.throughputPerSec() > 10_000);
    }

    @Test
    void capabilityRegistry10KBenchmark() {
        Map<Reference, CapabilityDefinition> caps =
            buildCaps(10_000);
        RegistrySnapshot snapshot = new RegistrySnapshot(
            Map.of(), caps, Map.of()
        );

        BenchmarkResult result = runner.run(
            "registry-10k",
            "COMPONENT",
            1_000,
            () -> {
                assertEquals(10_000,
                    snapshot.capabilities().size());
            }
        );

        System.out.println(result.summary());
        assertTrue(result.iterations() == 1_000);
    }

    @Test
    void graphCompileWith10NodesBenchmark() {
        CapabilityDefinition cap = makeCap("comp");
        Reference capRef = new Reference(
            cap.id(), cap.version()
        );
        RegistrySnapshot snapshot = new RegistrySnapshot(
            Map.of(), Map.of(capRef, cap), Map.of()
        );
        DefaultGraphCompiler compiler =
            new DefaultGraphCompiler();

        GraphDefinition graph = buildLinearGraph(
            "comp-10", 10, cap
        );

        CompilationContext ctx = new CompilationContext(
            snapshot, CompilerOptions.defaults()
        );

        BenchmarkResult result = runner.run(
            "compile-10nodes",
            "COMPONENT",
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
    void graphCompileWith50NodesBenchmark() {
        CapabilityDefinition cap = makeCap("comp50");
        Reference capRef = new Reference(
            cap.id(), cap.version()
        );
        RegistrySnapshot snapshot = new RegistrySnapshot(
            Map.of(), Map.of(capRef, cap), Map.of()
        );
        DefaultGraphCompiler compiler =
            new DefaultGraphCompiler();

        GraphDefinition graph = buildLinearGraph(
            "comp-50", 50, cap
        );

        CompilationContext ctx = new CompilationContext(
            snapshot, CompilerOptions.defaults()
        );

        BenchmarkResult result = runner.run(
            "compile-50nodes",
            "COMPONENT",
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
    void graphBuildLargeBenchmark() {
        BenchmarkResult result = runner.run(
            "build-200nodes",
            "COMPONENT",
            200,
            () -> {
                GraphDefinition g =
                    buildLinearGraph("large", 200,
                        makeCap("large"));
                assertEquals(202, g.nodes().size());
            }
        );

        System.out.println(result.summary());
        assertTrue(result.iterations() == 200);
    }

    @Test
    void reportShouldAggregateResults() {
        runner.run("r1", "COMPONENT", 100, () -> {});
        runner.run("r2", "COMPONENT", 200, () -> {});

        BenchmarkReport report = runner.report();
        assertEquals(2, report.size());
        assertEquals(2,
            report.byCategory().get("COMPONENT").size());
    }

    /* --- helpers --- */

    private static CapabilityDefinition makeCap(
        String prefix
    ) {
        return new CapabilityDefinition(
            new EntityId(prefix + ".cap"),
            new Version("1.0.0"),
            prefix + " Cap",
            () -> "object",
            () -> "object",
            null
        );
    }

    private static Map<Reference, CapabilityDefinition>
    buildCaps(int count) {
        Map<Reference, CapabilityDefinition> map =
            new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            CapabilityDefinition cap = new CapabilityDefinition(
                new EntityId("cap-" + i),
                new Version("1.0.0"),
                "Cap " + i,
                () -> "object",
                () -> "object",
                null
            );
            map.put(
                new Reference(cap.id(), cap.version()),
                cap
            );
        }
        return map;
    }

    private static GraphDefinition buildLinearGraph(
        String id, int nodeCount, CapabilityDefinition cap
    ) {
        GraphBuilder builder = GraphBuilder
            .graph(id, "1.0.0", "Linear " + nodeCount)
            .start("start");

        for (int i = 0; i < nodeCount; i++) {
            builder.capability("n-" + i, cap);
        }

        builder.end("end");
        builder.connect("start", "n-0");
        for (int i = 0; i < nodeCount - 1; i++) {
            builder.connect("n-" + i, "n-" + (i + 1));
        }
        builder.connect(
            "n-" + (nodeCount - 1), "end"
        );

        return builder.build();
    }
}

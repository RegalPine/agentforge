package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.capability.CapabilityDefinition;
import io.github.regalpine.agentforge.compiler.*;
import io.github.regalpine.agentforge.graph.*;
import io.github.regalpine.agentforge.hardening.*;
import io.github.regalpine.agentforge.kernel.*;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * conformance-graph: Validation / IR
 * / Plan / Routing / Parallel / Loop.
 * AF-RELEASE-02 §4, T-H6.4.
 */
class GraphConformanceTest {

    private final CapabilityDefinition cap =
        new CapabilityDefinition(
            new EntityId("g.cap"),
            new Version("1.0.0"),
            "GraphCap",
            null, null, null
        );

    @Test
    void afGraph001_linearGraphBuild() {
        var graph = GraphBuilder
            .graph("linear", "1.0.0", "Linear")
            .start("start")
            .capability("a", cap)
            .end("end")
            .connect("start", "a")
            .connect("a", "end")
            .build();

        assertNotNull(graph);
        assertEquals(3, graph.nodes().size());
    }

    @Test
    void afGraph002_parallelGraphBuild() {
        var graph = GraphBuilder
            .graph("parallel", "1.0.0", "Parallel")
            .start("start")
            .capability("left", cap)
            .capability("right", cap)
            .end("end")
            .connect("start", "left")
            .connect("start", "right")
            .connect("left", "end")
            .connect("right", "end")
            .build();

        assertNotNull(graph);
        assertEquals(4, graph.nodes().size());
    }

    @Test
    void afGraph003_compilationSuccess() {
        var graph = GraphBuilder
            .graph("compile", "1.0.0", "Compile")
            .start("start")
            .capability("node", cap)
            .end("end")
            .connect("start", "node")
            .connect("node", "end")
            .build();

        var compiler = new DefaultGraphCompiler();
        var ref = new Reference(
            new EntityId("g.cap"),
            new Version("1.0.0")
        );
        var snapshot = new RegistrySnapshot(
            Map.of(),
            Map.of(ref, cap),
            Map.of()
        );
        var ctx = new CompilationContext(
            snapshot, CompilerOptions.defaults()
        );

        var result = compiler.compile(graph, ctx);
        assertInstanceOf(
            CompilationResult.Success.class, result
        );
    }

    @Test
    void afGraph004_graphMetadata() {
        var graph = GraphBuilder
            .graph("meta", "1.0.0", "Meta Graph")
            .start("s")
            .end("e")
            .connect("s", "e")
            .build();

        assertEquals(new EntityId("meta"), graph.id());
        assertEquals(new Version("1.0.0"), graph.version());
    }

    @Test
    void afGraph005_routingGraph() {
        var graph = GraphBuilder
            .graph("route", "1.0.0", "Route")
            .start("start")
            .capability("r1", cap)
            .capability("r2", cap)
            .end("end")
            .connect("start", "r1")
            .connect("start", "r2")
            .connect("r1", "end")
            .connect("r2", "end")
            .build();

        assertNotNull(graph);
        assertTrue(graph.nodes().size() >= 4);
    }
}

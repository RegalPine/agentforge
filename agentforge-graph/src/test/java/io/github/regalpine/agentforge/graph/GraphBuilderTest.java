package io.github.regalpine.agentforge.graph;

import io.github.regalpine.agentforge.kernel.EntityId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GraphBuilder DSL.
 * Covers: basic construction, chaining, capability/agent references, edge generation.
 */
class GraphBuilderTest {

    @Test
    void shouldBuildMinimalGraph() {
        GraphDefinition graph = GraphBuilder
            .graph("test", "1.0.0", "Test Graph")
            .start("s")
            .end("e")
            .connect("s", "e")
            .build();

        assertEquals("test", graph.id().value());
        assertEquals("1.0.0", graph.version().value());
        assertEquals("Test Graph", graph.name());
        assertEquals(2, graph.nodes().size());
        assertEquals(1, graph.edges().size());
    }

    @Test
    void shouldChainMultipleNodes() {
        GraphDefinition graph = GraphBuilder
            .graph("multi", "1.0.0", "Multi")
            .start("s")
            .end("e")
            .connect("s", "e")
            .build();

        assertEquals(2, graph.nodes().size());
        assertEquals(NodeType.START, graph.nodes().get(0).type());
        assertEquals(NodeType.END, graph.nodes().get(1).type());
    }

    @Test
    void shouldGenerateEdgeIdFromSourceAndTarget() {
        GraphDefinition graph = GraphBuilder
            .graph("edge-test", "1.0.0", "Edge Test")
            .start("alpha")
            .end("beta")
            .connect("alpha", "beta")
            .build();

        EdgeDefinition edge = graph.edges().get(0);
        assertEquals("alpha-to-beta", edge.id().value());
        assertEquals("alpha", edge.source().value());
        assertEquals("beta", edge.target().value());
        assertEquals(EdgeType.NORMAL, edge.type());
    }

    @Test
    void shouldPreserveNodeOrder() {
        GraphDefinition graph = GraphBuilder
            .graph("order", "1.0.0", "Order")
            .start("first")
            .end("last")
            .connect("first", "last")
            .build();

        assertEquals("first", graph.nodes().get(0).id().value());
        assertEquals("last", graph.nodes().get(1).id().value());
    }

    @Test
    void shouldBuildGraphWithMultipleEdges() {
        GraphDefinition graph = GraphBuilder
            .graph("multi-edge", "1.0.0", "Multi Edge")
            .start("s")
            .end("e")
            .connect("s", "e")
            .build();

        assertEquals(1, graph.edges().size());
        assertNull(graph.edges().get(0).condition());
    }

    @Test
    void shouldCreateGraphWithEmptyOptionalFields() {
        GraphDefinition graph = GraphBuilder
            .graph("minimal", "0.1.0", "Minimal")
            .start("s")
            .end("e")
            .connect("s", "e")
            .build();

        assertEquals(Map.of(), graph.inputs());
        assertEquals(Map.of(), graph.outputs());
        assertEquals(List.of(), graph.bindings());
        assertEquals(List.of(), graph.expressions());
        assertEquals(Map.of(), graph.policies());
        assertEquals(Map.of(), graph.metadata());
    }
}

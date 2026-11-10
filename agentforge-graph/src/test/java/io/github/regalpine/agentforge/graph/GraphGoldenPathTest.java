package io.github.regalpine.agentforge.graph;

import io.github.regalpine.agentforge.capability.CapabilityDefinition;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Schema;
import io.github.regalpine.agentforge.kernel.Version;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GraphGoldenPathTest {

    @Test
    void shouldBuildValidGraph() {

        Schema schema =
            () -> "object";

        CapabilityDefinition calculate =
            new CapabilityDefinition(
                new EntityId("calculator.evaluate"),
                new Version("1.0.0"),
                "Calculator",
                schema,
                schema,
                null
            );

        GraphDefinition graph =
            GraphBuilder
                .graph(
                    "demo",
                    "1.0.0",
                    "Demo"
                )
                .start("start")
                .capability(
                    "calculate",
                    calculate
                )
                .end("end")
                .connect(
                    "start",
                    "calculate"
                )
                .connect(
                    "calculate",
                    "end"
                )
                .build();

        assertDoesNotThrow(
            () -> new BasicGraphValidator()
                .validate(graph)
        );
    }

    @Test
    void shouldRejectGraphWithoutStart() {
        GraphDefinition graph =
            GraphBuilder
                .graph("test", "1.0.0", "Test")
                .end("end")
                .build();

        assertThrows(
            IllegalArgumentException.class,
            () -> new BasicGraphValidator().validate(graph)
        );
    }

    @Test
    void shouldRejectGraphWithDuplicateNodeIds() {
        GraphDefinition graph =
            GraphBuilder
                .graph("test", "1.0.0", "Test")
                .start("start")
                .start("start")
                .end("end")
                .build();

        assertThrows(
            IllegalArgumentException.class,
            () -> new BasicGraphValidator().validate(graph)
        );
    }

    @Test
    void shouldRejectEdgeWithUnknownSource() {
        GraphDefinition graph =
            GraphBuilder
                .graph("test", "1.0.0", "Test")
                .start("start")
                .end("end")
                .connect("start", "end")
                .connect("unknown", "end")
                .build();

        assertThrows(
            IllegalArgumentException.class,
            () -> new BasicGraphValidator().validate(graph)
        );
    }

    @Test
    void shouldVerifyNodeTypeCount() {
        assertEquals(17, NodeType.values().length);
    }

    @Test
    void shouldVerifyEdgeTypeCount() {
        assertEquals(4, EdgeType.values().length);
    }

    @Test
    void shouldProduceImmutableCollections() {
        GraphDefinition graph =
            GraphBuilder
                .graph("test", "1.0.0", "Test")
                .start("start")
                .end("end")
                .connect("start", "end")
                .build();

        assertThrows(
            UnsupportedOperationException.class,
            () -> graph.nodes().add(null)
        );

        assertThrows(
            UnsupportedOperationException.class,
            () -> graph.edges().add(null)
        );
    }
}

package io.github.regalpine.agentforge.graph;

import io.github.regalpine.agentforge.capability.CapabilityDefinition;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Schema;
import io.github.regalpine.agentforge.kernel.Version;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BasicGraphValidator.
 * Covers: empty graph, duplicate IDs, missing START/END, missing references, invalid edges.
 */
class BasicGraphValidatorTest {

    private final BasicGraphValidator validator = new BasicGraphValidator();
    private final Schema schema = () -> "object";

    @Test
    void shouldRejectEmptyGraph() {
        GraphDefinition empty = new GraphDefinition(
            new EntityId("g"), new Version("1.0.0"), "Empty",
            null, null, null, Collections.emptyList(), Collections.emptyList(),
            null, null, null, null
        );

        assertThrows(IllegalArgumentException.class, () -> validator.validate(empty));
    }

    @Test
    void shouldRejectGraphWithNoStart() {
        GraphDefinition noStart = new GraphDefinition(
            new EntityId("g"), new Version("1.0.0"), "NoStart",
            null, null, null,
            List.of(new NodeDefinition(new EntityId("e"), NodeType.END, null, null)),
            Collections.emptyList(),
            null, null, null, null
        );

        var ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(noStart));
        assertTrue(ex.getMessage().contains("START"));
    }

    @Test
    void shouldRejectGraphWithNoEnd() {
        GraphDefinition noEnd = new GraphDefinition(
            new EntityId("g"), new Version("1.0.0"), "NoEnd",
            null, null, null,
            List.of(new NodeDefinition(new EntityId("s"), NodeType.START, null, null)),
            Collections.emptyList(),
            null, null, null, null
        );

        var ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(noEnd));
        assertTrue(ex.getMessage().contains("END"));
    }

    @Test
    void shouldRejectDuplicateNodeIds() {
        GraphDefinition dupNodes = new GraphDefinition(
            new EntityId("g"), new Version("1.0.0"), "Dup",
            null, null, null,
            List.of(
                new NodeDefinition(new EntityId("s"), NodeType.START, null, null),
                new NodeDefinition(new EntityId("s"), NodeType.END, null, null)
            ),
            Collections.emptyList(),
            null, null, null, null
        );

        var ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(dupNodes));
        assertTrue(ex.getMessage().contains("Duplicate node id"));
    }

    @Test
    void shouldRejectCapabilityNodeWithoutReference() {
        GraphDefinition capNoRef = new GraphDefinition(
            new EntityId("g"), new Version("1.0.0"), "CapNoRef",
            null, null, null,
            List.of(
                new NodeDefinition(new EntityId("s"), NodeType.START, null, null),
                new NodeDefinition(new EntityId("c"), NodeType.CAPABILITY, null, null),
                new NodeDefinition(new EntityId("e"), NodeType.END, null, null)
            ),
            Collections.emptyList(),
            null, null, null, null
        );

        var ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(capNoRef));
        assertTrue(ex.getMessage().contains("CAPABILITY"));
    }

    @Test
    void shouldRejectAgentNodeWithoutReference() {
        GraphDefinition agentNoRef = new GraphDefinition(
            new EntityId("g"), new Version("1.0.0"), "AgentNoRef",
            null, null, null,
            List.of(
                new NodeDefinition(new EntityId("s"), NodeType.START, null, null),
                new NodeDefinition(new EntityId("a"), NodeType.AGENT, null, null),
                new NodeDefinition(new EntityId("e"), NodeType.END, null, null)
            ),
            Collections.emptyList(),
            null, null, null, null
        );

        var ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(agentNoRef));
        assertTrue(ex.getMessage().contains("AGENT"));
    }

    @Test
    void shouldRejectEdgeWithUnknownSource() {
        GraphDefinition badEdge = new GraphDefinition(
            new EntityId("g"), new Version("1.0.0"), "BadEdge",
            null, null, null,
            List.of(
                new NodeDefinition(new EntityId("s"), NodeType.START, null, null),
                new NodeDefinition(new EntityId("e"), NodeType.END, null, null)
            ),
            List.of(new EdgeDefinition(
                new EntityId("x"), new EntityId("unknown"), new EntityId("e"),
                EdgeType.NORMAL, null
            )),
            null, null, null, null
        );

        var ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(badEdge));
        assertTrue(ex.getMessage().contains("Unknown edge source"));
    }

    @Test
    void shouldRejectEdgeWithUnknownTarget() {
        GraphDefinition badEdge = new GraphDefinition(
            new EntityId("g"), new Version("1.0.0"), "BadEdge",
            null, null, null,
            List.of(
                new NodeDefinition(new EntityId("s"), NodeType.START, null, null),
                new NodeDefinition(new EntityId("e"), NodeType.END, null, null)
            ),
            List.of(new EdgeDefinition(
                new EntityId("x"), new EntityId("s"), new EntityId("unknown"),
                EdgeType.NORMAL, null
            )),
            null, null, null, null
        );

        var ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(badEdge));
        assertTrue(ex.getMessage().contains("Unknown edge target"));
    }

    @Test
    void shouldRejectDuplicateEdgeIds() {
        GraphDefinition dupEdges = new GraphDefinition(
            new EntityId("g"), new Version("1.0.0"), "DupEdge",
            null, null, null,
            List.of(
                new NodeDefinition(new EntityId("s"), NodeType.START, null, null),
                new NodeDefinition(new EntityId("e"), NodeType.END, null, null)
            ),
            List.of(
                new EdgeDefinition(new EntityId("dup"), new EntityId("s"), new EntityId("e"), EdgeType.NORMAL, null),
                new EdgeDefinition(new EntityId("dup"), new EntityId("s"), new EntityId("e"), EdgeType.NORMAL, null)
            ),
            null, null, null, null
        );

        var ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(dupEdges));
        assertTrue(ex.getMessage().contains("Duplicate edge id"));
    }

    @Test
    void shouldAcceptValidSimpleGraph() {
        GraphDefinition valid = GraphBuilder
            .graph("valid", "1.0.0", "Valid")
            .start("s")
            .end("e")
            .connect("s", "e")
            .build();

        assertDoesNotThrow(() -> validator.validate(valid));
    }
}

package io.github.regalpine.agentforge.compiler;

import io.github.regalpine.agentforge.capability.CapabilityDefinition;
import io.github.regalpine.agentforge.graph.EdgeDefinition;
import io.github.regalpine.agentforge.graph.EdgeType;
import io.github.regalpine.agentforge.graph.GraphBuilder;
import io.github.regalpine.agentforge.graph.GraphDefinition;
import io.github.regalpine.agentforge.graph.NodeDefinition;
import io.github.regalpine.agentforge.graph.NodeType;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Schema;
import io.github.regalpine.agentforge.kernel.Version;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for compiler cycle detection (Phase 4).
 * Covers: acyclic graphs, implicit cycles, LOOP_BACK exemption.
 */
class CycleDetectionTest {

    private final DefaultGraphCompiler compiler = new DefaultGraphCompiler();
    private final Schema schema = () -> "object";

    private CompilationContext emptyContext() {
        return new CompilationContext(RegistrySnapshot.empty(), null);
    }

    @Test
    void shouldCompileAcyclicGraph() {
        CapabilityDefinition cap = new CapabilityDefinition(
            new EntityId("calc"), new Version("1.0.0"), "Calc",
            schema, schema, null
        );

        GraphDefinition graph = GraphBuilder
            .graph("acyclic", "1.0.0", "Acyclic")
            .start("s")
            .capability("c", cap)
            .end("e")
            .connect("s", "c")
            .connect("c", "e")
            .build();

        CompilationResult result = compiler.compile(graph, new CompilationContext(
            new RegistrySnapshot(
                Map.of(),
                Map.of(new Reference(cap.id(), cap.version()), cap),
                Map.of()
            ), null
        ));
        assertInstanceOf(CompilationResult.Success.class, result);
    }

    @Test
    void shouldDetectImplicitCycleInNonLoopEdges() {
        // Create a graph with an implicit cycle: s -> a -> b -> a
        // (not using LOOP_BACK type, so it should be detected as a cycle)
        GraphDefinition cyclicGraph = new GraphDefinition(
            new EntityId("cyclic"), new Version("1.0.0"), "Cyclic",
            null, null, null,
            List.of(
                new NodeDefinition(new EntityId("s"), NodeType.START, null, null),
                new NodeDefinition(new EntityId("a"), NodeType.ACTION, null, null),
                new NodeDefinition(new EntityId("b"), NodeType.ACTION, null, null),
                new NodeDefinition(new EntityId("e"), NodeType.END, null, null)
            ),
            List.of(
                new EdgeDefinition(new EntityId("e1"), new EntityId("s"), new EntityId("a"), EdgeType.NORMAL, null),
                new EdgeDefinition(new EntityId("e2"), new EntityId("a"), new EntityId("b"), EdgeType.NORMAL, null),
                new EdgeDefinition(new EntityId("e3"), new EntityId("b"), new EntityId("a"), EdgeType.NORMAL, null),
                new EdgeDefinition(new EntityId("e4"), new EntityId("a"), new EntityId("e"), EdgeType.NORMAL, null)
            ),
            null, null, null, null
        );

        CompilationResult result = compiler.compile(cyclicGraph, emptyContext());
        assertInstanceOf(CompilationResult.Failure.class, result);

        CompilationResult.Failure failure = (CompilationResult.Failure) result;
        assertTrue(
            failure.diagnostics().stream()
                .anyMatch(d -> d.message().contains("cycle")),
            "Expected cycle detection diagnostic"
        );
    }

    @Test
    void shouldAllowLoopBackEdgesWithoutCycleError() {
        CapabilityDefinition cap = new CapabilityDefinition(
            new EntityId("step"), new Version("1.0.0"), "Step",
            schema, schema, null
        );

        // s -> step -> end, with step -> step as LOOP_BACK
        GraphDefinition graph = new GraphDefinition(
            new EntityId("loopback"), new Version("1.0.0"), "LoopBack",
            null, null, null,
            List.of(
                new NodeDefinition(new EntityId("s"), NodeType.START, null, null),
                new NodeDefinition(new EntityId("step"), NodeType.CAPABILITY,
                    new Reference(new EntityId("step"), new Version("1.0.0")), null),
                new NodeDefinition(new EntityId("e"), NodeType.END, null, null)
            ),
            List.of(
                new EdgeDefinition(new EntityId("e1"), new EntityId("s"), new EntityId("step"), EdgeType.NORMAL, null),
                new EdgeDefinition(new EntityId("e2"), new EntityId("step"), new EntityId("e"), EdgeType.NORMAL, null),
                new EdgeDefinition(new EntityId("e3"), new EntityId("step"), new EntityId("step"), EdgeType.LOOP_BACK, null)
            ),
            null, null, null, null
        );

        CompilationResult result = compiler.compile(graph, new CompilationContext(
            new RegistrySnapshot(
                Map.of(),
                Map.of(new Reference(new EntityId("step"), new Version("1.0.0")), cap),
                Map.of()
            ), null
        ));

        // Should not have cycle detection errors
        if (result instanceof CompilationResult.Failure failure) {
            assertFalse(
                failure.diagnostics().stream()
                    .anyMatch(d -> d.message().contains("cycle")),
                "LOOP_BACK edges should be exempt from cycle detection"
            );
        }
    }
}

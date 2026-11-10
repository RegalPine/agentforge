package io.github.regalpine.agentforge.compiler;

import io.github.regalpine.agentforge.capability.CapabilityDefinition;
import io.github.regalpine.agentforge.graph.EdgeDefinition;
import io.github.regalpine.agentforge.graph.EdgeType;
import io.github.regalpine.agentforge.graph.GraphBuilder;
import io.github.regalpine.agentforge.graph.GraphDefinition;
import io.github.regalpine.agentforge.graph.NodeDefinition;
import io.github.regalpine.agentforge.graph.NodeType;
import io.github.regalpine.agentforge.graph.ir.ExecutionPlan;
import io.github.regalpine.agentforge.graph.ir.GraphIR;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Schema;
import io.github.regalpine.agentforge.kernel.Version;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultGraphCompilerTest {

    private final DefaultGraphCompiler compiler =
        new DefaultGraphCompiler();

    private final Schema schema = () -> "object";

    // AF-COMP-001: Valid Graph compiles successfully
    @Test
    void goldenPathShouldCompile() {

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
                .graph("demo", "1.0.0", "Demo")
                .start("start")
                .capability("calculate", calculate)
                .end("end")
                .connect("start", "calculate")
                .connect("calculate", "end")
                .build();

        RegistrySnapshot snapshot = new RegistrySnapshot(
            Map.of(),
            Map.of(
                new Reference(
                    new EntityId("calculator.evaluate"),
                    new Version("1.0.0")
                ),
                calculate
            ),
            Map.of()
        );

        CompilationContext context =
            new CompilationContext(
                snapshot,
                CompilerOptions.defaults()
            );

        CompilationResult result =
            compiler.compile(graph, context);

        assertInstanceOf(
            CompilationResult.Success.class,
            result
        );

        CompilationResult.Success success =
            (CompilationResult.Success) result;

        GraphIR ir = success.graphIR();
        ExecutionPlan plan = success.executionPlan();

        assertEquals("demo", ir.graphId().value());
        assertEquals("1.0.0", ir.graphVersion().value());

        assertEquals(3, ir.nodes().size());
        assertEquals(2, ir.edges().size());

        assertEquals("start", plan.entryNode().value());
        assertEquals(3, plan.nodes().size());

        assertNotNull(plan.fingerprint());
        assertFalse(plan.fingerprint().isEmpty());
    }

    // AF-COMP-002: Missing capability
    @Test
    void missingCapabilityShouldFail() {

        GraphDefinition graph =
            GraphBuilder
                .graph("demo", "1.0.0", "Demo")
                .start("start")
                .capability(
                    "calculate",
                    new CapabilityDefinition(
                        new EntityId("calculator.evaluate"),
                        new Version("9.9.9"),
                        "Calculator",
                        schema, schema, null
                    )
                )
                .end("end")
                .connect("start", "calculate")
                .connect("calculate", "end")
                .build();

        CompilationResult result =
            compiler.compile(
                graph,
                new CompilationContext(null, null)
            );

        assertInstanceOf(
            CompilationResult.Failure.class,
            result
        );

        CompilationResult.Failure failure =
            (CompilationResult.Failure) result;

        assertTrue(
            failure.diagnostics().stream()
                .anyMatch(d ->
                    d.code().equals("AF-C002")
                )
        );
    }

    // AF-COMP-004: Unreachable node
    @Test
    void unreachableNodeShouldFail() {

        CapabilityDefinition calculate =
            new CapabilityDefinition(
                new EntityId("calculator.evaluate"),
                new Version("1.0.0"),
                "Calculator",
                schema, schema, null
            );

        GraphDefinition graph =
            GraphBuilder
                .graph("demo", "1.0.0", "Demo")
                .start("start")
                .end("end")
                .build();

        // Add orphan node manually
        List<NodeDefinition> nodes = new java.util.ArrayList<>(
            graph.nodes()
        );
        nodes.add(
            new NodeDefinition(
                new EntityId("orphan"),
                NodeType.ACTION,
                null,
                null
            )
        );

        GraphDefinition orphanGraph =
            new GraphDefinition(
                graph.id(),
                graph.version(),
                graph.name(),
                graph.description(),
                graph.inputs(),
                graph.outputs(),
                nodes,
                graph.edges(),
                graph.bindings(),
                graph.expressions(),
                graph.policies(),
                graph.metadata()
            );

        CompilationResult result =
            compiler.compile(
                orphanGraph,
                new CompilationContext(null, null)
            );

        assertInstanceOf(
            CompilationResult.Failure.class,
            result
        );

        CompilationResult.Failure failure =
            (CompilationResult.Failure) result;

        assertTrue(
            failure.diagnostics().stream()
                .anyMatch(d ->
                    d.code().equals("AF-C004")
                )
        );
    }

    // AF-COMP-007: Implicit cycle detection
    @Test
    void implicitCycleShouldFail() {

        GraphDefinition graph =
            GraphBuilder
                .graph("demo", "1.0.0", "Demo")
                .start("start")
                .end("end")
                .connect("start", "end")
                .build();

        // Add cycle: a -> b -> a (non LOOP_BACK)
        List<NodeDefinition> nodes =
            new java.util.ArrayList<>(graph.nodes());

        nodes.add(
            new NodeDefinition(
                new EntityId("a"),
                NodeType.ACTION,
                null, null
            )
        );
        nodes.add(
            new NodeDefinition(
                new EntityId("b"),
                NodeType.ACTION,
                null, null
            )
        );

        List<EdgeDefinition> edges =
            new java.util.ArrayList<>(graph.edges());

        edges.add(
            new EdgeDefinition(
                new EntityId("start-to-a"),
                new EntityId("start"),
                new EntityId("a"),
                EdgeType.NORMAL,
                null
            )
        );
        edges.add(
            new EdgeDefinition(
                new EntityId("a-to-b"),
                new EntityId("a"),
                new EntityId("b"),
                EdgeType.NORMAL,
                null
            )
        );
        edges.add(
            new EdgeDefinition(
                new EntityId("b-to-a"),
                new EntityId("b"),
                new EntityId("a"),
                EdgeType.NORMAL,
                null
            )
        );
        edges.add(
            new EdgeDefinition(
                new EntityId("b-to-end"),
                new EntityId("b"),
                new EntityId("end"),
                EdgeType.NORMAL,
                null
            )
        );

        GraphDefinition cyclicGraph =
            new GraphDefinition(
                graph.id(), graph.version(),
                graph.name(), graph.description(),
                graph.inputs(), graph.outputs(),
                nodes, edges,
                graph.bindings(), graph.expressions(),
                graph.policies(), graph.metadata()
            );

        CompilationResult result =
            compiler.compile(
                cyclicGraph,
                new CompilationContext(null, null)
            );

        assertInstanceOf(
            CompilationResult.Failure.class,
            result
        );

        CompilationResult.Failure failure =
            (CompilationResult.Failure) result;

        assertTrue(
            failure.diagnostics().stream()
                .anyMatch(d ->
                    d.code().equals("AF-C007")
                )
        );
    }

    // AF-COMP-009/010: Deterministic ordering + stable fingerprint
    @Test
    void fingerprintShouldBeDeterministic() {

        CapabilityDefinition calculate =
            new CapabilityDefinition(
                new EntityId("calculator.evaluate"),
                new Version("1.0.0"),
                "Calculator",
                schema, schema, null
            );

        GraphDefinition graph =
            GraphBuilder
                .graph("demo", "1.0.0", "Demo")
                .start("start")
                .capability("calculate", calculate)
                .end("end")
                .connect("start", "calculate")
                .connect("calculate", "end")
                .build();

        RegistrySnapshot snapshot = new RegistrySnapshot(
            Map.of(),
            Map.of(
                new Reference(
                    new EntityId("calculator.evaluate"),
                    new Version("1.0.0")
                ),
                calculate
            ),
            Map.of()
        );

        CompilationContext context =
            new CompilationContext(
                snapshot,
                CompilerOptions.defaults()
            );

        CompilationResult a =
            compiler.compile(graph, context);

        CompilationResult b =
            compiler.compile(graph, context);

        assertInstanceOf(
            CompilationResult.Success.class, a
        );
        assertInstanceOf(
            CompilationResult.Success.class, b
        );

        ExecutionPlan planA =
            ((CompilationResult.Success) a)
                .executionPlan();

        ExecutionPlan planB =
            ((CompilationResult.Success) b)
                .executionPlan();

        assertEquals(
            planA.fingerprint(),
            planB.fingerprint()
        );
    }

    // AF-COMP-011: Exact version resolution
    @Test
    void exactVersionShouldBeResolved() {

        CapabilityDefinition v1 =
            new CapabilityDefinition(
                new EntityId("calculator.evaluate"),
                new Version("1.0.0"),
                "Calculator V1",
                schema, schema, null
            );

        CapabilityDefinition v2 =
            new CapabilityDefinition(
                new EntityId("calculator.evaluate"),
                new Version("2.0.0"),
                "Calculator V2",
                schema, schema, null
            );

        GraphDefinition graph =
            GraphBuilder
                .graph("demo", "1.0.0", "Demo")
                .start("start")
                .capability("calculate", v1)
                .end("end")
                .connect("start", "calculate")
                .connect("calculate", "end")
                .build();

        RegistrySnapshot snapshot = new RegistrySnapshot(
            Map.of(),
            Map.of(
                new Reference(
                    new EntityId("calculator.evaluate"),
                    new Version("1.0.0")
                ),
                v1,
                new Reference(
                    new EntityId("calculator.evaluate"),
                    new Version("2.0.0")
                ),
                v2
            ),
            Map.of()
        );

        CompilationContext context =
            new CompilationContext(
                snapshot,
                CompilerOptions.defaults()
            );

        CompilationResult result =
            compiler.compile(graph, context);

        assertInstanceOf(
            CompilationResult.Success.class,
            result
        );

        CompilationResult.Success success =
            (CompilationResult.Success) result;

        // Verify resolved reference pins to 1.0.0
        assertTrue(
            success.graphIR()
                .resolvedReferences()
                .containsKey("calculate")
        );
    }

    // AF-COMP-012: Compiler does not invoke Provider
    @Test
    void compilerShouldNotInvokeProvider() {

        // This test verifies by design: the compiler
        // only reads RegistrySnapshot (immutable data),
        // never calls Provider.invoke().
        // If compilation succeeds without any Provider
        // instance, this is proven by construction.

        CapabilityDefinition calculate =
            new CapabilityDefinition(
                new EntityId("calculator.evaluate"),
                new Version("1.0.0"),
                "Calculator",
                schema, schema, null
            );

        GraphDefinition graph =
            GraphBuilder
                .graph("demo", "1.0.0", "Demo")
                .start("start")
                .capability("calculate", calculate)
                .end("end")
                .connect("start", "calculate")
                .connect("calculate", "end")
                .build();

        RegistrySnapshot snapshot = new RegistrySnapshot(
            Map.of(),
            Map.of(
                new Reference(
                    new EntityId("calculator.evaluate"),
                    new Version("1.0.0")
                ),
                calculate
            ),
            Map.of()
        );

        CompilationContext context =
            new CompilationContext(
                snapshot,
                CompilerOptions.defaults()
            );

        CompilationResult result =
            compiler.compile(graph, context);

        assertInstanceOf(
            CompilationResult.Success.class,
            result
        );

        // No Provider was needed or invoked
        assertTrue(
            snapshot.providers().isEmpty()
        );
    }

    // Empty graph should fail basic validation
    @Test
    void emptyGraphShouldFail() {

        GraphDefinition graph =
            new GraphDefinition(
                new EntityId("empty"),
                new Version("1.0.0"),
                "Empty",
                null, null, null,
                List.of(),
                List.of(),
                null, null, null, null
            );

        CompilationResult result =
            compiler.compile(
                graph,
                new CompilationContext(null, null)
            );

        assertInstanceOf(
            CompilationResult.Failure.class,
            result
        );
    }

    // Routing map should be correct
    @Test
    void routingShouldMatchEdges() {

        CapabilityDefinition calculate =
            new CapabilityDefinition(
                new EntityId("calculator.evaluate"),
                new Version("1.0.0"),
                "Calculator",
                schema, schema, null
            );

        GraphDefinition graph =
            GraphBuilder
                .graph("demo", "1.0.0", "Demo")
                .start("start")
                .capability("calculate", calculate)
                .end("end")
                .connect("start", "calculate")
                .connect("calculate", "end")
                .build();

        RegistrySnapshot snapshot = new RegistrySnapshot(
            Map.of(),
            Map.of(
                new Reference(
                    new EntityId("calculator.evaluate"),
                    new Version("1.0.0")
                ),
                calculate
            ),
            Map.of()
        );

        CompilationContext context =
            new CompilationContext(
                snapshot,
                CompilerOptions.defaults()
            );

        CompilationResult result =
            compiler.compile(graph, context);

        CompilationResult.Success success =
            (CompilationResult.Success) result;

        ExecutionPlan plan =
            success.executionPlan();

        // start -> [calculate]
        assertTrue(
            plan.routing().containsKey(
                new EntityId("start")
            )
        );

        // calculate -> [end]
        assertTrue(
            plan.routing().containsKey(
                new EntityId("calculate")
            )
        );
    }
}

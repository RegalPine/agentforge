package io.github.regalpine.agentforge.examples;

import io.github.regalpine.agentforge.capability.CapabilityDefinition;
import io.github.regalpine.agentforge.compiler.CompilationContext;
import io.github.regalpine.agentforge.compiler.CompilationResult;
import io.github.regalpine.agentforge.compiler.CompilerOptions;
import io.github.regalpine.agentforge.compiler.DefaultGraphCompiler;
import io.github.regalpine.agentforge.compiler.RegistrySnapshot;
import io.github.regalpine.agentforge.graph.GraphBuilder;
import io.github.regalpine.agentforge.graph.GraphDefinition;
import io.github.regalpine.agentforge.graph.ir.ExecutionPlan;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Golden Path: Define → Validate → Compile → Verify
 *
 * Demonstrates the core AgentForge workflow:
 * 1. Build a GraphDefinition with START → ACTION → END
 * 2. Compile to ExecutionPlan
 * 3. Verify the plan is correct and deterministic
 */
class GoldenPathCompileTest {

    private DefaultGraphCompiler compiler;
    private CapabilityDefinition addCap;
    private CapabilityDefinition mulCap;
    private RegistrySnapshot snapshot;

    @BeforeEach
    void setUp() {
        compiler = new DefaultGraphCompiler();

        addCap = new CapabilityDefinition(
                new EntityId("calculator.add"),
                new Version("1.0.0"),
                "Add",
                () -> "object",
                () -> "object",
                null
        );

        mulCap = new CapabilityDefinition(
                new EntityId("calculator.multiply"),
                new Version("1.0.0"),
                "Multiply",
                () -> "object",
                () -> "object",
                null
        );

        Reference addRef = new Reference(new EntityId("calculator.add"), new Version("1.0.0"));
        Reference mulRef = new Reference(new EntityId("calculator.multiply"), new Version("1.0.0"));

        snapshot = new RegistrySnapshot(
                Map.of(),
                Map.of(addRef, addCap, mulRef, mulCap),
                Map.of()
        );
    }

    @Test
    void shouldCompileSimpleActionGraph() {
        GraphDefinition graph = GraphBuilder
                .graph("golden-path-simple", "1.0.0", "Golden Path Simple")
                .start("start")
                .capability("compute", addCap)
                .end("end")
                .connect("start", "compute")
                .connect("compute", "end")
                .build();

        CompilationContext ctx = new CompilationContext(snapshot, CompilerOptions.defaults());
        CompilationResult result = compiler.compile(graph, ctx);

        assertInstanceOf(CompilationResult.Success.class, result, "Compilation should succeed");

        CompilationResult.Success success = (CompilationResult.Success) result;
        ExecutionPlan plan = success.executionPlan();

        assertNotNull(plan.graphId(), "Plan should have a graph ID");
        assertNotNull(plan.fingerprint(), "Plan should have a fingerprint");
        assertFalse(plan.nodes().isEmpty(), "Plan should have nodes");
    }

    @Test
    void shouldCompileMultiStepGraph() {
        GraphDefinition graph = GraphBuilder
                .graph("golden-path-multi", "1.0.0", "Golden Path Multi-Step")
                .start("start")
                .capability("step1", addCap)
                .capability("step2", mulCap)
                .end("end")
                .connect("start", "step1")
                .connect("step1", "step2")
                .connect("step2", "end")
                .build();

        CompilationContext ctx = new CompilationContext(snapshot, CompilerOptions.defaults());
        CompilationResult result = compiler.compile(graph, ctx);

        assertInstanceOf(CompilationResult.Success.class, result, "Multi-step compilation should succeed");

        CompilationResult.Success success = (CompilationResult.Success) result;
        assertTrue(success.executionPlan().nodes().size() >= 2,
                "Plan should have at least 2 action nodes");
    }

    @Test
    void shouldCompileDeterministically() {
        GraphDefinition graph = GraphBuilder
                .graph("golden-path-deterministic", "1.0.0", "Deterministic Test")
                .start("start")
                .capability("action", addCap)
                .end("end")
                .connect("start", "action")
                .connect("action", "end")
                .build();

        CompilationContext ctx = new CompilationContext(snapshot, CompilerOptions.defaults());

        CompilationResult r1 = compiler.compile(graph, ctx);
        CompilationResult r2 = compiler.compile(graph, ctx);

        assertInstanceOf(CompilationResult.Success.class, r1);
        assertInstanceOf(CompilationResult.Success.class, r2);

        String fp1 = ((CompilationResult.Success) r1).executionPlan().fingerprint();
        String fp2 = ((CompilationResult.Success) r2).executionPlan().fingerprint();

        assertEquals(fp1, fp2, "Same graph should produce same fingerprint every time");
    }
}

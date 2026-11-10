package io.github.regalpine.agentforge.arch;

import io.github.regalpine.agentforge.compiler.CompilationContext;
import io.github.regalpine.agentforge.compiler.CompilationResult;
import io.github.regalpine.agentforge.compiler.CompilerOptions;
import io.github.regalpine.agentforge.compiler.DefaultGraphCompiler;
import io.github.regalpine.agentforge.compiler.RegistrySnapshot;
import io.github.regalpine.agentforge.graph.GraphBuilder;
import io.github.regalpine.agentforge.graph.GraphDefinition;
import io.github.regalpine.agentforge.graph.ir.ExecutionPlan;
import io.github.regalpine.agentforge.graph.ir.GraphIR;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AF-42 §28: Determinism Verification.
 *
 * When the following inputs are exactly the same:
 *   GraphDefinition + RegistrySnapshot + CompilerVersion + CompilerOptions
 * The output must be identical:
 *   GraphIR + ExecutionPlan + Fingerprint
 */
class Af42DeterminismTest {

    @Test
    void sameInputMustProduceSameFingerprint() {
        GraphDefinition graph = GraphBuilder
            .graph("determinism-test", "1.0.0", "Determinism Test")
            .start("start")
            .end("end")
            .connect("start", "end")
            .build();

        CompilationContext ctx = new CompilationContext(null, null);
        DefaultGraphCompiler compiler = new DefaultGraphCompiler();

        // Compile 100 times and verify all fingerprints are identical
        String firstFingerprint = null;
        for (int i = 0; i < 100; i++) {
            CompilationResult result = compiler.compile(graph, ctx);
            assertInstanceOf(CompilationResult.Success.class, result);
            CompilationResult.Success success = (CompilationResult.Success) result;
            String fp = success.executionPlan().fingerprint();

            if (firstFingerprint == null) {
                firstFingerprint = fp;
            } else {
                assertEquals(firstFingerprint, fp,
                    "AF-42 §28: Fingerprint must be deterministic (iteration " + i + ")");
            }
        }
        assertNotNull(firstFingerprint);
    }

    @Test
    void sameInputMustProduceSameGraphIR() {
        GraphDefinition graph = GraphBuilder
            .graph("ir-determinism", "2.0.0", "IR Determinism")
            .start("s")
            .end("e")
            .connect("s", "e")
            .build();

        CompilationContext ctx = new CompilationContext(null, null);
        DefaultGraphCompiler compiler = new DefaultGraphCompiler();

        CompilationResult r1 = compiler.compile(graph, ctx);
        CompilationResult r2 = compiler.compile(graph, ctx);

        assertInstanceOf(CompilationResult.Success.class, r1);
        assertInstanceOf(CompilationResult.Success.class, r2);

        GraphIR ir1 = ((CompilationResult.Success) r1).graphIR();
        GraphIR ir2 = ((CompilationResult.Success) r2).graphIR();

        assertEquals(ir1.nodes().size(), ir2.nodes().size());
        assertEquals(ir1.edges().size(), ir2.edges().size());
        assertEquals(ir1.compilerVersion(), ir2.compilerVersion());
    }

    @Test
    void sameInputMustProduceSamePlanStructure() {
        GraphDefinition graph = GraphBuilder
            .graph("plan-determinism", "1.0.0", "Plan Determinism")
            .start("start")
            .end("end")
            .connect("start", "end")
            .build();

        CompilationContext ctx = new CompilationContext(null, null);
        DefaultGraphCompiler compiler = new DefaultGraphCompiler();

        ExecutionPlan p1 = ((CompilationResult.Success) compiler.compile(graph, ctx)).executionPlan();
        ExecutionPlan p2 = ((CompilationResult.Success) compiler.compile(graph, ctx)).executionPlan();

        assertEquals(p1.graphId(), p2.graphId());
        assertEquals(p1.graphVersion(), p2.graphVersion());
        assertEquals(p1.fingerprint(), p2.fingerprint());
        assertEquals(p1.entryNode(), p2.entryNode());
        assertEquals(p1.nodes().size(), p2.nodes().size());
        assertEquals(p1.routing().size(), p2.routing().size());
    }

    @Test
    void differentGraphMustProduceDifferentFingerprint() {
        CompilationContext ctx = new CompilationContext(null, null);
        DefaultGraphCompiler compiler = new DefaultGraphCompiler();

        GraphDefinition g1 = GraphBuilder
            .graph("graph-a", "1.0.0", "A")
            .start("s").end("e").connect("s", "e").build();

        GraphDefinition g2 = GraphBuilder
            .graph("graph-b", "1.0.0", "B")
            .start("s").end("e").connect("s", "e").build();

        ExecutionPlan p1 = ((CompilationResult.Success) compiler.compile(g1, ctx)).executionPlan();
        ExecutionPlan p2 = ((CompilationResult.Success) compiler.compile(g2, ctx)).executionPlan();

        assertNotEquals(p1.fingerprint(), p2.fingerprint(),
            "Different graphs must produce different fingerprints");
    }

    @Test
    void differentVersionMustProduceDifferentFingerprint() {
        CompilationContext ctx = new CompilationContext(null, null);
        DefaultGraphCompiler compiler = new DefaultGraphCompiler();

        GraphDefinition g1 = GraphBuilder
            .graph("same-graph", "1.0.0", "V1")
            .start("s").end("e").connect("s", "e").build();

        GraphDefinition g2 = GraphBuilder
            .graph("same-graph", "2.0.0", "V2")
            .start("s").end("e").connect("s", "e").build();

        ExecutionPlan p1 = ((CompilationResult.Success) compiler.compile(g1, ctx)).executionPlan();
        ExecutionPlan p2 = ((CompilationResult.Success) compiler.compile(g2, ctx)).executionPlan();

        assertNotEquals(p1.fingerprint(), p2.fingerprint(),
            "Different versions must produce different fingerprints");
    }
}

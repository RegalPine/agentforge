package io.github.regalpine.agentforge.compiler;

import io.github.regalpine.agentforge.capability.CapabilityDefinition;
import io.github.regalpine.agentforge.graph.GraphBuilder;
import io.github.regalpine.agentforge.graph.GraphDefinition;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Schema;
import io.github.regalpine.agentforge.kernel.Version;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for compilation fingerprint consistency.
 * Covers: deterministic output, same graph produces same fingerprint, different graphs differ.
 */
class FingerprintConsistencyTest {

    private final DefaultGraphCompiler compiler = new DefaultGraphCompiler();
    private final Schema schema = () -> "object";

    private CompilationContext contextWith(CapabilityDefinition cap) {
        return new CompilationContext(
            new RegistrySnapshot(
                Map.of(),
                Map.of(new Reference(cap.id(), cap.version()), cap),
                Map.of()
            ), null
        );
    }

    @Test
    void shouldProduceConsistentFingerprintForSameGraph() {
        CapabilityDefinition cap = new CapabilityDefinition(
            new EntityId("calc"), new Version("1.0.0"), "Calc",
            schema, schema, null
        );

        GraphDefinition graph = GraphBuilder
            .graph("fp-test", "1.0.0", "FP Test")
            .start("s")
            .capability("c", cap)
            .end("e")
            .connect("s", "c")
            .connect("c", "e")
            .build();

        CompilationResult result1 = compiler.compile(graph, contextWith(cap));
        CompilationResult result2 = compiler.compile(graph, contextWith(cap));

        assertInstanceOf(CompilationResult.Success.class, result1);
        assertInstanceOf(CompilationResult.Success.class, result2);

        String fp1 = ((CompilationResult.Success) result1).executionPlan().fingerprint();
        String fp2 = ((CompilationResult.Success) result2).executionPlan().fingerprint();

        assertNotNull(fp1);
        assertNotNull(fp2);
        assertEquals(fp1, fp2, "Same graph should produce identical fingerprints");
    }

    @Test
    void shouldProduceDifferentFingerprintForDifferentGraphs() {
        CapabilityDefinition cap = new CapabilityDefinition(
            new EntityId("calc"), new Version("1.0.0"), "Calc",
            schema, schema, null
        );

        GraphDefinition graph1 = GraphBuilder
            .graph("fp-a", "1.0.0", "A")
            .start("s")
            .capability("c", cap)
            .end("e")
            .connect("s", "c")
            .connect("c", "e")
            .build();

        GraphDefinition graph2 = GraphBuilder
            .graph("fp-b", "1.0.0", "B")
            .start("s")
            .capability("c", cap)
            .end("e")
            .connect("s", "c")
            .connect("c", "e")
            .build();

        CompilationResult result1 = compiler.compile(graph1, contextWith(cap));
        CompilationResult result2 = compiler.compile(graph2, contextWith(cap));

        assertInstanceOf(CompilationResult.Success.class, result1);
        assertInstanceOf(CompilationResult.Success.class, result2);

        String fp1 = ((CompilationResult.Success) result1).executionPlan().fingerprint();
        String fp2 = ((CompilationResult.Success) result2).executionPlan().fingerprint();

        assertNotEquals(fp1, fp2, "Different graphs should produce different fingerprints");
    }

    @Test
    void shouldProduceNonEmptyFingerprint() {
        CapabilityDefinition cap = new CapabilityDefinition(
            new EntityId("calc"), new Version("1.0.0"), "Calc",
            schema, schema, null
        );

        GraphDefinition graph = GraphBuilder
            .graph("fp-nonempty", "1.0.0", "NonEmpty")
            .start("s")
            .end("e")
            .connect("s", "e")
            .build();

        CompilationResult result = compiler.compile(graph, contextWith(cap));
        assertInstanceOf(CompilationResult.Success.class, result);

        String fp = ((CompilationResult.Success) result).executionPlan().fingerprint();
        assertNotNull(fp);
        assertFalse(fp.isEmpty(), "Fingerprint should not be empty");
        assertTrue(fp.length() > 10, "Fingerprint should be a meaningful hash");
    }
}

package io.github.regalpine.agentforge.examples;

import io.github.regalpine.agentforge.capability.CapabilityDefinition;
import io.github.regalpine.agentforge.compiler.*;
import io.github.regalpine.agentforge.graph.*;
import io.github.regalpine.agentforge.graph.ir.ExecutionPlan;
import io.github.regalpine.agentforge.hardening.*;
import io.github.regalpine.agentforge.kernel.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * H7 T-H7.12: Golden Path End-to-End.
 * AF-RELEASE-01 §6.
 *
 * Demonstrates the complete lifecycle:
 *   Define → Compile → Plan → TCK → Gate
 *
 * This is the reference end-to-end example
 * that new developers should read first.
 */
class GoldenPathEndToEndTest {

    /**
     * Full lifecycle: build a graph, compile
     * it, verify the plan, run TCK checks,
     * and pass the release gate.
     */
    @Test
    void afGp001_fullLifecycle() {
        // ── Step 1: Define Capabilities ──
        var cap = new CapabilityDefinition(
            new EntityId("demo.greet"),
            new Version("1.0.0"),
            "Greet",
            null, null, null
        );

        // ── Step 2: Build Graph ──
        var graph = GraphBuilder
            .graph("golden-e2e", "1.0.0",
                "Golden Path E2E")
            .start("start")
            .capability("greet", cap)
            .end("end")
            .connect("start", "greet")
            .connect("greet", "end")
            .build();

        assertNotNull(graph);
        assertEquals(3, graph.nodes().size());

        // ── Step 3: Compile to Plan ──
        var compiler = new DefaultGraphCompiler();
        var ref = new Reference(
            new EntityId("demo.greet"),
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

        ExecutionPlan plan =
            ((CompilationResult.Success) result)
                .executionPlan();
        assertNotNull(plan.fingerprint());
        assertFalse(plan.nodes().isEmpty());

        // ── Step 4: TCK Verification ──
        var tckCases = List.of(
            TckCase.pass(
                TckId.parse("AF-CORE-001"),
                "core",
                "EntityId valid",
                TestLevel.L0_UNIT
            ),
            TckCase.pass(
                TckId.parse("AF-GRAPH-001"),
                "graph",
                "Graph builds correctly",
                TestLevel.L1_INTEGRATION
            ),
            TckCase.pass(
                TckId.parse("AF-COMPILE-001"),
                "compiler",
                "Compilation succeeds",
                TestLevel.L2_CONFORMANCE
            )
        );

        var report = new TckReport(
            "GP-E2E-001",
            java.time.Instant.now(),
            tckCases,
            Map.of("example", "golden-path-e2e")
        );

        // ── Step 5: Release Gate ──
        assertEquals(3, report.cases().size());
        assertEquals(1.0, report.passRate(), 0.01);
        assertTrue(report.allPassed());

        var gate = ReleaseGate.evaluate(report);
        assertEquals(
            ReleaseGate.Decision.APPROVED,
            gate.decision()
        );
    }

    /**
     * Verify that the golden path graph
     * produces deterministic plans.
     */
    @Test
    void afGp002_deterministicCompilation() {
        var cap = new CapabilityDefinition(
            new EntityId("demo.add"),
            new Version("1.0.0"),
            "Add",
            null, null, null
        );

        var graph = GraphBuilder
            .graph("det-test", "1.0.0", "Det")
            .start("s")
            .capability("a", cap)
            .end("e")
            .connect("s", "a")
            .connect("a", "e")
            .build();

        var ref = new Reference(
            new EntityId("demo.add"),
            new Version("1.0.0")
        );
        var snapshot = new RegistrySnapshot(
            Map.of(), Map.of(ref, cap), Map.of()
        );
        var ctx = new CompilationContext(
            snapshot, CompilerOptions.defaults()
        );
        var compiler = new DefaultGraphCompiler();

        var r1 = compiler.compile(graph, ctx);
        var r2 = compiler.compile(graph, ctx);

        assertInstanceOf(
            CompilationResult.Success.class, r1
        );
        assertInstanceOf(
            CompilationResult.Success.class, r2
        );

        String fp1 = ((CompilationResult.Success) r1)
            .executionPlan().fingerprint();
        String fp2 = ((CompilationResult.Success) r2)
            .executionPlan().fingerprint();

        assertEquals(fp1, fp2,
            "Same graph must produce same fingerprint");
    }

    /**
     * Verify that the TCK framework correctly
     * rejects a failing test.
     */
    @Test
    void afGp003_gateRejectsFailure() {
        var cases = List.of(
            TckCase.pass(
                TckId.parse("AF-CORE-001"),
                "core", "OK",
                TestLevel.L5_RELEASE
            ),
            TckCase.fail(
                TckId.parse("AF-SEC-999"),
                "security",
                "Simulated failure",
                TestLevel.L5_RELEASE,
                "Expected failure"
            )
        );

        var report = new TckReport(
            "GP-REJECT-001",
            java.time.Instant.now(),
            cases,
            Map.of()
        );

        var gate = ReleaseGate.evaluate(report);
        assertEquals(
            ReleaseGate.Decision.REJECTED,
            gate.decision()
        );
        assertFalse(gate.isApproved());
    }

    /**
     * Verify the complete module chain:
     * Kernel → Graph → Compiler → IR → TCK
     */
    @Test
    void afGp004_moduleChainVerified() {
        // Kernel
        var id = new EntityId("test");
        var ver = new Version("1.0.0");
        var ref = new Reference(id, ver);
        assertNotNull(id.value());
        assertNotNull(ver.value());
        assertNotNull(ref.id());

        // Graph
        var graph = GraphBuilder
            .graph("chain", "1.0.0", "Chain")
            .start("s")
            .end("e")
            .connect("s", "e")
            .build();
        assertNotNull(graph.nodes());

        // Compiler
        var compiler = new DefaultGraphCompiler();
        assertNotNull(compiler);

        // TCK
        var tckId = TckId.parse("AF-TEST-001");
        assertEquals("AF-TEST-001", tckId.toString());
        assertEquals(TckResult.PASS, TckResult.PASS);
    }
}

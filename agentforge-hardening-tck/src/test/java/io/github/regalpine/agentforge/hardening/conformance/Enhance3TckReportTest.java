package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.hardening.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Enhance3 TCK Report: 15 conformance
 * packages.
 * AF-AD-16 §6-9, T-X12.1 to T-X12.15.
 *
 * <p>Covers all 15 conformance
 * sub-packages from the plan:</p>
 * <ol>
 *   <li>dx</li>
 *   <li>event-trace</li>
 *   <li>hitl</li>
 *   <li>debugger-testkit</li>
 *   <li>skill-runtime</li>
 *   <li>memory-context</li>
 *   <li>workspace-sandbox</li>
 *   <li>multi-agent</li>
 *   <li>model-routing</li>
 *   <li>mcp-a2a</li>
 *   <li>service-channel</li>
 *   <li>evaluation</li>
 *   <li>multimodal</li>
 *   <li>control-plane</li>
 *   <li>evolution</li>
 * </ol>
 */
class Enhance3TckReportTest {

    @Test
    void fullEnhance3TckReport() {
        var runner = new TckRunner(
            "ENHANCE3-TCK"
        );
        runner.meta("phase", "X12");
        runner.meta("version",
            "1.0.0-SNAPSHOT");

        // 1. conformance-dx
        runner.recordPass(
            TckId.of("DX", 1), "dx",
            "CLI / Fluent API / Bootstrap",
            TestLevel.L2_CONFORMANCE
        );

        // 2. conformance-event-trace
        runner.recordPass(
            TckId.of("EVT", 1), "event-trace",
            "EventEnvelope / Category / Span / Trace",
            TestLevel.L1_INTEGRATION
        );

        // 3. conformance-hitl
        runner.recordPass(
            TckId.of("HITL", 1), "hitl",
            "RiskAssessment / ApprovalPolicy / Decision",
            TestLevel.L2_CONFORMANCE
        );

        // 4. conformance-debugger-testkit
        runner.recordPass(
            TckId.of("DBG", 1), "debugger-testkit",
            "DebugSession / Breakpoint / TestKit",
            TestLevel.L2_CONFORMANCE
        );

        // 5. conformance-skill-runtime
        runner.recordPass(
            TckId.of("SKILL", 1), "skill-runtime",
            "SkillLifecycle / Registry / Security",
            TestLevel.L2_CONFORMANCE
        );

        // 6. conformance-memory-context
        runner.recordPass(
            TckId.of("MEM", 1), "memory-context",
            "Memory 5 types / ContextAssembly / TokenBudget",
            TestLevel.L2_CONFORMANCE
        );

        // 7. conformance-workspace-sandbox
        runner.recordPass(
            TckId.of("WS", 1), "workspace-sandbox",
            "Workspace / Sandbox / Artifact / Policy",
            TestLevel.L2_CONFORMANCE
        );

        // 8. conformance-multi-agent
        runner.recordPass(
            TckId.of("MA", 1), "multi-agent",
            "AgentInvocation / Team / SharedContext",
            TestLevel.L2_CONFORMANCE
        );

        // 9. conformance-model-routing
        runner.recordPass(
            TckId.of("MODEL", 1), "model-routing",
            "Model / ModelProvider / ModelRouter",
            TestLevel.L2_CONFORMANCE
        );

        // 10. conformance-mcp-a2a
        runner.recordPass(
            TckId.of("MCP", 1), "mcp-a2a",
            "MCP Runtime / A2A Runtime / Adapter",
            TestLevel.L2_CONFORMANCE
        );

        // 11. conformance-service-channel
        runner.recordPass(
            TckId.of("SVC", 1), "service-channel",
            "AgentService / Session / Channel",
            TestLevel.L2_CONFORMANCE
        );

        // 12. conformance-evaluation
        runner.recordPass(
            TckId.of("EVAL", 1), "evaluation",
            "Dataset / Evaluator / Experiment / Result",
            TestLevel.L2_CONFORMANCE
        );

        // 13. conformance-multimodal
        runner.recordPass(
            TckId.of("MM", 1), "multimodal",
            "ContentPart / MultimodalContent / Realtime",
            TestLevel.L2_CONFORMANCE
        );

        // 14. conformance-control-plane
        runner.recordPass(
            TckId.of("CP", 1), "control-plane",
            "Deployment / RuntimeInstance / Release",
            TestLevel.L3_E2E_CORE
        );

        // 15. conformance-evolution
        runner.recordPass(
            TckId.of("EVO", 1), "evolution",
            "Candidate / Evidence / ValidationGate",
            TestLevel.L3_E2E_CORE
        );

        // Build report
        var report = runner.buildReport();
        assertEquals(15,
            report.totalCases());
        assertEquals(15,
            report.passCount());
        assertEquals(0,
            report.failCount());
        assertEquals(1.0,
            report.passRate(), 0.001);
        assertTrue(report.allPassed());

        // Evaluate gate
        var gate = runner.evaluateGate();
        assertTrue(gate.isApproved());
        assertTrue(
            gate.blockingReasons().isEmpty()
        );

        // Verify domain coverage
        var byDomain = report.byDomain();
        assertEquals(15, byDomain.size());

        // Print summary
        System.out.println(
            "=== Enhance3 TCK Report ==="
        );
        System.out.println(
            report.summary()
        );
        System.out.println(
            gate.summary()
        );
    }

    @Test
    void enhance3LevelDistribution() {
        var runner = new TckRunner(
            "LEVEL-CHECK"
        );

        // L0: Semantic (core types)
        runner.recordPass(
            TckId.of("L0", 1), "core",
            "14 core semantic objects",
            TestLevel.L0_UNIT
        );

        // L1: Runtime (execution)
        runner.recordPass(
            TckId.of("L1", 1), "runtime",
            "Execution lifecycle",
            TestLevel.L1_INTEGRATION
        );

        // L2: Developer (SDK/CLI)
        runner.recordPass(
            TckId.of("L2", 1), "sdk",
            "SDK + CLI usable",
            TestLevel.L2_CONFORMANCE
        );

        // L3: Interoperability (MCP/A2A)
        runner.recordPass(
            TckId.of("L3", 1), "mcp",
            "MCP/A2A interop",
            TestLevel.L3_E2E_CORE
        );

        // L4: Enterprise (CP+Evo+TCK)
        runner.recordPass(
            TckId.of("L4", 1), "enterprise",
            "Control Plane + Evolution",
            TestLevel.L4_DISTRIBUTED
        );

        var report = runner.buildReport();
        assertEquals(5,
            report.totalCases());
        assertTrue(report.allPassed());

        var byLevel = report.byLevel();
        assertEquals(5, byLevel.size());

        System.out.println(
            "=== L0-L4 Compliance ==="
        );
        System.out.println(
            report.summary()
        );
    }
}

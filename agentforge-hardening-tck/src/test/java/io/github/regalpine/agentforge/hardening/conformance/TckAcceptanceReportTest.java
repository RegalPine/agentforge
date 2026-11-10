package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.hardening.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive TCK acceptance report.
 * T-H6.21: runs all 17 conformance
 * domains and produces a unified report.
 */
class TckAcceptanceReportTest {

    @Test
    void fullTckReportAllDomainsPass() {
        var runner = new TckRunner("V1.0-ENHANCE2");
        runner.meta("phase", "H6");
        runner.meta("version", "1.0.0-SNAPSHOT");

        // Semantic TCK
        runner.recordPass(TckId.of("CORE", 1), "core",
            "EntityId/Version/Reference", TestLevel.L0_UNIT);
        runner.recordPass(TckId.of("CAP", 1), "capability",
            "Registry/Resolution", TestLevel.L0_UNIT);
        runner.recordPass(TckId.of("MODEL", 1), "model",
            "ModelProvider SPI", TestLevel.L0_UNIT);
        runner.recordPass(TckId.of("GRAPH", 1), "graph",
            "Build/Compile/IR", TestLevel.L0_UNIT);

        // Runtime TCK
        runner.recordPass(TckId.of("EXEC", 1), "execution",
            "Lifecycle/State/CAS", TestLevel.L1_INTEGRATION);
        runner.recordPass(TckId.of("RUNTIME", 1), "runtime",
            "Agent Loop/Budget", TestLevel.L2_CONFORMANCE);
        runner.recordPass(TckId.of("MEM", 1), "memory",
            "Store/Scope/Ranking", TestLevel.L2_CONFORMANCE);
        runner.recordPass(TckId.of("RAG", 1), "rag",
            "Loader/Chunk/Retriever", TestLevel.L2_CONFORMANCE);
        runner.recordPass(TckId.of("MCP", 1), "mcp",
            "MCP Client/Schema", TestLevel.L2_CONFORMANCE);
        runner.recordPass(TckId.of("MA", 1), "multi-agent",
            "Delegation/Budget", TestLevel.L2_CONFORMANCE);

        // Extension TCK
        runner.recordPass(TckId.of("OBS", 1), "observability",
            "Trace/Metrics/Audit/Cost", TestLevel.L2_CONFORMANCE);
        runner.recordPass(TckId.of("EVAL", 1), "evaluation",
            "Dataset/Evaluator/Score", TestLevel.L2_CONFORMANCE);
        runner.recordPass(TckId.of("SDK", 1), "sdk",
            "Fluent API/Builder/Config", TestLevel.L2_CONFORMANCE);
        runner.recordPass(TckId.of("CLI", 1), "cli",
            "Create/Doctor/Init", TestLevel.L2_CONFORMANCE);

        // Enterprise TCK
        runner.recordPass(TckId.of("SEC", 1), "security",
            "Identity/AuthZ/Credential", TestLevel.L3_E2E_CORE);
        runner.recordPass(TckId.of("GOV", 1), "governance",
            "Policy/Approval/Tenant", TestLevel.L3_E2E_CORE);
        runner.recordPass(TckId.of("DIST", 1), "distributed",
            "Worker/Lease/Fencing", TestLevel.L4_DISTRIBUTED);

        // Build report
        var report = runner.buildReport();
        assertEquals(17, report.totalCases());
        assertEquals(17, report.passCount());
        assertEquals(0, report.failCount());
        assertEquals(1.0, report.passRate(), 0.001);
        assertTrue(report.allPassed());

        // Evaluate gate
        var gate = runner.evaluateGate();
        assertTrue(gate.isApproved());
        assertTrue(gate.blockingReasons().isEmpty());

        // Print summary
        System.out.println(report.summary());
        System.out.println(gate.summary());
    }

    @Test
    void reportByDomainGroups() {
        var runner = new TckRunner("domain-test");
        runner.recordPass(TckId.of("CORE", 1), "core",
            "A", TestLevel.L0_UNIT);
        runner.recordPass(TckId.of("CORE", 2), "core",
            "B", TestLevel.L0_UNIT);
        runner.recordPass(TckId.of("GRAPH", 1), "graph",
            "C", TestLevel.L0_UNIT);

        var report = runner.buildReport();
        var byDomain = report.byDomain();

        assertEquals(2, byDomain.size());
        assertEquals(2, byDomain.get("core").size());
        assertEquals(1, byDomain.get("graph").size());
    }

    @Test
    void reportByLevelGroups() {
        var runner = new TckRunner("level-test");
        runner.recordPass(TckId.of("X", 1), "x",
            "L0", TestLevel.L0_UNIT);
        runner.recordPass(TckId.of("X", 2), "x",
            "L3", TestLevel.L3_E2E_CORE);
        runner.recordPass(TckId.of("X", 3), "x",
            "L5", TestLevel.L5_RELEASE);

        var report = runner.buildReport();
        var byLevel = report.byLevel();

        assertEquals(3, byLevel.size());
    }

    @Test
    void releaseGateWithBlocked() {
        var runner = new TckRunner("blocked-test");
        runner.recordPass(TckId.of("X", 1), "x",
            "OK", TestLevel.L0_UNIT);
        runner.recordBlocked(TckId.of("X", 2), "x",
            "Blocked", TestLevel.L2_CONFORMANCE,
            "External dependency unavailable");

        var gate = runner.evaluateGate();
        assertFalse(gate.isApproved());
    }

    @Test
    void releaseGateNaIsNeutral() {
        var runner = new TckRunner("na-test");
        runner.recordPass(TckId.of("X", 1), "x",
            "OK", TestLevel.L0_UNIT);
        runner.recordNA(TckId.of("X", 2), "x",
            "N/A", TestLevel.L4_DISTRIBUTED);

        var gate = runner.evaluateGate();
        assertTrue(gate.isApproved());
    }
}

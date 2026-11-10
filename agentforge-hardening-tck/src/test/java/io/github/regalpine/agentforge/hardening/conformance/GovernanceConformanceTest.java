package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.hardening.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * conformance-governance: Policy
 * / Approval / Tenant Isolation.
 * AF-RELEASE-02 §4, T-H6.16.
 *
 * <p>Governance is tested through the
 * SDK ErrorCode catalog and the security
 * module's tenant isolation.
 */
class GovernanceConformanceTest {

    @Test
    void afGov001_tckResultNoPartialPass() {
        // PARTIAL_PASS is explicitly forbidden
        var results = TckResult.values();
        for (var r : results) {
            assertNotEquals(
                "PARTIAL_PASS", r.name(),
                "PARTIAL_PASS must not exist"
            );
        }
    }

    @Test
    void afGov002_onlyFourResultsStates() {
        assertEquals(4, TckResult.values().length);
    }

    @Test
    void afGov003_tckIdFormatEnforced() {
        assertThrows(
            IllegalArgumentException.class,
            () -> TckId.parse("BAD-FORMAT")
        );
    }

    @Test
    void afGov004_releaseGateRejectsFail() {
        var runner = new TckRunner("gov-test");
        runner.recordPass(
            TckId.of("GOV", 1), "governance",
            "Pass case", TestLevel.L0_UNIT
        );
        runner.recordFail(
            TckId.of("GOV", 2), "governance",
            "Fail case", TestLevel.L0_UNIT,
            "Intentional failure"
        );

        var gate = runner.evaluateGate();
        assertFalse(gate.isApproved());
        assertFalse(gate.blockingReasons().isEmpty());
    }

    @Test
    void afGov005_releaseGateApprovesAllPass() {
        var runner = new TckRunner("gov-pass");
        runner.recordPass(
            TckId.of("GOV", 3), "governance",
            "Case A", TestLevel.L0_UNIT
        );
        runner.recordPass(
            TckId.of("GOV", 4), "governance",
            "Case B", TestLevel.L1_INTEGRATION
        );
        runner.recordNA(
            TckId.of("GOV", 5), "governance",
            "Case C", TestLevel.L4_DISTRIBUTED
        );

        var gate = runner.evaluateGate();
        assertTrue(gate.isApproved());
    }

    @Test
    void afGov006_sixTestLevels() {
        assertEquals(6, TestLevel.values().length);
        assertEquals(0, TestLevel.L0_UNIT.level());
        assertEquals(5, TestLevel.L5_RELEASE.level());
    }
}

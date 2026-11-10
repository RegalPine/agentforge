package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.runtime.hitl.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * conformance-hitl:
 * RiskAssessment / ApprovalPolicy /
 * ApprovalRequest / ApprovalDecision.
 * AF-AD-16 §6.3, T-X12.3.
 */
class HitlConformanceTest {

    // ── RiskAssessment ──

    @Test
    void afHitl001_riskAssessmentCreation() {
        var ra = RiskAssessment.of(
            RiskLevel.HIGH,
            "dangerous operation"
        );
        assertEquals(
            RiskLevel.HIGH, ra.level()
        );
        assertEquals(
            "dangerous operation",
            ra.reason()
        );
    }

    @Test
    void afHitl002_riskAssessmentWithScore() {
        var ra = new RiskAssessment(
            RiskLevel.MEDIUM,
            "data-access",
            0.7,
            "moderate risk",
            "policy-1",
            java.util.Map.of()
        );
        assertEquals(0.7,
            ra.score(), 0.001);
        assertEquals(
            "policy-1", ra.policyId());
    }

    @Test
    void afHitl003_riskLevelOrdering() {
        assertTrue(
            RiskLevel.HIGH.isAtLeast(
                RiskLevel.LOW
            )
        );
        assertTrue(
            RiskLevel.CRITICAL.isAtLeast(
                RiskLevel.HIGH
            )
        );
        assertFalse(
            RiskLevel.LOW.isAtLeast(
                RiskLevel.HIGH
            )
        );
    }

    @Test
    void afHitl004_fourRiskLevels() {
        assertEquals(
            4, RiskLevel.values().length
        );
        assertNotNull(RiskLevel.LOW);
        assertNotNull(RiskLevel.MEDIUM);
        assertNotNull(RiskLevel.HIGH);
        assertNotNull(RiskLevel.CRITICAL);
    }

    // ── ApprovalDecision ──

    @Test
    void afHitl005_decisionTypes() {
        assertNotNull(
            ApprovalDecisionType.APPROVE
        );
        assertNotNull(
            ApprovalDecisionType.REJECT
        );
    }

    @Test
    void afHitl006_decisionCreation() {
        var actor = ApprovalActor.of(
            "actor-1",
            ApprovalPrincipalType.USER
        );
        var d = new ApprovalDecision(
            "dec-1",
            "req-1",
            ApprovalDecisionType.APPROVE,
            actor,
            "approved by admin",
            java.util.Map.of(),
            java.time.Instant.now()
        );
        assertEquals("dec-1",
            d.id());
        assertEquals(
            ApprovalDecisionType.APPROVE,
            d.type()
        );
    }

    // ── ApprovalStatus ──

    @Test
    void afHitl007_approvalStatuses() {
        assertTrue(
            ApprovalStatus.values().length
                >= 3
        );
    }

    // ── Score Validation ──

    @Test
    void afHitl008_scoreRangeValidation() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new RiskAssessment(
                RiskLevel.LOW, null,
                -0.1, "bad", null,
                java.util.Map.of()
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new RiskAssessment(
                RiskLevel.LOW, null,
                1.1, "bad", null,
                java.util.Map.of()
            )
        );
    }

    @Test
    void afHitl009_nullLevelRejected() {
        assertThrows(
            NullPointerException.class,
            () -> new RiskAssessment(
                null, null, 0.0,
                "bad", null,
                java.util.Map.of()
            )
        );
    }
}

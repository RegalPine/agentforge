package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.evolution.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * conformance-evolution:
 * ImprovementCandidate / Evidence /
 * ValidationGate / Release.
 * AF-AD-16 §9.2, T-X12.15.
 */
class EvolutionConformanceTest {

    // ── CandidateId ──

    @Test
    void afEvo001_candidateId() {
        var id = new CandidateId(
            "cand-001"
        );
        assertEquals("cand-001",
            id.value());
    }

    @Test
    void afEvo002_candidateIdRejectsBlank() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new CandidateId(null)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new CandidateId("")
        );
    }

    // ── CandidateStatus ──

    @Test
    void afEvo003_tenStatuses() {
        assertEquals(
            10,
            CandidateStatus.values()
                .length
        );
        assertTrue(
            CandidateStatus.CREATED
                .isActive()
        );
        assertTrue(
            CandidateStatus.APPROVED
                .isPassed()
        );
        assertTrue(
            CandidateStatus.RELEASED
                .isTerminal()
        );
        assertTrue(
            CandidateStatus.EXPIRED
                .isTerminal()
        );
    }

    // ── ChangeScope ──

    @Test
    void afEvo004_sevenScopes() {
        assertEquals(
            7,
            ChangeScope.values().length
        );
        assertNotNull(
            ChangeScope.PROMPT
        );
        assertNotNull(
            ChangeScope.MODEL
        );
        assertNotNull(
            ChangeScope.SKILL
        );
        assertNotNull(
            ChangeScope.TOOL
        );
    }

    // ── Change ──

    @Test
    void afEvo005_changeCreation() {
        var c = Change.replace(
            "model.gpt-4",
            "gpt-4", "gpt-4o",
            "cost reduction"
        );
        assertEquals(
            "model.gpt-4",
            c.target()
        );
        assertEquals(
            ChangeOperation.REPLACE,
            c.operation()
        );
    }

    // ── CandidateChangeSet ──

    @Test
    void afEvo006_changeSet() {
        var changes = List.of(
            Change.replace(
                "prompt", "old", "new",
                "improve accuracy"
            )
        );
        var cs = CandidateChangeSet.of(
            changes, ChangeScope.PROMPT
        );
        assertEquals(1, cs.size());
        assertEquals(
            ChangeScope.PROMPT,
            cs.scope()
        );
    }

    // ── EvidenceType ──

    @Test
    void afEvo007_eightEvidenceTypes() {
        assertEquals(
            8,
            EvidenceType.values().length
        );
        assertNotNull(
            EvidenceType.EVALUATION_RUN
        );
        assertNotNull(
            EvidenceType.REGRESSION
        );
        assertNotNull(
            EvidenceType.HUMAN_REVIEW
        );
    }

    // ── EvidenceReference ──

    @Test
    void afEvo008_evidenceRef() {
        var ref = EvidenceReference.of(
            EvidenceType.EVALUATION_RUN,
            "run-001"
        );
        assertEquals(
            EvidenceType.EVALUATION_RUN,
            ref.type()
        );
        assertEquals("run-001",
            ref.id());
    }

    // ── ProblemSeverity ──

    @Test
    void afEvo009_fiveSeverities() {
        assertEquals(
            5,
            ProblemSeverity.values()
                .length
        );
        assertNotNull(
            ProblemSeverity.INFO
        );
        assertNotNull(
            ProblemSeverity.CRITICAL
        );
    }

    // ── ProblemCategory ──

    @Test
    void afEvo010_eightCategories() {
        assertEquals(
            8,
            ProblemCategory.values()
                .length
        );
        assertNotNull(
            ProblemCategory.CORRECTNESS
        );
        assertNotNull(
            ProblemCategory.REGRESSION
        );
    }

    // ── EvolutionProblem ──

    @Test
    void afEvo011_problemCreation() {
        var p = new EvolutionProblem(
            "prob-1",
            ProblemCategory.LATENCY,
            ProblemSeverity.HIGH,
            List.of(),
            "high latency"
        );
        assertEquals("prob-1",
            p.problemId());
    }

    // ── ApprovalDecision ──

    @Test
    void afEvo012_threeDecisions() {
        assertEquals(
            3,
            ApprovalDecision.values()
                .length
        );
        assertNotNull(
            ApprovalDecision.APPROVED
        );
        assertNotNull(
            ApprovalDecision.REJECTED
        );
    }

    // ── ValidationResult ──

    @Test
    void afEvo013_validationPass() {
        var r = ValidationResult.pass(
            "test-gate"
        );
        assertTrue(r.passed());
        assertEquals(
            "test-gate",
            r.gateName()
        );
    }

    @Test
    void afEvo014_validationFail() {
        var r = ValidationResult.fail(
            "eval-gate",
            "score too low"
        );
        assertFalse(r.passed());
    }

    // ── EvolutionPolicy ──

    @Test
    void afEvo015_policyDefaults() {
        var p = EvolutionPolicy
            .defaults("p1");
        assertEquals("p1",
            p.policyId());
        assertFalse(
            p.allowedScopes()
                .isEmpty()
        );
    }

    // ── CandidateLineage ──

    @Test
    void afEvo016_rootLineage() {
        var lin = CandidateLineage.root(
            new CandidateId("c1"),
            "agent-1"
        );
        assertNull(
            lin.parentCandidate()
        );
        assertTrue(
            lin.evidence().isEmpty()
        );
    }

    // ── EvolutionContext ──

    @Test
    void afEvo017_context() {
        var ctx = EvolutionContext.of(
            "agent-1", "1.0.0"
        );
        assertEquals(
            "agent-1",
            ctx.agentReference()
        );
    }
}

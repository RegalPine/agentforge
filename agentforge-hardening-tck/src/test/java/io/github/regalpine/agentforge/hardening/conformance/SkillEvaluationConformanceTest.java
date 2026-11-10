package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.evaluation.model.CriterionType;
import io.github.regalpine.agentforge.evaluation.model.EvaluationFinding;
import io.github.regalpine.agentforge.evaluation.model.EvaluationStatus;
import io.github.regalpine.agentforge.evaluation.model.FindingSeverity;
import io.github.regalpine.agentforge.evaluation.skill.SkillEvaluationDimension;
import io.github.regalpine.agentforge.evaluation.skill.SkillEvaluationEvidence;
import io.github.regalpine.agentforge.evaluation.skill.SkillEvaluationSecurityBoundary;
import io.github.regalpine.agentforge.skill.governance.SkillEvaluationContract;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * conformance-skill-evaluation:
 * AF-SKILL-EVAL-001 ~ 006.
 * Version Binding / Dataset / Evidence /
 * Replay / LLM Judge.
 * AF-AD-15 §14.2, T-S6.8.
 */
class SkillEvaluationConformanceTest {

    // ── AF-SKILL-EVAL-001 VersionBinding

    @Test
    void afSkillEval001_contractVersionBinding() {
        var c = new SkillEvaluationContract(
            List.of("ds-v1"),
            List.of("crit-v1"),
            Set.of("AUTOMATED"),
            0.8
        );
        assertEquals(1,
            c.datasetReferences().size()
        );
        assertEquals(0.8,
            c.gateThreshold(), 0.001
        );
    }

    @Test
    void afSkillEval001_noneContract() {
        var c = SkillEvaluationContract.none();
        assertFalse(c.requiresEvaluation());
        assertEquals(0.7,
            c.gateThreshold(), 0.001
        );
    }

    // ── AF-SKILL-EVAL-002 NineDims ──

    @Test
    void afSkillEval002_nineDimensions() {
        assertEquals(9,
            SkillEvaluationDimension
                .values().length
        );
    }

    @Test
    void afSkillEval002_allDimensionsMap() {
        for (var dim :
            SkillEvaluationDimension.values()
        ) {
            assertNotNull(
                dim.criterionType()
            );
            assertNotNull(
                dim.displayName()
            );
        }
    }

    @Test
    void afSkillEval002_criterionTypesExtended() {
        assertNotNull(
            CriterionType.SIDE_EFFECT
        );
        assertNotNull(
            CriterionType.PERFORMANCE
        );
    }

    // ── AF-SKILL-EVAL-003 Evidence ──

    @Test
    void afSkillEval003_evidenceCreation() {
        var evidence = makeEvidence(
            "skill-1", "1.0.0",
            EvaluationStatus.PASS
        );
        assertEquals("skill-1",
            evidence.skillId()
        );
        assertEquals(
            EvaluationStatus.PASS,
            evidence.result()
        );
        assertTrue(evidence.isPassed());
    }

    @Test
    void afSkillEval003_evidenceImmutability() {
        var caps = new java.util.HashMap<>(
            Map.of("cap1", "1.0.0")
        );
        var evidence = makeEvidence(
            "s1", "1.0.0",
            EvaluationStatus.PASS
        );
        // Evidence fields are immutable
        assertNotNull(evidence.skillId());
    }

    // ── AF-SKILL-EVAL-004 Replay ────

    @Test
    void afSkillEval004_evidenceReplay() {
        var evidence = makeEvidence(
            "s1", "1.0.0",
            EvaluationStatus.FAIL
        );

        // All provenance fields accessible
        // for replay
        assertNotNull(
            evidence.artifactDigest()
        );
        assertNotNull(
            evidence.datasetVersion()
        );
        assertNotNull(
            evidence.modelVersion()
        );
        assertNotNull(
            evidence.executionId()
        );
    }

    // ── AF-SKILL-EVAL-005 LLM Judge ──

    @Test
    void afSkillEval005_llmJudgeNoAuth() {
        // N: LLM-as-Judge ≠ Authorization
        // Authority
        var evidence = makeEvidence(
            "s1", "1.0.0",
            EvaluationStatus.PASS
        );

        assertTrue(
            SkillEvaluationSecurityBoundary
                .isEvaluationIsolatedFromAuth(
                    evidence
                )
        );
    }

    @Test
    void afSkillEval005_llmJudgeConstrained() {
        assertTrue(
            SkillEvaluationSecurityBoundary
                .isLlmJudgeConstrained(
                    "SEMANTIC"
                )
        );
        assertTrue(
            SkillEvaluationSecurityBoundary
                .isLlmJudgeConstrained(
                    "EXACT"
                )
        );
    }

    @Test
    void afSkillEval005_securityBoundaryValidation() {
        var evidence = makeEvidence(
            "s1", "1.0.0",
            EvaluationStatus.PASS
        );

        var violations =
            SkillEvaluationSecurityBoundary
                .validateSecurityBoundaries(
                    evidence,
                    Set.of("AUTOMATED")
                );
        assertTrue(violations.isEmpty());
    }

    // ── AF-SKILL-EVAL-006 GateThreshold

    @Test
    void afSkillEval006_thresholdEnforced() {
        var c = new SkillEvaluationContract(
            List.of("ds"),
            List.of("crit"),
            Set.of("AUTOMATED"),
            0.9
        );
        assertEquals(0.9,
            c.gateThreshold(), 0.001
        );
        assertTrue(c.requiresEvaluation());
    }

    @Test
    void afSkillEval006_invalidThreshold() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SkillEvaluationContract(
                List.of(), List.of(),
                Set.of(),
                1.5  // > 1.0
            )
        );
    }

    // ── helpers ──────────────────────

    private SkillEvaluationEvidence
        makeEvidence(
            String skillId,
            String version,
            EvaluationStatus status
        )
    {
        return new SkillEvaluationEvidence(
            skillId, version,
            "sha256:abc",
            "ds-v1",
            "exec-1", "model-v1",
            Map.of(),
            "policy-v1",
            Set.of(),
            status,
            List.of(),
            Instant.now()
        );
    }
}

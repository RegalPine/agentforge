package io.github.regalpine.agentforge.evaluation.skill;

import io.github.regalpine.agentforge.evaluation.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Skill 评估集成测试.
 * T-S4.3~T-S4.6: 九维评估 / Evidence
 * 完整 / PASS 不绕过 Security /
 * LLM Judge 不提升权限.
 */
class SkillEvaluationIntegrationTest {

    // ── 九维评估维度 ──────────

    @Test
    void shouldHaveNineDimensions() {
        assertEquals(9,
            SkillEvaluationDimension
                .values().length
        );
    }

    @Test
    void shouldMapAllCriterionTypes() {
        for (var dim
            : SkillEvaluationDimension
                .values()
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
    void shouldResolveFromCriterionType() {
        var dim =
            SkillEvaluationDimension
                .fromCriterionType(
                    CriterionType
                        .CORRECTNESS
                );

        assertEquals(
            SkillEvaluationDimension
                .CORRECTNESS,
            dim
        );
    }

    @Test
    void shouldRejectUnmappedCriterionType() {
        assertThrows(
            IllegalArgumentException.class,
            () -> SkillEvaluationDimension
                .fromCriterionType(
                    CriterionType
                        .STATE_TRANSITION
                )
        );
    }

    @Test
    void shouldCheckCoverage() {
        assertTrue(
            SkillEvaluationDimension
                .isCovered(
                    CriterionType
                        .SIDE_EFFECT
                )
        );
        assertTrue(
            SkillEvaluationDimension
                .isCovered(
                    CriterionType
                        .PERFORMANCE
                )
        );
        assertFalse(
            SkillEvaluationDimension
                .isCovered(
                    CriterionType
                        .STATE_TRANSITION
                )
        );
    }

    // ── Evidence 完整性 ────────

    @Test
    void shouldCreateValidEvidence() {
        SkillEvaluationEvidence ev =
            makeEvidence(
                EvaluationStatus.PASS
            );

        assertEquals("s1", ev.skillId());
        assertEquals("1.0.0",
            ev.skillVersion()
        );
        assertEquals("sha256:abc",
            ev.artifactDigest()
        );
        assertTrue(ev.isPassed());
        assertFalse(ev.isBlocked());
    }

    @Test
    void shouldRejectNullSkillId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SkillEvaluationEvidence(
                null, "1.0.0",
                "sha256:abc", "ds-v1",
                "exec-1", "model-v1",
                Map.of(), "policy-v1",
                Set.of(),
                EvaluationStatus.PASS,
                List.of(),
                Instant.now()
            )
        );
    }

    @Test
    void shouldRejectBlankSkillVersion() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SkillEvaluationEvidence(
                "s1", "",
                "sha256:abc", "ds-v1",
                "exec-1", "model-v1",
                Map.of(), "policy-v1",
                Set.of(),
                EvaluationStatus.PASS,
                List.of(),
                Instant.now()
            )
        );
    }

    @Test
    void shouldDetectBlockedEvidence() {
        var finding = new EvaluationFinding(
            "f1",
            FindingSeverity.CRITICAL,
            "Security blocked",
            "SECURITY_POLICY_CHECK"
        );

        SkillEvaluationEvidence ev =
            new SkillEvaluationEvidence(
                "s1", "1.0.0",
                "sha256:abc", "ds-v1",
                "exec-1", "model-v1",
                Map.of(), "policy-v1",
                Set.of(),
                EvaluationStatus.ERROR,
                List.of(finding),
                Instant.now()
            );

        assertTrue(ev.isBlocked());
        assertFalse(ev.isPassed());
    }

    // ── PASS 不绕过 Security ──

    @Test
    void passShouldNotConferAuth() {
        SkillEvaluationEvidence ev =
            makeEvidence(
                EvaluationStatus.PASS
            );

        assertTrue(
            SkillEvaluationSecurityBoundary
                .isEvaluationIsolatedFromAuth(
                    ev
                )
        );
    }

    @Test
    void shouldDetectAuthAttemptInFinding() {
        var finding = new EvaluationFinding(
            "f1",
            FindingSeverity.INFO,
            "Granted permission",
            "AUTH_GRANT"
        );

        SkillEvaluationEvidence ev =
            new SkillEvaluationEvidence(
                "s1", "1.0.0",
                "sha256:abc", "ds-v1",
                "exec-1", "model-v1",
                Map.of(), "policy-v1",
                Set.of(),
                EvaluationStatus.PASS,
                List.of(finding),
                Instant.now()
            );

        assertFalse(
            SkillEvaluationSecurityBoundary
                .isEvaluationIsolatedFromAuth(
                    ev
                )
        );
    }

    // ── LLM Judge 约束 ────────

    @Test
    void llmJudgeShouldNotBeAuth() {
        assertTrue(
            SkillEvaluationSecurityBoundary
                .isLlmJudgeConstrained(
                    "SEMANTIC"
                )
        );
        assertTrue(
            SkillEvaluationSecurityBoundary
                .isLlmJudgeConstrained(
                    "BEHAVIORAL"
                )
        );
    }

    @Test
    void nonLlmModeShouldBeUnconstrained() {
        assertTrue(
            SkillEvaluationSecurityBoundary
                .isLlmJudgeConstrained(
                    "EXACT"
                )
        );
    }

    @Test
    void shouldIdentifyLlmJudgeModes() {
        assertTrue(
            SkillEvaluationSecurityBoundary
                .isLlmJudgeMode("SEMANTIC")
        );
        assertFalse(
            SkillEvaluationSecurityBoundary
                .isLlmJudgeMode("EXACT")
        );
    }

    @Test
    void shouldValidateSecurityBoundaries() {
        SkillEvaluationEvidence ev =
            makeEvidence(
                EvaluationStatus.PASS
            );

        var violations =
            SkillEvaluationSecurityBoundary
                .validateSecurityBoundaries(
                    ev,
                    Set.of("EXACT", "SEMANTIC")
                );

        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldReportAuthViolation() {
        var finding = new EvaluationFinding(
            "f1",
            FindingSeverity.INFO,
            "Granted",
            "PERMISSION_GRANT"
        );

        SkillEvaluationEvidence ev =
            new SkillEvaluationEvidence(
                "s1", "1.0.0",
                "sha256:abc", "ds-v1",
                "exec-1", "model-v1",
                Map.of(), "policy-v1",
                Set.of(),
                EvaluationStatus.PASS,
                List.of(finding),
                Instant.now()
            );

        var violations =
            SkillEvaluationSecurityBoundary
                .validateSecurityBoundaries(
                    ev, Set.of("EXACT")
                );

        assertFalse(
            violations.isEmpty()
        );
    }

    // ── Helpers ────────────────

    private SkillEvaluationEvidence
        makeEvidence(
            EvaluationStatus status
        )
    {
        return new SkillEvaluationEvidence(
            "s1", "1.0.0",
            "sha256:abc", "ds-v1",
            "exec-1", "model-v1",
            Map.of("http-client", "2.0"),
            "policy-v1",
            Set.of("correctness"),
            status,
            List.of(),
            Instant.now()
        );
    }
}

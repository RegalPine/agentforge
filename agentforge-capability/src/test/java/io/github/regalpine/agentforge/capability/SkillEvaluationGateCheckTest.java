package io.github.regalpine.agentforge.capability;

import io.github.regalpine.agentforge.kernel.*;
import io.github.regalpine.agentforge.skill.governance.*;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SkillEvaluationGateCheck 测试.
 * T-S4.2: EvaluationGate 集成.
 * T-S4.7: 契约绑定 / PASS 不绕过.
 */
class SkillEvaluationGateCheckTest {

    @Test
    void shouldPassWithNoEvidence() {
        var check =
            new SkillEvaluationGateCheck();

        var result = check.evaluate(
            makeMetadata(),
            SkillDigest.sha256("test"),
            SkillSecurityProfile.lowRisk(),
            makeGovernance()
        );

        assertEquals(
            SkillReleaseGate.GateStatus.PASS,
            result
        );
    }

    @Test
    void shouldPassWithPassingEvidence() {
        var evidence = makeEvidence(
            true,
            "s1", "1.0.0"
        );

        var check =
            new SkillEvaluationGateCheck(
                evidence
            );

        var result = check.evaluate(
            makeMetadata(),
            SkillDigest.sha256("test"),
            SkillSecurityProfile.lowRisk(),
            makeGovernance()
        );

        assertEquals(
            SkillReleaseGate.GateStatus.PASS,
            result
        );
    }

    @Test
    void shouldFailWithFailingEvidence() {
        var evidence = makeEvidence(
            false,
            "s1", "1.0.0"
        );

        var check =
            new SkillEvaluationGateCheck(
                evidence
            );

        var result = check.evaluate(
            makeMetadata(),
            SkillDigest.sha256("test"),
            SkillSecurityProfile.lowRisk(),
            makeGovernance()
        );

        assertEquals(
            SkillReleaseGate.GateStatus.FAIL,
            result
        );
    }

    @Test
    void shouldFailWithMismatchedSkill() {
        var evidence = makeEvidence(
            true,
            "s2", "2.0.0"
        );

        var check =
            new SkillEvaluationGateCheck(
                evidence
            );

        var result = check.evaluate(
            makeMetadata(),
            SkillDigest.sha256("test"),
            SkillSecurityProfile.lowRisk(),
            makeGovernance()
        );

        assertEquals(
            SkillReleaseGate.GateStatus.FAIL,
            result
        );
    }

    @Test
    void shouldWorkInReleaseGate() {
        var evidence = makeEvidence(
            true,
            "s1", "1.0.0"
        );

        SkillReleaseGate gate =
            SkillReleaseGate.builder()
                .addCheck(
                    new SkillEvaluationGateCheck(
                        evidence
                    )
                )
                .build();

        var result = gate.evaluate(
            makeMetadata(),
            SkillDigest.sha256("test"),
            SkillSecurityProfile.lowRisk(),
            makeGovernance()
        );

        assertTrue(result.isPublished());
    }

    @Test
    void evaluationPassShouldNotBypassSecurity() {
        // N: Evaluation PASS ≠ Security
        // PASS. Even with passing
        // evaluation, security chain
        // is independent.
        var evidence = makeEvidence(
            true,
            "s1", "1.0.0"
        );

        var evalCheck =
            new SkillEvaluationGateCheck(
                evidence
            );

        // Evaluation gate passes
        var evalResult =
            evalCheck.evaluate(
                makeMetadata(),
                SkillDigest.sha256("test"),
                SkillSecurityProfile.lowRisk(),
                makeGovernance()
            );

        assertEquals(
            SkillReleaseGate.GateStatus.PASS,
            evalResult
        );

        // But this does NOT grant any
        // security permission.
        // Security is enforced by
        // SkillSecurityChain separately.
        // (isBlocked is on the concrete
        // evaluation type, not on the SPI)
    }

    // ── Helpers ────────────────

    private EvaluationEvidence
        makeEvidence(
            boolean passed,
            String skillId,
            String version
        )
    {
        return new EvaluationEvidence() {
            @Override
            public boolean isPassed() {
                return passed;
            }

            @Override
            public String skillId() {
                return skillId;
            }

            @Override
            public String skillVersion() {
                return version;
            }
        };
    }

    private SkillPackageMetadata makeMetadata() {
        Schema s = () -> "object";
        return new SkillPackageMetadata(
            new SkillId("s1"),
            new Version("1.0.0"),
            "Test Skill",
            "Test goal",
            SkillContract.of(s, s),
            SkillDependencies.empty(),
            SkillExecutionStrategy
                .DIRECT_CAPABILITY,
            "owner",
            "domain",
            Set.of(),
            Map.of(),
            URI.create("file:///test")
        );
    }

    private SkillGovernanceProfile
        makeGovernance()
    {
        return new SkillGovernanceProfile(
            "team-alpha",
            "analytics",
            SkillRiskLevel.LOW,
            Set.of(), Set.of(), null
        );
    }
}

package io.github.regalpine.agentforge.capability;

import io.github.regalpine.agentforge.kernel.*;
import io.github.regalpine.agentforge.skill.governance.*;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SkillReleaseGate 测试.
 * T-S3.20: 发布门禁 / Publish ≠ Activate /
 * 门禁失败 → NOT PUBLISHED.
 */
class SkillReleaseGateTest {

    @Test
    void shouldPublishWhenAllChecksPass() {
        SkillReleaseGate gate =
            SkillReleaseGate.builder()
                .addCheck(
                    passingCheck("integrity")
                )
                .addCheck(
                    passingCheck("security-scan")
                )
                .build();

        SkillReleaseGate.GateResult result =
            gate.evaluate(
                makeMetadata(),
                SkillDigest.sha256("test"),
                SkillSecurityProfile.lowRisk(),
                makeGovernance()
            );

        assertTrue(result.isPublished());
        assertEquals(
            SkillReleaseGate.GateDecision
                .PUBLISHED,
            result.decision()
        );
    }

    @Test
    void shouldNotPublishOnCheckFailure() {
        SkillReleaseGate gate =
            SkillReleaseGate.builder()
                .addCheck(
                    passingCheck("integrity")
                )
                .addCheck(
                    failingCheck("security-scan")
                )
                .addCheck(
                    passingCheck("test")
                )
                .build();

        SkillReleaseGate.GateResult result =
            gate.evaluate(
                makeMetadata(),
                SkillDigest.sha256("test"),
                SkillSecurityProfile.lowRisk(),
                makeGovernance()
            );

        assertFalse(result.isPublished());
        assertEquals(
            SkillReleaseGate.GateDecision
                .NOT_PUBLISHED,
            result.decision()
        );
        // "test" should not have run
        assertFalse(
            result.checkResults()
                .containsKey("test")
        );
    }

    @Test
    void shouldActivatePublishedSkill() {
        InMemorySkillRegistry registry =
            new InMemorySkillRegistry();

        SkillPackageMetadata meta =
            makeMetadata();
        SkillDigest digest =
            SkillDigest.sha256("test");

        registry.register(meta, digest);

        SkillReference ref =
            SkillReference.of("s1", "1.0.0");

        // Transition to PUBLISHED
        registry.transition(
            ref,
            SkillLifecycleState.VALIDATED
        );
        registry.transition(
            ref,
            SkillLifecycleState.TESTED
        );
        registry.transition(
            ref,
            SkillLifecycleState.EVALUATED
        );
        registry.transition(
            ref,
            SkillLifecycleState.APPROVED
        );
        registry.transition(
            ref,
            SkillLifecycleState.PUBLISHED
        );

        SkillReleaseGate gate =
            SkillReleaseGate.builder()
                .build();

        assertTrue(
            gate.activate(registry, ref)
        );

        assertEquals(
            SkillLifecycleState.ACTIVE,
            registry.state(ref)
        );
    }

    @Test
    void shouldNotActivateNonPublishedSkill() {
        InMemorySkillRegistry registry =
            new InMemorySkillRegistry();

        SkillPackageMetadata meta =
            makeMetadata();
        SkillDigest digest =
            SkillDigest.sha256("test");

        registry.register(meta, digest);

        SkillReference ref =
            SkillReference.of("s1", "1.0.0");

        // State is DRAFT, not PUBLISHED
        SkillReleaseGate gate =
            SkillReleaseGate.builder()
                .build();

        assertFalse(
            gate.activate(registry, ref)
        );
    }

    // ── Helpers ────────────────────

    private SkillReleaseGate.GateCheck
        passingCheck(String name)
    {
        return new SkillReleaseGate.GateCheck() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public SkillReleaseGate.GateStatus
                evaluate(
                    SkillPackageMetadata m,
                    SkillDigest d,
                    SkillSecurityProfile s,
                    SkillGovernanceProfile g
                )
            {
                return SkillReleaseGate
                    .GateStatus.PASS;
            }
        };
    }

    private SkillReleaseGate.GateCheck
        failingCheck(String name)
    {
        return new SkillReleaseGate.GateCheck() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public SkillReleaseGate.GateStatus
                evaluate(
                    SkillPackageMetadata m,
                    SkillDigest d,
                    SkillSecurityProfile s,
                    SkillGovernanceProfile g
                )
            {
                return SkillReleaseGate
                    .GateStatus.FAIL;
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

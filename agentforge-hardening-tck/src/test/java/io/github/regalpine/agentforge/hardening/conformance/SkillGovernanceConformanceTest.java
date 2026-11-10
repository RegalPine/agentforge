package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.capability.InMemorySkillRegistry;
import io.github.regalpine.agentforge.capability.SkillReleaseGate;
import io.github.regalpine.agentforge.kernel.*;
import io.github.regalpine.agentforge.skill.governance.*;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * conformance-skill-governance:
 * AF-SKILL-GOV-001 ~ 005.
 * Owner / Security Review / Publish Gate /
 * Activate / Retire.
 * AF-AD-15 §14.2, T-S6.7.
 */
class SkillGovernanceConformanceTest {

    // ── AF-SKILL-GOV-001 Owner ──────

    @Test
    void afSkillGov001_ownerRequired() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SkillGovernanceProfile(
                null, "domain",
                SkillRiskLevel.LOW,
                Set.of(), Set.of(), null
            )
        );
    }

    @Test
    void afSkillGov001_ownerNotBlank() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SkillGovernanceProfile(
                "  ", "domain",
                SkillRiskLevel.LOW,
                Set.of(), Set.of(), null
            )
        );
    }

    @Test
    void afSkillGov001_validGovernance() {
        var g = new SkillGovernanceProfile(
            "team-alpha", "finance",
            SkillRiskLevel.MEDIUM,
            Set.of("developer"),
            Set.of("reviewer"),
            "policy-ref-1"
        );
        assertEquals("team-alpha", g.owner());
        assertEquals("finance", g.domain());
    }

    // ── AF-SKILL-GOV-002 SecurityReview

    @Test
    void afSkillGov002_approvalRolesForCritical() {
        var g = new SkillGovernanceProfile(
            "team-a", "domain",
            SkillRiskLevel.CRITICAL,
            Set.of(),
            Set.of("security", "compliance"),
            null
        );
        assertTrue(
            g.riskLevel().requiresApproval()
        );
        assertEquals(2,
            g.approvalRoles().size()
        );
    }

    // ── AF-SKILL-GOV-003 PublishGate ─

    @Test
    void afSkillGov003_releaseGatePass() {
        var gate = SkillReleaseGate.builder()
            .addCheck(new SkillReleaseGate.GateCheck() {
                public String name() { return "integrity"; }
                public SkillReleaseGate.GateStatus evaluate(
                    SkillPackageMetadata m,
                    SkillDigest d,
                    SkillSecurityProfile s,
                    SkillGovernanceProfile g
                ) {
                    return SkillReleaseGate.GateStatus.PASS;
                }
            })
            .addCheck(new SkillReleaseGate.GateCheck() {
                public String name() { return "security-scan"; }
                public SkillReleaseGate.GateStatus evaluate(
                    SkillPackageMetadata m,
                    SkillDigest d,
                    SkillSecurityProfile s,
                    SkillGovernanceProfile g
                ) {
                    return SkillReleaseGate.GateStatus.PASS;
                }
            })
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
    void afSkillGov003_releaseGateFail() {
        var gate = SkillReleaseGate.builder()
            .addCheck(new SkillReleaseGate.GateCheck() {
                public String name() { return "security-scan"; }
                public SkillReleaseGate.GateStatus evaluate(
                    SkillPackageMetadata m,
                    SkillDigest d,
                    SkillSecurityProfile s,
                    SkillGovernanceProfile g
                ) {
                    return SkillReleaseGate.GateStatus.FAIL;
                }
            })
            .build();

        var result = gate.evaluate(
            makeMetadata(),
            SkillDigest.sha256("test"),
            SkillSecurityProfile.lowRisk(),
            makeGovernance()
        );
        assertFalse(result.isPublished());
    }

    // ── AF-SKILL-GOV-004 Activate ───

    @Test
    void afSkillGov004_lifecycleToActive() {
        var registry = new InMemorySkillRegistry();
        var metadata = makeMetadata();
        var digest = SkillDigest.sha256("act");
        registry.register(metadata, digest);

        var ref = new SkillReference(
            metadata.skillId(),
            metadata.version()
        );

        // DRAFT → VALIDATED → TESTED →
        // EVALUATED → APPROVED → PUBLISHED
        // → ACTIVE
        registry.transition(
            ref, SkillLifecycleState.VALIDATED
        );
        registry.transition(
            ref, SkillLifecycleState.TESTED
        );
        registry.transition(
            ref, SkillLifecycleState.EVALUATED
        );
        registry.transition(
            ref, SkillLifecycleState.APPROVED
        );
        registry.transition(
            ref, SkillLifecycleState.PUBLISHED
        );
        registry.transition(
            ref, SkillLifecycleState.ACTIVE
        );

        assertEquals(
            SkillLifecycleState.ACTIVE,
            registry.state(ref)
        );
        assertTrue(
            registry.state(ref).isExecutable()
        );
    }

    // ── AF-SKILL-GOV-005 Retire ─────

    @Test
    void afSkillGov005_lifecycleToRetired() {
        var registry = new InMemorySkillRegistry();
        var metadata = makeMetadata();
        var digest = SkillDigest.sha256("ret");
        registry.register(metadata, digest);

        var ref = new SkillReference(
            metadata.skillId(),
            metadata.version()
        );

        // Full chain to RETIRED
        for (var state : List.of(
            SkillLifecycleState.VALIDATED,
            SkillLifecycleState.TESTED,
            SkillLifecycleState.EVALUATED,
            SkillLifecycleState.APPROVED,
            SkillLifecycleState.PUBLISHED,
            SkillLifecycleState.ACTIVE,
            SkillLifecycleState.DEPRECATED,
            SkillLifecycleState.RETIRED
        )) {
            registry.transition(ref, state);
        }

        var s = registry.state(ref);
        assertEquals(
            SkillLifecycleState.RETIRED, s
        );
        assertTrue(s.isTerminal());
        assertFalse(s.isExecutable());
    }

    @Test
    void afSkillGov005_retiredCannotRevoke() {
        // RETIRED is terminal, no transitions
        assertFalse(
            SkillLifecycleState.RETIRED
                .canTransitionTo(
                    SkillLifecycleState.REVOKED
                )
        );
    }

    // ── helpers ──────────────────────

    private SkillPackageMetadata makeMetadata() {
        Schema s = () -> "object";
        return new SkillPackageMetadata(
            new SkillId("gov-test"),
            new Version("1.0.0"),
            "GovTest Skill",
            "Governance test goal",
            SkillContract.of(s, s),
            SkillDependencies.empty(),
            SkillExecutionStrategy
                .DIRECT_CAPABILITY,
            "owner1", "domain1",
            null, null,
            URI.create("file:///gov.tar.gz")
        );
    }

    private SkillGovernanceProfile makeGovernance() {
        return new SkillGovernanceProfile(
            "owner1", "domain1",
            SkillRiskLevel.LOW,
            Set.of(), Set.of(), null
        );
    }
}

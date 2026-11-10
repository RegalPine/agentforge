package io.github.regalpine.agentforge.skill.governance;

import io.github.regalpine.agentforge.kernel.Skill;


import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Skill 安全治理类型测试.
 * T-S3.19: RiskLevel / DataBoundary /
 * ExecutionConstraint / SecurityProfile /
 * GovernanceProfile.
 */
class SkillSecurityGovernanceTest {

    // ── SkillRiskLevel ────────────

    @Test
    void shouldHaveFourLevels() {
        assertEquals(4,
            SkillRiskLevel.values().length);
    }

    @Test
    void criticalShouldRequireApproval() {
        assertTrue(
            SkillRiskLevel.CRITICAL
                .requiresApproval()
        );
        assertFalse(
            SkillRiskLevel.HIGH
                .requiresApproval()
        );
        assertFalse(
            SkillRiskLevel.LOW
                .requiresApproval()
        );
    }

    @Test
    void shouldCompareSeverity() {
        assertTrue(
            SkillRiskLevel.CRITICAL.exceeds(
                SkillRiskLevel.HIGH
            )
        );
        assertFalse(
            SkillRiskLevel.LOW.exceeds(
                SkillRiskLevel.HIGH
            )
        );
    }

    // ── SkillDataBoundary ─────────

    @Test
    void restrictiveShouldBlockAll() {
        SkillDataBoundary b =
            SkillDataBoundary.restrictive();

        assertFalse(
            b.isEgressAllowed("api.test.com")
        );
        assertFalse(b.allowsExternalNetwork());
        assertFalse(b.allowsFileSystemAccess());
    }

    @Test
    void shouldAllowConfiguredHost() {
        SkillDataBoundary b =
            new SkillDataBoundary(
                Set.of("api.test.com"),
                true, false, false
            );

        assertTrue(
            b.isEgressAllowed("api.test.com")
        );
        assertFalse(
            b.isEgressAllowed("evil.com")
        );
    }

    @Test
    void shouldBlockWhenNetworkDisabled() {
        SkillDataBoundary b =
            new SkillDataBoundary(
                Set.of("api.test.com"),
                false, false, false
            );

        assertFalse(
            b.isEgressAllowed("api.test.com")
        );
    }

    // ── SkillExecutionConstraint ──

    @Test
    void shouldCreateDefaults() {
        SkillExecutionConstraint c =
            SkillExecutionConstraint
                .defaults();

        assertEquals(
            Duration.ofSeconds(30),
            c.timeout()
        );
        assertEquals(0, c.maxRetries());
        assertFalse(c.allowsSideEffects());
    }

    @Test
    void shouldRejectNegativeTimeout() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SkillExecutionConstraint(
                Duration.ofSeconds(-1),
                0, 0, 0, false
            )
        );
    }

    @Test
    void shouldRejectNegativeRetries() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SkillExecutionConstraint(
                Duration.ofSeconds(10),
                -1, 0, 0, false
            )
        );
    }

    // ── SkillSecurityProfile ──────

    @Test
    void shouldCreateLowRisk() {
        SkillSecurityProfile p =
            SkillSecurityProfile.lowRisk();

        assertEquals(
            SkillRiskLevel.LOW,
            p.riskLevel()
        );
        assertFalse(
            p.requiresApproval()
        );
    }

    @Test
    void criticalSecurityProfileShouldRequireApproval() {
        SkillSecurityProfile p =
            new SkillSecurityProfile(
                SkillRiskLevel.CRITICAL,
                "CONFIDENTIAL",
                "CONFIDENTIAL",
                Set.of("admin"),
                Set.of(),
                Set.of(),
                SkillDataBoundary
                    .restrictive(),
                SkillExecutionConstraint
                    .defaults()
            );

        assertTrue(
            p.requiresApproval()
        );
    }

    // ── SkillGovernanceProfile ────

    @Test
    void shouldRequireOwner() {
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
    void shouldRejectBlankOwner() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SkillGovernanceProfile(
                "", "domain",
                SkillRiskLevel.LOW,
                Set.of(), Set.of(), null
            )
        );
    }

    @Test
    void shouldCreateValidProfile() {
        SkillGovernanceProfile g =
            new SkillGovernanceProfile(
                "team-alpha",
                "analytics",
                SkillRiskLevel.MEDIUM,
                Set.of("developer"),
                Set.of("security-lead"),
                "policy-v1"
            );

        assertEquals("team-alpha", g.owner());
        assertTrue(g.requiresApproval());
        assertTrue(
            g.isApprovalRole("security-lead")
        );
        assertFalse(
            g.isApprovalRole("intern")
        );
    }

    @Test
    void lowRiskNoApprovalRoles() {
        SkillGovernanceProfile g =
            new SkillGovernanceProfile(
                "owner", "domain",
                SkillRiskLevel.LOW,
                Set.of(), Set.of(), null
            );

        assertFalse(
            g.requiresApproval()
        );
    }
}

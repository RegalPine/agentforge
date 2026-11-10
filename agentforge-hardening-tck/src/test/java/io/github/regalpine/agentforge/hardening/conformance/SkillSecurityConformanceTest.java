package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.kernel.*;
import io.github.regalpine.agentforge.skill.governance.*;
import io.github.regalpine.agentforge.security.skill.SkillSecurityChain;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * conformance-skill-security:
 * AF-SKILL-SEC-001 ~ 010.
 * Profile / Authorization / Data Boundary /
 * Credential / Tenant / Approval / Revocation.
 * AF-AD-15 §14.2, T-S6.6.
 */
class SkillSecurityConformanceTest {

    // ── AF-SKILL-SEC-001 RiskLevel ──

    @Test
    void afSkillSec001_fourRiskLevels() {
        assertEquals(4,
            SkillRiskLevel.values().length
        );
        assertEquals(0,
            SkillRiskLevel.LOW.severity()
        );
        assertEquals(3,
            SkillRiskLevel.CRITICAL.severity()
        );
    }

    @Test
    void afSkillSec001_criticalRequiresApproval() {
        assertTrue(
            SkillRiskLevel.CRITICAL
                .requiresApproval()
        );
        assertFalse(
            SkillRiskLevel.HIGH
                .requiresApproval()
        );
    }

    // ── AF-SKILL-SEC-002 DataBoundary ─

    @Test
    void afSkillSec002_restrictiveBoundary() {
        var b = SkillDataBoundary
            .restrictive();
        assertFalse(
            b.allowsExternalNetwork()
        );
        assertFalse(
            b.allowsFileSystemAccess()
        );
        assertFalse(
            b.allowsCredentialAccess()
        );
        assertTrue(
            b.permittedEgressHosts()
                .isEmpty()
        );
    }

    @Test
    void afSkillSec002_customBoundary() {
        var b = new SkillDataBoundary(
            Set.of("api.example.com"),
            true, true, false
        );
        assertTrue(
            b.allowsExternalNetwork()
        );
        assertTrue(
            b.allowsFileSystemAccess()
        );
        assertTrue(
            b.isEgressAllowed(
                "api.example.com"
            )
        );
        assertFalse(
            b.isEgressAllowed(
                "evil.com"
            )
        );
    }

    // ── AF-SKILL-SEC-003 ExecConstraint

    @Test
    void afSkillSec003_defaultConstraint() {
        var c = SkillExecutionConstraint
            .defaults();
        assertEquals(
            Duration.ofSeconds(30),
            c.timeout()
        );
        assertEquals(0, c.maxRetries());
        assertFalse(c.allowsSideEffects());
    }

    @Test
    void afSkillSec003_rejectsNegativeTimeout() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SkillExecutionConstraint(
                Duration.ofSeconds(-1),
                0, 0, 0, false
            )
        );
    }

    // ── AF-SKILL-SEC-004 SecurityProfile

    @Test
    void afSkillSec004_lowRiskProfile() {
        var p = SkillSecurityProfile.lowRisk();
        assertEquals(
            SkillRiskLevel.LOW,
            p.riskLevel()
        );
        assertFalse(
            p.requiresApproval()
        );
    }

    @Test
    void afSkillSec004_criticalProfileApproval() {
        var p = new SkillSecurityProfile(
            SkillRiskLevel.CRITICAL,
            "CONFIDENTIAL",
            "CONFIDENTIAL",
            Set.of("admin"),
            Set.of(), Set.of(),
            SkillDataBoundary.restrictive(),
            SkillExecutionConstraint.defaults()
        );
        assertTrue(p.requiresApproval());
    }

    // ── AF-SKILL-SEC-005 PrivEsc ────

    @Test
    void afSkillSec005_privilegeEscalationDenied() {
        var profile = new SkillSecurityProfile(
            SkillRiskLevel.HIGH,
            "INTERNAL", "INTERNAL",
            Set.of("admin", "delete"),
            Set.of(), Set.of(),
            SkillDataBoundary.restrictive(),
            SkillExecutionConstraint.defaults()
        );

        var chain = SkillSecurityChain.builder()
            .profile(profile)
            .callerPermissions(Set.of("read"))
            .build();

        var result =
            chain.checkPrivilegeEscalation();
        assertFalse(result.allowed());
    }

    // ── AF-SKILL-SEC-006 DataBoundary

    @Test
    void afSkillSec006_dataBoundaryEnforced() {
        var profile = new SkillSecurityProfile(
            SkillRiskLevel.MEDIUM,
            "INTERNAL", "INTERNAL",
            Set.of(),
            Set.of(), Set.of(),
            SkillDataBoundary.restrictive(),
            SkillExecutionConstraint.defaults()
        );

        var chain = SkillSecurityChain.builder()
            .profile(profile)
            .callerPermissions(Set.of())
            .build();

        // restrictive boundary should pass
        // for basic input without egress
        var result = chain.checkAll(
            Map.of(), null
        );
        assertTrue(result.allowed());
    }

    // ── AF-SKILL-SEC-007 Tenant ─────

    @Test
    void afSkillSec007_tenantIsolation() {
        var profile = new SkillSecurityProfile(
            SkillRiskLevel.LOW,
            "INTERNAL", "INTERNAL",
            Set.of(),
            Set.of(), Set.of(),
            SkillDataBoundary.restrictive(),
            SkillExecutionConstraint.defaults()
        );

        var chain = SkillSecurityChain.builder()
            .profile(profile)
            .callerPermissions(Set.of())
            .tenantId("tenant-A")
            .build();

        var result = chain.checkTenantIsolation(
            "tenant-A"
        );
        assertTrue(result.allowed());
    }

    // ── AF-SKILL-SEC-008 Governance ──

    @Test
    void afSkillSec008_governanceRequiresOwner() {
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
    void afSkillSec008_governanceOwnerRequired() {
        var g = new SkillGovernanceProfile(
            "team-a", "finance",
            SkillRiskLevel.MEDIUM,
            Set.of("developer"),
            Set.of("security-reviewer"),
            "policy-ref"
        );
        assertEquals("team-a", g.owner());
        assertEquals("finance", g.domain());
    }

    // ── AF-SKILL-SEC-009 Approval ───

    @Test
    void afSkillSec009_approvalRoles() {
        var g = new SkillGovernanceProfile(
            "team-a", "domain",
            SkillRiskLevel.CRITICAL,
            Set.of(),
            Set.of("security", "compliance"),
            null
        );
        assertEquals(2,
            g.approvalRoles().size()
        );
        assertTrue(
            g.approvalRoles()
                .contains("security")
        );
    }

    // ── AF-SKILL-SEC-010 Immutability ─

    @Test
    void afSkillSec010_boundaryDefensiveCopy() {
        var hosts = new java.util.HashSet<>(
            Set.of("api.example.com")
        );
        var b = new SkillDataBoundary(
            hosts, false, false, false
        );
        hosts.add("evil.com");
        assertEquals(1,
            b.permittedEgressHosts().size()
        );
        assertFalse(
            b.permittedEgressHosts()
                .contains("evil.com")
        );
    }

    @Test
    void afSkillSec010_riskLevelExceeds() {
        assertTrue(
            SkillRiskLevel.CRITICAL.exceeds(
                SkillRiskLevel.LOW
            )
        );
        assertFalse(
            SkillRiskLevel.LOW.exceeds(
                SkillRiskLevel.CRITICAL
            )
        );
    }
}

package io.github.regalpine.agentforge.security.skill;

import io.github.regalpine.agentforge.kernel.*;
import io.github.regalpine.agentforge.skill.governance.*;
import io.github.regalpine.agentforge.security.SecurityCheckResult;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SkillSecurityChain 测试.
 * T-S3.19: 权限不提升 / Capability 权限独立 /
 * Context Projection / Credential Isolation /
 * Data Boundary / Tenant Isolation.
 */
class SkillSecurityChainTest {

    @Test
    void shouldAllowMatchingPermissions() {
        SkillSecurityProfile profile =
            new SkillSecurityProfile(
                SkillRiskLevel.LOW,
                "INTERNAL", "INTERNAL",
                Set.of("read"),
                Set.of(), Set.of(),
                SkillDataBoundary
                    .restrictive(),
                SkillExecutionConstraint
                    .defaults()
            );

        SkillSecurityChain chain =
            SkillSecurityChain.builder()
                .profile(profile)
                .callerPermissions(
                    Set.of("read")
                )
                .build();

        SecurityCheckResult result =
            chain.checkPrivilegeEscalation();

        assertTrue(result.allowed());
    }

    @Test
    void shouldDenyPrivilegeEscalation() {
        SkillSecurityProfile profile =
            new SkillSecurityProfile(
                SkillRiskLevel.HIGH,
                "INTERNAL", "INTERNAL",
                Set.of("admin"),
                Set.of(), Set.of(),
                SkillDataBoundary
                    .restrictive(),
                SkillExecutionConstraint
                    .defaults()
            );

        SkillSecurityChain chain =
            SkillSecurityChain.builder()
                .profile(profile)
                .callerPermissions(
                    Set.of("read")
                )
                .build();

        SecurityCheckResult result =
            chain.checkPrivilegeEscalation();

        assertFalse(result.allowed());
        assertEquals(
            "PRIVILEGE_ESCALATION",
            result.reasonCode()
        );
    }

    @Test
    void shouldDenyCapabilityWithoutPermission() {
        SkillSecurityChain chain =
            SkillSecurityChain.builder()
                .profile(
                    SkillSecurityProfile.lowRisk()
                )
                .callerPermissions(
                    Set.of("read")
                )
                .build();

        SecurityCheckResult result =
            chain.checkCapabilityPermission(
                "http-client"
            );

        assertFalse(result.allowed());
        assertEquals(
            "CAPABILITY_PERMISSION_DENIED",
            result.reasonCode()
        );
    }

    @Test
    void shouldAllowCapabilityWithPermission() {
        SkillSecurityChain chain =
            SkillSecurityChain.builder()
                .profile(
                    SkillSecurityProfile.lowRisk()
                )
                .callerPermissions(Set.of(
                    "capability:http-client"
                ))
                .build();

        SecurityCheckResult result =
            chain.checkCapabilityPermission(
                "http-client"
            );

        assertTrue(result.allowed());
    }

    @Test
    void shouldBlockSecretsInContext() {
        SkillSecurityChain chain =
            SkillSecurityChain.builder()
                .profile(
                    SkillSecurityProfile.lowRisk()
                )
                .callerPermissions(Set.of())
                .build();

        SecurityCheckResult result =
            chain.checkContextProjection(
                Map.of(
                    "data", "ok",
                    "api_key", "secret123"
                )
            );

        assertFalse(result.allowed());
        assertTrue(
            result.reasonCode()
                .contains("CONTEXT_PROJECTION")
        );
    }

    @Test
    void shouldBlockSystemPromptInContext() {
        SkillSecurityChain chain =
            SkillSecurityChain.builder()
                .profile(
                    SkillSecurityProfile.lowRisk()
                )
                .callerPermissions(Set.of())
                .build();

        SecurityCheckResult result =
            chain.checkContextProjection(
                Map.of(
                    "system_prompt",
                    "you are..."
                )
            );

        assertFalse(result.allowed());
    }

    @Test
    void shouldAllowCleanContext() {
        SkillSecurityChain chain =
            SkillSecurityChain.builder()
                .profile(
                    SkillSecurityProfile.lowRisk()
                )
                .callerPermissions(Set.of())
                .build();

        SecurityCheckResult result =
            chain.checkContextProjection(
                Map.of("data", "ok")
            );

        assertTrue(result.allowed());
    }

    @Test
    void shouldBlockCrossTenantAccess() {
        SkillSecurityChain chain =
            SkillSecurityChain.builder()
                .profile(
                    SkillSecurityProfile.lowRisk()
                )
                .callerPermissions(Set.of())
                .tenantId("tenant-a")
                .build();

        SecurityCheckResult result =
            chain.checkTenantIsolation(
                "tenant-b"
            );

        assertFalse(result.allowed());
        assertTrue(
            result.reasonCode()
                .contains("TENANT_ISOLATION")
        );
    }

    @Test
    void shouldAllowSameTenant() {
        SkillSecurityChain chain =
            SkillSecurityChain.builder()
                .profile(
                    SkillSecurityProfile.lowRisk()
                )
                .callerPermissions(Set.of())
                .tenantId("tenant-a")
                .build();

        SecurityCheckResult result =
            chain.checkTenantIsolation(
                "tenant-a"
            );

        assertTrue(result.allowed());
    }

    @Test
    void shouldBlockDataBoundaryViolation() {
        SkillSecurityChain chain =
            SkillSecurityChain.builder()
                .profile(
                    SkillSecurityProfile.lowRisk()
                )
                .callerPermissions(Set.of())
                .build();

        SecurityCheckResult result =
            chain.checkDataBoundary(
                "evil.com",
                "CONFIDENTIAL"
            );

        assertFalse(result.allowed());
        assertEquals(
            "DATA_BOUNDARY_VIOLATION",
            result.reasonCode()
        );
    }

    @Test
    void checkAllShouldDenyOnFirstFailure() {
        SkillSecurityProfile profile =
            new SkillSecurityProfile(
                SkillRiskLevel.HIGH,
                "INTERNAL", "INTERNAL",
                Set.of("admin"),
                Set.of(), Set.of(),
                SkillDataBoundary
                    .restrictive(),
                SkillExecutionConstraint
                    .defaults()
            );

        SkillSecurityChain chain =
            SkillSecurityChain.builder()
                .profile(profile)
                .callerPermissions(
                    Set.of("read")
                )
                .tenantId("t1")
                .build();

        SecurityCheckResult result =
            chain.checkAll(
                Map.of("data", "ok"),
                "t1"
            );

        assertFalse(result.allowed());
        assertEquals(
            "PRIVILEGE_ESCALATION",
            result.reasonCode()
        );
    }

    @Test
    void criticalSkillRequiresApproval() {
        SkillSecurityProfile profile =
            new SkillSecurityProfile(
                SkillRiskLevel.CRITICAL,
                "INTERNAL", "INTERNAL",
                Set.of(), Set.of(), Set.of(),
                SkillDataBoundary
                    .restrictive(),
                SkillExecutionConstraint
                    .defaults()
            );

        SkillSecurityChain chain =
            SkillSecurityChain.builder()
                .profile(profile)
                .build();

        assertTrue(
            chain.requiresApproval()
        );
    }
}

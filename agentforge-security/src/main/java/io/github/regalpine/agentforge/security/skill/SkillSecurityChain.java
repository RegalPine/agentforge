package io.github.regalpine.agentforge.security.skill;

import io.github.regalpine.agentforge.skill.governance.SkillDataBoundary;
import io.github.regalpine.agentforge.skill.governance.SkillRiskLevel;
import io.github.regalpine.agentforge.skill.governance.SkillSecurityProfile;
import io.github.regalpine.agentforge.security.SecurityChainRequest;
import io.github.regalpine.agentforge.security.SecurityCheckResult;
import io.github.regalpine.agentforge.security.SecurityExecutionChain;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Skill 安全执行链.
 * AF-SKILL-03 §9-§12: Security Chain.
 * AF-AD-15 §8.1: 安全执行链.
 *
 * <p>Provides SecurityCheck implementations
 * for Skill-specific security controls:
 * privilege escalation, context projection,
 * credential isolation, data boundary,
 * tenant isolation.
 *
 * <p>Usage:
 * <pre>
 * SkillSecurityChain chain =
 *     SkillSecurityChain.builder()
 *         .profile(securityProfile)
 *         .callerPermissions(callerPerms)
 *         .tenantId(tenantId)
 *         .build();
 *
 * SecurityChainResult result =
 *     chain.check(request);
 * </pre>
 */
public final class SkillSecurityChain {

    private final SkillSecurityProfile profile;
    private final Set<String> callerPermissions;
    private final String tenantId;

    private SkillSecurityChain(
        SkillSecurityProfile profile,
        Set<String> callerPermissions,
        String tenantId
    ) {
        this.profile = Objects.requireNonNull(
            profile, "profile"
        );
        this.callerPermissions =
            callerPermissions == null
                ? Set.of()
                : Set.copyOf(callerPermissions);
        this.tenantId = tenantId;
    }

    /**
     * Check privilege escalation:
     * Privileges(Skill) ⊆ Privileges(Caller).
     * AF-SKILL-03 §10.
     */
    public SecurityCheckResult
        checkPrivilegeEscalation()
    {
        Set<String> skillPerms =
            profile.requiredActions();

        for (String perm : skillPerms) {
            if (!callerPermissions.contains(perm)) {
                return SecurityCheckResult.deny(
                    "PRIVILEGE_ESCALATION",
                    "Skill requires permission '"
                        + perm
                        + "' not held by caller"
                );
            }
        }

        return SecurityCheckResult.allow();
    }

    /**
     * Check capability permission
     * independence: Skill dependency
     * declaration ≠ auto Capability
     * permission. AF-SKILL-03 §11.
     */
    public SecurityCheckResult
        checkCapabilityPermission(
            String capabilityId
        ) {
        // N: Skill 声明依赖 ≠ 自动获得
        // Capability 权限. The caller
        // must independently have the
        // capability permission.
        String capPerm =
            "capability:" + capabilityId;

        if (!callerPermissions.contains(capPerm)) {
            return SecurityCheckResult.deny(
                "CAPABILITY_PERMISSION_DENIED",
                "Caller lacks permission "
                    + "for capability: "
                    + capabilityId
            );
        }

        return SecurityCheckResult.allow();
    }

    /**
     * Check context projection:
     * no credentials/secrets in context.
     * AF-SKILL-03 §13-§14.
     */
    public SecurityCheckResult
        checkContextProjection(
            Map<String, Object> context
        ) {
        if (context == null) {
            return SecurityCheckResult
                .allow();
        }

        for (String key : context.keySet()) {
            String lower = key.toLowerCase();

            if (lower.contains("secret")
                || lower.contains("password")
                || lower.contains("token")
                || lower.contains("api_key")
                || lower.contains("apikey")
                || lower.contains("credential")
                || lower.contains(
                    "system_prompt"
                )
            ) {
                return SecurityCheckResult
                    .deny(
                        "CONTEXT_PROJECTION_"
                            + "VIOLATION",
                        "Context contains "
                            + "forbidden key: "
                            + key
                    );
            }
        }

        return SecurityCheckResult
            .allow();
    }

    /**
     * Check data boundary:
     * block CONFIDENTIAL → External API.
     * AF-SKILL-03 §16-§17.
     */
    public SecurityCheckResult
        checkDataBoundary(
            String targetHost,
            String dataClassification
        ) {
        SkillDataBoundary boundary =
            profile.dataBoundary();

        if (!boundary.isEgressAllowed(
            targetHost
        )) {
            return SecurityCheckResult
                .deny(
                    "DATA_BOUNDARY_VIOLATION",
                    "Egress to '"
                        + targetHost
                        + "' not allowed"
                );
        }

        return SecurityCheckResult
            .allow();
    }

    /**
     * Check tenant isolation:
     * Tenant A cannot access Tenant B.
     * AF-SKILL-03 §18.
     */
    public SecurityCheckResult
        checkTenantIsolation(
            String targetTenantId
        ) {
        if (tenantId == null
            || targetTenantId == null
        ) {
            return SecurityCheckResult
                .allow();
        }

        if (!tenantId.equals(targetTenantId)) {
            return SecurityCheckResult
                .deny(
                    "TENANT_ISOLATION_"
                        + "VIOLATION",
                    "Tenant '" + tenantId
                        + "' cannot access "
                        + "tenant '"
                        + targetTenantId
                        + "'"
                );
        }

        return SecurityCheckResult
            .allow();
    }

    /**
     * Check if Skill requires approval.
     */
    public boolean requiresApproval() {
        return profile.requiresApproval();
    }

    /**
     * Run all security checks.
     */
    public SecurityCheckResult checkAll(
        Map<String, Object> context,
        String targetTenantId
    ) {
        // 1. Privilege escalation
        SecurityCheckResult perm =
            checkPrivilegeEscalation();
        if (!perm.allowed()) {
            return perm;
        }

        // 2. Context projection
        SecurityCheckResult ctx =
            checkContextProjection(context);
        if (!ctx.allowed()) {
            return ctx;
        }

        // 3. Tenant isolation
        if (targetTenantId != null) {
            SecurityCheckResult tenant =
                checkTenantIsolation(
                    targetTenantId
                );
            if (!tenant.allowed()) {
                return tenant;
            }
        }

        return SecurityCheckResult.allow();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private SkillSecurityProfile profile;
        private Set<String> callerPermissions;
        private String tenantId;

        public Builder profile(
            SkillSecurityProfile profile
        ) {
            this.profile = profile;
            return this;
        }

        public Builder callerPermissions(
            Set<String> perms
        ) {
            this.callerPermissions = perms;
            return this;
        }

        public Builder tenantId(
            String tenantId
        ) {
            this.tenantId = tenantId;
            return this;
        }

        public SkillSecurityChain build() {
            return new SkillSecurityChain(
                profile,
                callerPermissions,
                tenantId
            );
        }
    }
}

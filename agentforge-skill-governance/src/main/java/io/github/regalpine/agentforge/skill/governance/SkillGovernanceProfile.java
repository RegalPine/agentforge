package io.github.regalpine.agentforge.skill.governance;

import java.util.Objects;
import java.util.Set;

/**
 * Skill 治理配置.
 * AF-SKILL-03 §20-§22: SkillGovernanceProfile.
 * AF-AD-15 §8.4: Governance Profile.
 *
 * <p>Defines the governance requirements
 * for a Skill: ownership, domain, roles,
 * approval policy.
 *
 * <p>N: Skill 必须具有 Owner.
 * Policy 不嵌入 Manifest.
 *
 * <p>Immutable. All sets are defensively
 * copied.
 */
public record SkillGovernanceProfile(
    String owner,
    String domain,
    SkillRiskLevel riskLevel,
    Set<String> requiredRoles,
    Set<String> approvalRoles,
    String policyReference
) {

    public SkillGovernanceProfile {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException(
                "Skill must have an owner"
            );
        }

        requiredRoles =
            requiredRoles == null
                ? Set.of()
                : Set.copyOf(requiredRoles);

        approvalRoles =
            approvalRoles == null
                ? Set.of()
                : Set.copyOf(approvalRoles);
    }

    /**
     * Check if this Skill requires
     * approval from specific roles.
     */
    public boolean requiresApproval() {
        return !approvalRoles.isEmpty()
            || riskLevel.requiresApproval();
    }

    /**
     * Check if a given role is allowed
     * to approve this Skill.
     */
    public boolean isApprovalRole(String role) {
        return approvalRoles.contains(
            Objects.requireNonNull(role)
        );
    }
}

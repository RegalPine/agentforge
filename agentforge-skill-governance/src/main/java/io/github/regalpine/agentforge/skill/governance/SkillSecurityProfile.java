package io.github.regalpine.agentforge.skill.governance;

import io.github.regalpine.agentforge.kernel.CapabilityRef;
import io.github.regalpine.agentforge.kernel.Reference;

import java.util.Objects;
import java.util.Set;

/**
 * Skill 安全配置.
 * AF-SKILL-03 §4-§8: SkillSecurityProfile.
 * AF-AD-15 §8.2: Security Profile.
 *
 * <p>Defines the security requirements
 * for a Skill. Determines what data
 * can flow in/out, what capabilities
 * require independent authorization,
 * and what execution constraints apply.
 *
 * <p>Immutable. All sets are defensively
 * copied.
 */
public record SkillSecurityProfile(
    SkillRiskLevel riskLevel,
    String maxInputClassification,
    String maxOutputClassification,
    Set<String> requiredActions,
    Set<CapabilityRef> capabilityRequirements,
    Set<Reference> knowledgeRequirements,
    SkillDataBoundary dataBoundary,
    SkillExecutionConstraint executionConstraint
) {

    public SkillSecurityProfile {
        Objects.requireNonNull(
            riskLevel, "riskLevel"
        );

        requiredActions =
            requiredActions == null
                ? Set.of()
                : Set.copyOf(requiredActions);

        capabilityRequirements =
            capabilityRequirements == null
                ? Set.of()
                : Set.copyOf(capabilityRequirements);

        knowledgeRequirements =
            knowledgeRequirements == null
                ? Set.of()
                : Set.copyOf(knowledgeRequirements);

        Objects.requireNonNull(
            dataBoundary, "dataBoundary"
        );
        Objects.requireNonNull(
            executionConstraint,
            "executionConstraint"
        );
    }

    /**
     * Default low-risk profile.
     */
    public static SkillSecurityProfile lowRisk() {
        return new SkillSecurityProfile(
            SkillRiskLevel.LOW,
            "INTERNAL",
            "INTERNAL",
            Set.of(),
            Set.of(),
            Set.of(),
            SkillDataBoundary.restrictive(),
            SkillExecutionConstraint
                .defaults()
        );
    }

    /**
     * Check if this Skill requires
     * human approval.
     */
    public boolean requiresApproval() {
        return riskLevel.requiresApproval();
    }
}

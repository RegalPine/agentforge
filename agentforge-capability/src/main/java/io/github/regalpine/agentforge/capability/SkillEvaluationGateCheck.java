package io.github.regalpine.agentforge.capability;

import io.github.regalpine.agentforge.skill.governance.SkillDigest;
import io.github.regalpine.agentforge.skill.governance.SkillGovernanceProfile;
import io.github.regalpine.agentforge.skill.governance.SkillPackageMetadata;
import io.github.regalpine.agentforge.skill.governance.SkillSecurityProfile;

import java.util.Objects;
import java.util.Optional;

/**
 * Skill 评估门禁检查.
 * AF-SKILL-03 §30: EvaluationGate
 *   Integration.
 * AF-AD-15 §9.1: Evaluation 结果作为
 *   Release Gate 条件之一.
 *
 * <p>Bridges the evaluation module with
 * the capability release gate. If
 * evaluation evidence is provided, the
 * gate check verifies that the evaluation
 * passed.
 *
 * <p>N: Evaluation PASS ≠ Authorization
 * PASS. This gate check only verifies
 * evaluation results. Security policy
 * is enforced separately by
 * SkillSecurityChain.
 *
 * <p>N: LLM-as-Judge is not an
 * Authorization Authority. Even if an
 * LLM judge produced the evaluation,
 * it does not grant any permission.
 */
public final class SkillEvaluationGateCheck
    implements SkillReleaseGate.GateCheck {

    private final Optional<EvaluationEvidence>
        evidence;

    /**
     * Create a gate check with no
     * evidence (evaluation not required).
     */
    public SkillEvaluationGateCheck() {
        this.evidence = Optional.empty();
    }

    /**
     * Create a gate check with
     * evaluation evidence.
     */
    public SkillEvaluationGateCheck(
        EvaluationEvidence evidence
    ) {
        this.evidence = Optional.ofNullable(
            evidence
        );
    }

    @Override
    public String name() {
        return "evaluation";
    }

    @Override
    public SkillReleaseGate.GateStatus
        evaluate(
            SkillPackageMetadata metadata,
            SkillDigest digest,
            SkillSecurityProfile security,
            SkillGovernanceProfile governance
        )
    {
        if (evidence.isEmpty()) {
            // No evaluation required
            return SkillReleaseGate.GateStatus
                .PASS;
        }

        EvaluationEvidence ev =
            evidence.get();

        // N: Evaluation PASS ≠ Security
        // PASS. We only check evaluation
        // result here. Security is
        // enforced by SkillSecurityChain.
        if (!ev.isPassed()) {
            return SkillReleaseGate.GateStatus
                .FAIL;
        }

        // Verify evidence matches the
        // skill being published
        if (!matchesSkill(ev, metadata)) {
            return SkillReleaseGate.GateStatus
                .FAIL;
        }

        return SkillReleaseGate.GateStatus
            .PASS;
    }

    /**
     * Verify that the evidence matches
     * the skill metadata.
     */
    private boolean matchesSkill(
        EvaluationEvidence ev,
        SkillPackageMetadata metadata
    ) {
        String expectedId =
            metadata.skillId().value();
        String expectedVersion =
            metadata.version().value();

        return expectedId.equals(
            ev.skillId()
        ) && expectedVersion.equals(
            ev.skillVersion()
        );
    }
}

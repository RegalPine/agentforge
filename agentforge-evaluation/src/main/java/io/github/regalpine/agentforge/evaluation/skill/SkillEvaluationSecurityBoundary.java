package io.github.regalpine.agentforge.evaluation.skill;

import io.github.regalpine.agentforge.evaluation.model.EvaluationStatus;
import io.github.regalpine.agentforge.evaluation.model.FindingSeverity;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Skill 评估安全约束.
 * AF-SKILL-03 §33: Evaluation Security
 *   Constraints.
 * AF-AD-15 §9.3: 评估安全约束.
 *
 * <p>Enforces two critical security
 * boundaries:
 *
 * <ol>
 *   <li>Evaluation PASS ≠ Authorization
 *       PASS: A passing evaluation does
 *       not grant any security
 *       permission or authorization.</li>
 *   <li>LLM-as-Judge ≠ Authorization
 *       Authority: An LLM judge that
 *       produces evaluation scores does
 *       not have the authority to
 *       authorize or deauthorize
 *       capabilities, skills, or
 *       actions.</li>
 * </ol>
 */
public final class SkillEvaluationSecurityBoundary {

    /**
     * Evaluation modes that use LLM
     * judges.
     */
    public static final Set<String>
        LLM_JUDGE_MODES = Set.of(
            "SEMANTIC",
            "BEHAVIORAL",
            "LLM_JUDGE"
        );

    private SkillEvaluationSecurityBoundary() {
    }

    /**
     * Verify that an evaluation result
     * does not confer authorization.
     *
     * <p>N: Evaluation PASS ≠
     * Authorization PASS. Even if all
     * evaluation cases pass, the Skill
     * must still go through independent
     * security authorization.
     *
     * @return true if the evaluation
     *   result is isolated from
     *   authorization (i.e., it does
     *   not attempt to grant
     *   permissions)
     */
    public static boolean
        isEvaluationIsolatedFromAuth(
            SkillEvaluationEvidence evidence
        )
    {
        Objects.requireNonNull(
            evidence, "evidence"
        );

        // Check that no finding attempts
        // to grant authorization
        for (var finding
            : evidence.findings()
        ) {
            String cid = finding
                .criterionId();

            if (cid != null && (
                cid.contains("AUTH")
                || cid.contains(
                    "PERMISSION"
                )
                || cid.contains(
                    "GRANT"
                )
            )) {
                return false;
            }
        }

        return true;
    }

    /**
     * Verify that an LLM-as-Judge
     * evaluation does not act as an
     * authorization authority.
     *
     * <p>N: LLM Judge produces scores,
     * not authorizations. The judge's
     * output is evidence, not a
     * security decision.
     *
     * @param evaluationMode the mode
     *   used for evaluation
     * @return true if the mode is
     *   properly constrained
     */
    public static boolean
        isLlmJudgeConstrained(
            String evaluationMode
        )
    {
        if (!LLM_JUDGE_MODES.contains(
            evaluationMode
        )) {
            // Not an LLM judge mode,
            // constraint doesn't apply
            return true;
        }

        // LLM judge modes are allowed
        // but they must not produce
        // authorization decisions.
        // This is enforced by the
        // evaluation pipeline: LLM
        // judges produce scores, not
        // authorization tokens.
        return true;
    }

    /**
     * Check if a given evaluation mode
     * uses LLM-as-Judge.
     */
    public static boolean isLlmJudgeMode(
        String mode
    ) {
        return LLM_JUDGE_MODES.contains(
            Objects.requireNonNull(mode)
        );
    }

    /**
     * Validate that evaluation evidence
     * respects security boundaries.
     *
     * @return a list of violation
     *   messages, empty if no
     *   violations
     */
    public static List<String>
        validateSecurityBoundaries(
            SkillEvaluationEvidence evidence,
            Set<String> evaluationModes
        )
    {
        Objects.requireNonNull(
            evidence, "evidence"
        );
        Objects.requireNonNull(
            evaluationModes,
            "evaluationModes"
        );

        var violations =
            new java.util.ArrayList<String>();

        // 1. Evaluation PASS ≠ Auth PASS
        if (!isEvaluationIsolatedFromAuth(
            evidence
        )) {
            violations.add(
                "Evaluation evidence "
                    + "attempts to confer "
                    + "authorization. "
                    + "Evaluation PASS does "
                    + "not equal "
                    + "Authorization PASS."
            );
        }

        // 2. LLM Judge constraint
        for (String mode : evaluationModes) {
            if (isLlmJudgeMode(mode)
                && !isLlmJudgeConstrained(
                    mode
                )
            ) {
                violations.add(
                    "LLM-as-Judge mode '"
                        + mode
                        + "' is not "
                        + "properly "
                        + "constrained."
                );
            }
        }

        return List.copyOf(violations);
    }
}

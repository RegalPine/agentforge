package io.github.regalpine.agentforge.evaluation.skill;

import io.github.regalpine.agentforge.capability.EvaluationEvidence;
import io.github.regalpine.agentforge.evaluation.model.EvaluationFinding;
import io.github.regalpine.agentforge.evaluation.model.EvaluationStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Skill 评估证据.
 * AF-SKILL-03 §32-§33: Evaluation
 *   Evidence.
 * AF-AD-15 §9.3: Evidence 收集.
 *
 * <p>Captures the full provenance chain
 * for a Skill evaluation run:
 * <pre>
 * skillId + skillVersion + artifactDigest
 * + datasetVersion + criteria
 * + executionId + modelVersion
 * + capabilityVersions + policyVersion
 * + result (PASS / FAIL / BLOCKED)
 * + findings
 * </pre>
 *
 * <p>Immutable. All collections are
 * defensively copied.
 */
public record SkillEvaluationEvidence(
    String skillId,
    String skillVersion,
    String artifactDigest,
    String datasetVersion,
    String executionId,
    String modelVersion,
    Map<String, String> capabilityVersions,
    String policyVersion,
    Set<String> criteriaIds,
    EvaluationStatus result,
    List<EvaluationFinding> findings,
    Instant evaluatedAt
) implements EvaluationEvidence {

    public SkillEvaluationEvidence {
        if (skillId == null
            || skillId.isBlank()
        ) {
            throw new IllegalArgumentException(
                "skillId is required"
            );
        }

        if (skillVersion == null
            || skillVersion.isBlank()
        ) {
            throw new IllegalArgumentException(
                "skillVersion is required"
            );
        }

        Objects.requireNonNull(
            artifactDigest, "artifactDigest"
        );
        Objects.requireNonNull(
            datasetVersion, "datasetVersion"
        );
        Objects.requireNonNull(
            executionId, "executionId"
        );
        Objects.requireNonNull(
            modelVersion, "modelVersion"
        );

        capabilityVersions =
            capabilityVersions == null
                ? Map.of()
                : Map.copyOf(capabilityVersions);

        Objects.requireNonNull(
            policyVersion, "policyVersion"
        );

        criteriaIds =
            criteriaIds == null
                ? Set.of()
                : Set.copyOf(criteriaIds);

        Objects.requireNonNull(
            result, "result"
        );

        findings =
            findings == null
                ? List.of()
                : List.copyOf(findings);

        Objects.requireNonNull(
            evaluatedAt, "evaluatedAt"
        );
    }

    /**
     * Check if this evidence represents
     * a passing evaluation.
     *
     * <p>N: Evaluation PASS ≠ Authorization
     * PASS. A passing evaluation does not
     * grant any security permission.
     *
     * <p>Implements {@link EvaluationEvidence#isPassed()}.
     */
    @Override
    public boolean isPassed() {
        return result == EvaluationStatus.PASS;
    }

    /**
     * Check if this evidence represents
     * a blocked evaluation (security
     * policy blocked the evaluation).
     */
    public boolean isBlocked() {
        return result == EvaluationStatus.ERROR
            && findings.stream().anyMatch(
                f -> f.criterionId() != null
                    && f.criterionId()
                        .contains("SECURITY")
            );
    }
}

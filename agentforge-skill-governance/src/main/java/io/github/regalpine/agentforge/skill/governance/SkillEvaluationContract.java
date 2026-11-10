package io.github.regalpine.agentforge.skill.governance;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Skill 评估契约.
 * AF-SKILL-03 §27-§29: SkillEvaluationContract.
 * AF-AD-15 §9.1: Evaluation Contract.
 *
 * <p>Defines the evaluation requirements
 * for a Skill version. Each Skill version
 * carries its own contract that specifies
 * what datasets, criteria, and evaluation
 * modes are required.
 *
 * <p>N: Evaluation PASS ≠ Authorization PASS.
 * Evaluation result does not grant any
 * security permission.
 *
 * <p>Immutable.
 */
public record SkillEvaluationContract(
    List<String> datasetReferences,
    List<String> criteriaReferences,
    Set<String> requiredEvaluationModes,
    double gateThreshold
) {

    public SkillEvaluationContract {
        datasetReferences =
            datasetReferences == null
                ? List.of()
                : List.copyOf(datasetReferences);

        criteriaReferences =
            criteriaReferences == null
                ? List.of()
                : List.copyOf(criteriaReferences);

        requiredEvaluationModes =
            requiredEvaluationModes == null
                ? Set.of()
                : Set.copyOf(requiredEvaluationModes);

        if (gateThreshold < 0.0
            || gateThreshold > 1.0
        ) {
            throw new IllegalArgumentException(
                "gateThreshold must be in [0, 1]: "
                    + gateThreshold
            );
        }
    }

    /**
     * Default contract: no evaluation
     * required, threshold 0.7.
     */
    public static SkillEvaluationContract
        none()
    {
        return new SkillEvaluationContract(
            List.of(),
            List.of(),
            Set.of(),
            0.7
        );
    }

    /**
     * Create a contract with datasets and
     * criteria.
     */
    public static SkillEvaluationContract of(
        List<String> datasets,
        List<String> criteria,
        Set<String> modes,
        double threshold
    ) {
        return new SkillEvaluationContract(
            datasets, criteria, modes, threshold
        );
    }

    /**
     * Check if this contract requires
     * evaluation.
     */
    public boolean requiresEvaluation() {
        return !datasetReferences.isEmpty()
            || !criteriaReferences.isEmpty();
    }
}

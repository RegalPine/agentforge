package io.github.regalpine.agentforge.evaluation.skill;

import io.github.regalpine.agentforge.evaluation.model.CriterionType;

/**
 * Skill 九维评估维度.
 * AF-SKILL-03 §31: Nine Evaluation
 *   Dimensions.
 * AF-AD-15 §9.2: 评估维度.
 *
 * <p>Each dimension maps to an existing
 * {@link CriterionType} in the evaluation
 * module, providing a Skill-specific
 * semantic layer.
 */
public enum SkillEvaluationDimension {

    CORRECTNESS(
        "Correctness",
        CriterionType.CORRECTNESS
    ),

    STRUCTURE(
        "Structure",
        CriterionType.STRUCTURE
    ),

    RELEVANCE(
        "Relevance",
        CriterionType.RELEVANCE
    ),

    COMPLETENESS(
        "Completeness",
        CriterionType.COMPLETENESS
    ),

    GROUNDEDNESS(
        "Groundedness",
        CriterionType.GROUNDEDNESS
    ),

    POLICY_COMPLIANCE(
        "Policy Compliance",
        CriterionType.POLICY_COMPLIANCE
    ),

    TOOL_USAGE(
        "Tool Usage",
        CriterionType.TOOL_USAGE
    ),

    SIDE_EFFECT(
        "Side Effect",
        CriterionType.SIDE_EFFECT
    ),

    PERFORMANCE(
        "Performance",
        CriterionType.PERFORMANCE
    );

    private final String displayName;
    private final CriterionType criterionType;

    SkillEvaluationDimension(
        String displayName,
        CriterionType criterionType
    ) {
        this.displayName = displayName;
        this.criterionType = criterionType;
    }

    public String displayName() {
        return displayName;
    }

    public CriterionType criterionType() {
        return criterionType;
    }

    /**
     * Resolve a dimension from a
     * CriterionType.
     *
     * @throws IllegalArgumentException
     *   if no dimension maps to the
     *   given type
     */
    public static SkillEvaluationDimension
        fromCriterionType(
            CriterionType type
        )
    {
        for (var dim : values()) {
            if (dim.criterionType == type) {
                return dim;
            }
        }

        throw new IllegalArgumentException(
            "No Skill evaluation dimension "
                + "maps to CriterionType: "
                + type
        );
    }

    /**
     * Check if a given CriterionType is
     * covered by the nine dimensions.
     */
    public static boolean isCovered(
        CriterionType type
    ) {
        for (var dim : values()) {
            if (dim.criterionType == type) {
                return true;
            }
        }
        return false;
    }
}

package io.github.regalpine.agentforge.kernel;

import java.util.Objects;
import java.util.Optional;

/**
 * Skill Discovery 用只读摘要.
 * AF-SKILL-02 §9: SkillDescriptor.
 * AF-AD-15 §4.1: Discovery 用只读摘要.
 *
 * <p>Provides identity, status, version,
 * owner, risk and strategy information
 * for Skill discovery results. Immutable.
 */
public record SkillDescriptor(
    SkillId skillId,
    String name,
    Version version,
    String goal,
    SkillExecutionStrategy strategy,
    String owner,
    String status
) {

    public SkillDescriptor {
        Objects.requireNonNull(skillId, "skillId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(goal, "goal");
        Objects.requireNonNull(strategy, "strategy");

        if (name.isBlank()) {
            throw new IllegalArgumentException(
                "SkillDescriptor name cannot be blank"
            );
        }

        if (goal.isBlank()) {
            throw new IllegalArgumentException(
                "SkillDescriptor goal cannot be blank"
            );
        }

        owner = owner == null ? "" : owner;
        status = status == null ? "DRAFT" : status;
    }

    /**
     * Create a descriptor from a Skill definition.
     */
    public static SkillDescriptor from(Skill skill) {
        Objects.requireNonNull(skill, "skill");

        return new SkillDescriptor(
            skill.skillId(),
            skill.name(),
            skill.version(),
            skill.goal(),
            skill.executionStrategy(),
            "",
            "DRAFT"
        );
    }

    /**
     * Create a descriptor with owner and status.
     */
    public static SkillDescriptor from(
        Skill skill,
        String owner,
        String status
    ) {
        Objects.requireNonNull(skill, "skill");

        return new SkillDescriptor(
            skill.skillId(),
            skill.name(),
            skill.version(),
            skill.goal(),
            skill.executionStrategy(),
            owner,
            status
        );
    }
}

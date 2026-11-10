package io.github.regalpine.agentforge.kernel;

import java.util.Objects;
import java.util.Optional;

/**
 * Skill 默认 record 实现.
 * ASM-SKILL-01 §4: Skill 语义定义.
 *
 * <p>Immutable record implementing the
 * {@link Skill} interface. Follows the
 * same pattern as CapabilityDefinition
 * implementing Capability.
 */
public record SkillDefinition(
    SkillId skillId,
    Version version,
    String name,
    String goal,
    SkillContract contract,
    SkillDependencies dependencies,
    SkillExecutionStrategy executionStrategy
) implements Skill {

    public SkillDefinition {
        Objects.requireNonNull(skillId, "skillId");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(goal, "goal");
        Objects.requireNonNull(contract, "contract");

        if (name.isBlank()) {
            throw new IllegalArgumentException(
                "Skill name cannot be blank"
            );
        }

        if (goal.isBlank()) {
            throw new IllegalArgumentException(
                "Skill goal cannot be blank"
            );
        }

        dependencies =
            dependencies == null
                ? SkillDependencies.empty()
                : dependencies;

        executionStrategy =
            executionStrategy == null
                ? SkillExecutionStrategy.DIRECT_CAPABILITY
                : executionStrategy;
    }

    @Override
    public EntityId id() {
        return skillId.toEntityId();
    }

    /**
     * Create a minimal SkillDefinition
     * with defaults.
     */
    public static SkillDefinition of(
        String skillId,
        String version,
        String name,
        String goal,
        SkillContract contract
    ) {
        return new SkillDefinition(
            new SkillId(skillId),
            new Version(version),
            name,
            goal,
            contract,
            SkillDependencies.empty(),
            SkillExecutionStrategy.DIRECT_CAPABILITY
        );
    }
}

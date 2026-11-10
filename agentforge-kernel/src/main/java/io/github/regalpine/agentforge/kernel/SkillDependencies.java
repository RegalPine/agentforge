package io.github.regalpine.agentforge.kernel;

import java.util.List;
import java.util.Set;

/**
 * Skill 依赖声明.
 * ASM-SKILL-01 §32: SkillDependencies.
 *
 * <p>Declares the Capability, Knowledge and
 * Skill dependencies required by a Skill.
 * Immutable — all sets are defensively copied.
 */
public record SkillDependencies(
    Set<CapabilityRef> capabilities,
    Set<Reference> knowledge,
    Set<SkillReference> skills
) {

    public SkillDependencies {
        capabilities =
            capabilities == null
                ? Set.of()
                : Set.copyOf(capabilities);

        knowledge =
            knowledge == null
                ? Set.of()
                : Set.copyOf(knowledge);

        skills =
            skills == null
                ? Set.of()
                : Set.copyOf(skills);
    }

    public static SkillDependencies empty() {
        return new SkillDependencies(
            Set.of(), Set.of(), Set.of()
        );
    }

    public static SkillDependencies of(
        Set<CapabilityRef> capabilities,
        Set<Reference> knowledge,
        Set<SkillReference> skills
    ) {
        return new SkillDependencies(
            capabilities, knowledge, skills
        );
    }

    /**
     * @return true if no dependencies declared.
     */
    public boolean isEmpty() {
        return capabilities.isEmpty()
            && knowledge.isEmpty()
            && skills.isEmpty();
    }
}

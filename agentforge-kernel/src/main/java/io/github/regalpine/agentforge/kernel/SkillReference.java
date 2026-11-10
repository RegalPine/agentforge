package io.github.regalpine.agentforge.kernel;

import java.util.Objects;

/**
 * Typed reference to a Skill entity.
 * ASM-SKILL-01 §8: SkillReference.
 *
 * <p>Provides type-safe skill references
 * distinct from generic {@link Reference}.
 * Carries skillId + version for cross-module
 * immutable reference passing.
 */
public record SkillReference(
    SkillId skillId,
    Version version
) {

    public SkillReference {
        Objects.requireNonNull(skillId, "skillId");
        Objects.requireNonNull(version, "version");
    }

    public static SkillReference of(
        String skillId,
        String version
    ) {
        return new SkillReference(
            new SkillId(skillId),
            new Version(version)
        );
    }

    public Reference toReference() {
        return new Reference(
            skillId.toEntityId(),
            version
        );
    }
}

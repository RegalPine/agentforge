package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.capability.SkillRegistry;
import io.github.regalpine.agentforge.kernel.ResolvedSkill;
import io.github.regalpine.agentforge.kernel.Schema;
import io.github.regalpine.agentforge.kernel.Skill;
import io.github.regalpine.agentforge.kernel.SkillContract;
import io.github.regalpine.agentforge.kernel.SkillDefinition;
import io.github.regalpine.agentforge.kernel.SkillExecutionStrategy;
import io.github.regalpine.agentforge.skill.governance.SkillLifecycleState;
import io.github.regalpine.agentforge.skill.governance.SkillPackageMetadata;
import io.github.regalpine.agentforge.kernel.SkillReference;

/**
 * Default SkillResolver implementation.
 * AF-SKILL-01 §6: resolve from Registry.
 * AF-AD-15 §6.2: Registry 解析绑定版本.
 *
 * <p>Resolves a SkillReference by looking
 * up the SkillRegistry, validating lifecycle
 * state, and building a ResolvedSkill.
 */
public final class DefaultSkillResolver
    implements SkillResolver {

    private final SkillRegistry registry;

    public DefaultSkillResolver(
        SkillRegistry registry
    ) {
        this.registry =
            java.util.Objects.requireNonNull(
                registry, "registry"
            );
    }

    @Override
    public ResolvedSkill resolve(
        SkillReference reference
    ) {
        java.util.Objects.requireNonNull(
            reference, "reference"
        );

        // Check lifecycle state
        SkillLifecycleState state;
        try {
            state = registry.state(reference);
        } catch (Exception e) {
            throw new SkillResolutionException(
                reference.skillId().value(),
                "Skill not found: "
                    + reference.skillId().value()
            );
        }

        // Revoked Skills cannot be resolved
        if (state ==
            SkillLifecycleState.REVOKED
        ) {
            throw new SkillResolutionException(
                reference.skillId().value(),
                "Skill is revoked: "
                    + reference.skillId().value()
            );
        }

        // Get metadata
        SkillPackageMetadata metadata =
            registry.metadata(reference)
                .orElseThrow(
                    () -> new SkillResolutionException(
                        reference.skillId().value(),
                        "Skill metadata not found"
                    )
                );

        // Build Skill definition
        Skill skill = toDefinition(metadata);

        return new ResolvedSkill(
            skill,
            reference,
            metadata.executionStrategy()
        );
    }

    private Skill toDefinition(
        SkillPackageMetadata metadata
    ) {
        return new SkillDefinition(
            metadata.skillId(),
            metadata.version(),
            metadata.name(),
            metadata.goal(),
            metadata.contract(),
            metadata.dependencies(),
            metadata.executionStrategy()
        );
    }
}

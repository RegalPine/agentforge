package io.github.regalpine.agentforge.skill.governance;

import io.github.regalpine.agentforge.kernel.SkillContract;
import io.github.regalpine.agentforge.kernel.SkillDefinition;
import io.github.regalpine.agentforge.kernel.SkillDependencies;
import io.github.regalpine.agentforge.kernel.SkillDescriptor;
import io.github.regalpine.agentforge.kernel.SkillExecutionStrategy;
import io.github.regalpine.agentforge.kernel.SkillId;
import io.github.regalpine.agentforge.kernel.Version;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Skill Package 元数据.
 * AF-SKILL-02 §10-§11: SkillManifest.
 * AF-AD-15 §5.4: Skill Package 结构.
 *
 * <p>Represents the parsed manifest.yaml
 * content. Declarative configuration only —
 * must NOT contain if/for/while/workflow
 * syntax/runtime instructions.
 *
 * <p>Immutable. All collections are
 * defensively copied.
 */
public record SkillPackageMetadata(
    SkillId skillId,
    Version version,
    String name,
    String goal,
    SkillContract contract,
    SkillDependencies dependencies,
    SkillExecutionStrategy executionStrategy,
    String owner,
    String domain,
    Set<String> tags,
    Map<String, String> metadata,
    URI artifactUri
) {

    public SkillPackageMetadata {
        Objects.requireNonNull(skillId, "skillId");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(goal, "goal");
        Objects.requireNonNull(
            contract, "contract"
        );

        if (name.isBlank()) {
            throw new IllegalArgumentException(
                "name cannot be blank"
            );
        }

        if (goal.isBlank()) {
            throw new IllegalArgumentException(
                "goal cannot be blank"
            );
        }

        dependencies =
            dependencies == null
                ? SkillDependencies.empty()
                : dependencies;

        executionStrategy =
            executionStrategy == null
                ? SkillExecutionStrategy
                    .DIRECT_CAPABILITY
                : executionStrategy;

        tags =
            tags == null
                ? Set.of()
                : Set.copyOf(tags);

        metadata =
            metadata == null
                ? Map.of()
                : Map.copyOf(metadata);

        owner = owner == null ? "" : owner;
        domain = domain == null ? "" : domain;
    }

    /**
     * Convert to a SkillDefinition.
     */
    public SkillDefinition toDefinition() {
        return new SkillDefinition(
            skillId, version, name, goal,
            contract, dependencies,
            executionStrategy
        );
    }

    /**
     * Convert to a SkillDescriptor.
     */
    public SkillDescriptor toDescriptor(
        String status
    ) {
        return new SkillDescriptor(
            skillId, name, version, goal,
            executionStrategy, owner, status
        );
    }
}

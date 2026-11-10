package io.github.regalpine.agentforge.sdk;

/**
 * SDK-level Skill descriptor.
 * AF-SKILL-04 §24: SkillDescriptor (SDK).
 *
 * <p>Developer-facing read-only view of
 * a Skill. Does not expose internal
 * kernel types.
 *
 * <p>Immutable.
 */
public record SkillDescriptor(
    String skillId,
    String name,
    String version,
    String goal,
    String strategy,
    String owner,
    String status
) {

    /**
     * Create from kernel descriptor.
     */
    public static SkillDescriptor from(
        io.github.regalpine.agentforge.kernel.SkillDescriptor kernel
    ) {
        return new SkillDescriptor(
            kernel.skillId().value(),
            kernel.name(),
            kernel.version().value(),
            kernel.goal(),
            kernel.strategy().name(),
            kernel.owner(),
            kernel.status()
        );
    }
}

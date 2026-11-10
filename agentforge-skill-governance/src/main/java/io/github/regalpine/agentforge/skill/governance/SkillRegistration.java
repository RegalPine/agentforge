package io.github.regalpine.agentforge.skill.governance;

import io.github.regalpine.agentforge.kernel.SkillDescriptor;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Skill 注册结果.
 * AF-SKILL-02 §6: SkillRegistration.
 * AF-AD-15 §5.1: register 返回值.
 *
 * <p>Returned by SkillRegistry.register().
 * Contains the registered descriptor,
 * lifecycle state, digest and timestamps.
 * Immutable.
 */
public record SkillRegistration(
    SkillDescriptor descriptor,
    SkillLifecycleState state,
    SkillDigest digest,
    Instant registeredAt,
    Instant updatedAt
) {

    public SkillRegistration {
        Objects.requireNonNull(
            descriptor, "descriptor"
        );
        Objects.requireNonNull(
            state, "state"
        );
        Objects.requireNonNull(
            digest, "digest"
        );
        Objects.requireNonNull(
            registeredAt, "registeredAt"
        );

        updatedAt =
            updatedAt == null
                ? registeredAt
                : updatedAt;
    }

    /**
     * Create a new registration at DRAFT.
     */
    public static SkillRegistration create(
        SkillDescriptor descriptor,
        SkillDigest digest
    ) {
        Instant now = Instant.now();
        return new SkillRegistration(
            descriptor,
            SkillLifecycleState.DRAFT,
            digest,
            now,
            now
        );
    }
}

package io.github.regalpine.agentforge.sdk;

import java.util.Objects;

/**
 * SDK-level Skill discovery query.
 * AF-SKILL-04 §22: SkillClient discover.
 *
 * <p>Developer-facing query object for
 * Skill discovery. Immutable.
 */
public record SkillDiscoveryQuery(
    String idPattern,
    String owner,
    String status
) {

    public SkillDiscoveryQuery {
        // all fields optional (nullable)
    }

    /**
     * Query all Skills.
     */
    public static SkillDiscoveryQuery all() {
        return new SkillDiscoveryQuery(
            null, null, null
        );
    }

    /**
     * Query by ID pattern.
     */
    public static SkillDiscoveryQuery byId(
        String idPattern
    ) {
        Objects.requireNonNull(idPattern);
        return new SkillDiscoveryQuery(
            idPattern, null, null
        );
    }

    /**
     * Query by owner.
     */
    public static SkillDiscoveryQuery byOwner(
        String owner
    ) {
        return new SkillDiscoveryQuery(
            null, owner, null
        );
    }

    /**
     * Convert to kernel SkillQuery.
     */
    public io.github.regalpine.agentforge.skill.governance.SkillQuery toKernelQuery() {
        return io.github.regalpine.agentforge.skill.governance.SkillQuery
            .builder()
            .idPattern(idPattern)
            .owner(owner)
            .build();
    }
}

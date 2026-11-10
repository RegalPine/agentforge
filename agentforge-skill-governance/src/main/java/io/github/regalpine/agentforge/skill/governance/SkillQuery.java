package io.github.regalpine.agentforge.skill.governance;

import io.github.regalpine.agentforge.kernel.SkillId;
import io.github.regalpine.agentforge.kernel.Version;

import java.util.Objects;
import java.util.Optional;

/**
 * Skill 查询条件.
 * AF-SKILL-02 §9: Skill Discovery.
 * AF-AD-15 §5.1: SkillRegistry discover.
 *
 * <p>Immutable query object for Skill
 * discovery. All fields are optional —
 * null means "no filter on this field".
 */
public record SkillQuery(
    String idPattern,
    Version version,
    String owner,
    String domain,
    String riskLevel,
    SkillLifecycleState lifecycleState,
    String tag
) {

    public SkillQuery {
        // all fields are optional (nullable)
        // idPattern is a simple substring
        // match, not a full regex
    }

    /**
     * Empty query — matches all Skills.
     */
    public static SkillQuery all() {
        return new SkillQuery(
            null, null, null, null,
            null, null, null
        );
    }

    /**
     * Query by exact SkillId.
     */
    public static SkillQuery byId(
        SkillId skillId
    ) {
        Objects.requireNonNull(skillId);
        return new SkillQuery(
            skillId.value(), null, null,
            null, null, null, null
        );
    }

    /**
     * Query by owner.
     */
    public static SkillQuery byOwner(
        String owner
    ) {
        return new SkillQuery(
            null, null, owner, null,
            null, null, null
        );
    }

    /**
     * Query by lifecycle state.
     */
    public static SkillQuery byState(
        SkillLifecycleState state
    ) {
        Objects.requireNonNull(state);
        return new SkillQuery(
            null, null, null, null,
            null, state, null
        );
    }

    /**
     * Builder for complex queries.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String idPattern;
        private Version version;
        private String owner;
        private String domain;
        private String riskLevel;
        private SkillLifecycleState state;
        private String tag;

        public Builder idPattern(String v) {
            idPattern = v; return this;
        }
        public Builder version(Version v) {
            version = v; return this;
        }
        public Builder owner(String v) {
            owner = v; return this;
        }
        public Builder domain(String v) {
            domain = v; return this;
        }
        public Builder riskLevel(String v) {
            riskLevel = v; return this;
        }
        public Builder lifecycleState(
            SkillLifecycleState v
        ) {
            state = v; return this;
        }
        public Builder tag(String v) {
            tag = v; return this;
        }

        public SkillQuery build() {
            return new SkillQuery(
                idPattern, version, owner,
                domain, riskLevel, state, tag
            );
        }
    }
}

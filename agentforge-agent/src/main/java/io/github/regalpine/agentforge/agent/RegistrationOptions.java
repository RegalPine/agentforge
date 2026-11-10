package io.github.regalpine.agentforge.agent;

import io.github.regalpine.agentforge.skill.governance.SkillGovernanceProfile;

import java.util.Map;
import java.util.Objects;

/**
 * Options supplied when registering an Agent
 * through {@link AgentRegistrar}.
 *
 * <p>Carries optional governance binding,
 * tags, tenant id, and auto-start flag.
 *
 * <p>Immutable. All maps are defensively
 * copied.
 */
public record RegistrationOptions(
    SkillGovernanceProfile governance,
    Map<String, String> tags,
    String tenantId,
    boolean autoStart
) {

    public RegistrationOptions {
        tags = tags == null
            ? Map.of()
            : Map.copyOf(tags);
    }

    /**
     * Create default options:
     * no governance, no tags, no tenant,
     * autoStart = true.
     */
    public static RegistrationOptions defaults() {
        return new RegistrationOptions(
            null, Map.of(), null, true
        );
    }

    /**
     * Create options with autoStart flag.
     */
    public static RegistrationOptions of(
        boolean autoStart
    ) {
        return new RegistrationOptions(
            null, Map.of(), null, autoStart
        );
    }

    /**
     * Builder for fluent construction.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private SkillGovernanceProfile governance;
        private Map<String, String> tags = Map.of();
        private String tenantId;
        private boolean autoStart = true;

        public Builder governance(
            SkillGovernanceProfile governance
        ) {
            this.governance = governance;
            return this;
        }

        public Builder tags(Map<String, String> tags) {
            this.tags = Objects.requireNonNull(tags);
            return this;
        }

        public Builder tag(String key, String value) {
            var next = new java.util.HashMap<>(tags);
            next.put(
                Objects.requireNonNull(key),
                Objects.requireNonNull(value)
            );
            this.tags = Map.copyOf(next);
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder autoStart(boolean autoStart) {
            this.autoStart = autoStart;
            return this;
        }

        public RegistrationOptions build() {
            return new RegistrationOptions(
                governance, tags, tenantId, autoStart
            );
        }
    }
}

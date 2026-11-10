package io.github.regalpine.agentforge.sdk;

import java.util.Map;
import java.util.Objects;

/**
 * Configuration for bootstrapping AgentForge SDK.
 * <p>
 * Uses builder pattern. Does not create a new DSL — this is just configuration.
 */
public final class AgentForgeConfig {

    private final String applicationId;
    private final String tenantId;
    private final Map<String, Object> modelConfig;
    private final Map<String, Object> governanceConfig;
    private final Map<String, Object> observabilityConfig;
    private final Map<String, Object> properties;

    private AgentForgeConfig(Builder builder) {
        this.applicationId = builder.applicationId != null ? builder.applicationId : "default";
        this.tenantId = builder.tenantId != null ? builder.tenantId : "default";
        this.modelConfig = builder.modelConfig == null ? Map.of() : Map.copyOf(builder.modelConfig);
        this.governanceConfig = builder.governanceConfig == null ? Map.of() : Map.copyOf(builder.governanceConfig);
        this.observabilityConfig = builder.observabilityConfig == null ? Map.of() : Map.copyOf(builder.observabilityConfig);
        this.properties = builder.properties == null ? Map.of() : Map.copyOf(builder.properties);
    }

    public String applicationId() {
        return applicationId;
    }

    public String tenantId() {
        return tenantId;
    }

    public Map<String, Object> modelConfig() {
        return modelConfig;
    }

    public Map<String, Object> governanceConfig() {
        return governanceConfig;
    }

    public Map<String, Object> observabilityConfig() {
        return observabilityConfig;
    }

    public Map<String, Object> properties() {
        return properties;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static AgentForgeConfig defaults() {
        return builder().build();
    }

    public static final class Builder {
        private String applicationId;
        private String tenantId;
        private Map<String, Object> modelConfig;
        private Map<String, Object> governanceConfig;
        private Map<String, Object> observabilityConfig;
        private Map<String, Object> properties;

        private Builder() {}

        public Builder applicationId(String applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder modelConfig(Map<String, Object> modelConfig) {
            this.modelConfig = modelConfig;
            return this;
        }

        public Builder governanceConfig(Map<String, Object> governanceConfig) {
            this.governanceConfig = governanceConfig;
            return this;
        }

        public Builder observabilityConfig(Map<String, Object> observabilityConfig) {
            this.observabilityConfig = observabilityConfig;
            return this;
        }

        public Builder properties(Map<String, Object> properties) {
            this.properties = properties;
            return this;
        }

        public AgentForgeConfig build() {
            return new AgentForgeConfig(this);
        }
    }
}

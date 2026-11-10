package io.github.regalpine.agentforge.a2a;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Agent Card — external agent metadata.
 * AF-ENHANCE-10 §28: Agent Card.
 *
 * <p>Describes an external agent's
 * identity, capabilities, endpoint,
 * and trust level. Not registered
 * as an internal agent instance.
 */
public record AgentCard(
    String agentId,
    String name,
    String description,
    String version,
    String endpoint,
    Set<String> capabilities,
    ExternalAgentTrust trust,
    Map<String, Object> metadata
) {

    public AgentCard {
        Objects.requireNonNull(
            agentId, "agentId"
        );
        Objects.requireNonNull(
            name, "name"
        );
        Objects.requireNonNull(
            endpoint, "endpoint"
        );

        description = description == null
            ? "" : description;
        version = version == null
            ? "1.0.0" : version;
        capabilities = capabilities == null
            ? Set.of() : Set.copyOf(capabilities);
        trust = trust == null
            ? ExternalAgentTrust.UNTRUSTED
            : trust;
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Check if the agent has a
     * specific capability.
     */
    public boolean hasCapability(
        String capability
    ) {
        return capabilities.contains(
            capability
        );
    }

    /**
     * Create a builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String agentId;
        private String name;
        private String description;
        private String version;
        private String endpoint;
        private Set<String> capabilities;
        private ExternalAgentTrust trust;
        private Map<String, Object> metadata;

        private Builder() {}

        public Builder agentId(
            String agentId
        ) {
            this.agentId = agentId;
            return this;
        }

        public Builder name(
            String name
        ) {
            this.name = name;
            return this;
        }

        public Builder description(
            String description
        ) {
            this.description = description;
            return this;
        }

        public Builder version(
            String version
        ) {
            this.version = version;
            return this;
        }

        public Builder endpoint(
            String endpoint
        ) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder capabilities(
            Set<String> capabilities
        ) {
            this.capabilities = capabilities;
            return this;
        }

        public Builder trust(
            ExternalAgentTrust trust
        ) {
            this.trust = trust;
            return this;
        }

        public Builder metadata(
            Map<String, Object> metadata
        ) {
            this.metadata = metadata;
            return this;
        }

        public AgentCard build() {
            return new AgentCard(
                agentId, name, description,
                version, endpoint, capabilities,
                trust, metadata
            );
        }
    }
}

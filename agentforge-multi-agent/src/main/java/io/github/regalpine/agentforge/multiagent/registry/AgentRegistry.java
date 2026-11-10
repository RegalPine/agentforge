package io.github.regalpine.agentforge.multiagent.registry;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * AgentRegistry — AF-ENHANCE-08 §24.
 *
 * Registry for agent definitions.
 * Responsible for:
 * Identity, Metadata, Capabilities,
 * Version, Status, Policy Metadata.
 *
 * <p>Does NOT handle Agent Execution.
 */
public interface AgentRegistry {

    /**
     * Register an agent definition.
     */
    void register(
        AgentDefinition definition
    );

    /**
     * Get an agent by ID.
     */
    Optional<AgentDefinition> get(
        String agentId
    );

    /**
     * Find agents matching a query.
     */
    List<AgentDefinition> find(
        AgentQuery query
    );

    /**
     * Unregister an agent.
     */
    boolean unregister(String agentId);

    /**
     * List all registered agents.
     */
    List<AgentDefinition> listAll();

    /**
     * Agent definition record.
     * AF-ENHANCE-08 §24.
     */
    record AgentDefinition(
        String agentId,
        String name,
        String version,
        String description,
        Set<String> capabilities,
        Set<String> skills,
        Map<String, String> metadata,
        AgentStatus status
    ) {
        public AgentDefinition {
            Objects.requireNonNull(
                agentId, "agentId"
            );
            Objects.requireNonNull(
                name, "name"
            );

            version = version == null
                ? "1.0.0" : version;

            capabilities = capabilities == null
                ? Set.of()
                : Set.copyOf(capabilities);

            skills = skills == null
                ? Set.of()
                : Set.copyOf(skills);

            metadata = metadata == null
                ? Map.of()
                : Map.copyOf(metadata);

            status = status == null
                ? AgentStatus.ACTIVE
                : status;
        }

        /**
         * Create a definition.
         */
        public static AgentDefinition of(
            String agentId,
            String name
        ) {
            return new AgentDefinition(
                agentId, name,
                null, null, null, null,
                null, null
            );
        }
    }

    /**
     * Agent status in registry.
     */
    enum AgentStatus {
        ACTIVE,
        INACTIVE,
        DEPRECATED,
        SUSPENDED
    }
}

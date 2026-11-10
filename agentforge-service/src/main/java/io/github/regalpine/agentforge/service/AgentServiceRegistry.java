package io.github.regalpine.agentforge.service;

import java.util.List;
import java.util.Optional;

/**
 * Agent Service registry.
 * AF-ENHANCE-11 §63: Service Registry.
 */
public interface AgentServiceRegistry {

    /**
     * Register a service definition.
     */
    void register(
        AgentServiceDefinition definition
    );

    /**
     * Get a service definition by ID.
     */
    Optional<AgentServiceDefinition> get(
        AgentServiceId id
    );

    /**
     * List all registered services.
     */
    List<AgentServiceDefinition> list();

    /**
     * Disable a service.
     */
    void disable(AgentServiceId id);

    /**
     * Unregister a service.
     */
    void unregister(AgentServiceId id);
}

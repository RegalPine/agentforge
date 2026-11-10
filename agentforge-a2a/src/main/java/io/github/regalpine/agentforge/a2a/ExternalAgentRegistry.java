package io.github.regalpine.agentforge.a2a;

import java.util.List;
import java.util.Optional;

/**
 * External agent registry.
 * AF-ENHANCE-10 §27-28: Agent
 * Discovery / Agent Card.
 *
 * <p>Manages external agent
 * registrations (Agent Cards).
 * External agents are NOT
 * registered as internal
 * agent instances.
 */
public interface ExternalAgentRegistry {

    /**
     * Register an external agent.
     */
    void register(AgentCard card);

    /**
     * Get an agent card by ID.
     */
    Optional<AgentCard> get(String agentId);

    /**
     * List all registered agents.
     */
    List<AgentCard> list();

    /**
     * Find agents by capability.
     */
    List<AgentCard> findByCapability(
        String capability
    );

    /**
     * Unregister an agent.
     */
    void unregister(String agentId);
}

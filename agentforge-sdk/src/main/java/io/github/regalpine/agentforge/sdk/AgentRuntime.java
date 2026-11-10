package io.github.regalpine.agentforge.sdk;

import io.github.regalpine.agentforge.sdk.bootstrap.BootstrapLifecycle;
import io.github.regalpine.agentforge.sdk.bootstrap.BootstrapState;

import java.util.*;

/**
 * Agent runtime container.
 * AF-ENHANCE-01 §16: AgentRuntime.
 *
 * <p>Manages a set of Agents with
 * a Bootstrap lifecycle. Provides
 * lookup and execution capabilities.
 */
public final class AgentRuntime
    implements AutoCloseable {

    private final List<Agent> agents;
    private final AgentForgeConfig config;
    private final BootstrapLifecycle lifecycle;
    private final Map<String, Agent> agentIndex;

    AgentRuntime(
        List<Agent> agents,
        AgentForgeConfig config
    ) {
        this.agents = agents;
        this.config = config;
        this.lifecycle = new BootstrapLifecycle();

        Map<String, Agent> index =
            new LinkedHashMap<>();
        for (Agent a : agents) {
            index.put(a.id(), a);
        }
        this.agentIndex = Map.copyOf(index);

        // Bootstrap to READY
        lifecycle.start();
    }

    /**
     * Get an agent by ID.
     */
    public Agent agent(String agentId) {
        Agent agent = agentIndex.get(agentId);
        if (agent == null) {
            throw new ResolutionException(
                "Agent", agentId
            );
        }
        return agent;
    }

    /**
     * List all agent IDs.
     */
    public List<String> agentIds() {
        return List.copyOf(agentIndex.keySet());
    }

    /**
     * Get the bootstrap lifecycle.
     */
    public BootstrapLifecycle lifecycle() {
        return lifecycle;
    }

    /**
     * Get the runtime configuration.
     */
    public AgentForgeConfig config() {
        return config;
    }

    /**
     * Run an agent by ID with input.
     */
    public ExecutionResult run(
        String agentId, String message
    ) {
        return agent(agentId).execute(
            AgentInput.of(message)
        );
    }

    @Override
    public void close() {
        lifecycle.stop();
    }
}

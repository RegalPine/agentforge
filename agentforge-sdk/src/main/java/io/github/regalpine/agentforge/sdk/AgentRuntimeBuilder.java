package io.github.regalpine.agentforge.sdk;

import io.github.regalpine.agentforge.sdk.bootstrap.BootstrapLifecycle;

import java.util.*;

/**
 * Builder for AgentRuntime.
 * AF-ENHANCE-01 §16: AgentRuntime Builder.
 *
 * <p>Usage:
 * <pre>{@code
 * AgentRuntime runtime = AgentForge.runtime()
 *     .agent(agent)
 *     .build();
 * }</pre>
 */
public final class AgentRuntimeBuilder {

    private final List<Agent> agents =
        new ArrayList<>();

    private AgentForgeConfig config;

    AgentRuntimeBuilder() {}

    /**
     * Add an agent to the runtime.
     */
    public AgentRuntimeBuilder agent(Agent agent) {
        agents.add(
            Objects.requireNonNull(agent, "agent")
        );
        return this;
    }

    /**
     * Add multiple agents to the runtime.
     */
    public AgentRuntimeBuilder agents(
        Agent... agents
    ) {
        Collections.addAll(this.agents, agents);
        return this;
    }

    /**
     * Set the runtime configuration.
     */
    public AgentRuntimeBuilder config(
        AgentForgeConfig config
    ) {
        this.config = config;
        return this;
    }

    /**
     * Build the AgentRuntime.
     */
    public AgentRuntime build() {
        AgentForgeConfig cfg = config != null
            ? config : AgentForgeConfig.defaults();

        return new AgentRuntime(
            List.copyOf(agents), cfg
        );
    }
}

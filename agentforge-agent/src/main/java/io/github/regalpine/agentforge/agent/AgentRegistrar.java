package io.github.regalpine.agentforge.agent;

import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.skill.governance.SkillGovernanceProfile;

/**
 * Platform-level Agent registrar SPI.
 *
 * <p>Extends the basic {@link AgentRegistry}
 * CRUD with lifecycle, health, metrics,
 * and governance binding.
 *
 * <p>Implementations are provided by the
 * server module (e.g.
 * {@code AgentForgeAgentRegistrar}).
 * User code programs to this interface.
 *
 * <p>N: Event publishing is intentionally
 * NOT part of this SPI. The server-side
 * implementation publishes events as a
 * side effect of lifecycle transitions.
 */
public interface AgentRegistrar
    extends AgentRegistry {

    /**
     * Register an Agent with options.
     *
     * @param agent   the Agent definition
     * @param options registration options
     *   (governance, tags, tenant, autoStart)
     */
    void register(
        AgentDefinition agent,
        RegistrationOptions options
    );

    /**
     * Register and immediately start
     * an Agent.
     *
     * @param agent   the Agent definition
     * @param options registration options
     * @return a handle for subsequent
     *   lifecycle operations
     */
    default RegistrationHandle registerAndStart(
        AgentDefinition agent,
        RegistrationOptions options
    ) {
        register(agent, options);
        start(agentReference(agent));
        return new RegistrationHandle(
            agentReference(agent)
        );
    }

    // ── Lifecycle ──

    /**
     * Transition an Agent to STARTING
     * then ACTIVE.
     *
     * @throws IllegalStateException if
     *   the Agent is not registered or
     *   the transition is illegal
     */
    void start(Reference reference);

    /**
     * Transition an Agent to STOPPED.
     * Refuses new invocations immediately.
     */
    void stop(Reference reference);

    /**
     * Transition an Agent to DRAINING.
     * Refuses new invocations but lets
     * in-flight work complete before
     * moving to STOPPED.
     */
    void drain(Reference reference);

    /**
     * Query the current lifecycle state.
     *
     * @return the state, or
     *   {@code null} if not registered
     */
    AgentLifecycleState lifecycleState(
        Reference reference
    );

    // ── Health & Metrics ──

    /**
     * Snapshot the Agent's health.
     */
    AgentHealth health(Reference reference);

    /**
     * Snapshot the Agent's runtime metrics.
     */
    AgentMetrics metrics(Reference reference);

    // ── Governance ──

    /**
     * Bind a governance profile to a
     * registered Agent.
     *
     * <p>Typically called at registration
     * time via {@link RegistrationOptions},
     * but may be re-bound later.
     */
    void bindGovernance(
        Reference reference,
        SkillGovernanceProfile governance
    );

    // ── Events ──

    /**
     * Subscribe to lifecycle and invocation
     * events for an Agent.
     */
    void subscribe(
        Reference reference,
        AgentEventListener listener
    );

    /**
     * Derive the {@link Reference} for an
     * Agent definition.
     */
    private static Reference agentReference(
        AgentDefinition agent
    ) {
        return new Reference(
            agent.id(), agent.version()
        );
    }
}

package io.github.regalpine.agentforge.server.agent;

import io.github.regalpine.agentforge.agent.AgentDefinition;
import io.github.regalpine.agentforge.agent.AgentHealth;
import io.github.regalpine.agentforge.agent.AgentLifecycleState;
import io.github.regalpine.agentforge.agent.AgentMetrics;
import io.github.regalpine.agentforge.agent.AgentRegistrar;
import io.github.regalpine.agentforge.agent.InMemoryAgentRegistrar;
import io.github.regalpine.agentforge.agent.RegistrationHandle;
import io.github.regalpine.agentforge.agent.RegistrationOptions;
import io.github.regalpine.agentforge.event.EventPublisher;
import io.github.regalpine.agentforge.event.ExecutionEvent;
import io.github.regalpine.agentforge.event.ExecutionId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.skill.governance.SkillGovernanceProfile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Server-side {@link AgentRegistrar}
 * implementation.
 *
 * <p>Delegates core storage to
 * {@link InMemoryAgentRegistrar} and
 * publishes lifecycle events through
 * the platform {@link EventPublisher}
 * as a side effect.
 *
 * <p>This is the bridge between the
 * Agent SPI (agent module) and the
 * platform event bus (event module).
 */
public final class AgentForgeAgentRegistrar
    implements AgentRegistrar {

    private final InMemoryAgentRegistrar delegate;
    private final EventPublisher eventPublisher;

    public AgentForgeAgentRegistrar(
        EventPublisher eventPublisher
    ) {
        this(
            new InMemoryAgentRegistrar(),
            Objects.requireNonNull(
                eventPublisher, "eventPublisher"
            )
        );
    }

    public AgentForgeAgentRegistrar(
        InMemoryAgentRegistrar delegate,
        EventPublisher eventPublisher
    ) {
        this.delegate = Objects.requireNonNull(
            delegate, "delegate"
        );
        this.eventPublisher = Objects.requireNonNull(
            eventPublisher, "eventPublisher"
        );
    }

    // ── CRUD ──

    @Override
    public void register(AgentDefinition agent) {
        delegate.register(agent);
        publish(agent, "AGENT_REGISTERED");
    }

    @Override
    public void register(
        AgentDefinition agent,
        RegistrationOptions options
    ) {
        delegate.register(agent, options);
        publish(agent, "AGENT_REGISTERED");
    }

    @Override
    public RegistrationHandle registerAndStart(
        AgentDefinition agent,
        RegistrationOptions options
    ) {
        RegistrationHandle handle =
            delegate.registerAndStart(agent, options);
        publish(agent, "AGENT_REGISTERED");
        publish(agent, "AGENT_STARTED");
        return handle;
    }

    @Override
    public Optional<AgentDefinition> find(
        Reference reference
    ) {
        return delegate.find(reference);
    }

    @Override
    public List<AgentDefinition> list() {
        return delegate.list();
    }

    @Override
    public void unregister(Reference reference) {
        Optional<AgentDefinition> def = find(reference);
        delegate.unregister(reference);
        def.ifPresent(d -> publish(d, "AGENT_UNREGISTERED"));
    }

    // ── Lifecycle ──

    @Override
    public void start(Reference reference) {
        delegate.start(reference);
        publishByRef(reference, "AGENT_STARTED");
    }

    @Override
    public void stop(Reference reference) {
        delegate.stop(reference);
        publishByRef(reference, "AGENT_STOPPED");
    }

    @Override
    public void drain(Reference reference) {
        delegate.drain(reference);
        publishByRef(reference, "AGENT_DRAINED");
    }

    @Override
    public AgentLifecycleState lifecycleState(
        Reference reference
    ) {
        return delegate.lifecycleState(reference);
    }

    // ── Health & Metrics ──

    @Override
    public AgentHealth health(Reference reference) {
        return delegate.health(reference);
    }

    @Override
    public AgentMetrics metrics(Reference reference) {
        return delegate.metrics(reference);
    }

    // ── Governance ──

    @Override
    public void bindGovernance(
        Reference reference,
        SkillGovernanceProfile governance
    ) {
        delegate.bindGovernance(reference, governance);
        publishByRef(reference, "AGENT_GOVERNANCE_BOUND");
    }

    /**
     * Query the governance profile bound
     * to an Agent (delegates to
     * {@link InMemoryAgentRegistrar}).
     */
    public SkillGovernanceProfile governanceOf(
        Reference reference
    ) {
        return delegate.governanceOf(reference);
    }

    // ── Events ──

    @Override
    public void subscribe(
        Reference reference,
        io.github.regalpine.agentforge.agent.AgentEventListener listener
    ) {
        delegate.subscribe(reference, listener);
    }

    // ── Event publishing ──

    private void publish(
        AgentDefinition agent,
        String type
    ) {
        publishEvent(
            new ExecutionId(
                "agent:" + agent.id().value()
                    + ":" + agent.version().value()
            ),
            type,
            Map.of(
                "agentId", agent.id().value(),
                "version", agent.version().value(),
                "name", agent.name()
            )
        );
    }

    private void publishByRef(
        Reference reference,
        String type
    ) {
        publishEvent(
            new ExecutionId(
                "agent:" + reference.id().value()
                    + ":" + reference.version().value()
            ),
            type,
            Map.of(
                "agentId", reference.id().value(),
                "version", reference.version().value()
            )
        );
    }

    private void publishEvent(
        ExecutionId executionId,
        String type,
        Map<String, Object> payload
    ) {
        try {
            eventPublisher.publish(new ExecutionEvent(
                executionId,
                0L,
                Instant.now(),
                type,
                payload
            ));
        } catch (RuntimeException ignored) {
            // Event publishing failure must not
            // break the registrar operation.
        }
    }
}

package io.github.regalpine.agentforge.agent;

import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.skill.governance.SkillGovernanceProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory {@link AgentRegistrar}
 * implementation.
 *
 * <p>Suitable for tests, demos, and
 * single-process deployments that do
 * not need persistence.
 *
 * <p>Thread-safe: backed by
 * {@link ConcurrentHashMap}.
 */
public final class InMemoryAgentRegistrar
    implements AgentRegistrar {

    private final Map<Reference, AgentDefinition> agents =
        new ConcurrentHashMap<>();

    private final Map<Reference, AgentLifecycleState> states =
        new ConcurrentHashMap<>();

    private final Map<Reference, SkillGovernanceProfile> governance =
        new ConcurrentHashMap<>();

    private final Map<Reference, List<AgentEventListener>> listeners =
        new ConcurrentHashMap<>();

    private final Map<Reference, AgentMetrics> metrics =
        new ConcurrentHashMap<>();

    @Override
    public void register(AgentDefinition agent) {
        register(
            Objects.requireNonNull(agent, "agent"),
            RegistrationOptions.defaults()
        );
    }

    @Override
    public void register(
        AgentDefinition agent,
        RegistrationOptions options
    ) {
        Objects.requireNonNull(agent, "agent");
        Objects.requireNonNull(options, "options");

        Reference ref = ref(agent);
        AgentDefinition previous =
            agents.putIfAbsent(ref, agent);
        if (previous != null) {
            throw new IllegalStateException(
                "Agent already registered: " + ref
            );
        }

        states.put(ref, AgentLifecycleState.REGISTERED);
        metrics.put(ref, AgentMetrics.empty(ref));

        if (options.governance() != null) {
            governance.put(ref, options.governance());
        }

        if (options.autoStart()) {
            start(ref);
        }

        emit(ref, AgentEventListener.AgentEventKind.REGISTERED);
    }

    @Override
    public Optional<AgentDefinition> find(
        Reference reference
    ) {
        return Optional.ofNullable(
            agents.get(reference)
        );
    }

    @Override
    public List<AgentDefinition> list() {
        return List.copyOf(
            new ArrayList<>(agents.values())
        );
    }

    @Override
    public void unregister(Reference reference) {
        agents.remove(reference);
        states.remove(reference);
        governance.remove(reference);
        listeners.remove(reference);
        metrics.remove(reference);
    }

    // ── Lifecycle ──

    @Override
    public void start(Reference reference) {
        AgentLifecycleState current = states.get(reference);
        if (current == null) {
            throw new IllegalStateException(
                "Agent not registered: " + reference
            );
        }
        // Idempotent: already runnable
        if (current == AgentLifecycleState.ACTIVE) {
            return;
        }
        // STOPPED / FAILED must re-enter through REGISTERED
        if (current == AgentLifecycleState.STOPPED
            || current == AgentLifecycleState.FAILED) {
            states.put(reference, AgentLifecycleState.REGISTERED);
            current = AgentLifecycleState.REGISTERED;
        }
        if (current == AgentLifecycleState.REGISTERED) {
            transition(reference, AgentLifecycleState.STARTING);
            transition(reference, AgentLifecycleState.ACTIVE);
            emit(reference, AgentEventListener.AgentEventKind.STARTED);
            return;
        }
        if (current == AgentLifecycleState.STARTING) {
            transition(reference, AgentLifecycleState.ACTIVE);
            emit(reference, AgentEventListener.AgentEventKind.STARTED);
            return;
        }
        // DRAINING → ACTIVE (recovery) is handled by transition()
        transition(reference, AgentLifecycleState.ACTIVE);
        emit(reference, AgentEventListener.AgentEventKind.STARTED);
    }

    @Override
    public void stop(Reference reference) {
        AgentLifecycleState current = states.get(reference);
        if (current == null) {
            // Tolerate stop of unknown Agent (no-op)
            return;
        }
        if (current.isTerminal()) {
            return;
        }
        transition(reference, AgentLifecycleState.STOPPED);
        emit(reference, AgentEventListener.AgentEventKind.STOPPED);
    }

    @Override
    public void drain(Reference reference) {
        AgentLifecycleState current = states.get(reference);
        if (current == null) return;
        transition(reference, AgentLifecycleState.DRAINING);
        emit(reference, AgentEventListener.AgentEventKind.DRAINED);
    }

    @Override
    public AgentLifecycleState lifecycleState(
        Reference reference
    ) {
        return states.get(reference);
    }

    // ── Health & Metrics ──

    @Override
    public AgentHealth health(Reference reference) {
        AgentLifecycleState state = states.get(reference);
        if (state == null) {
            return new AgentHealth(
                reference,
                AgentHealth.AgentHealthStatus.DOWN,
                Map.of("registry",
                    AgentHealth.AgentComponentHealth.down(
                        "registry", "Agent not registered"))
            );
        }
        AgentHealth.AgentHealthStatus status =
            state.isRunnable()
                ? AgentHealth.AgentHealthStatus.UP
                : state.isTerminal()
                    ? AgentHealth.AgentHealthStatus.DOWN
                    : AgentHealth.AgentHealthStatus.DEGRADED;
        return new AgentHealth(
            reference,
            status,
            Map.of("lifecycle",
                AgentHealth.AgentComponentHealth.up("lifecycle"))
        );
    }

    @Override
    public AgentMetrics metrics(Reference reference) {
        AgentMetrics m = metrics.get(reference);
        return m != null
            ? m
            : AgentMetrics.empty(reference);
    }

    // ── Governance ──

    @Override
    public void bindGovernance(
        Reference reference,
        SkillGovernanceProfile profile
    ) {
        Objects.requireNonNull(profile, "profile");
        requireRegistered(reference);
        governance.put(reference, profile);
        emit(reference,
            AgentEventListener.AgentEventKind.GOVERNANCE_BOUND);
    }

    /**
     * Query the governance profile bound
     * to an Agent (may be null).
     */
    public SkillGovernanceProfile governanceOf(
        Reference reference
    ) {
        return governance.get(reference);
    }

    // ── Events ──

    @Override
    public void subscribe(
        Reference reference,
        AgentEventListener listener
    ) {
        Objects.requireNonNull(listener, "listener");
        requireRegistered(reference);
        listeners
            .computeIfAbsent(reference, k -> new ArrayList<>())
            .add(listener);
    }

    // ── Internals ──

    private void transition(
        Reference reference,
        AgentLifecycleState target
    ) {
        AgentLifecycleState current = states.get(reference);
        if (current == null) {
            throw new IllegalStateException(
                "Agent not registered: " + reference
            );
        }
        AgentLifecycleState next = current.transitionTo(target);
        states.put(reference, next);
    }

    private void requireRegistered(Reference reference) {
        if (!states.containsKey(reference)) {
            throw new IllegalStateException(
                "Agent not registered: " + reference
            );
        }
    }

    private void emit(
        Reference reference,
        AgentEventListener.AgentEventKind kind
    ) {
        List<AgentEventListener> subs = listeners.get(reference);
        if (subs == null || subs.isEmpty()) return;
        var event = AgentEventListener.AgentRegistrationEvent.of(
            kind, reference
        );
        for (AgentEventListener l : subs) {
            try {
                l.onEvent(event);
            } catch (RuntimeException ignored) {
                // Listener exceptions do not affect registrar
            }
        }
    }

    private static Reference ref(AgentDefinition agent) {
        return new Reference(agent.id(), agent.version());
    }
}

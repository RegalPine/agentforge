package io.github.regalpine.agentforge.agent;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Listener for Agent lifecycle and
 * invocation events.
 *
 * <p>Used with
 * {@link AgentRegistrar#subscribe}
 * to observe Agent behavior from
 * outside the runtime.
 *
 * <p>Functional interface.
 */
@FunctionalInterface
public interface AgentEventListener {

    /**
     * Called when an event occurs on
     * the subscribed Agent.
     *
     * @param event the event
     */
    void onEvent(AgentRegistrationEvent event);

    /**
     * Event payload delivered to listeners.
     *
     * <p>Immutable record.
     */
    record AgentRegistrationEvent(
        AgentEventKind kind,
        io.github.regalpine.agentforge.kernel.Reference reference,
        Instant timestamp,
        Map<String, Object> payload
    ) {

        public AgentRegistrationEvent {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(reference, "reference");
            Objects.requireNonNull(timestamp, "timestamp");
            payload = payload == null
                ? Map.of()
                : Map.copyOf(payload);
        }

        /**
         * Create an event with empty payload.
         */
        public static AgentRegistrationEvent of(
            AgentEventKind kind,
            io.github.regalpine.agentforge.kernel.Reference reference
        ) {
            return new AgentRegistrationEvent(
                kind, reference,
                Instant.now(),
                Map.of()
            );
        }
    }

    /**
     * Event kinds emitted by the registrar.
     */
    enum AgentEventKind {
        REGISTERED,
        STARTED,
        STOPPED,
        DRAINED,
        FAILED,
        INVOKED,
        GOVERNANCE_BOUND
    }
}

package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.event.ExecutionState;
import io.github.regalpine.agentforge.event.ExecutionEvent;

import java.util.List;
import java.util.Map;

/**
 * Atomic commit port for durable execution state transitions.
 * Ensures that state update + event publish happen atomically.
 * G2-5: Unified transactional boundary for state + event + dispatch intent.
 */
public interface CommitPort {

    /**
     * Atomically commit a state transition and its associated event.
     * Both operations succeed or fail together.
     *
     * @param command the commit command containing state, event, and expected version
     * @return true if the commit succeeded (CAS matched), false if version conflict
     */
    boolean commit(CommitCommand command);

    /**
     * Persist the initial execution state so that subsequent
     * {@link #commit(CommitCommand)} CAS transitions can find it.
     * <p>
     * Default is no-op (in-memory implementations override this).
     * JDBC implementations insert the initial row into execution_state.
     */
    default void create(ExecutionState initialState) {}

    /**
     * A commit command bundles a state transition with its triggering event,
     * optional lease fencing, activation changes, event batches and dispatch intent.
     * <p>
     * Runtime defines the protocol data only; JDBC/Redis adapters live outside.
     */
    record CommitCommand(
        ExecutionState newState,
        ExecutionEvent event,
        long expectedVersion,
        LeaseFence leaseFence,
        List<ActivationChange> activationChanges,
        List<ExecutionEvent> additionalEvents,
        List<DispatchIntent> dispatchIntents,
        Map<String, Object> outboxEntries
    ) {
        /** Backward-compatible 3-arg constructor. */
        public CommitCommand(ExecutionState newState, ExecutionEvent event, long expectedVersion) {
            this(newState, event, expectedVersion, null, List.of(), List.of(), List.of(), Map.of());
        }

        public CommitCommand {
            if (activationChanges == null) activationChanges = List.of();
            if (additionalEvents == null) additionalEvents = List.of();
            if (dispatchIntents == null) dispatchIntents = List.of();
            if (outboxEntries == null) outboxEntries = Map.of();
            activationChanges = List.copyOf(activationChanges);
            additionalEvents = List.copyOf(additionalEvents);
            dispatchIntents = List.copyOf(dispatchIntents);
            outboxEntries = Map.copyOf(outboxEntries);
        }
    }

    /** Lease fencing token for commit-time verification. */
    record LeaseFence(String workerId, long fencingToken) {
        public LeaseFence {
            if (workerId == null || workerId.isBlank()) throw new IllegalArgumentException("workerId required");
            if (fencingToken <= 0) throw new IllegalArgumentException("fencingToken must be positive");
        }
    }
}

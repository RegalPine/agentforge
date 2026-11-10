package io.github.regalpine.agentforge.runtime.signal;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.event.ExecutionId;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the lifecycle of wait signals for WAIT and HUMAN nodes.
 * Provides a high-level API for creating, resolving, and querying signals.
 *
 * <p>G-02: Wait/Signal manager for persistent WAIT/HUMAN support.
 */
public final class WaitSignalManager {

    private final SignalStore signalStore;

    public WaitSignalManager(SignalStore signalStore) {
        this.signalStore = signalStore;
    }

    /**
     * Create a new wait signal, putting the execution into WAITING state.
     */
    public WaitSignal createWait(ExecutionId executionId, EntityId waitingNode,
                                  SignalType type, Map<String, Object> metadata) {
        WaitSignal signal = new WaitSignal(
                UUID.randomUUID().toString(),
                executionId, waitingNode, type, SignalStatus.PENDING,
                metadata, Instant.now(), null
        );
        signalStore.save(signal);
        return signal;
    }

    /**
     * Resolve a pending signal, allowing the execution to resume.
     * Returns true if the signal was found and resolved.
     */
    public boolean resolveSignal(String signalId, Map<String, Object> resultPayload) {
        return signalStore.resolve(signalId, resultPayload != null ? resultPayload : Map.of());
    }

    /**
     * Cancel a pending signal.
     */
    public boolean cancelSignal(String signalId) {
        return signalStore.cancel(signalId);
    }

    /**
     * List all pending signals for an execution.
     */
    public List<WaitSignal> pendingSignals(ExecutionId executionId) {
        return signalStore.listPending(executionId);
    }

    /**
     * Check if an execution has any pending signals.
     */
    public boolean hasPendingSignals(ExecutionId executionId) {
        return !signalStore.listPending(executionId).isEmpty();
    }
}

package io.github.regalpine.agentforge.runtime.signal;

import io.github.regalpine.agentforge.event.ExecutionId;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link SignalStore}.
 */
public final class InMemorySignalStore implements SignalStore {

    private final Map<String, WaitSignal> signals = new ConcurrentHashMap<>();

    @Override
    public void save(WaitSignal signal) {
        signals.put(signal.signalId(), signal);
    }

    @Override
    public Optional<WaitSignal> find(String signalId) {
        return Optional.ofNullable(signals.get(signalId));
    }

    @Override
    public List<WaitSignal> listPending(ExecutionId executionId) {
        return signals.values().stream()
                .filter(s -> s.executionId().equals(executionId) && s.status() == SignalStatus.PENDING)
                .toList();
    }

    @Override
    public List<WaitSignal> listByStatus(SignalStatus status) {
        return signals.values().stream()
                .filter(s -> s.status() == status)
                .toList();
    }

    @Override
    public boolean resolve(String signalId, Map<String, Object> payload) {
        WaitSignal existing = signals.get(signalId);
        if (existing == null || existing.status() != SignalStatus.PENDING) {
            return false;
        }
        WaitSignal resolved = new WaitSignal(
                existing.signalId(), existing.executionId(), existing.waitingNode(),
                existing.type(), SignalStatus.RESOLVED,
                payload.isEmpty() ? existing.payload() : Map.copyOf(payload),
                existing.createdAt(), Instant.now()
        );
        signals.put(signalId, resolved);
        return true;
    }

    @Override
    public boolean cancel(String signalId) {
        WaitSignal existing = signals.get(signalId);
        if (existing == null || existing.status() != SignalStatus.PENDING) {
            return false;
        }
        WaitSignal cancelled = new WaitSignal(
                existing.signalId(), existing.executionId(), existing.waitingNode(),
                existing.type(), SignalStatus.CANCELLED, existing.payload(),
                existing.createdAt(), Instant.now()
        );
        signals.put(signalId, cancelled);
        return true;
    }
}

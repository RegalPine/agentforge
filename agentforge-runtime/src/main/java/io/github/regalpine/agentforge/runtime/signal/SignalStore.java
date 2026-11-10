package io.github.regalpine.agentforge.runtime.signal;

import io.github.regalpine.agentforge.event.ExecutionId;

import java.util.List;
import java.util.Optional;

/**
 * SPI for persisting wait signals.
 */
public interface SignalStore {

    void save(WaitSignal signal);

    Optional<WaitSignal> find(String signalId);

    List<WaitSignal> listPending(ExecutionId executionId);

    List<WaitSignal> listByStatus(SignalStatus status);

    boolean resolve(String signalId, java.util.Map<String, Object> payload);

    boolean cancel(String signalId);
}

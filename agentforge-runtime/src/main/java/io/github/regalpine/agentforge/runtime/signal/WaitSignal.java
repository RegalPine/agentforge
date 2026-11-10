package io.github.regalpine.agentforge.runtime.signal;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.event.ExecutionId;

import java.time.Instant;
import java.util.Map;

/**
 * A signal that can resume a waiting execution (WAIT or HUMAN node).
 *
 * <p>G-02: Persistent wait/signal mechanism for WAIT/HUMAN nodes.
 */
public record WaitSignal(
        String signalId,
        ExecutionId executionId,
        EntityId waitingNode,
        SignalType type,
        SignalStatus status,
        Map<String, Object> payload,
        Instant createdAt,
        Instant resolvedAt
) {

    public WaitSignal {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}

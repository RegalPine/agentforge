package io.github.regalpine.agentforge.event;

import io.github.regalpine.agentforge.event.ExecutionId;

import java.time.Instant;
import java.util.Map;

public record ExecutionEvent(
    ExecutionId executionId,
    long sequence,
    Instant timestamp,
    String type,
    Map<String, Object> payload
) {

    public ExecutionEvent {
        payload =
            payload == null
                ? Map.of()
                : Map.copyOf(payload);
    }
}

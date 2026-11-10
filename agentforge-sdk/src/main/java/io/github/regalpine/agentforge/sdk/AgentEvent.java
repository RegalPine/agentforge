package io.github.regalpine.agentforge.sdk;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * An event produced during Agent streaming.
 * AF-ENHANCE-01 §15: streaming API.
 *
 * <p>Lightweight event object for the
 * streaming execution model.
 * Full Event/Trace system is defined
 * in AF-ENHANCE-02 (X1 phase).
 *
 * <p>Immutable.
 */
public record AgentEvent(
    String type,
    String executionId,
    String agentId,
    Object data,
    Instant timestamp
) {

    public AgentEvent {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(timestamp, "timestamp");
    }

    /**
     * Create a text delta event.
     */
    public static AgentEvent textDelta(
        String executionId,
        String agentId,
        String text
    ) {
        return new AgentEvent(
            "message.delta",
            executionId,
            agentId,
            Map.of("text", text),
            Instant.now()
        );
    }

    /**
     * Create a completion event.
     */
    public static AgentEvent completed(
        String executionId,
        String agentId
    ) {
        return new AgentEvent(
            "execution.completed",
            executionId,
            agentId,
            Map.of(),
            Instant.now()
        );
    }

    /**
     * Create an error event.
     */
    public static AgentEvent error(
        String executionId,
        String agentId,
        String errorMessage
    ) {
        return new AgentEvent(
            "error.occurred",
            executionId,
            agentId,
            Map.of("message", errorMessage),
            Instant.now()
        );
    }
}

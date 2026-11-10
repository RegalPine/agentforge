package io.github.regalpine.agentforge.multiagent.event;


import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * DelegationEvent — AE-11 §38, §55.
 *
 * Event emitted during multi-agent delegation lifecycle.
 * Carries event type, parent/child agent IDs, execution
 * IDs, timestamp, and optional metadata.
 */
public record DelegationEvent(

    DelegationEventType type,

    String parentAgentId,

    String childAgentId,

    String parentExecutionId,

    String childExecutionId,

    String rootExecutionId,

    Instant timestamp,

    Map<String, Object> metadata
) {

    public DelegationEvent {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(timestamp, "timestamp");

        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    public static DelegationEvent of(
        DelegationEventType type,
        String parentAgentId,
        String childAgentId
    ) {
        return new DelegationEvent(
            type,
            parentAgentId,
            childAgentId,
            null, null, null,
            Instant.now(),
            Map.of()
        );
    }

    public static DelegationEvent withExecutions(
        DelegationEventType type,
        String parentAgentId,
        String childAgentId,
        String parentExecId,
        String childExecId,
        String rootExecId
    ) {
        return new DelegationEvent(
            type,
            parentAgentId,
            childAgentId,
            parentExecId,
            childExecId,
            rootExecId,
            Instant.now(),
            Map.of()
        );
    }
}

package io.github.regalpine.agentforge.multiagent.handoff;


import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * HandoffResult — AF-ENHANCE-08 §14.
 *
 * Result of a handoff operation.
 * Records the outcome and the
 * new controlling agent.
 */
public record HandoffResult(

    String handoffId,

    String callerAgentId,

    String targetAgentId,

    HandoffStatus status,

    String targetExecutionId,

    HandoffPolicy appliedPolicy,

    Instant completedAt,

    Map<String, Object> metadata

) {

    public HandoffResult {
        Objects.requireNonNull(
            handoffId, "handoffId"
        );
        Objects.requireNonNull(
            callerAgentId, "callerAgentId"
        );
        Objects.requireNonNull(
            targetAgentId, "targetAgentId"
        );
        Objects.requireNonNull(
            status, "status"
        );

        appliedPolicy = appliedPolicy == null
            ? HandoffPolicy.WAIT
            : appliedPolicy;

        completedAt = completedAt == null
            ? Instant.now() : completedAt;

        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create a success result.
     */
    public static HandoffResult success(
        String handoffId,
        String callerAgentId,
        String targetAgentId,
        String targetExecutionId,
        HandoffPolicy policy
    ) {
        return new HandoffResult(
            handoffId,
            callerAgentId,
            targetAgentId,
            HandoffStatus.COMPLETED,
            targetExecutionId,
            policy, null, null
        );
    }

    /**
     * Create a rejected result.
     */
    public static HandoffResult rejected(
        String handoffId,
        String callerAgentId,
        String targetAgentId,
        String reason
    ) {
        return new HandoffResult(
            handoffId,
            callerAgentId,
            targetAgentId,
            HandoffStatus.REJECTED,
            null, null, null,
            Map.of("reason", reason)
        );
    }

    /**
     * Handoff result status.
     */
    public enum HandoffStatus {
        COMPLETED,
        REJECTED,
        FAILED,
        TIMEOUT
    }
}

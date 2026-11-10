package io.github.regalpine.agentforge.multiagent.invocation;


import java.time.Instant;
import java.util.Objects;

/**
 * AgentInvocation — AF-ENHANCE-08 §5.
 *
 * Represents an agent-to-agent execution
 * relationship. Each invocation creates
 * an independent Execution.
 *
 * <p>Agent A → Invocation → Agent B
 * creates Execution B with
 * parentExecutionId = Execution A.
 *
 * <p>Not a SubAgent relationship.
 * Both caller and target are full Agents.
 */
public record AgentInvocation(

    InvocationId id,

    String callerAgentId,

    String targetAgentId,

    String callerExecutionId,

    String targetExecutionId,

    InvocationMode mode,

    InvocationStatus status,

    Instant createdAt,

    Instant updatedAt,

    String parentInvocationId

) {

    public AgentInvocation {
        Objects.requireNonNull(
            id, "id"
        );
        Objects.requireNonNull(
            callerAgentId, "callerAgentId"
        );
        Objects.requireNonNull(
            targetAgentId, "targetAgentId"
        );
        Objects.requireNonNull(
            mode, "mode"
        );

        status = status == null
            ? InvocationStatus.REQUESTED
            : status;

        createdAt = createdAt == null
            ? Instant.now() : createdAt;

        updatedAt = updatedAt == null
            ? createdAt : updatedAt;
    }

    /**
     * Create a new invocation.
     */
    public static AgentInvocation create(
        String callerAgentId,
        String targetAgentId,
        String callerExecutionId,
        InvocationMode mode
    ) {
        return new AgentInvocation(
            InvocationId.generate(),
            callerAgentId,
            targetAgentId,
            callerExecutionId,
            null,
            mode,
            InvocationStatus.REQUESTED,
            null, null, null
        );
    }

    /**
     * Transition to a new status.
     */
    public AgentInvocation withStatus(
        InvocationStatus newStatus
    ) {
        Objects.requireNonNull(
            newStatus, "newStatus"
        );
        if (!status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                "Cannot transition from "
                    + status + " to "
                    + newStatus
            );
        }
        return new AgentInvocation(
            id, callerAgentId, targetAgentId,
            callerExecutionId, targetExecutionId,
            mode, newStatus,
            createdAt, Instant.now(),
            parentInvocationId
        );
    }

    /**
     * Set the target execution ID.
     */
    public AgentInvocation withTargetExecution(
        String executionId
    ) {
        return new AgentInvocation(
            id, callerAgentId, targetAgentId,
            callerExecutionId, executionId,
            mode, status,
            createdAt, Instant.now(),
            parentInvocationId
        );
    }

    /**
     * Set the parent invocation ID.
     */
    public AgentInvocation withParent(
        String parentInvId
    ) {
        return new AgentInvocation(
            id, callerAgentId, targetAgentId,
            callerExecutionId, targetExecutionId,
            mode, status,
            createdAt, Instant.now(),
            parentInvId
        );
    }

    /**
     * Check if this invocation is complete.
     */
    public boolean isComplete() {
        return status.isTerminal();
    }

    /**
     * Check if this is a delegation.
     */
    public boolean isDelegation() {
        return mode == InvocationMode.DELEGATE;
    }

    /**
     * Check if this is a handoff.
     */
    public boolean isHandoff() {
        return mode == InvocationMode.HANDOFF;
    }

    /**
     * Check if this is a collaboration.
     */
    public boolean isCollaboration() {
        return mode == InvocationMode.COLLABORATE;
    }
}

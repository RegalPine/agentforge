package io.github.regalpine.agentforge.server.execution;

import io.github.regalpine.agentforge.event.ExecutionEvent;
import io.github.regalpine.agentforge.event.ExecutionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Execution store: tracks execution records, events and state transitions.
 * <p>
 * In production this is backed by PostgreSQL; for tests/demo an in-memory
 * implementation is provided.
 */
public interface ExecutionStore {

    /** Persist a new execution record. Throws if executionId already exists. */
    void create(ExecutionRecord record);

    /** Look up an execution by tenant + execution id. */
    Optional<ExecutionRecord> find(String tenantId, String executionId);

    /** List all executions for a tenant (newest first). */
    List<ExecutionRecord> list(String tenantId);

    /** Update the status of an execution (CAS by expected version). */
    boolean updateStatus(String tenantId, String executionId,
                         ExecutionStatus newStatus, long expectedVersion);

    /** Append an event to the execution timeline. */
    void appendEvent(String tenantId, String executionId, ExecutionEvent event);

    /** Get the full event timeline for an execution. */
    List<ExecutionEvent> timeline(String tenantId, String executionId);

    /** Execution record persisted in the store. */
    record ExecutionRecord(
        String executionId,
        String tenantId,
        String graphId,
        String graphVersion,
        String planFingerprint,
        ExecutionStatus status,
        long version,
        String idempotencyKey,
        Map<String, Object> inputs,
        Instant createdAt,
        Instant updatedAt
    ) {
        public ExecutionRecord {
            inputs = inputs == null ? Map.of() : Map.copyOf(inputs);
        }

        public ExecutionRecord withStatus(ExecutionStatus newStatus, Instant at) {
            return new ExecutionRecord(
                executionId, tenantId, graphId, graphVersion,
                planFingerprint, newStatus, version + 1,
                idempotencyKey, inputs, createdAt, at
            );
        }
    }
}

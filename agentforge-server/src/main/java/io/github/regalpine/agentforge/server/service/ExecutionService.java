package io.github.regalpine.agentforge.server.service;

import io.github.regalpine.agentforge.event.Execution;
import io.github.regalpine.agentforge.event.ExecutionId;
import io.github.regalpine.agentforge.event.ExecutionEvent;
import io.github.regalpine.agentforge.event.ExecutionState;
import io.github.regalpine.agentforge.event.ExecutionStatus;

import io.github.regalpine.agentforge.graph.ir.ExecutionPlan;
import io.github.regalpine.agentforge.graph.ir.ExecutionPlanRegistry;
import io.github.regalpine.agentforge.runtime.*;
import io.github.regalpine.agentforge.security.SecurityContext;
import io.github.regalpine.agentforge.server.api.ExecutionResponse;
import io.github.regalpine.agentforge.server.api.ExecutionSubmitRequest;
import io.github.regalpine.agentforge.server.execution.ExecutionStore;
import io.github.regalpine.agentforge.server.execution.ExecutionStore.ExecutionRecord;
import io.github.regalpine.agentforge.server.execution.InMemoryExecutionStore;
import io.github.regalpine.agentforge.server.idempotency.IdempotencyStore;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Execution service: manages the full execution lifecycle.
 * <p>
 * Submit creates an execution record, entry activation and outbox entry
 * atomically, then returns 202. Idempotency key is unique per tenant:
 * same key + same request digest → returns existing execution;
 * same key + different digest → 409 Conflict.
 */
public final class ExecutionService {

    private final GraphService graphService;
    private final ExecutionPlanRegistry planRegistry;
    private final IdempotencyStore idempotencyStore;
    private final ExecutionStore executionStore;
    private final CommitPort commitPort;

    public ExecutionService(
        GraphService graphService,
        ExecutionPlanRegistry planRegistry,
        IdempotencyStore idempotencyStore,
        ExecutionStore executionStore,
        CommitPort commitPort
    ) {
        this.graphService = graphService;
        this.planRegistry = planRegistry;
        this.idempotencyStore = idempotencyStore;
        this.executionStore = executionStore;
        this.commitPort = commitPort;
    }

    /** Backward-compatible constructor for tests that do not need ExecutionStore/CommitPort. */
    public ExecutionService(
        GraphService graphService,
        ExecutionPlanRegistry planRegistry,
        IdempotencyStore idempotencyStore
    ) {
        this(graphService, planRegistry, idempotencyStore,
            new InMemoryExecutionStore(), new InMemoryCommitPort(event -> {}));
    }

    // ── Submit ──

    public ExecutionResponse submit(SecurityContext context,
                                    ExecutionSubmitRequest request, String idempotencyKey) {
        ServiceAuthorization.require(context, "execution:create");
        return submit(context.tenant().tenantId(), request, idempotencyKey);
    }

    public ExecutionResponse submit(
        String tenantId,
        ExecutionSubmitRequest request,
        String idempotencyKey
    ) {
        if (tenantId == null || tenantId.isBlank()) throw new SecurityException("Tenant is required");

        // Tenant isolation check: throw SecurityException if graph not accessible
        if (graphService.findPlan(tenantId, request.graphId(), request.graphVersion()).isEmpty()) {
            throw new SecurityException("Graph not found or access denied");
        }

        Optional<ExecutionPlan> planOpt =
            graphService.findPlan(tenantId, request.graphId(), request.graphVersion());
        if (planOpt.isEmpty()) {
            throw new IllegalStateException(
                "Plan not found for graph: " + request.graphId() + "@" + request.graphVersion());
        }

        // ── Idempotency check ──
        if (idempotencyKey != null) {
            var existing = idempotencyStore.find(tenantId, idempotencyKey);
            if (existing.isPresent()) {
                String existingExecId = existing.get().executionId();
                // Same key: check if request digest matches
                var record = executionStore.find(tenantId, existingExecId);
                if (record.isPresent()) {
                    ExecutionRecord r = record.get();
                    if (!requestDigestMatches(r, request)) {
                        throw new IdempotencyConflictException(
                            "Idempotency key conflict: " + idempotencyKey);
                    }
                    return new ExecutionResponse(
                        r.executionId(), r.graphId(), r.graphVersion(),
                        r.status().name(), r.planFingerprint());
                }
            }
        }

        ExecutionPlan plan = planOpt.get();
        planRegistry.register(plan);

        ExecutionId execId = ExecutionId.generate();
        Instant now = Instant.now();

        // ── Atomic commit: create execution + entry activation + outbox ──
        ExecutionState initialState = new ExecutionState(
            execId, ExecutionStatus.CREATED, null,
            request.inputs() != null ? request.inputs() : Map.of(),
            java.util.Set.of(), java.util.Set.of(), 0);

        // Create in store first
        ExecutionRecord record = new ExecutionRecord(
            execId.value(), tenantId, request.graphId(), request.graphVersion(),
            plan.fingerprint(), ExecutionStatus.CREATED, 0,
            idempotencyKey, request.inputs(), now, now);
        executionStore.create(record);

        // Entry activation event
        ExecutionEvent entryEvent = new ExecutionEvent(
            execId, 0, now, "EXECUTION_CREATED",
            Map.of("graphId", request.graphId(), "graphVersion", request.graphVersion()));

        // Commit via CommitPort (atomic: state + event + outbox)
        commitPort.create(initialState);
        CommitPort.CommitCommand command = new CommitPort.CommitCommand(
            initialState, entryEvent, 0, null,
            List.of(), List.of(), List.of(),
            Map.of("execution:" + execId.value(), "created"));
        commitPort.commit(command);

        // Store creation event in execution timeline
        executionStore.appendEvent(tenantId, execId.value(), entryEvent);

        // Record idempotency
        if (idempotencyKey != null) {
            idempotencyStore.putIfAbsent(tenantId, idempotencyKey, execId.value());
        }

        return new ExecutionResponse(
            execId.value(), request.graphId(), request.graphVersion(),
            "CREATED", plan.fingerprint());
    }

    // ── Query ──

    public Optional<ExecutionRecord> find(SecurityContext context, String executionId) {
        ServiceAuthorization.require(context, "execution:read");
        return executionStore.find(context.tenant().tenantId(), executionId);
    }

    public Optional<ExecutionRecord> find(String tenantId, String executionId) {
        return executionStore.find(tenantId, executionId);
    }

    public List<ExecutionRecord> list(SecurityContext context) {
        ServiceAuthorization.require(context, "execution:list");
        return executionStore.list(context.tenant().tenantId());
    }

    public List<ExecutionRecord> list(String tenantId) {
        return executionStore.list(tenantId);
    }

    public List<ExecutionEvent> timeline(SecurityContext context, String executionId) {
        ServiceAuthorization.require(context, "execution:read");
        var record = executionStore.find(context.tenant().tenantId(), executionId);
        if (record.isEmpty()) throw new IllegalStateException("Execution not found: " + executionId);
        return executionStore.timeline(context.tenant().tenantId(), executionId);
    }

    public List<ExecutionEvent> timeline(String tenantId, String executionId) {
        return executionStore.timeline(tenantId, executionId);
    }

    // ── Lifecycle ──

    public ExecutionRecord cancel(SecurityContext context, String executionId) {
        ServiceAuthorization.require(context, "execution:cancel");
        return transition(context.tenant().tenantId(), executionId, ExecutionStatus.CANCELLED);
    }

    public ExecutionRecord cancel(String tenantId, String executionId) {
        return transition(tenantId, executionId, ExecutionStatus.CANCELLED);
    }

    public ExecutionRecord pause(SecurityContext context, String executionId) {
        ServiceAuthorization.require(context, "execution:pause");
        return transition(context.tenant().tenantId(), executionId, ExecutionStatus.SUSPENDED);
    }

    public ExecutionRecord pause(String tenantId, String executionId) {
        return transition(tenantId, executionId, ExecutionStatus.SUSPENDED);
    }

    public ExecutionRecord resume(SecurityContext context, String executionId) {
        ServiceAuthorization.require(context, "execution:resume");
        var record = executionStore.find(context.tenant().tenantId(), executionId);
        if (record.isEmpty()) throw new IllegalStateException("Execution not found: " + executionId);
        if (record.get().status() != ExecutionStatus.SUSPENDED &&
            record.get().status() != ExecutionStatus.WAITING) {
            throw new IllegalStateException(
                "Cannot resume execution in status: " + record.get().status());
        }
        return transition(context.tenant().tenantId(), executionId, ExecutionStatus.RUNNING);
    }

    public ExecutionRecord resume(String tenantId, String executionId) {
        var record = executionStore.find(tenantId, executionId);
        if (record.isEmpty()) throw new IllegalStateException("Execution not found: " + executionId);
        if (record.get().status() != ExecutionStatus.SUSPENDED &&
            record.get().status() != ExecutionStatus.WAITING) {
            throw new IllegalStateException(
                "Cannot resume execution in status: " + record.get().status());
        }
        return transition(tenantId, executionId, ExecutionStatus.RUNNING);
    }

    // ── Internal ──

    private ExecutionRecord transition(String tenantId, String executionId,
                                        ExecutionStatus targetStatus) {
        var record = executionStore.find(tenantId, executionId);
        if (record.isEmpty()) throw new IllegalStateException("Execution not found: " + executionId);

        ExecutionRecord current = record.get();
        validateTransition(current.status(), targetStatus);

        boolean updated = executionStore.updateStatus(
            tenantId, executionId, targetStatus, current.version());
        if (!updated) {
            throw new IllegalStateException("Concurrent modification on execution: " + executionId);
        }

        // Append lifecycle event
        ExecutionEvent event = new ExecutionEvent(
            new ExecutionId(executionId), current.version() + 1,
            Instant.now(), "STATUS_CHANGED",
            Map.of("from", current.status().name(), "to", targetStatus.name()));
        executionStore.appendEvent(tenantId, executionId, event);

        return executionStore.find(tenantId, executionId).orElseThrow();
    }

    private static void validateTransition(ExecutionStatus from, ExecutionStatus to) {
        switch (to) {
            case CANCELLED -> {
                if (from == ExecutionStatus.COMPLETED || from == ExecutionStatus.FAILED
                    || from == ExecutionStatus.CANCELLED) {
                    throw new IllegalStateException(
                        "Cannot cancel execution in terminal status: " + from);
                }
            }
            case SUSPENDED -> {
                if (from != ExecutionStatus.RUNNING && from != ExecutionStatus.READY) {
                    throw new IllegalStateException(
                        "Cannot pause execution in status: " + from);
                }
            }
            case RUNNING -> {
                if (from != ExecutionStatus.SUSPENDED && from != ExecutionStatus.WAITING) {
                    throw new IllegalStateException(
                        "Cannot resume execution in status: " + from);
                }
            }
            default -> throw new IllegalStateException("Unsupported target status: " + to);
        }
    }

    private static boolean requestDigestMatches(ExecutionRecord record, ExecutionSubmitRequest request) {
        return record.graphId().equals(request.graphId())
            && record.graphVersion().equals(request.graphVersion());
    }

    /** Thrown when an idempotency key is reused with a different request digest. */
    public static final class IdempotencyConflictException extends RuntimeException {
        public IdempotencyConflictException(String message) {
            super(message);
        }
    }
}

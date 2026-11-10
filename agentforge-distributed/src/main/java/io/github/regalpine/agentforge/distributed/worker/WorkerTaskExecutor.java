package io.github.regalpine.agentforge.distributed.worker;

import io.github.regalpine.agentforge.distributed.fencing.FencingValidator;
import io.github.regalpine.agentforge.distributed.lease.ExecutionLease;
import io.github.regalpine.agentforge.distributed.lease.LeaseStore;
import io.github.regalpine.agentforge.distributed.queue.TaskQueue;
import io.github.regalpine.agentforge.graph.NodeType;
import io.github.regalpine.agentforge.graph.ir.ExecutionNode;
import io.github.regalpine.agentforge.graph.ir.ExecutionPlan;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.runtime.CommitPort;
import io.github.regalpine.agentforge.event.EventPublisher;
import io.github.regalpine.agentforge.event.Execution;
import io.github.regalpine.agentforge.event.ExecutionEvent;
import io.github.regalpine.agentforge.event.ExecutionId;
import io.github.regalpine.agentforge.graph.ir.ExecutionPlanRegistry;
import io.github.regalpine.agentforge.runtime.ExecutionRouter;
import io.github.regalpine.agentforge.event.ExecutionState;
import io.github.regalpine.agentforge.runtime.ExecutionStateMachine;
import io.github.regalpine.agentforge.event.ExecutionStatus;
import io.github.regalpine.agentforge.runtime.ExecutionTask;
import io.github.regalpine.agentforge.runtime.NodeCompleted;
import io.github.regalpine.agentforge.runtime.NodeExecutionContext;
import io.github.regalpine.agentforge.runtime.NodeExecutionResult;
import io.github.regalpine.agentforge.runtime.NodeExecutor;
import io.github.regalpine.agentforge.runtime.NodeExecutorRegistry;
import io.github.regalpine.agentforge.runtime.NodeFailed;
import io.github.regalpine.agentforge.runtime.NodeWaiting;
import io.github.regalpine.agentforge.runtime.NodeCancelled;
import io.github.regalpine.agentforge.event.StateManager;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WorkerTaskExecutor {

    private final LeaseStore leaseStore;
    private final FencingValidator fencingValidator;
    private final StateManager stateManager;
    private final ExecutionPlanRegistry planRegistry;
    private final NodeExecutorRegistry executorRegistry;
    private final EventPublisher eventPublisher;
    private final TaskQueue taskQueue;
    private final CommitPort commitPort;
    private final ExecutionRouter router;
    private final ExecutionStateMachine stateMachine;

    public WorkerTaskExecutor(
        LeaseStore leaseStore,
        FencingValidator fencingValidator,
        StateManager stateManager,
        ExecutionPlanRegistry planRegistry,
        NodeExecutorRegistry executorRegistry,
        EventPublisher eventPublisher,
        TaskQueue taskQueue
    ) {
        this.leaseStore = leaseStore;
        this.fencingValidator = fencingValidator;
        this.stateManager = stateManager;
        this.planRegistry = planRegistry;
        this.executorRegistry = executorRegistry;
        this.eventPublisher = eventPublisher;
        this.taskQueue = taskQueue;
        this.commitPort =
            new WorkerCommitAdapter(
                stateManager, eventPublisher
            );
        this.router = new ExecutionRouter();
        this.stateMachine =
            new ExecutionStateMachine();
    }

    public WorkerTaskExecutor(
        LeaseStore leaseStore,
        FencingValidator fencingValidator,
        StateManager stateManager,
        ExecutionPlanRegistry planRegistry,
        NodeExecutorRegistry executorRegistry,
        EventPublisher eventPublisher,
        TaskQueue taskQueue,
        CommitPort commitPort
    ) {
        this.leaseStore = leaseStore;
        this.fencingValidator = fencingValidator;
        this.stateManager = stateManager;
        this.planRegistry = planRegistry;
        this.executorRegistry = executorRegistry;
        this.eventPublisher = eventPublisher;
        this.taskQueue = taskQueue;
        this.commitPort = commitPort;
        this.router = new ExecutionRouter();
        this.stateMachine =
            new ExecutionStateMachine();
    }

    public void executeTask(
        ExecutionTask task,
        WorkerId workerId,
        Execution execution
    ) {
        ExecutionLease lease =
            leaseStore.acquire(
                task.executionId(),
                workerId,
                30_000
            ).orElseThrow(
                () -> new IllegalStateException(
                    "Failed to acquire lease for execution: "
                        + task.executionId()
                )
            );

        try {

            if (!fencingValidator.isValid(lease)) {
                throw new IllegalStateException(
                    "Lease already expired after acquisition"
                );
            }

            verifyTask(task, execution);

            executeSingleNode(
                task,
                execution,
                lease
            );

        } finally {

            leaseStore.release(
                task.executionId(),
                workerId,
                lease.fencingToken()
            );
        }
    }

    private void verifyTask(
        ExecutionTask task,
        Execution execution
    ) {
        if (!execution.planFingerprint().equals(
            task.planFingerprint()
        )) {
            throw new IllegalStateException(
                "Plan fingerprint mismatch: task="
                    + task.planFingerprint()
                    + " execution="
                    + execution.planFingerprint()
            );
        }

        if (execution.status() == ExecutionStatus.COMPLETED
            || execution.status() == ExecutionStatus.FAILED
            || execution.status() == ExecutionStatus.CANCELLED
        ) {
            throw new IllegalStateException(
                "Execution already in terminal state: "
                    + execution.status()
            );
        }
    }

    private void executeSingleNode(
        ExecutionTask task,
        Execution execution,
        ExecutionLease lease
    ) {
        ExecutionPlan plan =
            planRegistry.find(
                execution.graphId(),
                execution.graphVersion()
            ).orElseThrow(
                () -> new IllegalStateException(
                    "ExecutionPlan not found"
                )
            );

        if (!plan.fingerprint().equals(
            task.planFingerprint()
        )) {
            throw new IllegalStateException(
                "Plan fingerprint does not match registry"
            );
        }

        ExecutionState state =
            stateManager.get(
                task.executionId()
            ).orElseThrow();

        // G2-1: Transition READY → RUNNING
        // before processing any node
        if (state.status() == ExecutionStatus.READY) {
            state = transitionStatus(
                state,
                ExecutionStatus.RUNNING,
                task.executionId()
            );
        }

        EntityId nodeId =
            task.nodeId();

        // G2-6: Idempotency check — skip
        // already-completed nodes
        if (state.completedNodes().contains(nodeId)) {
            // G3-1: Use nextOnSuccess for
            // already-completed nodes
            EntityId nextNode =
                router.nextOnSuccess(
                    plan, nodeId, state.data()
                );
            if (nextNode != null) {
                ExecutionTask nextTask =
                    ExecutionTask.create(
                        execution,
                        nextNode
                    );
                taskQueue.enqueue(nextTask);
            }
            return;
        }

        ExecutionNode node =
            findNode(plan, nodeId);

        // G2-3: Use state.nextEventSequence()
        long startSeq = state.nextEventSequence();

        publish(
            execution,
            startSeq,
            "NODE_STARTED",
            Map.of(
                "nodeId",
                nodeId.value(),
                "workerId",
                lease.workerId().value(),
                "fencingToken",
                lease.fencingToken()
            )
        );

        NodeExecutionContext context =
            new NodeExecutionContext(
                execution,
                state,
                plan,
                Map.of()
            );

        NodeExecutor executor =
            executorRegistry.resolve(
                node.type()
            );

        NodeExecutionResult result =
            executor.execute(
                node,
                context
            );

        // G2-1: Determine terminal status
        ExecutionStatus terminalStatus =
            resolveTerminalStatus(
                node.type(), result
            );

        String eventType =
            resolveEventType(result);

        Map<String, Object> eventPayload =
            buildEventPayload(nodeId, result, eventType);

        long eventSeq = startSeq + 1;

        // Build new state
        // G2-3: nextEventSeq = eventSeq + 1
        // (NODE_STARTED uses startSeq, result uses eventSeq=startSeq+1)
        ExecutionState newState =
            buildResultState(
                state,
                nodeId,
                result,
                terminalStatus,
                eventSeq + 1
            );

        // G2-1: Verify state transition only if
        // status actually changes
        if (newState.status() != state.status()) {
            stateMachine.verify(
                state.status(),
                newState.status()
            );
        }

        // G2-5: Atomic commit
        boolean committed =
            commitPort.commit(
                new CommitPort.CommitCommand(
                    newState,
                    new ExecutionEvent(
                        execution.id(),
                        eventSeq,
                        Instant.now(),
                        eventType,
                        eventPayload
                    ),
                    state.version()
                )
            );

        if (!committed) {
            publish(
                execution,
                eventSeq,
                "STATE_CAS_FAILED",
                Map.of(
                    "nodeId",
                    nodeId.value(),
                    "expectedVersion",
                    state.version()
                )
            );
            return;
        }

        // Terminal state — don't dispatch next
        if (terminalStatus != null) {
            return;
        }

        if (node.type() == NodeType.END) {
            return;
        }

        // G3-1: Route based on edge type
        // and execution result
        EntityId nextNode;
        if (result instanceof NodeFailed) {
            nextNode = router.nextOnFailure(
                plan, nodeId
            );
        } else {
            nextNode = router.nextOnSuccess(
                plan, nodeId, newState.data()
            );
        }

        if (nextNode != null) {
            ExecutionTask nextTask =
                ExecutionTask.create(
                    execution,
                    nextNode
                );
            taskQueue.enqueue(nextTask);
        }
    }

    // ── State transition helpers ──

    private ExecutionStatus resolveTerminalStatus(
        NodeType nodeType,
        NodeExecutionResult result
    ) {
        if (result instanceof NodeFailed) {
            return ExecutionStatus.FAILED;
        }
        if (result instanceof NodeWaiting) {
            return ExecutionStatus.WAITING;
        }
        if (result instanceof NodeCancelled) {
            return ExecutionStatus.CANCELLED;
        }
        if (nodeType == NodeType.END) {
            return ExecutionStatus.COMPLETED;
        }
        return null;
    }

    private String resolveEventType(
        NodeExecutionResult result
    ) {
        if (result instanceof NodeFailed) {
            return "NODE_FAILED";
        }
        if (result instanceof NodeWaiting) {
            return "NODE_WAITING";
        }
        if (result instanceof NodeCancelled) {
            return "NODE_CANCELLED";
        }
        return "NODE_COMPLETED";
    }

    private Map<String, Object> buildEventPayload(
        EntityId nodeId,
        NodeExecutionResult result,
        String eventType
    ) {
        Map<String, Object> payload =
            new HashMap<>();
        payload.put("nodeId", nodeId.value());

        switch (eventType) {
            case "NODE_FAILED" -> {
                NodeFailed failed =
                    (NodeFailed) result;
                payload.put(
                    "code",
                    failed.code()
                );
            }
            case "NODE_WAITING" -> {
                NodeWaiting waiting =
                    (NodeWaiting) result;
                payload.put(
                    "reason",
                    waiting.reason()
                );
            }
            case "NODE_CANCELLED" -> {
                NodeCancelled cancelled =
                    (NodeCancelled) result;
                payload.put(
                    "reason",
                    cancelled.reason()
                );
            }
            default -> { /* no extra payload */ }
        }

        return Map.copyOf(payload);
    }

    private ExecutionState buildResultState(
        ExecutionState state,
        EntityId node,
        NodeExecutionResult result,
        ExecutionStatus terminalStatus,
        long nextEventSeq
    ) {
        Map<String, Object> data =
            new HashMap<>(state.data());

        if (result instanceof NodeCompleted completed) {
            data.putAll(completed.output());
        }

        Set<EntityId> completedNodes =
            result instanceof NodeCompleted
                ? union(state.completedNodes(), node)
                : state.completedNodes();

        ExecutionStatus newStatus =
            terminalStatus != null
                ? terminalStatus
                : state.status();

        return new ExecutionState(
            state.executionId(),
            newStatus,
            node,
            data,
            completedNodes,
            state.waitingNodes(),
            state.version() + 1,
            nextEventSeq
        );
    }

    private ExecutionState transitionStatus(
        ExecutionState state,
        ExecutionStatus target,
        ExecutionId executionId
    ) {
        if (state.status() == target) {
            return state;
        }
        stateMachine.verify(
            state.status(), target
        );
        ExecutionState newState =
            new ExecutionState(
                state.executionId(),
                target,
                state.currentNode(),
                state.data(),
                state.completedNodes(),
                state.waitingNodes(),
                state.version() + 1,
                state.nextEventSequence()
            );
        stateManager.compareAndSet(
            executionId,
            state.version(),
            newState
        );
        return newState;
    }

    private ExecutionNode findNode(
        ExecutionPlan plan,
        EntityId id
    ) {
        return plan.nodes()
            .stream()
            .filter(
                node -> node.id().equals(id)
            )
            .findFirst()
            .orElseThrow(
                () -> new IllegalStateException(
                    "Execution node not found: "
                        + id
                )
            );
    }

    private Set<EntityId> union(
        Set<EntityId> source,
        EntityId value
    ) {
        Set<EntityId> result =
            new HashSet<>(source);
        result.add(value);
        return result;
    }

    private void publish(
        Execution execution,
        long sequence,
        String type,
        Map<String, Object> payload
    ) {
        eventPublisher.publish(
            new ExecutionEvent(
                execution.id(),
                sequence,
                Instant.now(),
                type,
                payload
            )
        );
    }

    /**
     * Adapter that wraps StateManager + EventPublisher
     * into a CommitPort for backward compatibility.
     */
    private static final class
        WorkerCommitAdapter
        implements CommitPort {

        private final StateManager stateManager;
        private final EventPublisher eventPublisher;

        WorkerCommitAdapter(
            StateManager sm,
            EventPublisher ep
        ) {
            this.stateManager = sm;
            this.eventPublisher = ep;
        }

        @Override
        public boolean commit(
            CommitCommand command
        ) {
            boolean ok =
                stateManager.compareAndSet(
                    command.newState()
                        .executionId(),
                    command.expectedVersion(),
                    command.newState()
                );
            if (ok) {
                eventPublisher.publish(
                    command.event()
                );
            }
            return ok;
        }
    }
}

package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.event.Execution;
import io.github.regalpine.agentforge.graph.ir.ExecutionPlanRegistry;
import io.github.regalpine.agentforge.event.ExecutionState;
import io.github.regalpine.agentforge.event.ExecutionStatus;
import io.github.regalpine.agentforge.event.StateManager;
import io.github.regalpine.agentforge.event.ExecutionEvent;
import io.github.regalpine.agentforge.event.EventPublisher;

import io.github.regalpine.agentforge.graph.NodeType;
import io.github.regalpine.agentforge.graph.ir.ExecutionNode;
import io.github.regalpine.agentforge.graph.ir.ExecutionPlan;
import io.github.regalpine.agentforge.kernel.EntityId;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ExecutionEngine {

    private final ExecutionPlanRegistry planRegistry;
    private final StateManager stateManager;
    private final NodeExecutorRegistry executorRegistry;
    private final EventPublisher eventPublisher;
    private final CommitPort commitPort;
    private final ExecutionRouter router;
    private final ExecutionStateMachine stateMachine;

    /**
     * New constructor with CommitPort (G2-5).
     * Uses CommitPort for atomic state + event commits.
     */
    public ExecutionEngine(
        ExecutionPlanRegistry planRegistry,
        CommitPort commitPort,
        StateManager stateManager,
        NodeExecutorRegistry executorRegistry,
        EventPublisher eventPublisher
    ) {
        this.planRegistry = planRegistry;
        this.commitPort = commitPort;
        this.stateManager = stateManager;
        this.executorRegistry = executorRegistry;
        this.eventPublisher = eventPublisher;
        this.router = new ExecutionRouter();
        this.stateMachine = new ExecutionStateMachine();
    }

    /**
     * Backward-compatible constructor.
     * Creates an internal CommitPort adapter from
     * StateManager + EventPublisher.
     */
    public ExecutionEngine(
        ExecutionPlanRegistry planRegistry,
        StateManager stateManager,
        NodeExecutorRegistry executorRegistry,
        EventPublisher eventPublisher
    ) {
        this(
            planRegistry,
            new StateManagerCommitAdapter(
                stateManager, eventPublisher
            ),
            stateManager,
            executorRegistry,
            eventPublisher
        );
    }

    public void execute(
        Execution execution
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
            execution.planFingerprint()
        )) {
            throw new IllegalStateException(
                "ExecutionPlan fingerprint mismatch"
            );
        }

        ExecutionState state =
            stateManager.get(
                execution.id()
            ).orElseThrow();

        // Transition to RUNNING
        state = transitionStatus(
            state,
            ExecutionStatus.RUNNING,
            execution
        );

        EntityId current =
            state.currentNode();

        while (current != null) {

            ExecutionNode node =
                findNode(plan, current);

            // G2-3: Use state.nextEventSequence() for
            // execution-level monotonic sequences
            long startSeq = state.nextEventSequence();

            publish(
                execution,
                startSeq,
                "NODE_STARTED",
                Map.of(
                    "nodeId",
                    current.value()
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

            // G2-1: Determine terminal status if applicable
            ExecutionStatus terminalStatus =
                resolveTerminalStatus(
                    node.type(), result
                );

            // Build new state with transition
            // G2-3: nextEventSeq = startSeq + 2
            // (NODE_STARTED uses startSeq, result uses startSeq+1)
            ExecutionState newState =
                buildResultState(
                    state,
                    current,
                    result,
                    terminalStatus,
                    startSeq + 2
                );

            // G2-1: Verify transition only if
            // status actually changes
            if (newState.status() != state.status()) {
                stateMachine.verify(
                    state.status(),
                    newState.status()
                );
            }

            // G2-5: Atomic commit of state + event
            String eventType =
                resolveEventType(result);

            Map<String, Object> eventPayload =
                buildEventPayload(
                    current, result, eventType
                );

            long eventSeq = startSeq + 1;

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
                // CAS conflict — publish conflict
                // event and stop
                publish(
                    execution,
                    eventSeq,
                    "STATE_CAS_FAILED",
                    Map.of(
                        "nodeId",
                        current.value(),
                        "expectedVersion",
                        state.version()
                    )
                );
                return;
            }

            state = newState;

            // G2-1: Stop on any terminal state
            // (COMPLETED, FAILED, WAITING, CANCELLED)
            if (state.status()
                != ExecutionStatus.RUNNING) {
                return;
            }

            // G3-1: Route based on edge type
            // and execution result
            if (result instanceof NodeFailed) {
                current = router.nextOnFailure(
                    plan, current
                );
            } else {
                current = router.nextOnSuccess(
                    plan, current, state.data()
                );
            }
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
        return null; // No terminal status
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
                ? union(
                    state.completedNodes(), node
                )
                : state.completedNodes();

        // For non-terminal results, status stays RUNNING
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
        Execution execution
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
            execution.id(),
            state.version(),
            newState
        );
        return newState;
    }

    // ── Internal helpers ──

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
        StateManagerCommitAdapter
        implements CommitPort {

        private final StateManager stateManager;
        private final EventPublisher eventPublisher;

        StateManagerCommitAdapter(
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

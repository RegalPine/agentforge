package io.github.regalpine.agentforge.persistence;

import io.github.regalpine.agentforge.graph.ir.ExecutionPlan;
import io.github.regalpine.agentforge.event.Execution;
import io.github.regalpine.agentforge.event.ExecutionEvent;
import io.github.regalpine.agentforge.event.ExecutionId;
import io.github.regalpine.agentforge.graph.ir.ExecutionPlanRegistry;
import io.github.regalpine.agentforge.event.ExecutionState;
import io.github.regalpine.agentforge.event.ExecutionStatus;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class DefaultRecoveryManager
    implements RecoveryManager {

    private final ExecutionStore executionStore;
    private final ExecutionStateStore stateStore;
    private final SnapshotStore snapshotStore;
    private final EventStore eventStore;
    private final ExecutionPlanRegistry planRegistry;
    private final StateReducer reducer;

    public DefaultRecoveryManager(
        ExecutionStore executionStore,
        ExecutionStateStore stateStore,
        SnapshotStore snapshotStore,
        EventStore eventStore,
        ExecutionPlanRegistry planRegistry,
        StateReducer reducer
    ) {
        this.executionStore = executionStore;
        this.stateStore = stateStore;
        this.snapshotStore = snapshotStore;
        this.eventStore = eventStore;
        this.planRegistry = planRegistry;
        this.reducer = reducer;
    }

    @Override
    public RecoveryResult recover(
        ExecutionId executionId
    ) {
        Optional<Execution> executionOpt =
            executionStore.find(executionId);

        if (executionOpt.isEmpty()) {
            throw new IllegalStateException(
                "Execution not found: " + executionId
            );
        }

        Execution execution = executionOpt.get();

        Optional<ExecutionPlan> planOpt =
            planRegistry.find(
                execution.graphId(),
                execution.graphVersion()
            );

        if (planOpt.isEmpty()) {
            throw new IllegalStateException(
                "Plan not found for execution: "
                    + executionId
            );
        }

        ExecutionPlan plan = planOpt.get();

        if (execution.planFingerprint() != null &&
            !execution.planFingerprint().equals(
                plan.fingerprint()
            )
        ) {
            throw new IllegalStateException(
                "Plan fingerprint mismatch for execution: "
                    + executionId
            );
        }

        Optional<Snapshot> snapshotOpt =
            snapshotStore.latest(executionId);

        ExecutionState state;
        long fromSequence;
        boolean fromSnapshot;

        if (snapshotOpt.isPresent()) {
            Snapshot snapshot = snapshotOpt.get();

            state = new ExecutionState(
                executionId,
                execution.status(),
                null,
                snapshot.state(),
                Set.of(),
                Set.of(),
                snapshot.stateVersion()
            );

            fromSequence = snapshot.eventSequence();
            fromSnapshot = true;
        } else {
            Optional<ExecutionState> stateOpt =
                stateStore.find(executionId);

            if (stateOpt.isPresent()) {
                state = stateOpt.get();
                fromSequence = 0;
                fromSnapshot = false;
            } else {
                state = new ExecutionState(
                    executionId,
                    ExecutionStatus.CREATED,
                    null,
                    java.util.Map.of(),
                    Set.of(),
                    Set.of(),
                    0
                );

                fromSequence = 0;
                fromSnapshot = false;
            }
        }

        List<ExecutionEvent> events =
            eventStore.listAfter(
                executionId,
                fromSequence
            );

        for (ExecutionEvent event : events) {
            state = reducer.apply(state, event);
        }

        return new RecoveryResult(
            state,
            fromSnapshot,
            events.size()
        );
    }
}

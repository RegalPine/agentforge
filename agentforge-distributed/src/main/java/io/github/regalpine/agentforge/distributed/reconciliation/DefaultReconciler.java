package io.github.regalpine.agentforge.distributed.reconciliation;

import io.github.regalpine.agentforge.distributed.lease.LeaseStore;
import io.github.regalpine.agentforge.event.Execution;
import io.github.regalpine.agentforge.event.ExecutionState;
import io.github.regalpine.agentforge.event.ExecutionStatus;
import io.github.regalpine.agentforge.runtime.ExecutionTask;
import io.github.regalpine.agentforge.event.StateManager;

import java.util.List;
import java.util.Set;

public final class DefaultReconciler
    implements Reconciler {

    private final StateManager stateManager;
    private final LeaseStore leaseStore;
    private final TaskRegistry taskRegistry;
    private final List<Execution> trackedExecutions;

    public DefaultReconciler(
        StateManager stateManager,
        LeaseStore leaseStore,
        TaskRegistry taskRegistry,
        List<Execution> trackedExecutions
    ) {
        this.stateManager = stateManager;
        this.leaseStore = leaseStore;
        this.taskRegistry = taskRegistry;
        this.trackedExecutions = trackedExecutions;
    }

    @Override
    public ReconciliationResult reconcile() {

        int recreated = 0;
        int removed = 0;
        int recovered = 0;
        int stale = 0;

        for (Execution execution : trackedExecutions) {

            ExecutionState state =
                stateManager.get(
                    execution.id()
                ).orElse(null);

            if (state == null) {
                continue;
            }

            ExecutionStatus status =
                state.status();

            boolean hasTask =
                taskRegistry.hasPendingTask(
                    execution.id()
                );

            boolean hasLease =
                leaseStore.current(
                    execution.id()
                ).isPresent();

            boolean leaseExpired =
                leaseStore.current(
                    execution.id()
                ).map(
                    lease -> lease.expiresAt()
                        .isBefore(
                            java.time.Instant.now()
                        )
                ).orElse(false);

            if (isTerminal(status)) {

                if (hasTask) {
                    taskRegistry.removeTasks(
                        execution.id()
                    );
                    removed++;
                }

                continue;
            }

            if (status == ExecutionStatus.RUNNING
                && !hasTask
                && (!hasLease || leaseExpired)
            ) {

                ExecutionTask newTask =
                    ExecutionTask.create(
                        execution,
                        state.currentNode()
                    );

                taskRegistry.register(
                    newTask
                );
                recreated++;

                if (hasLease && leaseExpired) {
                    recovered++;
                }
            }

            if (status == ExecutionStatus.RUNNING
                && hasTask
                && state.currentNode() != null
            ) {

                Set<String> taskNodes =
                    taskRegistry.getTaskNodeIds(
                        execution.id()
                    );

                for (String taskNode : taskNodes) {
                    if (!taskNode.equals(
                        state.currentNode().value()
                    )) {
                        taskRegistry.removeTaskForNode(
                            execution.id(),
                            taskNode
                        );
                        stale++;
                    }
                }
            }
        }

        return new ReconciliationResult(
            recreated,
            removed,
            recovered,
            stale
        );
    }

    private boolean isTerminal(
        ExecutionStatus status
    ) {
        return status == ExecutionStatus.COMPLETED
            || status == ExecutionStatus.FAILED
            || status == ExecutionStatus.CANCELLED;
    }
}

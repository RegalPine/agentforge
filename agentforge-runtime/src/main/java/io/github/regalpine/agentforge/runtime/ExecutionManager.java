package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.event.Execution;
import io.github.regalpine.agentforge.graph.ir.ExecutionPlanRegistry;
import io.github.regalpine.agentforge.event.ExecutionState;
import io.github.regalpine.agentforge.event.ExecutionStatus;
import io.github.regalpine.agentforge.event.StateManager;
import io.github.regalpine.agentforge.event.ExecutionId;

import io.github.regalpine.agentforge.graph.ir.ExecutionPlan;
import io.github.regalpine.agentforge.kernel.EntityId;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ExecutionManager {

    private final ExecutionPlanRegistry planRegistry;
    private final StateManager stateManager;

    private final Map<
        ExecutionId,
        Execution
    > executions =
        new ConcurrentHashMap<>();

    public ExecutionManager(
        ExecutionPlanRegistry planRegistry,
        StateManager stateManager
    ) {
        this.planRegistry = planRegistry;
        this.stateManager = stateManager;
    }

    public ExecutionHandle submit(
        ExecutionRequest request
    ) {

        ExecutionPlan plan =
            planRegistry.find(
                request.graphId(),
                request.graphVersion()
            ).orElseThrow(
                () -> new IllegalArgumentException(
                    "ExecutionPlan not found"
                )
            );

        ExecutionId executionId =
            ExecutionId.generate();

        Instant now =
            Instant.now();

        Execution execution =
            new Execution(
                executionId,
                request.graphId(),
                request.graphVersion(),
                plan.fingerprint(),
                ExecutionStatus.READY,
                now,
                null,
                null
            );

        executions.put(
            executionId,
            execution
        );

        EntityId entry =
            plan.entryNode();

        stateManager.create(
            new ExecutionState(
                executionId,
                ExecutionStatus.READY,
                entry,
                request.inputs(),
                Set.of(),
                Set.of(),
                0
            )
        );

        return new SimpleExecutionHandle(
            executionId
        );
    }

    public Optional<Execution> get(
        ExecutionId executionId
    ) {
        return Optional.ofNullable(
            executions.get(executionId)
        );
    }
}

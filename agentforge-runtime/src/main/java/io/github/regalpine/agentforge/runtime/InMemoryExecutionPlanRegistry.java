package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.graph.ir.ExecutionPlanRegistry;
import io.github.regalpine.agentforge.graph.ir.ExecutionPlan;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryExecutionPlanRegistry
    implements ExecutionPlanRegistry {

    private final Map<
        Reference,
        ExecutionPlan
    > plans = new ConcurrentHashMap<>();

    @Override
    public void register(
        ExecutionPlan plan
    ) {
        Reference reference =
            new Reference(
                plan.graphId(),
                plan.graphVersion()
            );

        ExecutionPlan previous =
            plans.putIfAbsent(
                reference,
                plan
            );

        // Idempotent: same reference re-registration is allowed (same graph version = same plan)
    }

    @Override
    public Optional<ExecutionPlan> find(
        EntityId graphId,
        Version graphVersion
    ) {
        return Optional.ofNullable(
            plans.get(
                new Reference(
                    graphId,
                    graphVersion
                )
            )
        );
    }
}

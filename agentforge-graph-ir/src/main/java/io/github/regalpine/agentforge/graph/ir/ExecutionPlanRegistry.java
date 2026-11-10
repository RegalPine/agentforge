package io.github.regalpine.agentforge.graph.ir;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;

import java.util.Optional;

public interface ExecutionPlanRegistry {

    void register(ExecutionPlan plan);

    Optional<ExecutionPlan> find(
        EntityId graphId,
        Version graphVersion
    );
}

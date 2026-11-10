package io.github.regalpine.agentforge.persistence.memory;

import io.github.regalpine.agentforge.persistence.ExecutionStore;
import io.github.regalpine.agentforge.event.Execution;
import io.github.regalpine.agentforge.event.ExecutionId;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryExecutionStore
    implements ExecutionStore {

    private final Map<
        ExecutionId,
        Execution
    > executions = new ConcurrentHashMap<>();

    @Override
    public void create(
        Execution execution
    ) {
        Execution previous =
            executions.putIfAbsent(
                execution.id(),
                execution
            );

        if (previous != null) {
            throw new IllegalStateException(
                "Execution already exists: "
                    + execution.id()
            );
        }
    }

    @Override
    public Optional<Execution> find(
        ExecutionId executionId
    ) {
        return Optional.ofNullable(
            executions.get(executionId)
        );
    }

    @Override
    public void update(
        Execution execution
    ) {
        executions.put(
            execution.id(),
            execution
        );
    }
}

package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.graph.NodeType;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class NodeExecutorRegistry {

    private final Map<
        NodeType,
        NodeExecutor
    > executors =
        new ConcurrentHashMap<>();

    public void register(
        NodeType type,
        NodeExecutor executor
    ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(executor);

        NodeExecutor previous =
            executors.putIfAbsent(
                type,
                executor
            );

        if (previous != null) {
            throw new IllegalStateException(
                "Executor already registered: "
                    + type
            );
        }
    }

    public NodeExecutor resolve(
        NodeType type
    ) {
        NodeExecutor executor =
            executors.get(type);

        if (executor == null) {
            throw new IllegalStateException(
                "No NodeExecutor registered for "
                    + type
            );
        }

        return executor;
    }
}

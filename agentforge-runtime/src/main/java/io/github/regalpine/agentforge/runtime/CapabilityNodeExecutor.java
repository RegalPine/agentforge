package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.graph.ir.ExecutionNode;

public final class CapabilityNodeExecutor
    implements NodeExecutor {

    private final CapabilityExecutor executor;

    public CapabilityNodeExecutor(
        CapabilityExecutor executor
    ) {
        this.executor = executor;
    }

    @Override
    public NodeExecutionResult execute(
        ExecutionNode node,
        NodeExecutionContext context
    ) {
        return executor.execute(
            node,
            context
        );
    }
}

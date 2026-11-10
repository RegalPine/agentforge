package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.graph.ir.ExecutionNode;

public final class StartNodeExecutor
    implements NodeExecutor {

    @Override
    public NodeExecutionResult execute(
        ExecutionNode node,
        NodeExecutionContext context
    ) {
        return new NodeCompleted(
            context.state().data()
        );
    }
}

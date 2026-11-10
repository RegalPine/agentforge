package io.github.regalpine.agentforge.provider.agent;

public record AgentCallContext(
    String rootExecutionId,
    int depth,
    int maxDepth
) {

    public static final int DEFAULT_MAX_DEPTH = 8;

    public AgentCallContext(
        String rootExecutionId
    ) {
        this(rootExecutionId, 0, DEFAULT_MAX_DEPTH);
    }
}

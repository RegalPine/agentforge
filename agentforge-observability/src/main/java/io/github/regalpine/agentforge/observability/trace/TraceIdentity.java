package io.github.regalpine.agentforge.observability.trace;

import java.util.Objects;

/**
 * Unified trace identity.
 * AF-ENHANCE-06 §5: full correlation
 * chain from trace down to invocation.
 *
 * <p>traceId → spanId → parentSpanId
 * → executionId → nodeExecutionId
 * → invocationId
 */
public record TraceIdentity(
    String traceId,
    String spanId,
    String parentSpanId,
    String executionId,
    String nodeExecutionId,
    String invocationId
) {

    public TraceIdentity {
        Objects.requireNonNull(traceId);
        Objects.requireNonNull(spanId);
    }

    /**
     * Create a root identity.
     */
    public static TraceIdentity root(
        String traceId,
        String executionId
    ) {
        return new TraceIdentity(
            traceId,
            traceId + "-span-0",
            null,
            executionId,
            null,
            null
        );
    }

    /**
     * Create a child identity.
     */
    public TraceIdentity child(
        String childSpanId
    ) {
        return new TraceIdentity(
            this.traceId,
            childSpanId,
            this.spanId,
            this.executionId,
            this.nodeExecutionId,
            this.invocationId
        );
    }

    /**
     * Create a node execution identity.
     */
    public TraceIdentity forNode(
        String nodeExecId
    ) {
        return new TraceIdentity(
            this.traceId,
            this.spanId + "-" + nodeExecId,
            this.spanId,
            this.executionId,
            nodeExecId,
            null
        );
    }

    /**
     * Create an invocation identity.
     */
    public TraceIdentity forInvocation(
        String invId
    ) {
        return new TraceIdentity(
            this.traceId,
            this.spanId + "-" + invId,
            this.spanId,
            this.executionId,
            this.nodeExecutionId,
            invId
        );
    }
}

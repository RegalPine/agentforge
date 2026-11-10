package io.github.regalpine.agentforge.observability;

public record TraceContext(
    String traceId,
    String spanId,
    String parentSpanId,
    String operation
) {
}

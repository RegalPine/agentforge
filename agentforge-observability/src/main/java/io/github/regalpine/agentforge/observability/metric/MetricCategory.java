package io.github.regalpine.agentforge.observability.metric;

/**
 * Metric category.
 * AF-ENHANCE-06 §7: 12 core metric
 * categories.
 */
public enum MetricCategory {

    RUNTIME,
    LATENCY,
    MODEL,
    CAPABILITY,
    MEMORY,
    KNOWLEDGE,
    MCP,
    MULTI_AGENT,
    GRAPH,
    CHECKPOINT,
    SECURITY,
    COST
}

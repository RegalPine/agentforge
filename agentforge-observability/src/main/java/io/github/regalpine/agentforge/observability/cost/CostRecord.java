package io.github.regalpine.agentforge.observability.cost;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Cost record.
 * AF-ENHANCE-06 §9: tracks cost at
 * model, execution, and tenant levels.
 */
public record CostRecord(
    String id,
    CostLevel level,
    String entityId,
    String tenantId,
    String modelId,
    double costUsd,
    int inputTokens,
    int outputTokens,
    Instant timestamp,
    Map<String, String> attributes
) {

    public CostRecord {
        Objects.requireNonNull(id);
        Objects.requireNonNull(level);
        Objects.requireNonNull(entityId);
        timestamp = timestamp == null
            ? Instant.now() : timestamp;
        attributes = attributes == null
            ? Map.of() : Map.copyOf(attributes);
    }

    /**
     * Total tokens.
     */
    public int totalTokens() {
        return inputTokens + outputTokens;
    }
}

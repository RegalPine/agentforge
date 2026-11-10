package io.github.regalpine.agentforge.observability.metric;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * A metric data point.
 */
public record MetricPoint(
    MetricCategory category,
    String name,
    double value,
    Instant timestamp,
    Map<String, String> attributes
) {

    public MetricPoint {
        Objects.requireNonNull(category);
        Objects.requireNonNull(name);
        timestamp = timestamp == null
            ? Instant.now() : timestamp;
        attributes = attributes == null
            ? Map.of() : Map.copyOf(attributes);
    }
}

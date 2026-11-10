package io.github.regalpine.agentforge.observability.metric;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * In-memory metric exporter for testing.
 */
public final class InMemoryMetricExporter
    implements MetricExporter {

    private final List<MetricPoint> exported =
        Collections.synchronizedList(
            new ArrayList<>()
        );

    @Override
    public void export(MetricPoint point) {
        exported.add(point);
    }

    @Override
    public String exporterName() {
        return "in-memory";
    }

    /**
     * Get all exported points.
     */
    public List<MetricPoint> points() {
        return List.copyOf(exported);
    }

    /**
     * Clear exported points.
     */
    public void reset() {
        exported.clear();
    }

    /**
     * Get count.
     */
    public int count() {
        return exported.size();
    }
}

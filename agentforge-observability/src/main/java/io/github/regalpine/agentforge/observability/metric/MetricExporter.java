package io.github.regalpine.agentforge.observability.metric;

/**
 * Metric exporter SPI.
 * AF-ENHANCE-06 §7: exports metrics
 * to external systems.
 */
public interface MetricExporter {

    /**
     * Export a single metric point.
     */
    void export(MetricPoint point);

    /**
     * Flush buffered metrics.
     */
    default void flush() {}

    /**
     * Shutdown exporter.
     */
    default void shutdown() {}

    /**
     * Exporter name.
     */
    String exporterName();
}

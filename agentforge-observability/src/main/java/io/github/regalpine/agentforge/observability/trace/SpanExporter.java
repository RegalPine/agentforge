package io.github.regalpine.agentforge.observability.trace;

import java.util.List;

/**
 * Span exporter SPI.
 * AF-ENHANCE-06 §6: exports completed
 * spans to external systems.
 *
 * <p>Implementations:
 * <ul>
 *   <li>OpenTelemetry adapter</li>
 *   <li>Custom JSON exporter</li>
 *   <li>In-memory (testing)</li>
 * </ul>
 */
public interface SpanExporter {

    /**
     * Export a batch of spans.
     */
    ExportResult export(List<SpanData> spans);

    /**
     * Flush pending exports.
     */
    default void flush() {}

    /**
     * Close and release resources.
     */
    default void shutdown() {}

    /**
     * Exporter name.
     */
    String exporterName();

    /**
     * Export result.
     */
    record ExportResult(
        boolean success,
        int exportedCount,
        String message
    ) {

        public static ExportResult ok(int count) {
            return new ExportResult(
                true, count, "OK"
            );
        }

        public static ExportResult fail(
            String msg
        ) {
            return new ExportResult(
                false, 0, msg
            );
        }
    }
}

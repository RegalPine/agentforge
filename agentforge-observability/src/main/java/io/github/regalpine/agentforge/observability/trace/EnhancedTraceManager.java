package io.github.regalpine.agentforge.observability.trace;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Enhanced trace manager with end-to-end
 * trace completeness.
 * AF-ENHANCE-06 §6: covers
 * Execution → NodeExecution → Invocation.
 */
public final class EnhancedTraceManager {

    private final List<SpanData> spans =
        new CopyOnWriteArrayList<>();

    private final Map<String, ActiveSpan> active =
        new ConcurrentHashMap<>();

    private final List<SpanExporter> exporters =
        new CopyOnWriteArrayList<>();

    private final AtomicLong spanCounter =
        new AtomicLong(0);

    /**
     * Register a span exporter.
     */
    public EnhancedTraceManager addExporter(
        SpanExporter exporter
    ) {
        exporters.add(
            Objects.requireNonNull(exporter)
        );
        return this;
    }

    /**
     * Start a root span.
     */
    public String startSpan(
        String operation,
        String traceId,
        Map<String, String> attrs
    ) {
        String spanId = "span-"
            + spanCounter.incrementAndGet();

        active.put(spanId, new ActiveSpan(
            traceId, spanId, null,
            operation, Instant.now(), attrs
        ));

        return spanId;
    }

    /**
     * Start a child span.
     */
    public String startChildSpan(
        String operation,
        String traceId,
        String parentSpanId,
        Map<String, String> attrs
    ) {
        String spanId = "span-"
            + spanCounter.incrementAndGet();

        active.put(spanId, new ActiveSpan(
            traceId, spanId, parentSpanId,
            operation, Instant.now(), attrs
        ));

        return spanId;
    }

    /**
     * End a span successfully.
     */
    public void endSpan(String spanId) {
        ActiveSpan a = active.remove(spanId);
        if (a == null) return;

        SpanData data = new SpanData(
            a.traceId, a.spanId,
            a.parentSpanId, a.operation,
            a.startTime, Instant.now(),
            SpanData.SpanStatus.OK,
            a.attributes, null
        );

        spans.add(data);
        exportToSinks(List.of(data));
    }

    /**
     * End a span with error.
     */
    public void endSpanError(
        String spanId,
        String errorMessage
    ) {
        ActiveSpan a = active.remove(spanId);
        if (a == null) return;

        SpanData data = new SpanData(
            a.traceId, a.spanId,
            a.parentSpanId, a.operation,
            a.startTime, Instant.now(),
            SpanData.SpanStatus.ERROR,
            a.attributes, errorMessage
        );

        spans.add(data);
        exportToSinks(List.of(data));
    }

    /**
     * Get all completed spans.
     */
    public List<SpanData> completedSpans() {
        return List.copyOf(spans);
    }

    /**
     * Get spans for a specific trace.
     */
    public List<SpanData> spansForTrace(
        String traceId
    ) {
        return spans.stream()
            .filter(s -> s.traceId()
                .equals(traceId))
            .toList();
    }

    /**
     * Get count of active spans.
     */
    public int activeSpanCount() {
        return active.size();
    }

    /**
     * Flush all exporters.
     */
    public void flush() {
        for (SpanExporter e : exporters) {
            e.flush();
        }
    }

    /**
     * Shutdown all exporters.
     */
    public void shutdown() {
        for (SpanExporter e : exporters) {
            e.shutdown();
        }
    }

    private void exportToSinks(
        List<SpanData> batch
    ) {
        for (SpanExporter e : exporters) {
            try {
                e.export(batch);
            } catch (Exception ignored) {
                // Exporter errors must not
                // break the pipeline
            }
        }
    }

    private record ActiveSpan(
        String traceId,
        String spanId,
        String parentSpanId,
        String operation,
        Instant startTime,
        Map<String, String> attributes
    ) {}
}

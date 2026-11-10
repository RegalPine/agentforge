package io.github.regalpine.agentforge.observability.trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * In-memory span exporter for testing.
 */
public final class InMemorySpanExporter
    implements SpanExporter {

    private final List<SpanData> exported =
        Collections.synchronizedList(
            new ArrayList<>()
        );

    @Override
    public ExportResult export(
        List<SpanData> spans
    ) {
        exported.addAll(spans);
        return ExportResult.ok(spans.size());
    }

    @Override
    public String exporterName() {
        return "in-memory";
    }

    /**
     * Get all exported spans.
     */
    public List<SpanData> spans() {
        return List.copyOf(exported);
    }

    /**
     * Clear exported spans.
     */
    public void reset() {
        exported.clear();
    }

    /**
     * Get the count of exported spans.
     */
    public int count() {
        return exported.size();
    }
}

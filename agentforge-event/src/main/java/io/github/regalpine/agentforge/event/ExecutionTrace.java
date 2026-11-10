package io.github.regalpine.agentforge.event;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 执行 Trace：一次 Execution 的完整视图。
 *
 * <p>Trace = Timeline + Graph。</p>
 */
public record ExecutionTrace(
    String executionId,
    String traceId,
    String parentExecutionId,
    String agentId,
    List<EventEnvelope> events,
    List<Span> spans,
    Map<String, Object> outputs
) {

    public ExecutionTrace {
        events = events == null
            ? List.of()
            : List.copyOf(events);
        spans = spans == null
            ? List.of()
            : List.copyOf(spans);
        outputs = outputs == null
            ? Map.of()
            : Map.copyOf(outputs);
    }

    /**
     * 从事件流构建 Trace。
     */
    public static ExecutionTrace fromEvents(
        String executionId,
        List<EventEnvelope> events
    ) {
        String traceId = null;
        String parentExecId = null;
        String agentId = null;
        for (EventEnvelope e : events) {
            if (traceId == null
                && e.traceId() != null
            ) {
                traceId = e.traceId();
            }
            if (parentExecId == null
                && e.parentExecutionId() != null
            ) {
                parentExecId =
                    e.parentExecutionId();
            }
            if (agentId == null
                && e.agentId() != null
            ) {
                agentId = e.agentId();
            }
        }
        return new ExecutionTrace(
            executionId,
            traceId != null ? traceId : executionId,
            parentExecId, agentId,
            events, List.of(), Map.of()
        );
    }

    /**
     * 返回按时间排序的事件列表。
     */
    public List<EventEnvelope> timeline() {
        List<EventEnvelope> sorted =
            new ArrayList<>(events);
        sorted.sort(
            Comparator.comparingLong(
                EventEnvelope::sequence
            )
        );
        return List.copyOf(sorted);
    }

    /**
     * 返回指定类型的事件。
     */
    public List<EventEnvelope> eventsOfType(
        String... types
    ) {
        var typeSet = java.util.Set.of(types);
        return events.stream()
            .filter(e ->
                typeSet.contains(e.eventType())
            )
            .toList();
    }

    /**
     * 是否有失败事件。
     */
    public boolean hasFailed() {
        return events.stream().anyMatch(e ->
            e.eventType().endsWith(".failed")
        );
    }

    /**
     * 返回 trace 的总时长（毫秒）。
     */
    public long durationMs() {
        if (events.isEmpty()) {
            return 0;
        }
        var sorted = timeline();
        var first = sorted.get(0).timestamp();
        var last = sorted.get(sorted.size() - 1)
            .timestamp();
        return java.time.Duration.between(
            first, last
        ).toMillis();
    }

    /**
     * Builder 用于增量构建 Trace。
     */
    public static TraceBuilder traceBuilder(
        String executionId
    ) {
        return new TraceBuilder(executionId);
    }

    public static final class TraceBuilder {
        private final String executionId;
        private String traceId;
        private String parentExecutionId;
        private String agentId;
        private final List<EventEnvelope> events =
            new ArrayList<>();
        private final List<Span> spans =
            new ArrayList<>();
        private final Map<String, Object> outputs =
            new ConcurrentHashMap<>();

        TraceBuilder(String executionId) {
            this.executionId = executionId;
            this.traceId = executionId;
        }

        public TraceBuilder traceId(String v) {
            this.traceId = v; return this;
        }
        public TraceBuilder parentExecutionId(
            String v
        ) {
            this.parentExecutionId = v;
            return this;
        }
        public TraceBuilder agentId(String v) {
            this.agentId = v; return this;
        }

        public TraceBuilder addEvent(
            EventEnvelope event
        ) {
            events.add(event);
            return this;
        }

        public TraceBuilder addSpan(Span span) {
            spans.add(span);
            return this;
        }

        public TraceBuilder output(
            String key, Object value
        ) {
            outputs.put(key, value);
            return this;
        }

        public ExecutionTrace build() {
            return new ExecutionTrace(
                executionId, traceId,
                parentExecutionId, agentId,
                events, spans, outputs
            );
        }
    }
}

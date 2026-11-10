package io.github.regalpine.agentforge.event;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Span 跟踪器。
 *
 * <p>管理 Span 的生命周期，
 * 维护层级关系。</p>
 */
public final class SpanTracker {

    private final Map<String, Span> spans =
        new ConcurrentHashMap<>();

    private final ThreadLocal<Deque<String>>
        stack = ThreadLocal.withInitial(
            ArrayDeque::new
        );

    /**
     * 开始新 Span。
     */
    public Span startSpan(
        SpanKind kind,
        String executionId,
        String agentId
    ) {
        String parentSpanId = currentSpanId();
        Span span = Span.builder()
            .kind(kind)
            .parentSpanId(parentSpanId)
            .executionId(executionId)
            .agentId(agentId)
            .startTime(Instant.now())
            .status(SpanStatus.RUNNING)
            .build();
        spans.put(span.spanId(), span);
        stack.get().push(span.spanId());
        return span;
    }

    /**
     * 完成当前 Span。
     */
    public Span completeSpan(
        SpanStatus finalStatus
    ) {
        Deque<String> s = stack.get();
        if (s.isEmpty()) {
            throw new IllegalStateException(
                "No active span"
            );
        }
        String spanId = s.pop();
        Span old = spans.get(spanId);
        Span completed = new Span(
            old.spanId(),
            old.parentSpanId(),
            old.kind(),
            old.startTime(),
            Instant.now(),
            finalStatus,
            old.executionId(),
            old.agentId(),
            old.attributes()
        );
        spans.put(spanId, completed);
        return completed;
    }

    /**
     * 获取当前活跃 Span ID。
     */
    public String currentSpanId() {
        Deque<String> s = stack.get();
        return s.isEmpty() ? null : s.peek();
    }

    /**
     * 获取所有 Span。
     */
    public List<Span> allSpans() {
        return List.copyOf(spans.values());
    }

    /**
     * 获取指定 execution 的 Span。
     */
    public List<Span> spansFor(
        String executionId
    ) {
        return spans.values().stream()
            .filter(s -> executionId.equals(
                s.executionId()
            ))
            .toList();
    }

    /**
     * 构建 Span 层级树（根 Span 列表）。
     */
    public List<Span> rootSpans() {
        return spans.values().stream()
            .filter(s ->
                s.parentSpanId() == null
            )
            .toList();
    }

    /**
     * 获取指定 Span 的子 Span。
     */
    public List<Span> children(
        String parentSpanId
    ) {
        return spans.values().stream()
            .filter(s ->
                parentSpanId.equals(
                    s.parentSpanId()
                )
            )
            .toList();
    }

    public void clear() {
        spans.clear();
        stack.remove();
    }
}

package io.github.regalpine.agentforge.event;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * 执行 Span：有边界的执行单元。
 */
public record Span(
    String spanId,
    String parentSpanId,
    SpanKind kind,
    Instant startTime,
    Instant endTime,
    SpanStatus status,
    String executionId,
    String agentId,
    Map<String, Object> attributes
) {

    public Span {
        Objects.requireNonNull(spanId, "spanId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(startTime, "startTime");
        Objects.requireNonNull(status, "status");
        attributes = attributes == null
            ? Map.of()
            : Map.copyOf(attributes);
    }

    public static SpanBuilder builder() {
        return new SpanBuilder();
    }

    public static final class SpanBuilder {
        private String spanId;
        private String parentSpanId;
        private SpanKind kind;
        private Instant startTime;
        private Instant endTime;
        private SpanStatus status = SpanStatus.RUNNING;
        private String executionId;
        private String agentId;
        private Map<String, Object> attributes;

        SpanBuilder() {}

        public SpanBuilder spanId(String v) {
            this.spanId = v; return this;
        }
        public SpanBuilder parentSpanId(String v) {
            this.parentSpanId = v; return this;
        }
        public SpanBuilder kind(SpanKind v) {
            this.kind = v; return this;
        }
        public SpanBuilder startTime(Instant v) {
            this.startTime = v; return this;
        }
        public SpanBuilder endTime(Instant v) {
            this.endTime = v; return this;
        }
        public SpanBuilder status(SpanStatus v) {
            this.status = v; return this;
        }
        public SpanBuilder executionId(String v) {
            this.executionId = v; return this;
        }
        public SpanBuilder agentId(String v) {
            this.agentId = v; return this;
        }
        public SpanBuilder attributes(Map<String, Object> v) {
            this.attributes = v; return this;
        }

        public Span build() {
            if (spanId == null) {
                spanId = EventEnvelope.generateEventId()
                    .replace("evt-", "span-");
            }
            if (startTime == null) {
                startTime = Instant.now();
            }
            return new Span(
                spanId, parentSpanId, kind,
                startTime, endTime, status,
                executionId, agentId, attributes
            );
        }
    }
}

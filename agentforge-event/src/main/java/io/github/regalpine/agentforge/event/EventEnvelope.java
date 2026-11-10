package io.github.regalpine.agentforge.event;


import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 统一事件信封。
 *
 * <p>所有 Runtime 事件必须使用此信封，
 * 负责 identity / ordering / correlation / routing。</p>
 */
public record EventEnvelope(
    String eventId,
    String eventType,
    int eventVersion,
    Instant timestamp,
    String executionId,
    String parentExecutionId,
    String traceId,
    String spanId,
    String parentSpanId,
    String agentId,
    String tenantId,
    long sequence,
    Map<String, Object> payload,
    Map<String, String> metadata
) {

    private static final AtomicLong SEQ =
        new AtomicLong(0);

    public EventEnvelope {
        Objects.requireNonNull(
            eventId, "eventId"
        );
        Objects.requireNonNull(
            eventType, "eventType"
        );
        Objects.requireNonNull(
            timestamp, "timestamp"
        );

        if (eventType.contains("$")) {
            throw new IllegalArgumentException(
                "eventType must use dot-notation, not Java class names"
            );
        }

        // Reject Java class names (e.g. com.example.MyEvent)
        // Valid event types use lowercase segments
        for (char c : eventType.toCharArray()) {
            if (Character.isUpperCase(c)) {
                throw new IllegalArgumentException(
                    "eventType must not use Java class names: "
                        + eventType
                );
            }
        }

        payload = payload == null
            ? Map.of()
            : Map.copyOf(payload);
        metadata = metadata == null
            ? Map.of()
            : Map.copyOf(metadata);
    }

    /**
     * 生成全局唯一 eventId。
     */
    public static String generateEventId() {
        return "evt-" + UUID.randomUUID();
    }

    /**
     * 生成单调递增 sequence。
     */
    public static long nextSequence() {
        return SEQ.incrementAndGet();
    }

    /**
     * 重置 sequence（仅测试用）。
     */
    public static void resetSequence() {
        SEQ.set(0);
    }

    /**
     * 返回此事件的 category。
     */
    public EventCategory category() {
        return EventCategory.fromEventType(eventType);
    }

    /**
     * 是否为 critical 事件（不可丢弃）。
     */
    public boolean isCritical() {
        return "execution.started".equals(eventType)
            || "execution.completed".equals(
                eventType
            )
            || "execution.failed".equals(eventType)
            || "policy.denied".equals(eventType)
            || "policy.approval.required".equals(
                eventType
            )
            || "human.approval.received".equals(
                eventType
            )
            || "security.violation".equals(
                eventType
            );
    }

    public Builder toBuilder() {
        return new Builder()
            .eventId(eventId)
            .eventType(eventType)
            .eventVersion(eventVersion)
            .timestamp(timestamp)
            .executionId(executionId)
            .parentExecutionId(parentExecutionId)
            .traceId(traceId)
            .spanId(spanId)
            .parentSpanId(parentSpanId)
            .agentId(agentId)
            .tenantId(tenantId)
            .sequence(sequence)
            .payload(payload)
            .metadata(metadata);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String eventId;
        private String eventType;
        private int eventVersion = 1;
        private Instant timestamp;
        private String executionId;
        private String parentExecutionId;
        private String traceId;
        private String spanId;
        private String parentSpanId;
        private String agentId;
        private String tenantId;
        private long sequence;
        private Map<String, Object> payload;
        private Map<String, String> metadata;

        Builder() {}

        public Builder eventId(String v) {
            this.eventId = v; return this;
        }
        public Builder eventType(String v) {
            this.eventType = v; return this;
        }
        public Builder eventVersion(int v) {
            this.eventVersion = v; return this;
        }
        public Builder timestamp(Instant v) {
            this.timestamp = v; return this;
        }
        public Builder executionId(String v) {
            this.executionId = v; return this;
        }
        public Builder parentExecutionId(String v) {
            this.parentExecutionId = v; return this;
        }
        public Builder traceId(String v) {
            this.traceId = v; return this;
        }
        public Builder spanId(String v) {
            this.spanId = v; return this;
        }
        public Builder parentSpanId(String v) {
            this.parentSpanId = v; return this;
        }
        public Builder agentId(String v) {
            this.agentId = v; return this;
        }
        public Builder tenantId(String v) {
            this.tenantId = v; return this;
        }
        public Builder sequence(long v) {
            this.sequence = v; return this;
        }
        public Builder payload(Map<String, Object> v) {
            this.payload = v; return this;
        }
        public Builder metadata(Map<String, String> v) {
            this.metadata = v; return this;
        }

        public EventEnvelope build() {
            if (eventId == null) {
                eventId = generateEventId();
            }
            if (timestamp == null) {
                timestamp = Instant.now();
            }
            if (sequence <= 0) {
                sequence = nextSequence();
            }
            return new EventEnvelope(
                eventId, eventType, eventVersion,
                timestamp, executionId,
                parentExecutionId, traceId,
                spanId, parentSpanId,
                agentId, tenantId,
                sequence, payload, metadata
            );
        }
    }
}

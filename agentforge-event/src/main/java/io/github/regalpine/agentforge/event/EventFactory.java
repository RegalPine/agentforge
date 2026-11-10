package io.github.regalpine.agentforge.event;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 事件工厂：快速创建标准事件。
 */
public final class EventFactory {

    private EventFactory() {}

    public static EventEnvelope lifecycle(
        String eventType,
        String executionId,
        String agentId
    ) {
        return EventEnvelope.builder()
            .eventType(eventType)
            .executionId(executionId)
            .agentId(agentId)
            .build();
    }

    public static EventEnvelope execution(
        String eventType,
        String executionId,
        String traceId
    ) {
        return EventEnvelope.builder()
            .eventType(eventType)
            .executionId(executionId)
            .traceId(traceId)
            .build();
    }

    public static EventEnvelope skill(
        String eventType,
        String executionId,
        String traceId,
        String spanId,
        String skillId
    ) {
        return EventEnvelope.builder()
            .eventType(eventType)
            .executionId(executionId)
            .traceId(traceId)
            .spanId(spanId)
            .payload(Map.of("skillId", skillId))
            .build();
    }

    public static EventEnvelope tool(
        String eventType,
        String executionId,
        String traceId,
        String spanId,
        String parentSpanId,
        String toolId
    ) {
        return EventEnvelope.builder()
            .eventType(eventType)
            .executionId(executionId)
            .traceId(traceId)
            .spanId(spanId)
            .parentSpanId(parentSpanId)
            .payload(Map.of("toolId", toolId))
            .build();
    }

    public static EventEnvelope model(
        String eventType,
        String executionId,
        String traceId,
        String spanId,
        String provider,
        String model
    ) {
        return EventEnvelope.builder()
            .eventType(eventType)
            .executionId(executionId)
            .traceId(traceId)
            .spanId(spanId)
            .payload(Map.of(
                "provider", provider,
                "model", model
            ))
            .build();
    }

    public static EventEnvelope error(
        String executionId,
        String traceId,
        String errorCode,
        String message,
        boolean retryable
    ) {
        return EventEnvelope.builder()
            .eventType(EventTypes.ERROR_OCCURRED)
            .executionId(executionId)
            .traceId(traceId)
            .payload(Map.of(
                "errorCode", errorCode,
                "message", message,
                "retryable", retryable
            ))
            .build();
    }
}

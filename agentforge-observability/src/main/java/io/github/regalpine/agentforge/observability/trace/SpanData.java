package io.github.regalpine.agentforge.observability.trace;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * A completed span.
 * AF-ENHANCE-06 §6: spans carry full
 * timing, status and attribute data.
 */
public record SpanData(
    String traceId,
    String spanId,
    String parentSpanId,
    String operation,
    Instant startTime,
    Instant endTime,
    SpanStatus status,
    Map<String, String> attributes,
    String errorMessage
) {

    public SpanData {
        Objects.requireNonNull(traceId);
        Objects.requireNonNull(spanId);
        Objects.requireNonNull(operation);
        startTime = startTime == null
            ? Instant.now() : startTime;
        attributes = attributes == null
            ? Map.of() : Map.copyOf(attributes);
        status = status == null
            ? SpanStatus.OK : status;
    }

    /**
     * Duration in milliseconds.
     */
    public long durationMs() {
        if (endTime == null
            || startTime == null
        ) {
            return 0;
        }
        return endTime.toEpochMilli()
            - startTime.toEpochMilli();
    }

    /**
     * Span status.
     */
    public enum SpanStatus {
        OK,
        ERROR,
        UNSET
    }
}

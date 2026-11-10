package io.github.regalpine.agentforge.debugger;

import java.util.Objects;

/**
 * 执行游标。
 *
 * <p>Debugger 当前观察位置。
 * 对应 AF-ENHANCE-04 §9。</p>
 */
public record ExecutionCursor(
    String eventId,
    long sequence,
    String spanId,
    String stepId,
    CursorType type
) {

    public ExecutionCursor {
        Objects.requireNonNull(
            eventId, "eventId"
        );
        Objects.requireNonNull(
            type, "type"
        );
    }

    public static ExecutionCursor of(
        String eventId,
        long sequence,
        CursorType type
    ) {
        return new ExecutionCursor(
            eventId, sequence,
            null, null, type
        );
    }

    public static ExecutionCursor at(
        String eventId,
        long sequence,
        String spanId,
        CursorType type
    ) {
        return new ExecutionCursor(
            eventId, sequence,
            spanId, null, type
        );
    }
}

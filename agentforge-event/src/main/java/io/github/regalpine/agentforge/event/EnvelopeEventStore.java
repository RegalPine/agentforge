package io.github.regalpine.agentforge.event;

import java.util.List;

/**
 * 事件存储。
 *
 * <p>支持 Append / Read / ReadByExecution /
 * ReadByTrace / ReadFromSequence。</p>
 */
public interface EnvelopeEventStore {

    void append(
        EventEnvelope event
    );

    List<EventEnvelope> read(
        String executionId
    );

    List<EventEnvelope> readAfter(
        String executionId,
        long sequence
    );

    List<EventEnvelope> readByTrace(
        String traceId
    );

    long count(
        String executionId
    );
}

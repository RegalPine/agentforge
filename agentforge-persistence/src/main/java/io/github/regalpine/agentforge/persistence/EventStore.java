package io.github.regalpine.agentforge.persistence;

import io.github.regalpine.agentforge.event.ExecutionEvent;
import io.github.regalpine.agentforge.event.ExecutionId;

import java.util.List;

public interface EventStore {

    void append(
        ExecutionEvent event
    );

    List<ExecutionEvent> list(
        ExecutionId executionId
    );

    List<ExecutionEvent> listAfter(
        ExecutionId executionId,
        long sequence
    );
}

package io.github.regalpine.agentforge.event;

import io.github.regalpine.agentforge.event.ExecutionEvent;

public interface EventPublisher {

    void publish(
        ExecutionEvent event
    );
}

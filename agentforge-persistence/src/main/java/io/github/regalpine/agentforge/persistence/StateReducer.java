package io.github.regalpine.agentforge.persistence;

import io.github.regalpine.agentforge.event.ExecutionEvent;
import io.github.regalpine.agentforge.event.ExecutionState;

public interface StateReducer {

    ExecutionState apply(
        ExecutionState state,
        ExecutionEvent event
    );
}

package io.github.regalpine.agentforge.persistence;

public interface OutboxDispatcher {

    void dispatch(
        OutboxMessage message
    );
}

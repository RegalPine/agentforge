package io.github.regalpine.agentforge.event;

/**
 * 事件订阅者。
 */
public interface EventSubscriber {

    void onEvent(
        EventEnvelope event
    );
}

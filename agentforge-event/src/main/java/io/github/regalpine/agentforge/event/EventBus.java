package io.github.regalpine.agentforge.event;

/**
 * 事件总线。
 *
 * <p>负责 publish / route / persist，
 * 不负责业务决策。</p>
 */
public interface EventBus {

    /**
     * 发布事件到所有匹配的订阅者。
     */
    void publish(
        EventEnvelope event
    );

    /**
     * 订阅事件（无过滤）。
     */
    Subscription subscribe(
        EventSubscriber subscriber
    );

    /**
     * 按过滤条件订阅事件。
     */
    Subscription subscribe(
        EventFilter filter,
        EventSubscriber subscriber
    );

    /**
     * 订阅句柄，用于取消订阅。
     */
    interface Subscription {
        void cancel();
    }
}

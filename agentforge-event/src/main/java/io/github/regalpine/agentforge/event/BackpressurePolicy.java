package io.github.regalpine.agentforge.event;

/**
 * 背压处理策略。
 */
public enum BackpressurePolicy {

    /**
     * 阻塞生产者。
     */
    BLOCK,

    /**
     * 丢弃非关键事件。
     */
    DROP_NON_CRITICAL,

    /**
     * 断开慢消费者。
     */
    DISCONNECT,

    /**
     * 持久化到存储后丢弃。
     */
    PERSIST
}

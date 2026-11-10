package io.github.regalpine.agentforge.runtime.streaming;

import java.util.function.Consumer;

/**
 * Event stream interface for consuming AgentForge events.
 * <p>
 * Supports both pull-based (iterator) and push-based (callback)
 * consumption patterns.
 */
public interface EventStream {

    /**
     * Subscribe to events with a callback.
     *
     * @param consumer the event consumer
     * @return a subscription handle for cancellation
     */
    Subscription subscribe(Consumer<AgentForgeEvent> consumer);

    /**
     * Subscribe to events of a specific type.
     *
     * @param type     the event type to filter
     * @param consumer the event consumer
     * @return a subscription handle
     */
    Subscription subscribe(EventType type, Consumer<AgentForgeEvent> consumer);

    /**
     * Close the event stream and release resources.
     */
    void close();

    /**
     * Subscription handle for cancelling an event subscription.
     */
    interface Subscription {

        /**
         * Cancel this subscription.
         */
        void cancel();

        /**
         * Check if this subscription is still active.
         *
         * @return true if active
         */
        boolean isActive();
    }
}

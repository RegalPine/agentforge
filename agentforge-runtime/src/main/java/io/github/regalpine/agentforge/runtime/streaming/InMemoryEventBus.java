package io.github.regalpine.agentforge.runtime.streaming;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * In-memory implementation of both EventStream and EventSink.
 * <p>
 * Thread-safe implementation using CopyOnWriteArrayList for
 * subscriber management. Suitable for single-process streaming.
 * For distributed scenarios, this can be replaced with a
 * message-broker-backed implementation.
 */
public final class InMemoryEventBus implements EventStream, EventSink {

    private final List<SubscriptionEntry> subscriptions = new CopyOnWriteArrayList<>();
    private final AtomicLong sequenceCounter = new AtomicLong(0);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    @Override
    public Subscription subscribe(Consumer<AgentForgeEvent> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        checkNotClosed();

        SubscriptionEntry entry = new SubscriptionEntry(null, consumer);
        subscriptions.add(entry);
        return entry;
    }

    @Override
    public Subscription subscribe(EventType type, Consumer<AgentForgeEvent> consumer) {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(consumer, "consumer cannot be null");
        checkNotClosed();

        SubscriptionEntry entry = new SubscriptionEntry(type, consumer);
        subscriptions.add(entry);
        return entry;
    }

    @Override
    public void publish(AgentForgeEvent event) {
        Objects.requireNonNull(event, "event cannot be null");
        if (closed.get()) return;

        for (SubscriptionEntry entry : subscriptions) {
            if (entry.isActive() && entry.matches(event)) {
                try {
                    entry.consumer().accept(event);
                } catch (Exception e) {
                    // Don't let one subscriber's failure affect others
                }
            }
        }
    }

    @Override
    public long publishWithSequence(AgentForgeEvent event) {
        long seq = sequenceCounter.incrementAndGet();
        publish(event.withSequence(seq));
        return seq;
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            for (SubscriptionEntry entry : subscriptions) {
                entry.cancel();
            }
            subscriptions.clear();
        }
    }

    /**
     * Get the current sequence number.
     *
     * @return the sequence number
     */
    public long currentSequence() {
        return sequenceCounter.get();
    }

    /**
     * Get the number of active subscriptions.
     *
     * @return the subscription count
     */
    public int subscriptionCount() {
        return (int) subscriptions.stream().filter(SubscriptionEntry::isActive).count();
    }

    /**
     * Check if this bus is closed.
     *
     * @return true if closed
     */
    public boolean isClosed() {
        return closed.get();
    }

    private void checkNotClosed() {
        if (closed.get()) {
            throw new IllegalStateException("EventBus is closed");
        }
    }

    private static final class SubscriptionEntry implements Subscription {
        private final EventType filterType;
        private final Consumer<AgentForgeEvent> consumer;
        private final AtomicBoolean active = new AtomicBoolean(true);

        SubscriptionEntry(EventType filterType, Consumer<AgentForgeEvent> consumer) {
            this.filterType = filterType;
            this.consumer = consumer;
        }

        @Override
        public void cancel() {
            active.set(false);
        }

        @Override
        public boolean isActive() {
            return active.get();
        }

        boolean matches(AgentForgeEvent event) {
            return filterType == null || filterType == event.type();
        }

        Consumer<AgentForgeEvent> consumer() {
            return consumer;
        }
    }
}

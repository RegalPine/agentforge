package io.github.regalpine.agentforge.event;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 内存 EventBus 实现。
 */
public final class InMemoryEventBus
    implements EventBus {

    private record Entry(
        EventFilter filter,
        EventSubscriber subscriber
    ) {}

    private final List<Entry> entries =
        new CopyOnWriteArrayList<>();

    private final List<EventEnvelope> history =
        new CopyOnWriteArrayList<>();

    @Override
    public void publish(
        EventEnvelope event
    ) {
        history.add(event);
        for (Entry entry : entries) {
            if (entry.filter().matches(event)) {
                entry.subscriber().onEvent(event);
            }
        }
    }

    @Override
    public EventBus.Subscription subscribe(
        EventSubscriber subscriber
    ) {
        return subscribe(EventFilter.all(), subscriber);
    }

    @Override
    public EventBus.Subscription subscribe(
        EventFilter filter,
        EventSubscriber subscriber
    ) {
        Entry entry = new Entry(filter, subscriber);
        entries.add(entry);
        return () -> entries.remove(entry);
    }

    /**
     * 返回已发布的所有事件（按 sequence 排序）。
     */
    public List<EventEnvelope> history() {
        List<EventEnvelope> sorted =
            new ArrayList<>(history);
        sorted.sort(
            Comparator.comparingLong(
                EventEnvelope::sequence
            )
        );
        return List.copyOf(sorted);
    }

    /**
     * 清除历史。
     */
    public void clear() {
        history.clear();
        entries.clear();
    }
}

package io.github.regalpine.agentforge.observability.metric;

import io.github.regalpine.agentforge.observability.MetricsRecorder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class InMemoryMetricsRecorder
    implements MetricsRecorder {

    private final Map<String, AtomicLong> counters =
        new ConcurrentHashMap<>();

    private final List<RecordedValue> values =
        Collections.synchronizedList(new ArrayList<>());

    @Override
    public void increment(
        String name,
        Map<String, String> attributes
    ) {
        counters
            .computeIfAbsent(
                name,
                k -> new AtomicLong(0)
            )
            .incrementAndGet();
    }

    @Override
    public void record(
        String name,
        double value,
        Map<String, String> attributes
    ) {
        values.add(
            new RecordedValue(
                name,
                value,
                attributes
            )
        );
    }

    public long getCount(
        String name
    ) {
        AtomicLong count =
            counters.get(name);

        return count == null
            ? 0
            : count.get();
    }

    public List<RecordedValue> recordedValues() {
        return List.copyOf(values);
    }

    public record RecordedValue(
        String name,
        double value,
        Map<String, String> attributes
    ) {
    }
}

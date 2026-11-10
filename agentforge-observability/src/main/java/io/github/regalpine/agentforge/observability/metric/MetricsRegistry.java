package io.github.regalpine.agentforge.observability.metric;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics registry with 12-category
 * classification.
 * AF-ENHANCE-06 §7: collects metrics
 * across all subsystems.
 */
public final class MetricsRegistry {

    private final Map<MetricCategory,
        List<MetricPoint>> metrics =
        new ConcurrentHashMap<>();

    private final Map<String, AtomicLong> counters =
        new ConcurrentHashMap<>();

    private final List<MetricExporter> exporters =
        new CopyOnWriteArrayList<>();

    /**
     * Register a metric exporter.
     */
    public MetricsRegistry addExporter(
        MetricExporter exporter
    ) {
        exporters.add(
            Objects.requireNonNull(exporter)
        );
        return this;
    }

    /**
     * Record a counter increment.
     */
    public void increment(
        MetricCategory category,
        String name,
        Map<String, String> attrs
    ) {
        String key = category + ":" + name;
        counters.computeIfAbsent(
            key, k -> new AtomicLong(0)
        ).incrementAndGet();

        record(category, name, 1.0, attrs);
    }

    /**
     * Record a gauge value.
     */
    public void record(
        MetricCategory category,
        String name,
        double value,
        Map<String, String> attrs
    ) {
        MetricPoint point = new MetricPoint(
            category, name, value,
            Instant.now(), attrs
        );

        metrics.computeIfAbsent(
            category,
            k -> Collections.synchronizedList(
                new ArrayList<>()
            )
        ).add(point);

        for (MetricExporter e : exporters) {
            try {
                e.export(point);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Get counter value.
     */
    public long getCount(
        MetricCategory category,
        String name
    ) {
        String key = category + ":" + name;
        AtomicLong c = counters.get(key);
        return c == null ? 0 : c.get();
    }

    /**
     * Get all points for a category.
     */
    public List<MetricPoint> pointsFor(
        MetricCategory category
    ) {
        List<MetricPoint> list =
            metrics.get(category);
        return list == null
            ? List.of() : List.copyOf(list);
    }

    /**
     * Get total point count.
     */
    public int totalPoints() {
        return metrics.values().stream()
            .mapToInt(List::size)
            .sum();
    }

    /**
     * Get all categories that have data.
     */
    public Set<MetricCategory> activeCategories() {
        return metrics.keySet().stream()
            .filter(c -> !metrics.get(c)
                .isEmpty())
            .collect(
                java.util.stream.Collectors
                    .toUnmodifiableSet()
            );
    }

    /**
     * Reset all metrics.
     */
    public void reset() {
        metrics.clear();
        counters.clear();
    }
}

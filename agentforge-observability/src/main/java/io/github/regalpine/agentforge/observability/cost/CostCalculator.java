package io.github.regalpine.agentforge.observability.cost;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Cost calculator.
 * AF-ENHANCE-06 §9: aggregates cost
 * at model, execution, and tenant levels.
 */
public final class CostCalculator {

    private final List<CostRecord> records =
        new CopyOnWriteArrayList<>();

    private final Map<String, Double> modelPricing =
        new ConcurrentHashMap<>();

    /**
     * Set pricing for a model
     * (USD per 1K tokens).
     */
    public CostCalculator setPricing(
        String modelId,
        double usdPer1kTokens
    ) {
        modelPricing.put(
            modelId, usdPer1kTokens
        );
        return this;
    }

    /**
     * Record a cost entry.
     */
    public void record(CostRecord record) {
        records.add(record);
    }

    /**
     * Calculate cost for a model call.
     */
    public CostRecord calculateModelCost(
        String modelId,
        String tenantId,
        int inputTokens,
        int outputTokens
    ) {
        double price = modelPricing.getOrDefault(
            modelId, 0.0
        );

        int totalTokens =
            inputTokens + outputTokens;
        double cost = (totalTokens / 1000.0) * price;

        CostRecord record = new CostRecord(
            UUID.randomUUID().toString(),
            CostLevel.MODEL,
            modelId,
            tenantId,
            modelId,
            cost,
            inputTokens,
            outputTokens,
            java.time.Instant.now(),
            Map.of()
        );

        records.add(record);
        return record;
    }

    /**
     * Get total cost for a tenant.
     */
    public double tenantCost(String tenantId) {
        return records.stream()
            .filter(r -> tenantId.equals(
                r.tenantId()
            ))
            .mapToDouble(CostRecord::costUsd)
            .sum();
    }

    /**
     * Get total cost for an execution.
     */
    public double executionCost(
        String executionId
    ) {
        return records.stream()
            .filter(r -> executionId.equals(
                r.entityId()
            ))
            .mapToDouble(CostRecord::costUsd)
            .sum();
    }

    /**
     * Get total cost across all records.
     */
    public double totalCost() {
        return records.stream()
            .mapToDouble(CostRecord::costUsd)
            .sum();
    }

    /**
     * Get all cost records.
     */
    public List<CostRecord> allRecords() {
        return List.copyOf(records);
    }

    /**
     * Get records for a specific level.
     */
    public List<CostRecord> recordsForLevel(
        CostLevel level
    ) {
        return records.stream()
            .filter(r -> r.level() == level)
            .toList();
    }

    /**
     * Get total token usage.
     */
    public long totalTokens() {
        return records.stream()
            .mapToLong(CostRecord::totalTokens)
            .sum();
    }

    /**
     * Reset all records.
     */
    public void reset() {
        records.clear();
    }
}

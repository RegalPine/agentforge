package io.github.regalpine.agentforge.distributed.quota;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * ResourceIsolation — AF-RUNTIME-01 §48-49.
 *
 * Provides per-worker resource isolation:
 * - Execution concurrency limit
 * - Model concurrency limit (per provider)
 * - Tool concurrency limit (per provider)
 * - Memory limit (advisory)
 * - CPU limit (advisory)
 * - Queue limit
 *
 * Prevents a single provider or tenant from
 * consuming all worker resources (§49).
 */
public final class ResourceIsolation {

    private final Semaphore executionSlots;
    private final int maxExecutions;
    private final int maxModelConcurrency;
    private final int maxToolConcurrency;
    private final long memoryLimitBytes;
    private final double cpuLimit;

    private final Map<String, Semaphore> modelSemaphores
        = new ConcurrentHashMap<>();

    private final Map<String, Semaphore> toolSemaphores
        = new ConcurrentHashMap<>();

    public ResourceIsolation(
        int maxExecutions,
        int maxModelConcurrency,
        int maxToolConcurrency,
        long memoryLimitBytes,
        double cpuLimit
    ) {
        if (maxExecutions <= 0) {
            throw new IllegalArgumentException(
                "maxExecutions must be > 0");
        }
        this.maxExecutions = maxExecutions;
        this.maxModelConcurrency = maxModelConcurrency;
        this.maxToolConcurrency = maxToolConcurrency;
        this.memoryLimitBytes = memoryLimitBytes;
        this.cpuLimit = cpuLimit;
        this.executionSlots = new Semaphore(
            maxExecutions, true);
    }

    /**
     * Default resource isolation.
     */
    public static ResourceIsolation defaults() {
        return new ResourceIsolation(
            50, 20, 10,
            Runtime.getRuntime()
                .maxMemory() / 2,
            Runtime.getRuntime()
                .availableProcessors() * 0.8
        );
    }

    /**
     * Try to acquire an execution slot.
     *
     * @return true if acquired, false if full
     */
    public boolean tryAcquireExecution() {
        return executionSlots.tryAcquire();
    }

    /**
     * Release an execution slot.
     */
    public void releaseExecution() {
        executionSlots.release();
    }

    /**
     * Get available execution slots.
     */
    public int availableExecutionSlots() {
        return executionSlots.availablePermits();
    }

    /**
     * Try to acquire a model call slot.
     *
     * @param modelId the model provider ID
     * @return true if acquired
     */
    public boolean tryAcquireModel(String modelId) {
        Semaphore sem = modelSemaphores
            .computeIfAbsent(
                modelId,
                k -> new Semaphore(
                    maxModelConcurrency, true)
            );
        return sem.tryAcquire();
    }

    /**
     * Release a model call slot.
     */
    public void releaseModel(String modelId) {
        Semaphore sem = modelSemaphores.get(modelId);
        if (sem != null) {
            sem.release();
        }
    }

    /**
     * Try to acquire a tool call slot.
     *
     * @param toolId the tool provider ID
     * @return true if acquired
     */
    public boolean tryAcquireTool(String toolId) {
        Semaphore sem = toolSemaphores
            .computeIfAbsent(
                toolId,
                k -> new Semaphore(
                    maxToolConcurrency, true)
            );
        return sem.tryAcquire();
    }

    /**
     * Release a tool call slot.
     */
    public void releaseTool(String toolId) {
        Semaphore sem = toolSemaphores.get(toolId);
        if (sem != null) {
            sem.release();
        }
    }

    public int maxExecutions() {
        return maxExecutions;
    }

    public int maxModelConcurrency() {
        return maxModelConcurrency;
    }

    public int maxToolConcurrency() {
        return maxToolConcurrency;
    }

    public long memoryLimitBytes() {
        return memoryLimitBytes;
    }

    public double cpuLimit() {
        return cpuLimit;
    }
}

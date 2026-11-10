package io.github.regalpine.agentforge.distributed.health;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * WorkerHealth — AF-RUNTIME-01 §12, §319.
 *
 * Health status of a worker, including:
 * - Status (HEALTHY / DEGRADED / UNHEALTHY)
 * - Active executions count
 * - CPU / memory metrics
 * - Queue depth
 * - Last check timestamp
 *
 * Used by scheduler to determine worker
 * availability for new executions.
 */
public record WorkerHealth(

    String workerId,

    HealthStatus status,

    int activeExecutions,

    double cpuUsage,

    long memoryUsedBytes,

    long memoryMaxBytes,

    int queueDepth,

    Instant timestamp,

    Map<String, Object> metadata
) {

    public WorkerHealth {
        Objects.requireNonNull(workerId, "workerId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(timestamp, "timestamp");

        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create a healthy status snapshot.
     */
    public static WorkerHealth healthy(
        String workerId,
        int activeExecutions,
        double cpuUsage,
        long memoryUsedBytes,
        long memoryMaxBytes
    ) {
        return new WorkerHealth(
            workerId,
            HealthStatus.HEALTHY,
            activeExecutions,
            cpuUsage,
            memoryUsedBytes,
            memoryMaxBytes,
            0,
            Instant.now(),
            Map.of()
        );
    }

    /**
     * Check if this worker is available for new work.
     */
    public boolean isAvailable() {
        return status == HealthStatus.HEALTHY
            || status == HealthStatus.DEGRADED;
    }

    /**
     * Memory usage ratio (0.0 - 1.0).
     */
    public double memoryUsageRatio() {
        if (memoryMaxBytes <= 0) return 0.0;
        return (double) memoryUsedBytes / memoryMaxBytes;
    }

    /**
     * Health status levels.
     */
    public enum HealthStatus {
        HEALTHY,
        DEGRADED,
        UNHEALTHY,
        UNKNOWN
    }
}

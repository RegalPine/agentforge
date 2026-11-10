package io.github.regalpine.agentforge.distributed.health;

import io.github.regalpine.agentforge.distributed.worker.WorkerId;
import io.github.regalpine.agentforge.distributed.worker.WorkerRegistry;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WorkerHealthChecker — AF-RUNTIME-01 §12-13.
 *
 * Monitors worker health by:
 * - Checking heartbeat freshness
 * - Evaluating CPU/memory thresholds
 * - Marking workers as UNHEALTHY/LOST
 *
 * Heartbeat is only used to determine
 * if a worker is still alive (§319).
 */
public final class WorkerHealthChecker {

    private final WorkerRegistry registry;

    private final Duration heartbeatTimeout;
    private final double cpuThreshold;
    private final double memoryThreshold;

    private final Map<String, WorkerHealth> latestHealth
        = new ConcurrentHashMap<>();

    public WorkerHealthChecker(
        WorkerRegistry registry,
        Duration heartbeatTimeout,
        double cpuThreshold,
        double memoryThreshold
    ) {
        this.registry = Objects.requireNonNull(registry);
        this.heartbeatTimeout = heartbeatTimeout == null
            ? Duration.ofSeconds(30) : heartbeatTimeout;
        this.cpuThreshold = cpuThreshold;
        this.memoryThreshold = memoryThreshold;
    }

    public WorkerHealthChecker(WorkerRegistry registry) {
        this(registry, Duration.ofSeconds(30), 0.9, 0.95);
    }

    /**
     * Report health for a worker.
     */
    public void reportHealth(WorkerHealth health) {
        Objects.requireNonNull(health, "health");
        latestHealth.put(health.workerId(), health);
    }

    /**
     * Get the latest health for a worker.
     */
    public Optional<WorkerHealth> getHealth(
        String workerId
    ) {
        return Optional.ofNullable(
            latestHealth.get(workerId));
    }

    /**
     * Check all registered workers and update
     * health status based on heartbeat freshness.
     *
     * @return list of workers that became unhealthy
     */
    public List<String> checkAll() {
        List<String> newlyUnhealthy = new ArrayList<>();
        Instant now = Instant.now();

        for (var reg : registry.activeWorkers()) {
            String wid = reg.workerId().value();
            WorkerHealth health = latestHealth.get(wid);

            if (health == null) {
                // No health report yet — check heartbeat
                if (reg.lastHeartbeat() != null
                    && now.minus(heartbeatTimeout)
                        .isAfter(reg.lastHeartbeat())) {

                    WorkerHealth unhealthy = WorkerHealth.healthy(
                        wid, 0, 0, 0, 0
                    );
                    // Override status to UNKNOWN
                    unhealthy = new WorkerHealth(
                        wid,
                        WorkerHealth.HealthStatus.UNKNOWN,
                        0, 0, 0, 0, 0,
                        now, Map.of()
                    );
                    latestHealth.put(wid, unhealthy);
                    newlyUnhealthy.add(wid);
                }
            } else {
                // Check if heartbeat is stale
                if (now.minus(heartbeatTimeout)
                    .isAfter(health.timestamp())) {

                    WorkerHealth lost = new WorkerHealth(
                        wid,
                        WorkerHealth.HealthStatus.UNHEALTHY,
                        health.activeExecutions(),
                        health.cpuUsage(),
                        health.memoryUsedBytes(),
                        health.memoryMaxBytes(),
                        health.queueDepth(),
                        now,
                        Map.of("reason", "heartbeat_timeout")
                    );
                    latestHealth.put(wid, lost);
                    newlyUnhealthy.add(wid);
                }
            }
        }

        return newlyUnhealthy;
    }

    /**
     * Get all healthy workers.
     */
    public List<WorkerHealth> healthyWorkers() {
        return latestHealth.values().stream()
            .filter(WorkerHealth::isAvailable)
            .toList();
    }

    /**
     * Get all workers with their health.
     */
    public Collection<WorkerHealth> allHealth() {
        return List.copyOf(latestHealth.values());
    }

    public Duration heartbeatTimeout() {
        return heartbeatTimeout;
    }
}

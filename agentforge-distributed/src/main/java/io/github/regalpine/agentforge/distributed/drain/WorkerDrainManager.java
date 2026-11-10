package io.github.regalpine.agentforge.distributed.drain;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WorkerDrainManager — AF-RUNTIME-01 §59.
 *
 * Manages graceful worker drain:
 * 1. Worker enters DRAINING state
 * 2. No new executions accepted
 * 3. Existing executions complete
 * 4. Worker transitions to STOPPED
 *
 * For executions that cannot complete
 * within the drain timeout:
 * - Checkpoint
 * - Recovery on another worker
 */
public final class WorkerDrainManager {

    private final Map<String, DrainState> drains
        = new ConcurrentHashMap<>();

    /**
     * Initiate drain for a worker.
     *
     * @param workerId the worker to drain
     * @param activeCount number of active executions
     * @return the drain state
     */
    public DrainState initiateDrain(
        String workerId,
        int activeCount
    ) {
        DrainState state = new DrainState(
            workerId,
            Instant.now(),
            activeCount
        );
        drains.put(workerId, state);
        return state;
    }

    /**
     * Check if a worker is draining.
     */
    public boolean isDraining(String workerId) {
        DrainState state = drains.get(workerId);
        return state != null
            && !state.isCompleted();
    }

    /**
     * Record an execution completion during drain.
     *
     * @return true if drain is now complete
     */
    public boolean recordCompletion(String workerId) {
        DrainState state = drains.get(workerId);
        if (state == null) return false;

        int remaining = state.remainingAtomic()
            .decrementAndGet();

        return remaining <= 0;
    }

    /**
     * Get the drain state for a worker.
     */
    public Optional<DrainState> getDrainState(
        String workerId
    ) {
        return Optional.ofNullable(
            drains.get(workerId));
    }

    /**
     * Complete the drain (worker stopped).
     */
    public void completeDrain(String workerId) {
        DrainState state = drains.get(workerId);
        if (state != null) {
            state.markCompleted();
        }
    }

    /**
     * Get all draining workers.
     */
    public Collection<DrainState> allDraining() {
        return drains.values().stream()
            .filter(s -> !s.isCompleted())
            .toList();
    }

    /**
     * Drain state for a single worker.
     */
    public static final class DrainState {

        private final String workerId;
        private final Instant drainStartedAt;
        private final AtomicInteger remaining;
        private volatile boolean completed;

        public DrainState(
            String workerId,
            Instant drainStartedAt,
            int initialActive
        ) {
            this.workerId = workerId;
            this.drainStartedAt = drainStartedAt;
            this.remaining = new AtomicInteger(
                initialActive);
            this.completed = false;
        }

        public String workerId() {
            return workerId;
        }

        public Instant drainStartedAt() {
            return drainStartedAt;
        }

        public int remaining() {
            return remaining.get();
        }

        public AtomicInteger remainingAtomic() {
            return remaining;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void markCompleted() {
            this.completed = true;
        }
    }
}

package io.github.regalpine.agentforge.memory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Memory metrics.
 * AE-08 §66: Memory Observability metrics.
 *
 * <p>Simple counters for memory operations.
 * In production these would be connected
 * to a metrics system (Micrometer, etc.).
 */
public final class MemoryMetrics {

    private final AtomicLong readTotal =
        new AtomicLong();
    private final AtomicLong writeTotal =
        new AtomicLong();
    private final AtomicLong hitTotal =
        new AtomicLong();
    private final AtomicLong missTotal =
        new AtomicLong();
    private final AtomicLong candidateTotal =
        new AtomicLong();
    private final AtomicLong promotedTotal =
        new AtomicLong();
    private final AtomicLong rejectedTotal =
        new AtomicLong();
    private final AtomicLong expiredTotal =
        new AtomicLong();
    private final AtomicLong conflictTotal =
        new AtomicLong();
    private final AtomicLong policyDeniedTotal =
        new AtomicLong();
    private final AtomicLong contextBuildTotal =
        new AtomicLong();

    public void recordRead() {
        readTotal.incrementAndGet();
    }

    public void recordWrite() {
        writeTotal.incrementAndGet();
    }

    public void recordHit() {
        hitTotal.incrementAndGet();
    }

    public void recordMiss() {
        missTotal.incrementAndGet();
    }

    public void recordCandidate() {
        candidateTotal.incrementAndGet();
    }

    public void recordPromoted() {
        promotedTotal.incrementAndGet();
    }

    public void recordRejected() {
        rejectedTotal.incrementAndGet();
    }

    public void recordExpired() {
        expiredTotal.incrementAndGet();
    }

    public void recordConflict() {
        conflictTotal.incrementAndGet();
    }

    public void recordPolicyDenied() {
        policyDeniedTotal.incrementAndGet();
    }

    public void recordContextBuild() {
        contextBuildTotal.incrementAndGet();
    }

    public long readTotal() {
        return readTotal.get();
    }

    public long writeTotal() {
        return writeTotal.get();
    }

    public long hitTotal() {
        return hitTotal.get();
    }

    public long missTotal() {
        return missTotal.get();
    }

    public long candidateTotal() {
        return candidateTotal.get();
    }

    public long promotedTotal() {
        return promotedTotal.get();
    }

    public long rejectedTotal() {
        return rejectedTotal.get();
    }

    public long expiredTotal() {
        return expiredTotal.get();
    }

    public long conflictTotal() {
        return conflictTotal.get();
    }

    public long policyDeniedTotal() {
        return policyDeniedTotal.get();
    }

    public long contextBuildTotal() {
        return contextBuildTotal.get();
    }

    /**
     * Reset all counters.
     */
    public void reset() {
        readTotal.set(0);
        writeTotal.set(0);
        hitTotal.set(0);
        missTotal.set(0);
        candidateTotal.set(0);
        promotedTotal.set(0);
        rejectedTotal.set(0);
        expiredTotal.set(0);
        conflictTotal.set(0);
        policyDeniedTotal.set(0);
        contextBuildTotal.set(0);
    }
}

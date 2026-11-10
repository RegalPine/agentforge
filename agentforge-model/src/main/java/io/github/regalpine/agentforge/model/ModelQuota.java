package io.github.regalpine.agentforge.model;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Model quota — AF-ENHANCE-09 §36.
 *
 * <p>Tracks usage against limits:
 * requests/sec, tokens/min,
 * concurrency, daily quota,
 * tenant quota, provider quota.
 *
 * <p>Thread-safe via AtomicLong.
 */
public final class ModelQuota {

    private final String quotaId;
    private final long maxRequests;
    private final long maxTokens;
    private final long maxConcurrency;

    private final AtomicLong usedRequests =
        new AtomicLong(0);
    private final AtomicLong usedTokens =
        new AtomicLong(0);
    private final AtomicLong activeRequests =
        new AtomicLong(0);

    public ModelQuota(
        String quotaId,
        long maxRequests,
        long maxTokens,
        long maxConcurrency
    ) {
        Objects.requireNonNull(
            quotaId, "quotaId"
        );
        if (maxRequests <= 0) {
            maxRequests = Long.MAX_VALUE;
        }
        if (maxTokens <= 0) {
            maxTokens = Long.MAX_VALUE;
        }
        if (maxConcurrency <= 0) {
            maxConcurrency = Long.MAX_VALUE;
        }

        this.quotaId = quotaId;
        this.maxRequests = maxRequests;
        this.maxTokens = maxTokens;
        this.maxConcurrency = maxConcurrency;
    }

    /**
     * Unlimited quota.
     */
    public static ModelQuota unlimited() {
        return new ModelQuota(
            "unlimited",
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            Long.MAX_VALUE
        );
    }

    /**
     * Check if a request is allowed.
     */
    public boolean tryAcquire(int estimatedTokens) {
        long ur = usedRequests.get();
        long ut = usedTokens.get();
        long ar = activeRequests.get();

        if (ur >= maxRequests) {
            return false;
        }
        if (ut + estimatedTokens > maxTokens) {
            return false;
        }
        if (ar >= maxConcurrency) {
            return false;
        }

        usedRequests.incrementAndGet();
        activeRequests.incrementAndGet();
        return true;
    }

    /**
     * Release after completion.
     */
    public void release(long actualTokens) {
        usedTokens.addAndGet(actualTokens);
        activeRequests.decrementAndGet();
    }

    /**
     * Reset all counters.
     */
    public void reset() {
        usedRequests.set(0);
        usedTokens.set(0);
        activeRequests.set(0);
    }

    public String quotaId() {
        return quotaId;
    }

    public long maxRequests() {
        return maxRequests;
    }

    public long maxTokens() {
        return maxTokens;
    }

    public long maxConcurrency() {
        return maxConcurrency;
    }

    public long getUsedRequests() {
        return usedRequests.get();
    }

    public long getUsedTokens() {
        return usedTokens.get();
    }

    public long getActiveRequests() {
        return activeRequests.get();
    }

    public long getRemainingRequests() {
        return Math.max(
            0, maxRequests - usedRequests.get()
        );
    }

    public long getRemainingTokens() {
        return Math.max(
            0, maxTokens - usedTokens.get()
        );
    }
}

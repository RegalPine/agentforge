package io.github.regalpine.agentforge.distributed.quota;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TenantQuotaManager — AF-RUNTIME-01 §25-26, §29.
 *
 * Manages per-tenant quotas and tracks usage.
 * Supports:
 * - Concurrent execution limits
 * - Queued execution limits
 * - Daily model/tool call limits
 * - Daily token/cost limits
 *
 * Priority cannot bypass quota (§28).
 * Fairness: no single tenant can exhaust all workers (§29).
 */
public final class TenantQuotaManager {

    private final Map<String, TenantQuota> quotas
        = new ConcurrentHashMap<>();

    private final Map<String, TenantUsage> usages
        = new ConcurrentHashMap<>();

    /**
     * Register or update a tenant quota.
     */
    public void setQuota(TenantQuota quota) {
        Objects.requireNonNull(quota, "quota");
        quotas.put(quota.tenantId(), quota);
    }

    /**
     * Get the effective quota for a tenant.
     * Returns defaults if not explicitly configured.
     */
    public TenantQuota getQuota(String tenantId) {
        return quotas.getOrDefault(
            tenantId,
            TenantQuota.defaults(tenantId)
        );
    }

    /**
     * Check if a tenant can admit a new concurrent execution.
     *
     * @throws QuotaExceededException if limit reached
     */
    public void admitExecution(String tenantId) {
        TenantQuota quota = getQuota(tenantId);
        TenantUsage usage = getOrCreateUsage(tenantId);

        int current = usage.concurrentExecutions()
            .incrementAndGet();

        if (current > quota.maxConcurrentExecutions()) {
            usage.concurrentExecutions()
                .decrementAndGet();
            throw new QuotaExceededException(
                tenantId,
                "maxConcurrentExecutions="
                    + quota.maxConcurrentExecutions()
            );
        }
    }

    /**
     * Release a concurrent execution slot.
     */
    public void releaseExecution(String tenantId) {
        TenantUsage usage = usages.get(tenantId);
        if (usage != null) {
            usage.concurrentExecutions()
                .decrementAndGet();
        }
    }

    /**
     * Check if a tenant can queue a new execution.
     *
     * @throws QuotaExceededException if limit reached
     */
    public void admitQueued(String tenantId) {
        TenantQuota quota = getQuota(tenantId);
        TenantUsage usage = getOrCreateUsage(tenantId);

        int current = usage.queuedExecutions()
            .incrementAndGet();

        if (current > quota.maxQueuedExecutions()) {
            usage.queuedExecutions()
                .decrementAndGet();
            throw new QuotaExceededException(
                tenantId,
                "maxQueuedExecutions="
                    + quota.maxQueuedExecutions()
            );
        }
    }

    /**
     * Release a queued execution slot.
     */
    public void releaseQueued(String tenantId) {
        TenantUsage usage = usages.get(tenantId);
        if (usage != null) {
            usage.queuedExecutions()
                .decrementAndGet();
        }
    }

    /**
     * Record model call usage.
     *
     * @throws QuotaExceededException if daily limit reached
     */
    public void recordModelCall(
        String tenantId,
        int count
    ) {
        TenantQuota quota = getQuota(tenantId);
        TenantUsage usage = getOrCreateUsage(tenantId);

        long total = usage.modelCalls()
            .addAndGet(count);

        if (total > quota.maxModelCallsPerDay()) {
            usage.modelCalls()
                .addAndGet(-count);
            throw new QuotaExceededException(
                tenantId,
                "maxModelCallsPerDay="
                    + quota.maxModelCallsPerDay()
            );
        }
    }

    /**
     * Record token usage.
     *
     * @throws QuotaExceededException if daily limit reached
     */
    public void recordTokens(
        String tenantId,
        long tokens
    ) {
        TenantQuota quota = getQuota(tenantId);
        TenantUsage usage = getOrCreateUsage(tenantId);

        long total = usage.tokens()
            .addAndGet(tokens);

        if (total > quota.maxTokensPerDay()) {
            usage.tokens()
                .addAndGet(-tokens);
            throw new QuotaExceededException(
                tenantId,
                "maxTokensPerDay="
                    + quota.maxTokensPerDay()
            );
        }
    }

    /**
     * Get current usage for a tenant.
     */
    public TenantUsage getUsage(String tenantId) {
        return getOrCreateUsage(tenantId);
    }

    /**
     * Reset daily counters (call at day boundary).
     */
    public void resetDailyCounters(String tenantId) {
        usages.remove(tenantId);
    }

    private TenantUsage getOrCreateUsage(String tenantId) {
        return usages.computeIfAbsent(
            tenantId,
            k -> new TenantUsage()
        );
    }

    /**
     * Mutable usage tracker per tenant.
     */
    public static final class TenantUsage {

        private final AtomicInteger concurrentExecutions
            = new AtomicInteger(0);
        private final AtomicInteger queuedExecutions
            = new AtomicInteger(0);
        private final AtomicLong modelCalls
            = new AtomicLong(0);
        private final AtomicLong toolCalls
            = new AtomicLong(0);
        private final AtomicLong tokens
            = new AtomicLong(0);

        public AtomicInteger concurrentExecutions() {
            return concurrentExecutions;
        }

        public AtomicInteger queuedExecutions() {
            return queuedExecutions;
        }

        public AtomicLong modelCalls() {
            return modelCalls;
        }

        public AtomicLong toolCalls() {
            return toolCalls;
        }

        public AtomicLong tokens() {
            return tokens;
        }

        public int currentConcurrent() {
            return concurrentExecutions.get();
        }

        public int currentQueued() {
            return queuedExecutions.get();
        }

        public long dailyModelCalls() {
            return modelCalls.get();
        }

        public long dailyTokens() {
            return tokens.get();
        }
    }
}

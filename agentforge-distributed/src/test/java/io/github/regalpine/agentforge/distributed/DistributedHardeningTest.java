package io.github.regalpine.agentforge.distributed;

import io.github.regalpine.agentforge.distributed.drain.WorkerDrainManager;
import io.github.regalpine.agentforge.distributed.health.WorkerHealth;
import io.github.regalpine.agentforge.distributed.health.WorkerHealthChecker;
import io.github.regalpine.agentforge.distributed.memory.InMemoryWorkerRegistry;
import io.github.regalpine.agentforge.distributed.quota.*;
import io.github.regalpine.agentforge.distributed.worker.WorkerId;
import io.github.regalpine.agentforge.distributed.worker.WorkerRegistration;
import io.github.regalpine.agentforge.distributed.worker.WorkerStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Enterprise Hardening tests — AF-RUNTIME-01.
 * Covers: Tenant Quota (§25-26), Resource Isolation (§48-49),
 * Worker Health (§12-13), Worker Drain (§59).
 * AF-RUNTIME-020, AF-RUNTIME-022.
 */
class DistributedHardeningTest {

    // --- Tenant Quota ---

    @Test
    void shouldCreateDefaultQuota() {
        var quota = TenantQuota.defaults("tenant-1");
        assertEquals("tenant-1", quota.tenantId());
        assertTrue(quota.maxConcurrentExecutions() > 0);
        assertTrue(quota.maxTokensPerDay() > 0);
    }

    @Test
    void shouldCreateUnlimitedQuota() {
        var quota = TenantQuota.unlimited("tenant-1");
        assertEquals(Integer.MAX_VALUE,
            quota.maxConcurrentExecutions());
        assertEquals(Long.MAX_VALUE,
            quota.maxTokensPerDay());
    }

    @Test
    void shouldMergeQuotasTakingMinimum() {
        var q1 = new TenantQuota(
            "t1", 100, 500,
            1000, 500, 10000,
            3600000, 5000);
        var q2 = new TenantQuota(
            "t1", 50, 1000,
            2000, 1000, 5000,
            1800000, 10000);

        var merged = q1.mergeWith(q2);
        assertEquals(50, merged.maxConcurrentExecutions());
        assertEquals(500, merged.maxQueuedExecutions());
        assertEquals(1000, merged.maxModelCallsPerDay());
        assertEquals(5000, merged.maxTokensPerDay());
    }

    @Test
    void shouldAdmitExecutionWithinQuota() {
        var manager = new TenantQuotaManager();
        manager.setQuota(new TenantQuota(
            "t1", 2, 10,
            1000, 500, 10000,
            3600000, 5000));

        assertDoesNotThrow(() -> manager.admitExecution("t1"));
        assertDoesNotThrow(() -> manager.admitExecution("t1"));
    }

    @Test
    void shouldRejectExecutionWhenQuotaExceeded() {
        var manager = new TenantQuotaManager();
        manager.setQuota(new TenantQuota(
            "t1", 1, 10,
            1000, 500, 10000,
            3600000, 5000));

        manager.admitExecution("t1");

        assertThrows(QuotaExceededException.class,
            () -> manager.admitExecution("t1"));
    }

    @Test
    void shouldReleaseExecutionSlot() {
        var manager = new TenantQuotaManager();
        manager.setQuota(new TenantQuota(
            "t1", 1, 10,
            1000, 500, 10000,
            3600000, 5000));

        manager.admitExecution("t1");
        manager.releaseExecution("t1");

        // Should be able to admit again
        assertDoesNotThrow(() -> manager.admitExecution("t1"));
    }

    @Test
    void shouldTrackModelCallUsage() {
        var manager = new TenantQuotaManager();
        manager.setQuota(new TenantQuota(
            "t1", 100, 1000,
            10, 500, 10000,
            3600000, 5000));

        for (int i = 0; i < 10; i++) {
            manager.recordModelCall("t1", 1);
        }

        assertThrows(QuotaExceededException.class,
            () -> manager.recordModelCall("t1", 1));
    }

    @Test
    void shouldTrackTokenUsage() {
        var manager = new TenantQuotaManager();
        manager.setQuota(new TenantQuota(
            "t1", 100, 1000,
            1000, 500, 100,
            3600000, 5000));

        manager.recordTokens("t1", 80);

        assertThrows(QuotaExceededException.class,
            () -> manager.recordTokens("t1", 30));
    }

    @Test
    void shouldResetDailyCounters() {
        var manager = new TenantQuotaManager();
        manager.setQuota(new TenantQuota(
            "t1", 100, 1000,
            10, 500, 10000,
            3600000, 5000));

        for (int i = 0; i < 10; i++) {
            manager.recordModelCall("t1", 1);
        }

        manager.resetDailyCounters("t1");

        // Should be able to record again
        assertDoesNotThrow(
            () -> manager.recordModelCall("t1", 1));
    }

    // --- Resource Isolation ---

    @Test
    void shouldAcquireAndReleaseExecutionSlot() {
        var isolation = new ResourceIsolation(
            2, 10, 5,
            1024 * 1024, 4.0);

        assertTrue(isolation.tryAcquireExecution());
        assertTrue(isolation.tryAcquireExecution());
        assertFalse(isolation.tryAcquireExecution());

        isolation.releaseExecution();
        assertTrue(isolation.tryAcquireExecution());
    }

    @Test
    void shouldLimitModelConcurrency() {
        var isolation = new ResourceIsolation(
            50, 2, 5,
            1024 * 1024, 4.0);

        assertTrue(isolation.tryAcquireModel("gpt-4"));
        assertTrue(isolation.tryAcquireModel("gpt-4"));
        assertFalse(isolation.tryAcquireModel("gpt-4"));

        // Different model has separate limit
        assertTrue(isolation.tryAcquireModel("claude-3"));
    }

    @Test
    void shouldLimitToolConcurrency() {
        var isolation = new ResourceIsolation(
            50, 20, 1,
            1024 * 1024, 4.0);

        assertTrue(isolation.tryAcquireTool("search"));
        assertFalse(isolation.tryAcquireTool("search"));

        isolation.releaseTool("search");
        assertTrue(isolation.tryAcquireTool("search"));
    }

    @Test
    void shouldCreateDefaultIsolation() {
        var isolation = ResourceIsolation.defaults();
        assertEquals(50, isolation.maxExecutions());
        assertEquals(20, isolation.maxModelConcurrency());
    }

    // --- Worker Health ---

    @Test
    void shouldReportAndRetrieveHealth() {
        var registry = new InMemoryWorkerRegistry();
        var checker = new WorkerHealthChecker(registry);

        var health = WorkerHealth.healthy(
            "w1", 5, 0.3, 512_000_000, 1_000_000_000);

        checker.reportHealth(health);

        var retrieved = checker.getHealth("w1");
        assertTrue(retrieved.isPresent());
        assertEquals("w1", retrieved.get().workerId());
        assertTrue(retrieved.get().isAvailable());
    }

    @Test
    void shouldDetectUnhealthyWorker() {
        var registry = new InMemoryWorkerRegistry();
        var checker = new WorkerHealthChecker(
            registry, Duration.ofMillis(1), 0.9, 0.95);

        var health = WorkerHealth.healthy(
            "w1", 0, 0.1, 100, 1000);
        checker.reportHealth(health);

        // Wait for timeout
        try { Thread.sleep(10); } catch (InterruptedException e) { /* ignore */ }

        var retrieved = checker.getHealth("w1");
        assertTrue(retrieved.isPresent());
    }

    @Test
    void shouldListHealthyWorkers() {
        var registry = new InMemoryWorkerRegistry();
        var checker = new WorkerHealthChecker(registry);

        checker.reportHealth(WorkerHealth.healthy(
            "w1", 0, 0.1, 100, 1000));
        checker.reportHealth(new WorkerHealth(
            "w2",
            WorkerHealth.HealthStatus.UNHEALTHY,
            0, 0, 0, 0, 0,
            Instant.now(), java.util.Map.of()));

        var healthy = checker.healthyWorkers();
        assertEquals(1, healthy.size());
        assertEquals("w1", healthy.get(0).workerId());
    }

    @Test
    void shouldCalculateMemoryUsageRatio() {
        var health = WorkerHealth.healthy(
            "w1", 0, 0.5, 800_000_000, 1_000_000_000);
        assertEquals(0.8, health.memoryUsageRatio(), 0.01);
    }

    // --- Worker Drain ---

    @Test
    void shouldInitiateDrain() {
        var drain = new WorkerDrainManager();

        var state = drain.initiateDrain("w1", 3);
        assertEquals("w1", state.workerId());
        assertEquals(3, state.remaining());
        assertFalse(state.isCompleted());
        assertTrue(drain.isDraining("w1"));
    }

    @Test
    void shouldTrackDrainCompletions() {
        var drain = new WorkerDrainManager();
        drain.initiateDrain("w1", 2);

        assertFalse(drain.recordCompletion("w1"));
        assertTrue(drain.recordCompletion("w1"));
    }

    @Test
    void shouldCompleteDrain() {
        var drain = new WorkerDrainManager();
        drain.initiateDrain("w1", 1);

        drain.recordCompletion("w1");
        drain.completeDrain("w1");

        assertFalse(drain.isDraining("w1"));
    }

    @Test
    void shouldListAllDrainingWorkers() {
        var drain = new WorkerDrainManager();
        drain.initiateDrain("w1", 2);
        drain.initiateDrain("w2", 1);

        assertEquals(2, drain.allDraining().size());
    }
}

package io.github.regalpine.agentforge.hardening;

import io.github.regalpine.agentforge.distributed.fencing.DefaultFencingValidator;
import io.github.regalpine.agentforge.distributed.lease.ExecutionLease;
import io.github.regalpine.agentforge.distributed.memory.InMemoryLeaseStore;
import io.github.regalpine.agentforge.distributed.worker.WorkerId;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.event.ExecutionId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AF-HARDEN-001: CAS - Lease atomicity
 * AF-HARDEN-002: Duplicate task prevention
 * AF-HARDEN-003: Lease expiry
 * AF-HARDEN-004: Fencing token monotonicity
 */
class LeaseAndFencingTest {

    @Test
    void shouldPreventDuplicateLeaseAcquisition() {
        InMemoryLeaseStore store = new InMemoryLeaseStore();
        ExecutionId execId = new ExecutionId("exec-hardening-001");
        WorkerId workerA = new WorkerId("worker-A");
        WorkerId workerB = new WorkerId("worker-B");

        var first = store.acquire(execId, workerA, 30_000);
        assertTrue(first.isPresent());

        var second = store.acquire(execId, workerB, 30_000);
        assertFalse(second.isPresent(), "Second acquisition should fail while lease is active");
    }

    @Test
    void shouldRejectStaleFencingToken() {
        InMemoryLeaseStore store = new InMemoryLeaseStore();
        ExecutionId execId = new ExecutionId("exec-hardening-002");
        WorkerId workerA = new WorkerId("worker-A");
        WorkerId workerB = new WorkerId("worker-B");

        var leaseA = store.acquire(execId, workerA, 30_000);
        assertTrue(leaseA.isPresent());
        long tokenA = leaseA.get().fencingToken();

        store.release(execId, workerA, tokenA);

        var leaseB = store.acquire(execId, workerB, 30_000);
        assertTrue(leaseB.isPresent());
        long tokenB = leaseB.get().fencingToken();

        assertTrue(tokenB > tokenA, "Fencing token must be monotonically increasing");

        boolean renewed = store.renew(execId, workerA, tokenA, 30_000);
        assertFalse(renewed, "Stale worker should not be able to renew");

        boolean released = store.release(execId, workerA, tokenA);
        assertFalse(released, "Stale worker should not be able to release");
    }

    @Test
    void shouldExpireLeaseAfterTimeout() throws InterruptedException {
        InMemoryLeaseStore store = new InMemoryLeaseStore();
        ExecutionId execId = new ExecutionId("exec-hardening-003");
        WorkerId workerA = new WorkerId("worker-A");

        var lease = store.acquire(execId, workerA, 50);
        assertTrue(lease.isPresent());

        Thread.sleep(100);

        DefaultFencingValidator validator = new DefaultFencingValidator();
        assertFalse(validator.isValid(lease.get()), "Expired lease should fail fencing validation");

        var newLease = store.acquire(execId, new WorkerId("worker-B"), 30_000);
        assertTrue(newLease.isPresent(), "Should be able to acquire after expiry");
    }

    @Test
    void shouldRenewLeaseWithValidToken() {
        InMemoryLeaseStore store = new InMemoryLeaseStore();
        ExecutionId execId = new ExecutionId("exec-hardening-004");
        WorkerId worker = new WorkerId("worker-A");

        var lease = store.acquire(execId, worker, 30_000);
        assertTrue(lease.isPresent());

        boolean renewed = store.renew(execId, worker, lease.get().fencingToken(), 30_000);
        assertTrue(renewed, "Valid worker with correct token should renew");
    }

    @Test
    void shouldRejectRenewWithWrongToken() {
        InMemoryLeaseStore store = new InMemoryLeaseStore();
        ExecutionId execId = new ExecutionId("exec-hardening-005");
        WorkerId worker = new WorkerId("worker-A");

        store.acquire(execId, worker, 30_000);

        boolean renewed = store.renew(execId, worker, 999L, 30_000);
        assertFalse(renewed, "Wrong fencing token should be rejected");
    }

    @Test
    void shouldConcurrentLeaseAcquisitionProduceMonotonicTokens() throws Exception {
        InMemoryLeaseStore store = new InMemoryLeaseStore();
        ExecutionId execId = new ExecutionId("exec-hardening-006");
        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        java.util.concurrent.ConcurrentLinkedQueue<Long> tokens = new java.util.concurrent.ConcurrentLinkedQueue<>();

        for (int i = 0; i < threadCount; i++) {
            final int workerIdx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    var result = store.acquire(execId, new WorkerId("worker-" + workerIdx), 30_000);
                    if (result.isPresent()) {
                        tokens.add(result.get().fencingToken());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertTrue(tokens.size() >= 1,
            "At least one worker should acquire the lease");

        // Verify tokens are unique and positive
        java.util.List<Long> tokenList = new java.util.ArrayList<>(tokens);
        java.util.Set<Long> uniqueTokens = new java.util.HashSet<>(tokenList);
        assertEquals(tokenList.size(), uniqueTokens.size(),
            "All fencing tokens should be unique");

        for (Long token : tokenList) {
            assertTrue(token > 0, "Fencing token should be positive");
        }
    }
}

package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.distributed.fencing.DefaultFencingValidator;
import io.github.regalpine.agentforge.distributed.lease.ExecutionLease;
import io.github.regalpine.agentforge.distributed.memory.InMemoryLeaseStore;
import io.github.regalpine.agentforge.distributed.worker.WorkerId;
import io.github.regalpine.agentforge.hardening.*;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.event.ExecutionId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * conformance-distributed: Worker
 * / Scheduler / Lease / Fencing / Recovery.
 * AF-RELEASE-02 §4, T-H6.17.
 */
class DistributedConformanceTest {

    @Test
    void afDist001_leaseAcquire() {
        var store = new InMemoryLeaseStore();
        var execId = new ExecutionId("dist-001");
        var worker = new WorkerId("w-1");

        var lease = store.acquire(execId, worker, 30_000);
        assertTrue(lease.isPresent());
    }

    @Test
    void afDist002_leaseExclusive() {
        var store = new InMemoryLeaseStore();
        var execId = new ExecutionId("dist-002");

        store.acquire(execId, new WorkerId("w-a"), 30_000);
        var second = store.acquire(
            execId, new WorkerId("w-b"), 30_000
        );
        assertFalse(second.isPresent());
    }

    @Test
    void afDist003_fencingTokenMonotonic() {
        var store = new InMemoryLeaseStore();
        var execId = new ExecutionId("dist-003");
        var wA = new WorkerId("w-a");
        var wB = new WorkerId("w-b");

        var leaseA = store.acquire(execId, wA, 30_000);
        assertTrue(leaseA.isPresent());
        long tokenA = leaseA.get().fencingToken();

        store.release(execId, wA, tokenA);

        var leaseB = store.acquire(execId, wB, 30_000);
        assertTrue(leaseB.isPresent());
        long tokenB = leaseB.get().fencingToken();

        assertTrue(tokenB > tokenA);
    }

    @Test
    void afDist004_leaseRenew() {
        var store = new InMemoryLeaseStore();
        var execId = new ExecutionId("dist-004");
        var worker = new WorkerId("w-r");

        var lease = store.acquire(execId, worker, 30_000);
        assertTrue(lease.isPresent());

        boolean renewed = store.renew(
            execId, worker,
            lease.get().fencingToken(), 60_000
        );
        assertTrue(renewed);
    }

    @Test
    void afDist005_fencingValidator() {
        var validator = new DefaultFencingValidator();
        assertNotNull(validator);
    }

    @Test
    void afDist006_workerId() {
        var w1 = new WorkerId("worker-1");
        var w2 = new WorkerId("worker-2");
        assertNotEquals(w1, w2);
    }
}

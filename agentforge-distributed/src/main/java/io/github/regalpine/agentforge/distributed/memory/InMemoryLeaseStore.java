package io.github.regalpine.agentforge.distributed.memory;

import io.github.regalpine.agentforge.distributed.lease.ExecutionLease;
import io.github.regalpine.agentforge.distributed.lease.LeaseStore;
import io.github.regalpine.agentforge.distributed.worker.WorkerId;
import io.github.regalpine.agentforge.event.ExecutionId;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class InMemoryLeaseStore
    implements LeaseStore {

    private final Map<
        ExecutionId,
        ExecutionLease
    > leases = new ConcurrentHashMap<>();

    private final AtomicLong tokenCounter =
        new AtomicLong(0);

    @Override
    public Optional<ExecutionLease> acquire(
        ExecutionId executionId,
        WorkerId workerId,
        long durationMillis
    ) {
        // G2-4: Use compute() for atomic
        // check-and-set to prevent TOCTOU race
        ExecutionLease[] acquired = new ExecutionLease[1];

        leases.compute(executionId, (key, existing) -> {
            if (existing != null
                && existing.expiresAt()
                    .isAfter(Instant.now())
            ) {
                // Lease still valid — reject
                return existing;
            }

            long token =
                tokenCounter.incrementAndGet();

            Instant now =
                Instant.now();

            ExecutionLease lease =
                new ExecutionLease(
                    executionId,
                    workerId,
                    token,
                    now,
                    now.plusMillis(durationMillis)
                );

            acquired[0] = lease;
            return lease;
        });

        return Optional.ofNullable(acquired[0]);
    }

    @Override
    public boolean renew(
        ExecutionId executionId,
        WorkerId workerId,
        long fencingToken,
        long durationMillis
    ) {
        ExecutionLease current =
            leases.get(executionId);

        if (current == null) {
            return false;
        }

        if (!current.workerId().equals(workerId)) {
            return false;
        }

        if (current.fencingToken() != fencingToken) {
            return false;
        }

        if (current.expiresAt()
            .isBefore(Instant.now())
        ) {
            return false;
        }

        ExecutionLease renewed =
            new ExecutionLease(
                executionId,
                workerId,
                fencingToken,
                current.acquiredAt(),
                Instant.now()
                    .plusMillis(durationMillis)
            );

        leases.put(executionId, renewed);

        return true;
    }

    @Override
    public boolean release(
        ExecutionId executionId,
        WorkerId workerId,
        long fencingToken
    ) {
        ExecutionLease current =
            leases.get(executionId);

        if (current == null) {
            return true;
        }

        if (!current.workerId().equals(workerId)) {
            return false;
        }

        if (current.fencingToken() != fencingToken) {
            return false;
        }

        leases.remove(executionId);

        return true;
    }

    @Override
    public Optional<ExecutionLease> current(
        ExecutionId executionId
    ) {
        return Optional.ofNullable(
            leases.get(executionId)
        );
    }
}

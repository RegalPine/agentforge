package io.github.regalpine.agentforge.distributed.lease;

import io.github.regalpine.agentforge.distributed.worker.WorkerId;
import io.github.regalpine.agentforge.event.ExecutionId;

import java.util.Optional;

public interface LeaseStore {

    Optional<ExecutionLease> acquire(
        ExecutionId executionId,
        WorkerId workerId,
        long durationMillis
    );

    boolean renew(
        ExecutionId executionId,
        WorkerId workerId,
        long fencingToken,
        long durationMillis
    );

    boolean release(
        ExecutionId executionId,
        WorkerId workerId,
        long fencingToken
    );

    Optional<ExecutionLease> current(
        ExecutionId executionId
    );
}

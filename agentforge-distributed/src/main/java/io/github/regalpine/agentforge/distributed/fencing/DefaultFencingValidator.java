package io.github.regalpine.agentforge.distributed.fencing;

import io.github.regalpine.agentforge.distributed.lease.ExecutionLease;

import java.time.Instant;

public final class DefaultFencingValidator
    implements FencingValidator {

    @Override
    public boolean isValid(
        ExecutionLease lease
    ) {
        if (lease == null) {
            return false;
        }

        return lease.expiresAt()
            .isAfter(
                Instant.now()
            );
    }
}

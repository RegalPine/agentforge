package io.github.regalpine.agentforge.distributed.fencing;

import io.github.regalpine.agentforge.distributed.lease.ExecutionLease;

public interface FencingValidator {

    boolean isValid(
        ExecutionLease lease
    );
}

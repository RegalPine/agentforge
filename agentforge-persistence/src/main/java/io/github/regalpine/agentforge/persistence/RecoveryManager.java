package io.github.regalpine.agentforge.persistence;

import io.github.regalpine.agentforge.event.ExecutionId;

public interface RecoveryManager {

    RecoveryResult recover(
        ExecutionId executionId
    );
}

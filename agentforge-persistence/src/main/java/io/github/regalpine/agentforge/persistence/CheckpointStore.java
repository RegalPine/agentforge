package io.github.regalpine.agentforge.persistence;

import io.github.regalpine.agentforge.event.ExecutionId;

import java.util.List;
import java.util.Optional;

public interface CheckpointStore {

    void save(
        Checkpoint checkpoint
    );

    Optional<Checkpoint> latest(
        ExecutionId executionId
    );

    List<Checkpoint> list(
        ExecutionId executionId
    );
}

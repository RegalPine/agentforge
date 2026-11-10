package io.github.regalpine.agentforge.persistence;

import io.github.regalpine.agentforge.event.ExecutionId;

import java.util.Optional;

public interface SnapshotStore {

    void save(
        Snapshot snapshot
    );

    Optional<Snapshot> latest(
        ExecutionId executionId
    );
}

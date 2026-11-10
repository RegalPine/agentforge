package io.github.regalpine.agentforge.persistence.memory;

import io.github.regalpine.agentforge.persistence.Snapshot;
import io.github.regalpine.agentforge.persistence.SnapshotStore;
import io.github.regalpine.agentforge.event.ExecutionId;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemorySnapshotStore
    implements SnapshotStore {

    private final Map<
        ExecutionId,
        Snapshot
    > snapshots = new ConcurrentHashMap<>();

    @Override
    public void save(
        Snapshot snapshot
    ) {
        Snapshot current =
            snapshots.get(
                snapshot.executionId()
            );

        if (current == null ||
            snapshot.eventSequence() >
                current.eventSequence()
        ) {
            snapshots.put(
                snapshot.executionId(),
                snapshot
            );
        }
    }

    @Override
    public Optional<Snapshot> latest(
        ExecutionId executionId
    ) {
        return Optional.ofNullable(
            snapshots.get(executionId)
        );
    }
}

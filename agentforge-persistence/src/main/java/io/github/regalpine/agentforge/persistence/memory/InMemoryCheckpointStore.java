package io.github.regalpine.agentforge.persistence.memory;

import io.github.regalpine.agentforge.persistence.Checkpoint;
import io.github.regalpine.agentforge.persistence.CheckpointStore;
import io.github.regalpine.agentforge.event.ExecutionId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InMemoryCheckpointStore
    implements CheckpointStore {

    private final List<Checkpoint> checkpoints =
        new CopyOnWriteArrayList<>();

    @Override
    public void save(
        Checkpoint checkpoint
    ) {
        checkpoints.add(checkpoint);
    }

    @Override
    public Optional<Checkpoint> latest(
        ExecutionId executionId
    ) {
        return list(executionId).stream()
            .max(
                Comparator.comparingLong(
                    Checkpoint::stateVersion
                )
            );
    }

    @Override
    public List<Checkpoint> list(
        ExecutionId executionId
    ) {
        List<Checkpoint> result =
            new ArrayList<>();

        for (Checkpoint cp : checkpoints) {
            if (cp.executionId().equals(executionId)) {
                result.add(cp);
            }
        }

        return List.copyOf(result);
    }
}

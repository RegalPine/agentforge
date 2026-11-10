package io.github.regalpine.agentforge.persistence.memory;

import io.github.regalpine.agentforge.persistence.InvocationStore;
import io.github.regalpine.agentforge.persistence.StoredInvocation;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryInvocationStore
    implements InvocationStore {

    private final Map<
        String,
        StoredInvocation
    > invocations = new ConcurrentHashMap<>();

    private final Map<
        String,
        StoredInvocation
    > idempotencyIndex = new ConcurrentHashMap<>();

    @Override
    public void create(
        StoredInvocation invocation
    ) {
        StoredInvocation previous =
            invocations.putIfAbsent(
                invocation.invocationId(),
                invocation
            );

        if (previous != null) {
            throw new IllegalStateException(
                "Invocation already exists: "
                    + invocation.invocationId()
            );
        }

        if (invocation.idempotencyKey() != null) {
            idempotencyIndex.putIfAbsent(
                invocation.idempotencyKey(),
                invocation
            );
        }
    }

    @Override
    public Optional<StoredInvocation> find(
        String invocationId
    ) {
        return Optional.ofNullable(
            invocations.get(invocationId)
        );
    }

    @Override
    public Optional<StoredInvocation> findByIdempotencyKey(
        String key
    ) {
        return Optional.ofNullable(
            idempotencyIndex.get(key)
        );
    }

    @Override
    public void update(
        StoredInvocation invocation
    ) {
        invocations.put(
            invocation.invocationId(),
            invocation
        );
    }
}

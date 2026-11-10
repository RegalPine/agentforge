package io.github.regalpine.agentforge.persistence;

import java.util.Optional;

public interface InvocationStore {

    void create(
        StoredInvocation invocation
    );

    Optional<StoredInvocation> find(
        String invocationId
    );

    Optional<StoredInvocation> findByIdempotencyKey(
        String key
    );

    void update(
        StoredInvocation invocation
    );
}

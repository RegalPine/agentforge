package io.github.regalpine.agentforge.server.idempotency;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class IdempotencyStore {

    private final Map<String, IdempotencyRecord> store =
        new ConcurrentHashMap<>();

    public Optional<IdempotencyRecord> find(
        String tenantId,
        String key
    ) {
        return Optional.ofNullable(
            store.get(compositeKey(tenantId, key))
        );
    }

    public IdempotencyRecord putIfAbsent(
        String tenantId,
        String key,
        String executionId
    ) {
        String ck = compositeKey(tenantId, key);

        IdempotencyRecord record =
            new IdempotencyRecord(
                ck,
                tenantId,
                key,
                executionId
            );

        IdempotencyRecord existing =
            store.putIfAbsent(ck, record);

        if (existing != null) {
            return existing;
        }

        return record;
    }

    private String compositeKey(
        String tenantId,
        String key
    ) {
        return tenantId + ":" + key;
    }

    public record IdempotencyRecord(
        String compositeKey,
        String tenantId,
        String key,
        String executionId
    ) {
    }
}

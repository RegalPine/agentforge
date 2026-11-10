package io.github.regalpine.agentforge.memory.redis;

import io.github.regalpine.agentforge.memory.*;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Redis memory provider.
 * AF-ENHANCE-03 §5: bundles the Redis
 * store, retriever and capabilities.
 *
 * <p>Optimised for session context and
 * high-frequency transient memory
 * retrieval. Does not support semantic
 * search.
 */
public final class RedisMemoryProvider
    implements MemoryProvider {

    private final RedisMemoryStore store;
    private final RedisMemoryRetriever retriever;
    private final MemoryProvider.MemoryCapabilities capabilities;

    public RedisMemoryProvider(
        RedisMemoryConfig config,
        RedisMemoryStore.RedisClient client
    ) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(client, "client");

        this.store = new RedisMemoryStore(
            config, client
        );
        this.retriever = new RedisMemoryRetriever(
            store, client, config
        );

        this.capabilities =
            new MemoryProvider.MemoryCapabilities(
                false,
                false,
                true,
                Set.of(
                    MemoryType.CONVERSATION,
                    MemoryType.FACT,
                    MemoryType.PREFERENCE,
                    MemoryType.SUMMARY
                ),
                Map.of(
                    "engine", "redis",
                    "ttl", config.defaultTtl().toString()
                )
            );
    }

    @Override
    public String providerType() {
        return "redis";
    }

    @Override
    public MemoryStore store() {
        return store;
    }

    @Override
    public MemoryRetriever retriever() {
        return retriever;
    }

    @Override
    public MemoryCapabilities capabilities() {
        return capabilities;
    }
}

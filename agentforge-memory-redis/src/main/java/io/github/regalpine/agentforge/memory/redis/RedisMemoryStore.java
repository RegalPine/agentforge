package io.github.regalpine.agentforge.memory.redis;

import io.github.regalpine.agentforge.memory.*;
import io.github.regalpine.agentforge.security.DataClassification;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Redis memory store.
 * AF-ENHANCE-03 §5: stores memory
 * records in Redis for session context,
 * transient and high-frequency retrieval.
 *
 * <p>Uses a pluggable {@link RedisClient}
 * abstraction so that different Redis
 * client libraries (Jedis, Lettuce) can
 * be used.
 *
 * <p>Data model:
 * <ul>
 *   <li>Key: prefix + memoryId → encoded record</li>
 *   <li>Set: prefix + "idx:subject:" + subjectId → memoryIds</li>
 *   <li>Set: prefix + "idx:type:" + subjectId + ":" + type → memoryIds</li>
 * </ul>
 */
public final class RedisMemoryStore
    implements MemoryStore {

    private final RedisMemoryConfig config;
    private final RedisClient client;

    public RedisMemoryStore(
        RedisMemoryConfig config,
        RedisClient client
    ) {
        this.config =
            Objects.requireNonNull(config, "config");
        this.client =
            Objects.requireNonNull(client, "client");
    }

    @Override
    public MemoryRecord save(MemoryRecord record) {
        Objects.requireNonNull(record, "record");

        String key = config.memoryKey(record.memoryId());
        String encoded = encodeRecord(record);

        Duration ttl = computeTtl(record);

        if (ttl != null) {
            client.setEx(key, encoded, ttl);
        } else {
            client.set(key, encoded);
        }

        // Update subject index
        client.sadd(
            config.subjectIndexKey(
                record.subjectId()
            ),
            record.memoryId()
        );

        // Update type index
        client.sadd(
            config.typeIndexKey(
                record.subjectId(),
                record.type().name()
            ),
            record.memoryId()
        );

        return record;
    }

    @Override
    public Optional<MemoryRecord> get(
        String memoryId
    ) {
        Objects.requireNonNull(memoryId, "memoryId");

        String key = config.memoryKey(memoryId);
        String encoded = client.get(key);

        if (encoded == null || encoded.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(decodeRecord(encoded));
    }

    @Override
    public void update(MemoryRecord record) {
        save(record);
    }

    @Override
    public void delete(String memoryId) {
        Objects.requireNonNull(memoryId, "memoryId");

        Optional<MemoryRecord> existing = get(memoryId);

        if (existing.isPresent()) {
            MemoryRecord softDeleted =
                existing.get().withStatus(
                    MemoryStatus.DELETED
                );

            String key = config.memoryKey(memoryId);
            client.set(key, encodeRecord(softDeleted));
        }
    }

    /**
     * Retrieve memory IDs from the
     * subject index.
     */
    public Set<String> findBySubject(
        String subjectId
    ) {
        return client.smembers(
            config.subjectIndexKey(subjectId)
        );
    }

    /**
     * Retrieve memory IDs from the
     * type index.
     */
    public Set<String> findBySubjectAndType(
        String subjectId, MemoryType type
    ) {
        return client.smembers(
            config.typeIndexKey(
                subjectId, type.name()
            )
        );
    }

    public RedisMemoryConfig config() {
        return config;
    }

    // --- encoding / decoding ---

    private String encodeRecord(MemoryRecord r) {
        long expiresMs = r.expiresAt() == null
            ? -1
            : r.expiresAt().toEpochMilli();

        return RedisMemoryCodec.encode(
            r.memoryId(),
            r.tenantId(),
            r.subjectId(),
            r.type().name(),
            r.scope().name(),
            r.content(),
            r.createdAt().toEpochMilli(),
            r.updatedAt().toEpochMilli(),
            expiresMs,
            r.status().name(),
            r.source().name(),
            r.confidence().score(),
            r.confidence().basis().name(),
            r.importance().name(),
            r.classification().name(),
            r.version()
        );
    }

    static MemoryRecord decodeRecord(
        String encoded
    ) {
        String[] f = RedisMemoryCodec.decode(encoded);

        long expiresMs = Long.parseLong(
            f[RedisMemoryCodec.F_EXPIRES_AT]
        );

        ConfidenceBasis confBasis;
        try {
            confBasis = ConfidenceBasis.valueOf(
                f[RedisMemoryCodec.F_CONF_BASIS]
            );
        } catch (Exception e) {
            confBasis = ConfidenceBasis.MANUAL;
        }

        return MemoryRecord.builder()
            .memoryId(f[RedisMemoryCodec.F_MEMORY_ID])
            .tenantId(f[RedisMemoryCodec.F_TENANT_ID])
            .subjectId(f[RedisMemoryCodec.F_SUBJECT_ID])
            .type(MemoryType.valueOf(
                f[RedisMemoryCodec.F_TYPE]
            ))
            .scope(MemoryScope.valueOf(
                f[RedisMemoryCodec.F_SCOPE]
            ))
            .content(f[RedisMemoryCodec.F_CONTENT])
            .createdAt(Instant.ofEpochMilli(
                Long.parseLong(f[RedisMemoryCodec.F_CREATED_AT])
            ))
            .updatedAt(Instant.ofEpochMilli(
                Long.parseLong(f[RedisMemoryCodec.F_UPDATED_AT])
            ))
            .expiresAt(expiresMs < 0 ? null
                : Instant.ofEpochMilli(expiresMs))
            .status(MemoryStatus.valueOf(
                f[RedisMemoryCodec.F_STATUS]
            ))
            .source(MemorySource.valueOf(
                f[RedisMemoryCodec.F_SOURCE]
            ))
            .confidence(new MemoryConfidence(
                Double.parseDouble(
                    f[RedisMemoryCodec.F_CONF_SCORE]
                ),
                confBasis
            ))
            .importance(MemoryImportance.valueOf(
                f[RedisMemoryCodec.F_IMPORTANCE]
            ))
            .classification(DataClassification.valueOf(
                f[RedisMemoryCodec.F_CLASSIFICATION]
            ))
            .version(Long.parseLong(
                f[RedisMemoryCodec.F_VERSION]
            ))
            .build();
    }

    private Duration computeTtl(
        MemoryRecord record
    ) {
        if (record.expiresAt() != null) {
            Duration remaining = Duration.between(
                Instant.now(), record.expiresAt()
            );
            if (remaining.compareTo(Duration.ZERO) > 0) {
                return remaining;
            }
        }

        if (record.scope() == MemoryScope.EXECUTION
            || record.scope() == MemoryScope.CONVERSATION
        ) {
            return config.defaultTtl();
        }

        return null;
    }

    /**
     * Redis client abstraction.
     * Allows different Redis libraries
     * (Jedis, Lettuce) to be plugged in.
     */
    public interface RedisClient {

        void set(String key, String value);

        void setEx(
            String key, String value, Duration ttl
        );

        String get(String key);

        void sadd(String key, String... members);

        Set<String> smembers(String key);

        void del(String key);
    }
}

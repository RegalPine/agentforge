package io.github.regalpine.agentforge.memory.redis;

import io.github.regalpine.agentforge.memory.*;
import io.github.regalpine.agentforge.security.DataClassification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis memory adapter tests.
 * AF-ENHANCE-03 §5: uses an in-memory
 * mock Redis client to verify the
 * store, retriever and provider.
 */
class RedisMemoryAdapterTest {

    private InMemoryRedisClient redis;
    private RedisMemoryStore store;
    private RedisMemoryRetriever retriever;

    @BeforeEach
    void setUp() {
        redis = new InMemoryRedisClient();

        var config = RedisMemoryConfig.of("localhost");

        store = new RedisMemoryStore(config, redis);
        retriever = new RedisMemoryRetriever(
            store, redis, config
        );
    }

    @Test
    void shouldSaveAndRetrieve() {
        var record = makeRecord("m1", "user1",
            MemoryType.FACT, "The sky is blue");

        store.save(record);

        Optional<MemoryRecord> found =
            store.get("m1");

        assertTrue(found.isPresent());
        assertEquals("The sky is blue",
            found.get().content());
    }

    @Test
    void shouldSoftDelete() {
        var record = makeRecord("m2", "user1",
            MemoryType.FACT, "test content");

        store.save(record);
        store.delete("m2");

        Optional<MemoryRecord> found =
            store.get("m2");

        assertTrue(found.isPresent());
        assertEquals(MemoryStatus.DELETED,
            found.get().status());
    }

    @Test
    void shouldRetrieveByExactSubject() {
        store.save(makeRecord("m1", "user1",
            MemoryType.FACT, "fact 1"));
        store.save(makeRecord("m2", "user1",
            MemoryType.PREFERENCE, "pref 1"));
        store.save(makeRecord("m3", "user2",
            MemoryType.FACT, "fact 2"));

        // Activate all
        activateAll();

        var query = new MemoryQuery(
            "user1", MemoryScope.USER,
            null, "", 10,
            RetrievalStrategy.EXACT
        );

        List<MemoryRecord> results =
            retriever.retrieve(query);

        assertEquals(2, results.size());
    }

    @Test
    void shouldRetrieveByType() {
        store.save(makeRecord("m1", "user1",
            MemoryType.FACT, "fact 1"));
        store.save(makeRecord("m2", "user1",
            MemoryType.PREFERENCE, "pref 1"));

        activateAll();

        var query = new MemoryQuery(
            "user1", MemoryScope.USER,
            MemoryType.FACT, "", 10,
            RetrievalStrategy.EXACT
        );

        List<MemoryRecord> results =
            retriever.retrieve(query);

        assertEquals(1, results.size());
        assertEquals(MemoryType.FACT,
            results.get(0).type());
    }

    @Test
    void shouldRetrieveByKeyword() {
        store.save(makeRecord("m1", "user1",
            MemoryType.FACT,
            "The weather is sunny today"));
        store.save(makeRecord("m2", "user1",
            MemoryType.FACT,
            "The food is delicious"));

        activateAll();

        var query = new MemoryQuery(
            "user1", MemoryScope.USER,
            null, "sunny weather", 10,
            RetrievalStrategy.KEYWORD
        );

        List<MemoryRecord> results =
            retriever.retrieve(query);

        assertEquals(1, results.size());
        assertTrue(results.get(0).content()
            .contains("sunny"));
    }

    @Test
    void shouldRejectSemanticSearch() {
        var query = new MemoryQuery(
            "user1", MemoryScope.USER,
            null, "test", 10,
            RetrievalStrategy.SEMANTIC
        );

        assertThrows(
            UnsupportedOperationException.class,
            () -> retriever.retrieve(query)
        );
    }

    @Test
    void shouldEncodeAndDecodeRecord() {
        var record = makeRecord("m1", "t1",
            MemoryType.FACT, "hello world");

        store.save(record);

        Optional<MemoryRecord> found =
            store.get("m1");

        assertTrue(found.isPresent());

        MemoryRecord decoded = found.get();

        assertEquals("m1", decoded.memoryId());
        assertEquals("tenant-1", decoded.tenantId());
        assertEquals(MemoryType.FACT,
            decoded.type());
        assertEquals("hello world",
            decoded.content());
    }

    // --- helpers ---

    private MemoryRecord makeRecord(
        String id, String subject,
        MemoryType type, String content
    ) {
        return MemoryRecord.builder()
            .memoryId(id)
            .tenantId("tenant-1")
            .subjectId(subject)
            .type(type)
            .scope(MemoryScope.USER)
            .content(content)
            .status(MemoryStatus.ACTIVE)
            .build();
    }

    private void activateAll() {
        // Records are already ACTIVE from builder
    }

    /**
     * In-memory Redis client mock.
     */
    static class InMemoryRedisClient
        implements RedisMemoryStore.RedisClient {

        final Map<String, String> data =
            new ConcurrentHashMap<>();

        final Map<String, Set<String>> sets =
            new ConcurrentHashMap<>();

        @Override
        public void set(String key, String value) {
            data.put(key, value);
        }

        @Override
        public void setEx(
            String key, String value, Duration ttl
        ) {
            data.put(key, value);
        }

        @Override
        public String get(String key) {
            return data.get(key);
        }

        @Override
        public void sadd(
            String key, String... members
        ) {
            sets.computeIfAbsent(key,
                k -> ConcurrentHashMap.newKeySet()
            ).addAll(Arrays.asList(members));
        }

        @Override
        public Set<String> smembers(String key) {
            return sets.getOrDefault(
                key, Set.of()
            );
        }

        @Override
        public void del(String key) {
            data.remove(key);
            sets.remove(key);
        }
    }
}

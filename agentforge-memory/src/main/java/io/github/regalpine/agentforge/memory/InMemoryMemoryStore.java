package io.github.regalpine.agentforge.memory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory memory store.
 * AE-08 §69: InMemory MemoryStore.
 *
 * <p>Development/testing implementation
 * backed by a ConcurrentHashMap.
 */
public final class InMemoryMemoryStore
    implements MemoryStore {

    private final Map<String, MemoryRecord> records =
        new ConcurrentHashMap<>();

    @Override
    public MemoryRecord save(MemoryRecord record) {
        records.put(record.memoryId(), record);
        return record;
    }

    @Override
    public Optional<MemoryRecord> get(
        String memoryId
    ) {
        return Optional.ofNullable(
            records.get(memoryId)
        );
    }

    @Override
    public void update(MemoryRecord record) {
        records.put(record.memoryId(), record);
    }

    @Override
    public void delete(String memoryId) {
        var existing = records.get(memoryId);
        if (existing != null) {
            records.put(
                memoryId,
                existing.withStatus(MemoryStatus.DELETED)
            );
        }
    }

    /**
     * Get all records (for testing/retrieval).
     */
    public Map<String, MemoryRecord> allRecords() {
        return Map.copyOf(records);
    }

    /**
     * Get count of all records.
     */
    public int size() {
        return records.size();
    }

    /**
     * Clear all records.
     */
    public void clear() {
        records.clear();
    }
}

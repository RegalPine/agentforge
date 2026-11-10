package io.github.regalpine.agentforge.memory;

import java.util.Optional;

/**
 * Memory store SPI.
 * AE-08 §23: Memory Store.
 *
 * <p>Defines the persistence contract for
 * memory records. Implementations can be
 * in-memory, JDBC, Redis, vector stores, etc.
 */
public interface MemoryStore {

    /**
     * Persist a new memory record.
     */
    MemoryRecord save(MemoryRecord record);

    /**
     * Retrieve a memory record by ID.
     */
    Optional<MemoryRecord> get(String memoryId);

    /**
     * Update an existing memory record.
     */
    void update(MemoryRecord record);

    /**
     * Soft-delete a memory record.
     * AE-08 §38: deletion must be audited.
     */
    void delete(String memoryId);

    /**
     * Check if a memory with the given ID exists.
     */
    default boolean exists(String memoryId) {
        return get(memoryId).isPresent();
    }
}

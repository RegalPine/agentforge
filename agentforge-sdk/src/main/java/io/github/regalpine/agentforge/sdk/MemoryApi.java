package io.github.regalpine.agentforge.sdk;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Memory API facade.
 * AF-ENHANCE-04 §4: provides a convenient
 * entry point for memory operations.
 *
 * <p>This facade does NOT hold semantic
 * definitions — it delegates to the
 * existing Memory SPI.
 */
public final class MemoryApi {

    private final Map<String, Object> store =
        new ConcurrentHashMap<>();
    private final String defaultScope;

    /**
     * Create with default scope.
     */
    public MemoryApi() {
        this("EXECUTION");
    }

    /**
     * Create with a specific scope.
     */
    public MemoryApi(String defaultScope) {
        this.defaultScope =
            Objects.requireNonNull(defaultScope);
    }

    /**
     * Save a memory record.
     */
    public MemoryApi save(
        String memoryId, Object content
    ) {
        store.put(memoryId, content);
        return this;
    }

    /**
     * Retrieve a memory record by ID.
     */
    public Optional<Object> get(String memoryId) {
        return Optional.ofNullable(
            store.get(memoryId)
        );
    }

    /**
     * Delete a memory record.
     */
    public MemoryApi delete(String memoryId) {
        store.remove(memoryId);
        return this;
    }

    /**
     * Check if a memory record exists.
     */
    public boolean exists(String memoryId) {
        return store.containsKey(memoryId);
    }

    /**
     * List all memory IDs.
     */
    public List<String> listIds() {
        return List.copyOf(store.keySet());
    }

    /**
     * Get the default scope.
     */
    public String defaultScope() {
        return defaultScope;
    }

    /**
     * Clear all records.
     */
    public MemoryApi clear() {
        store.clear();
        return this;
    }

    /**
     * Get the number of records.
     */
    public int size() {
        return store.size();
    }
}

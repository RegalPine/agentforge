package io.github.regalpine.agentforge.testkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模拟 Memory Provider。
 *
 * <p>对应 AF-ENHANCE-04 §63。</p>
 */
public final class MockMemoryProvider {

    private final Map<String,
        List<Map<String, Object>>>
        memories = new ConcurrentHashMap<>();

    private final Map<String,
        Map<String, Object>> store =
        new ConcurrentHashMap<>();

    /**
     * 预设检索结果。
     */
    public MockMemoryProvider returns(
        String query,
        List<Map<String, Object>> results
    ) {
        memories.put(
            query,
            new ArrayList<>(results)
        );
        return this;
    }

    /**
     * 存储键值。
     */
    public MockMemoryProvider put(
        String key,
        Map<String, Object> value
    ) {
        store.put(
            key,
            Map.copyOf(value)
        );
        return this;
    }

    /**
     * 检索。
     */
    public List<Map<String, Object>>
    search(String query) {
        return memories.getOrDefault(
            query, List.of()
        );
    }

    /**
     * 读取。
     */
    public Map<String, Object> get(
        String key
    ) {
        return store.getOrDefault(
            key, Map.of()
        );
    }

    /**
     * 重置。
     */
    public void reset() {
        memories.clear();
        store.clear();
    }
}

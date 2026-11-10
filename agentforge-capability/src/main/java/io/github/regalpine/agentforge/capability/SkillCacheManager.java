package io.github.regalpine.agentforge.capability;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Skill 缓存管理器。
 *
 * <p>支持 TTL 缓存和失效。
 * 对应 AF-ENHANCE-05 §8。</p>
 */
public final class SkillCacheManager {

    private final Map<String, CacheEntry>
        cache = new ConcurrentHashMap<>();

    private final Duration defaultTtl;

    public SkillCacheManager(
        Duration defaultTtl
    ) {
        Objects.requireNonNull(
            defaultTtl, "defaultTtl"
        );
        this.defaultTtl = defaultTtl;
    }

    public SkillCacheManager() {
        this(Duration.ofMinutes(30));
    }

    /**
     * 缓存值。
     */
    public void put(
        String key,
        Object value
    ) {
        cache.put(
            key,
            new CacheEntry(
                value,
                Instant.now().plus(
                    defaultTtl
                )
            )
        );
    }

    /**
     * 缓存值（自定义 TTL）。
     */
    public void put(
        String key,
        Object value,
        Duration ttl
    ) {
        cache.put(
            key,
            new CacheEntry(
                value,
                Instant.now().plus(ttl)
            )
        );
    }

    /**
     * 获取缓存值。
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            cache.remove(key);
            return null;
        }
        return (T) entry.value();
    }

    /**
     * 是否缓存命中。
     */
    public boolean containsKey(
        String key
    ) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return false;
        }
        if (entry.isExpired()) {
            cache.remove(key);
            return false;
        }
        return true;
    }

    /**
     * 失效指定 key。
     */
    public void invalidate(
        String key
    ) {
        cache.remove(key);
    }

    /**
     * 失效所有。
     */
    public void invalidateAll() {
        cache.clear();
    }

    /**
     * 清理过期条目。
     */
    public int evictExpired() {
        int count = 0;
        var it = cache.entrySet()
            .iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (entry.getValue()
                .isExpired()
            ) {
                it.remove();
                count++;
            }
        }
        return count;
    }

    /**
     * 缓存大小。
     */
    public int size() {
        return cache.size();
    }

    private record CacheEntry(
        Object value,
        Instant expiresAt
    ) {
        boolean isExpired() {
            return Instant.now()
                .isAfter(expiresAt);
        }
    }
}

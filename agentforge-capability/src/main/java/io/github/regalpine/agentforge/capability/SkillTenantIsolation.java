package io.github.regalpine.agentforge.capability;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Skill 租户隔离。
 *
 * <p>确保租户间 Skill 数据隔离。
 * 对应 AF-ENHANCE-05 §10。</p>
 */
public final class SkillTenantIsolation {

    private final Map<String,
        Set<String>> tenantSkills =
        new ConcurrentHashMap<>();

    private final Map<String,
        Map<String, Object>>
        tenantMetadata =
        new ConcurrentHashMap<>();

    /**
     * 注册租户的 Skill。
     */
    public void register(
        String tenantId,
        String skillId
    ) {
        Objects.requireNonNull(
            tenantId, "tenantId"
        );
        Objects.requireNonNull(
            skillId, "skillId"
        );
        tenantSkills.computeIfAbsent(
            tenantId,
            k -> ConcurrentHashMap.newKeySet()
        ).add(skillId);
    }

    /**
     * 检查租户是否有权访问 Skill。
     */
    public boolean isAccessible(
        String tenantId,
        String skillId
    ) {
        Set<String> skills =
            tenantSkills.get(tenantId);
        return skills != null
            && skills.contains(skillId);
    }

    /**
     * 获取租户可见的 Skill 列表。
     */
    public Set<String> visibleSkills(
        String tenantId
    ) {
        Set<String> skills =
            tenantSkills.get(tenantId);
        return skills == null
            ? Set.of()
            : Set.copyOf(skills);
    }

    /**
     * 设置租户元数据。
     */
    public void setMetadata(
        String tenantId,
        Map<String, Object> metadata
    ) {
        tenantMetadata.put(
            tenantId,
            Map.copyOf(metadata)
        );
    }

    /**
     * 获取租户元数据。
     */
    public Map<String, Object>
    getMetadata(String tenantId) {
        return tenantMetadata
            .getOrDefault(
                tenantId, Map.of()
            );
    }

    /**
     * 移除租户。
     */
    public void removeTenant(
        String tenantId
    ) {
        tenantSkills.remove(tenantId);
        tenantMetadata.remove(tenantId);
    }
}

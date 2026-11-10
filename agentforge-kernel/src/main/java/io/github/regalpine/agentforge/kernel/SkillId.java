package io.github.regalpine.agentforge.kernel;

import java.util.Objects;

/**
 * Skill 全局唯一稳定身份标识.
 * ASM-SKILL-01 §4: SkillId.
 *
 * <p>ASCII 字符集约束，不可变。
 * 与 {@link EntityId} 使用相同的字符集规则，
 * 但作为独立类型提供 Skill 域的类型安全。
 */
public record SkillId(String value) {

    public SkillId {
        Objects.requireNonNull(value, "value");

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                "SkillId cannot be blank"
            );
        }

        if (!value.matches("[A-Za-z0-9._:-]+")) {
            throw new IllegalArgumentException(
                "Invalid SkillId: " + value
            );
        }
    }

    /**
     * Convert to generic {@link EntityId}.
     */
    public EntityId toEntityId() {
        return new EntityId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

package io.github.regalpine.agentforge.runtime.hitl;

/**
 * 脱敏策略。
 *
 * <p>对应 AF-ENHANCE-03 §14。</p>
 */
public enum RedactionPolicy {

    /**
     * 部分遮盖。
     * 例: ***-**-1234
     */
    MASK,

    /**
     * 完全替换为 [REDACTED]。
     */
    REDACT,

    /**
     * 完全移除字段。
     */
    OMIT,

    /**
     * 替换为哈希值。
     */
    HASH,

    /**
     * 替换为摘要描述。
     */
    SUMMARY
}

package io.github.regalpine.agentforge.capability;

/**
 * Skill 信任级别。
 *
 * <p>三级信任模型。
 * 对应 AF-ENHANCE-05 §16。</p>
 */
public enum SkillTrustLevel {

    /**
     * 本地开发，未验证。
     */
    LOCAL,

    /**
     * 应用级验证。
     */
    APPLICATION,

    /**
     * 企业级签名验证。
     */
    ENTERPRISE;

    /**
     * 是否满足最低信任要求。
     */
    public boolean meetsMinimum(
        SkillTrustLevel minimum
    ) {
        return ordinal()
            >= minimum.ordinal();
    }

    /**
     * 是否可执行生产操作。
     */
    public boolean canExecuteProduction() {
        return this == ENTERPRISE
            || this == APPLICATION;
    }
}

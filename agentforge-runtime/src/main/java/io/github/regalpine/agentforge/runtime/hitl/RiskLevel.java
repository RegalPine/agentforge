package io.github.regalpine.agentforge.runtime.hitl;

import io.github.regalpine.agentforge.event.Execution;
/**
 * 风险等级。
 *
 * <p>绑定到具体 Execution Action，
 * 不是对 Agent 本身的评价。</p>
 */
public enum RiskLevel {

    LOW,

    MEDIUM,

    HIGH,

    CRITICAL;

    /**
     * 是否 >= 指定等级。
     */
    public boolean isAtLeast(
        RiskLevel other
    ) {
        return ordinal()
            >= other.ordinal();
    }
}

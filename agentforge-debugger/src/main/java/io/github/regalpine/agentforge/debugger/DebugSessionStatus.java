package io.github.regalpine.agentforge.debugger;

/**
 * Debug 会话状态。
 *
 * <p>对应 AF-ENHANCE-04 §8。</p>
 */
public enum DebugSessionStatus {

    CREATED,

    ATTACHED,

    RUNNING,

    PAUSED,

    REPLAYING,

    COMPLETED,

    TERMINATED;

    /**
     * 是否为活跃状态。
     */
    public boolean isActive() {
        return this == ATTACHED
            || this == RUNNING
            || this == PAUSED
            || this == REPLAYING;
    }

    /**
     * 是否为终态。
     */
    public boolean isTerminal() {
        return this == COMPLETED
            || this == TERMINATED;
    }
}

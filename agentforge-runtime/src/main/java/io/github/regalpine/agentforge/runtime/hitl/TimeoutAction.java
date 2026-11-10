package io.github.regalpine.agentforge.runtime.hitl;

/**
 * 超时处理策略。
 */
public enum TimeoutAction {

    /**
     * 超时后自动拒绝。
     */
    REJECT,

    /**
     * 超时后终止执行。
     */
    ABORT,

    /**
     * 超时后升级。
     */
    ESCALATE,

    /**
     * 超时后保持暂停。
     */
    REMAIN_PAUSED
}

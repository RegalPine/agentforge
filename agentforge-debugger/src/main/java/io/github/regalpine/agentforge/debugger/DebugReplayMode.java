package io.github.regalpine.agentforge.debugger;

/**
 * 重放模式。
 *
 * <p>对应 AF-ENHANCE-04 §31。</p>
 */
public enum DebugReplayMode {

    /**
     * 只查看历史。
     */
    OBSERVE,

    /**
     * 重新执行验证行为。
     */
    REPRODUCE,

    /**
     * 重放 + 断点。
     */
    DEBUG,

    /**
     * 真实 Agent + 虚拟外部世界。
     */
    SIMULATION
}

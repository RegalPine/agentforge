package io.github.regalpine.agentforge.event;

/**
 * 重放模式。
 */
public enum ReplayMode {

    /**
     * 真实调用外部资源。
     */
    LIVE,

    /**
     * 优先使用历史记录。
     */
    REPLAY,

    /**
     * 由模拟器提供结果。
     */
    SIMULATION
}

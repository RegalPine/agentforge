package io.github.regalpine.agentforge.debugger;

/**
 * 步进命令（7 种）。
 *
 * <p>对应 AF-ENHANCE-04 §20。</p>
 */
public enum StepCommand {

    CONTINUE,

    PAUSE,

    STEP_OVER,

    STEP_INTO,

    STEP_OUT,

    RESTART,

    STOP
}

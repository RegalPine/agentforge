package io.github.regalpine.agentforge.multiagent.invocation;

/**
 * InvocationMode — AF-ENHANCE-08 §6.
 *
 * Three modes of agent-to-agent invocation:
 *
 * <p>DELEGATE:
 * Caller delegates a subtask and retains
 * control. Caller continues to exist.
 *
 * <p>HANDOFF:
 * Caller transfers full control to target.
 * Target becomes the new owner.
 *
 * <p>COLLABORATE:
 * Multiple agents work together on the
 * same goal in parallel or interactively.
 */
public enum InvocationMode {

    /**
     * Delegate a subtask.
     * Caller retains control.
     */
    DELEGATE,

    /**
     * Transfer full control.
     * Target becomes owner.
     */
    HANDOFF,

    /**
     * Multi-agent cooperation.
     * Shared goal.
     */
    COLLABORATE
}

package io.github.regalpine.agentforge.a2a;

/**
 * A2A event type.
 * AF-ENHANCE-10 §40: A2A Event.
 */
public enum A2AEventType {

    TASK_CREATED,

    TASK_STARTED,

    TASK_PROGRESS,

    TASK_MESSAGE,

    TASK_ARTIFACT,

    TASK_COMPLETED,

    TASK_FAILED,

    TASK_CANCELLED
}

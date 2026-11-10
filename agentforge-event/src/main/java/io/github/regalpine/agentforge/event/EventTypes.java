package io.github.regalpine.agentforge.event;

/**
 * 稳定事件类型常量。
 *
 * <p>禁止使用 Java Class 名作为事件类型。</p>
 */
public final class EventTypes {

    private EventTypes() {}

    // --- Lifecycle ---
    public static final String AGENT_STARTED =
        "agent.started";
    public static final String AGENT_PAUSED =
        "agent.paused";
    public static final String AGENT_RESUMED =
        "agent.resumed";
    public static final String AGENT_COMPLETED =
        "agent.completed";
    public static final String AGENT_FAILED =
        "agent.failed";

    // --- Execution ---
    public static final String EXECUTION_STARTED =
        "execution.started";
    public static final String EXECUTION_PAUSED =
        "execution.paused";
    public static final String EXECUTION_RESUMED =
        "execution.resumed";
    public static final String EXECUTION_COMPLETED =
        "execution.completed";
    public static final String EXECUTION_FAILED =
        "execution.failed";
    public static final String EXECUTION_CANCELLED =
        "execution.cancelled";

    // --- Skill ---
    public static final String SKILL_STARTED =
        "skill.started";
    public static final String SKILL_COMPLETED =
        "skill.completed";
    public static final String SKILL_FAILED =
        "skill.failed";
    public static final String SKILL_CANCELLED =
        "skill.cancelled";

    // --- Tool ---
    public static final String TOOL_STARTED =
        "tool.started";
    public static final String TOOL_INPUT =
        "tool.input";
    public static final String TOOL_OUTPUT =
        "tool.output";
    public static final String TOOL_DELTA =
        "tool.delta";
    public static final String TOOL_COMPLETED =
        "tool.completed";
    public static final String TOOL_FAILED =
        "tool.failed";
    public static final String TOOL_CANCELLED =
        "tool.cancelled";

    // --- Model ---
    public static final String MODEL_STARTED =
        "model.started";
    public static final String MODEL_INPUT =
        "model.input";
    public static final String MODEL_DELTA =
        "model.delta";
    public static final String MODEL_OUTPUT =
        "model.output";
    public static final String MODEL_COMPLETED =
        "model.completed";
    public static final String MODEL_FAILED =
        "model.failed";

    // --- Message ---
    public static final String MESSAGE_CREATED =
        "message.created";
    public static final String MESSAGE_DELTA =
        "message.delta";
    public static final String MESSAGE_COMPLETED =
        "message.completed";

    // --- Plan ---
    public static final String PLAN_CREATED =
        "plan.created";
    public static final String PLAN_UPDATED =
        "plan.updated";
    public static final String PLAN_STARTED =
        "plan.started";
    public static final String PLAN_STEP_STARTED =
        "plan.step.started";
    public static final String PLAN_STEP_COMPLETED =
        "plan.step.completed";
    public static final String PLAN_COMPLETED =
        "plan.completed";
    public static final String PLAN_FAILED =
        "plan.failed";

    // --- Memory ---
    public static final String MEMORY_READ_STARTED =
        "memory.read.started";
    public static final String MEMORY_READ_COMPLETED =
        "memory.read.completed";
    public static final String MEMORY_WRITE_STARTED =
        "memory.write.started";
    public static final String MEMORY_WRITE_COMPLETED =
        "memory.write.completed";
    public static final String MEMORY_RETRIEVED =
        "memory.retrieved";
    public static final String MEMORY_COMPRESSED =
        "memory.compressed";

    // --- Policy ---
    public static final String POLICY_EVAL_STARTED =
        "policy.evaluation.started";
    public static final String POLICY_EVAL_COMPLETED =
        "policy.evaluation.completed";
    public static final String POLICY_ALLOWED =
        "policy.allowed";
    public static final String POLICY_DENIED =
        "policy.denied";
    public static final String POLICY_APPROVAL_REQUIRED =
        "policy.approval.required";

    // --- Human ---
    public static final String HUMAN_APPROVAL_REQUIRED =
        "human.approval.required";
    public static final String HUMAN_APPROVAL_RECEIVED =
        "human.approval.received";
    public static final String HUMAN_APPROVAL_REJECTED =
        "human.approval.rejected";
    public static final String HUMAN_INPUT_REQUIRED =
        "human.input.required";
    public static final String HUMAN_INPUT_RECEIVED =
        "human.input.received";

    // --- Error ---
    public static final String ERROR_OCCURRED =
        "error.occurred";
    public static final String SECURITY_VIOLATION =
        "security.violation";
}

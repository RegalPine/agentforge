package io.github.regalpine.agentforge.event;

/**
 * 事件分类。
 *
 * <p>11 类核心事件，对应 AF-ENHANCE-02 §18-28。</p>
 */
public enum EventCategory {

    LIFECYCLE,
    EXECUTION,
    SKILL,
    TOOL,
    MODEL,
    MESSAGE,
    PLAN,
    MEMORY,
    POLICY,
    HUMAN,
    ERROR;

    /**
     * 从 eventType 推导 category。
     */
    public static EventCategory fromEventType(
        String eventType
    ) {
        if (eventType == null) {
            throw new IllegalArgumentException(
                "eventType is null"
            );
        }
        String prefix = eventType.contains(".")
            ? eventType.substring(
                0, eventType.indexOf('.')
              )
            : eventType;
        return switch (prefix) {
            case "agent" -> LIFECYCLE;
            case "execution" -> EXECUTION;
            case "skill" -> SKILL;
            case "tool" -> TOOL;
            case "model" -> MODEL;
            case "message" -> MESSAGE;
            case "plan" -> PLAN;
            case "memory" -> MEMORY;
            case "policy" -> POLICY;
            case "human" -> HUMAN;
            case "error" -> ERROR;
            case "security" -> ERROR;
            default -> LIFECYCLE;
        };
    }
}

package io.github.regalpine.agentforge.model;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a tool call request from the model.
 * <p>
 * When a model decides to invoke a tool, it produces a {@code ToolCall}
 * with the tool identifier and arguments. The tool call must be structured
 * and contain all necessary information for tool execution.
 * <p>
 * Example:
 * <pre>
 * {
 *   "callId": "call-001",
 *   "toolId": "customer.query",
 *   "arguments": {
 *     "customerId": "C001"
 *   }
 * }
 * </pre>
 */
public record ToolCall(
    String callId,
    String toolId,
    Map<String, Object> arguments
) {

    public ToolCall {
        Objects.requireNonNull(callId, "callId");
        Objects.requireNonNull(toolId, "toolId");

        if (callId.isBlank()) {
            throw new IllegalArgumentException("callId cannot be blank");
        }
        if (toolId.isBlank()) {
            throw new IllegalArgumentException("toolId cannot be blank");
        }

        arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
    }

    /**
     * Create a tool call with the given parameters.
     *
     * @param callId    unique identifier for this call
     * @param toolId    identifier of the tool to invoke
     * @param arguments arguments to pass to the tool
     * @return a new ToolCall
     */
    public static ToolCall of(String callId, String toolId, Map<String, Object> arguments) {
        return new ToolCall(callId, toolId, arguments);
    }

    /**
     * Create a tool call with no arguments.
     *
     * @param callId unique identifier for this call
     * @param toolId identifier of the tool to invoke
     * @return a new ToolCall
     */
    public static ToolCall of(String callId, String toolId) {
        return new ToolCall(callId, toolId, Map.of());
    }
}

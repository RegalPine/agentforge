package io.github.regalpine.agentforge.sdk;

import java.util.Map;

/**
 * Result of a capability invocation.
 */
public record InvocationResult(
    boolean success,
    Map<String, Object> output,
    String errorMessage
) {

    public InvocationResult {
        output = output == null ? Map.of() : Map.copyOf(output);
    }

    public static InvocationResult success(Map<String, Object> output) {
        return new InvocationResult(true, output, null);
    }

    public static InvocationResult failure(String errorMessage) {
        return new InvocationResult(false, Map.of(), errorMessage);
    }
}

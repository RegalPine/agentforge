package io.github.regalpine.agentforge.server.api;

public record ExecutionSubmitRequest(
    String graphId,
    String graphVersion,
    java.util.Map<String, Object> inputs
) {

    public ExecutionSubmitRequest {
        inputs =
            inputs == null
                ? java.util.Map.of()
                : java.util.Map.copyOf(inputs);
    }
}

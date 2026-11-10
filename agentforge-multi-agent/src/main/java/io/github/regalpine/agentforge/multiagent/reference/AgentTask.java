package io.github.regalpine.agentforge.multiagent.reference;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * AgentTask — AF-ENHANCE-08 §9.
 *
 * A task to be delegated or
 * handed off to another agent.
 *
 * <p>Contains: task description,
 * input data, constraints,
 * and expected output type.
 */
public record AgentTask(

    String taskId,

    String description,

    Map<String, Object> inputData,

    List<String> requiredOutputs,

    Map<String, Object> constraints

) {

    public AgentTask {
        Objects.requireNonNull(
            description, "description"
        );

        inputData = inputData == null
            ? Map.of()
            : Map.copyOf(inputData);

        requiredOutputs =
            requiredOutputs == null
                ? List.of()
                : List.copyOf(requiredOutputs);

        constraints = constraints == null
            ? Map.of()
            : Map.copyOf(constraints);
    }

    /**
     * Create a simple task.
     */
    public static AgentTask of(
        String description
    ) {
        return new AgentTask(
            "task-" + System.nanoTime(),
            description,
            null, null, null
        );
    }

    /**
     * Create with input data.
     */
    public static AgentTask of(
        String description,
        Map<String, Object> input
    ) {
        return new AgentTask(
            "task-" + System.nanoTime(),
            description, input,
            null, null
        );
    }

    /**
     * Create with full params.
     */
    public static AgentTask of(
        String taskId,
        String description,
        Map<String, Object> input,
        List<String> outputs,
        Map<String, Object> constraints
    ) {
        return new AgentTask(
            taskId, description,
            input, outputs, constraints
        );
    }
}

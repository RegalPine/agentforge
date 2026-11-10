package io.github.regalpine.agentforge.sdk;

import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * Reference to a registered Graph.
 * <p>
 * Provides graph execution capabilities. SDK does not redefine Graph —
 * this references the existing Graph model, Graph IR, and Execution Plan.
 * <p>
 * Thread-safe.
 */
public interface GraphRef {

    /**
     * The unique graph identifier.
     */
    String id();

    /**
     * The graph version.
     */
    String version();

    /**
     * Execute the graph with input.
     *
     * @param input the input data
     * @return the execution result
     */
    ExecutionResult run(Map<String, Object> input);

    /**
     * Execute the graph with input and options.
     *
     * @param input   the input data
     * @param options execution options
     * @return the execution result
     */
    ExecutionResult run(Map<String, Object> input, ExecutionOptions options);

    /**
     * Execute the graph asynchronously.
     *
     * @param input the input data
     * @return a CompletionStage for the execution result
     */
    CompletionStage<ExecutionResult> runAsync(Map<String, Object> input);
}

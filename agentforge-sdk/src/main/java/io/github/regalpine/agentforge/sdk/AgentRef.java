package io.github.regalpine.agentforge.sdk;

import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * Reference to a registered Agent.
 * <p>
 * Provides execution and description capabilities.
 * SDK does not redefine Agent — this references the existing Agent model.
 * <p>
 * Thread-safe.
 */
public interface AgentRef {

    /**
     * The unique agent identifier.
     */
    String id();

    /**
     * The agent version.
     */
    String version();

    /**
     * Get the read-only agent descriptor.
     */
    AgentDescriptor descriptor();

    /**
     * Execute the agent with input.
     *
     * @param input the input data
     * @return the execution result
     */
    ExecutionResult run(Map<String, Object> input);

    /**
     * Execute the agent with input and options.
     *
     * @param input   the input data
     * @param options execution options
     * @return the execution result
     */
    ExecutionResult run(Map<String, Object> input, ExecutionOptions options);

    /**
     * Execute the agent asynchronously.
     *
     * @param input the input data
     * @return a CompletionStage for the execution result
     */
    CompletionStage<ExecutionResult> runAsync(Map<String, Object> input);

    /**
     * Execute the agent asynchronously with options.
     *
     * @param input   the input data
     * @param options execution options
     * @return a CompletionStage for the execution result
     */
    CompletionStage<ExecutionResult> runAsync(Map<String, Object> input, ExecutionOptions options);
}

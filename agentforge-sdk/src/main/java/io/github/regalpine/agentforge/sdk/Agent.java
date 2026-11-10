package io.github.regalpine.agentforge.sdk;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * First-class Agent interface.
 * AF-ENHANCE-01 §15: Agent API.
 *
 * <p>Represents a fully configured Agent
 * that can execute synchronously,
 * asynchronously, or via streaming.
 *
 * <p>This is the primary runtime object
 * produced by AgentBuilder and consumed
 * by AgentForge.run().
 *
 * <p>Thread-safe.
 */
public interface Agent {

    /**
     * The unique agent identifier.
     */
    String id();

    /**
     * The agent metadata.
     */
    AgentMetadata metadata();

    /**
     * Execute the agent synchronously.
     *
     * @param input the agent input
     * @return the execution result
     */
    ExecutionResult execute(AgentInput input);

    /**
     * Execute the agent asynchronously.
     *
     * @param input the agent input
     * @return a CompletionStage for the result
     */
    CompletionStage<ExecutionResult> executeAsync(
        AgentInput input
    );

    /**
     * Execute the agent with streaming.
     * Events are delivered to the consumer.
     *
     * @param input   the agent input
     * @param handler the event handler
     * @return the final execution result
     */
    ExecutionResult stream(
        AgentInput input,
        Consumer<AgentEvent> handler
    );

    /**
     * Simplified execution with a
     * text message.
     */
    default ExecutionResult run(String message) {
        return execute(AgentInput.of(message));
    }
}

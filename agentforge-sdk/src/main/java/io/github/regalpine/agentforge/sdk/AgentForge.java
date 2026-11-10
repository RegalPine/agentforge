package io.github.regalpine.agentforge.sdk;

import io.github.regalpine.agentforge.sdk.bootstrap.BootstrapLifecycle;

import java.util.List;
import java.util.Map;

/**
 * Main entry point for the AgentForge Developer SDK.
 * <p>
 * Provides unified access to Agent, Capability, Graph, and Execution APIs.
 * SDK is a developer-facing facade over the existing AgentForge internal architecture.
 * <p>
 * Lifecycle: CREATED → INITIALIZED → READY → RUNNING → CLOSING → CLOSED
 * <p>
 * Thread-safe. Supports AutoCloseable for try-with-resources.
 *
 * <pre>{@code
 * // Simple path
 * AgentForge.run("researcher", "Analyze AI trends");
 *
 * // Fluent API path
 * Agent agent = AgentForge.defineAgent("researcher")
 *     .model("openai", "gpt-4")
 *     .skill("research")
 *     .buildAgent();
 * AgentForge.run(agent, "Analyze AI trends");
 *
 * // Bootstrap path
 * try (AgentForge forge = AgentForge.bootstrap(config)) {
 *     ExecutionResult result = forge.agent("my-agent").run(input);
 * }
 * }</pre>
 */
public interface AgentForge extends AutoCloseable {

    // --- Bootstrap ---

    /**
     * Bootstrap an AgentForge instance with the given configuration.
     *
     * @param config the configuration
     * @return a new AgentForge instance
     */
    static AgentForge bootstrap(AgentForgeConfig config) {
        return new DefaultAgentForge(config);
    }

    /**
     * Bootstrap with default configuration.
     */
    static AgentForge bootstrap() {
        return bootstrap(AgentForgeConfig.defaults());
    }

    // --- Fluent API (AF-ENHANCE-01 §13-14) ---

    /**
     * Start building an Agent using
     * the fluent API.
     * AF-ENHANCE-01 §13.
     *
     * <pre>{@code
     * Agent agent = AgentForge.defineAgent("researcher")
     *     .model("openai", "gpt-4")
     *     .skill("research")
     *     .tool("web-search")
     *     .buildAgent();
     * }</pre>
     */
    static AgentBuilder defineAgent(String agentId) {
        return new AgentBuilder(agentId);
    }

    /**
     * Get a fluent builder for creating
     * agents, graphs, and configurations.
     */
    static FluentBuilder builder() {
        return FluentBuilder.create();
    }

    // --- Simple Path (AF-ENHANCE-01 §17) ---

    /**
     * Run an agent by ID with a text message.
     * AF-ENHANCE-01 §17: Simple path.
     *
     * <pre>{@code
     * AgentForge.run("researcher", "Analyze AI trends");
     * }</pre>
     */
    static ExecutionResult run(
        String agentId, String message
    ) {
        Agent agent = defineAgent(agentId).buildAgent();
        return run(agent, message);
    }

    /**
     * Run a built Agent with a text message.
     * AF-ENHANCE-01 §16.
     */
    static ExecutionResult run(
        Agent agent, String message
    ) {
        return agent.execute(
            AgentInput.of(message)
        );
    }

    /**
     * Run a built Agent with AgentInput.
     */
    static ExecutionResult run(
        Agent agent, AgentInput input
    ) {
        return agent.execute(input);
    }

    // --- Runtime Builder (AF-ENHANCE-01 §16) ---

    /**
     * Create a runtime builder for
     * configuring and launching an
     * Agent runtime.
     * AF-ENHANCE-01 §16.
     */
    static AgentRuntimeBuilder runtime() {
        return new AgentRuntimeBuilder();
    }

    /**
     * Get a reference to a registered Agent.
     *
     * @param agentId the agent identifier
     * @return the agent reference
     * @throws ResolutionException if the agent is not found
     */
    AgentRef agent(String agentId);

    /**
     * Get a reference to a registered Capability.
     *
     * @param capabilityId the capability identifier
     * @return the capability reference
     * @throws ResolutionException if the capability is not found
     */
    CapabilityRef capability(String capabilityId);

    /**
     * Get a reference to a registered Graph.
     *
     * @param graphId the graph identifier
     * @return the graph reference
     * @throws ResolutionException if the graph is not found
     */
    GraphRef graph(String graphId);

    /**
     * Get a reference to an active execution.
     *
     * @param executionId the execution identifier
     * @return the execution reference
     * @throws ResolutionException if the execution is not found
     */
    ExecutionRef execution(String executionId);

    /**
     * List available agent IDs.
     */
    List<String> agentIds();

    /**
     * List available capability IDs.
     */
    List<String> capabilityIds();

    /**
     * List available graph IDs.
     */
    List<String> graphIds();

    /**
     * Get the Memory API facade.
     */
    MemoryApi memory();

    /**
     * Get the RAG / Knowledge API facade.
     */
    RagApi rag();

    /**
     * Get the Evaluation API facade.
     */
    EvaluationApi evaluation();

    /**
     * Get the Skill API facade.
     * S5: SkillClient integration.
     */
    SkillApi skills();

    /**
     * Get the SDK configuration.
     */
    AgentForgeConfig config();

    /**
     * Get the current lifecycle status.
     */
    LifecycleStatus lifecycleStatus();

    /**
     * Close the AgentForge instance and release resources.
     */
    @Override
    void close();

    /**
     * SDK lifecycle status.
     */
    enum LifecycleStatus {
        CREATED,
        INITIALIZED,
        READY,
        RUNNING,
        CLOSING,
        CLOSED
    }
}

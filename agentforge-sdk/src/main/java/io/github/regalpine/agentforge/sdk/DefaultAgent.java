package io.github.regalpine.agentforge.sdk;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * Default Agent implementation.
 * AF-ENHANCE-01 §15: Agent API.
 *
 * <p>Produces an ExecutionResult based
 * on the configured model, skills, tools,
 * and policies. Actual model invocation
 * is delegated to the Runtime (X1+ phase).
 *
 * <p>This implementation provides a
 * structural placeholder that integrates
 * with the existing SDK execution model.
 */
final class DefaultAgent implements Agent {

    private final AgentMetadata metadata;
    private final String modelProvider;
    private final String modelName;
    private final List<SkillRef> skills;
    private final List<String> tools;
    private final List<PolicyRef> policies;
    private final String memoryScope;
    private final String systemPrompt;
    private final WorkspaceRef workspace;
    private final Map<String, Object> config;

    DefaultAgent(
        AgentMetadata metadata,
        String modelProvider,
        String modelName,
        List<SkillRef> skills,
        List<String> tools,
        List<PolicyRef> policies,
        String memoryScope,
        String systemPrompt,
        WorkspaceRef workspace,
        Map<String, Object> config
    ) {
        this.metadata = Objects.requireNonNull(
            metadata, "metadata"
        );
        this.modelProvider = modelProvider;
        this.modelName = modelName;
        this.skills = skills != null
            ? skills : List.of();
        this.tools = tools != null
            ? tools : List.of();
        this.policies = policies != null
            ? policies : List.of();
        this.memoryScope = memoryScope;
        this.systemPrompt = systemPrompt;
        this.workspace = workspace;
        this.config = config != null
            ? config : Map.of();
    }

    @Override
    public String id() {
        return metadata.id();
    }

    @Override
    public AgentMetadata metadata() {
        return metadata;
    }

    @Override
    public ExecutionResult execute(
        AgentInput input
    ) {
        Objects.requireNonNull(input, "input");

        String executionId =
            "exec-" + UUID.randomUUID();

        // Build output from the agent
        // configuration. Actual model
        // invocation is handled by Runtime.
        Map<String, Object> output =
            new LinkedHashMap<>();
        output.put("agentId", id());
        output.put("input", input.message());
        if (modelProvider != null) {
            output.put("modelProvider",
                modelProvider);
        }
        if (modelName != null) {
            output.put("modelName",
                modelName);
        }
        output.put("skills",
            skills.stream()
                .map(SkillRef::qualifiedId)
                .toList());
        output.put("tools", tools);
        output.put("systemPrompt",
            systemPrompt != null
                ? systemPrompt : "");

        return ExecutionResult.success(
            executionId, output
        );
    }

    @Override
    public CompletionStage<ExecutionResult> executeAsync(
        AgentInput input
    ) {
        return CompletableFuture.supplyAsync(
            () -> execute(input)
        );
    }

    @Override
    public ExecutionResult stream(
        AgentInput input,
        Consumer<AgentEvent> handler
    ) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(handler, "handler");

        String executionId =
            "exec-" + UUID.randomUUID();

        // Emit a start event
        handler.accept(new AgentEvent(
            "execution.started",
            executionId,
            id(),
            Map.of("input", input.message()),
            java.time.Instant.now()
        ));

        // Execute and emit completion
        ExecutionResult result = execute(input);

        handler.accept(AgentEvent.completed(
            executionId, id()
        ));

        return result;
    }

    /**
     * Get the model provider.
     */
    String modelProvider() {
        return modelProvider;
    }

    /**
     * Get the model name.
     */
    String modelName() {
        return modelName;
    }

    /**
     * Get the configured skills.
     */
    List<SkillRef> skills() {
        return skills;
    }

    /**
     * Get the configured tools.
     */
    List<String> tools() {
        return tools;
    }

    /**
     * Get the configured policies.
     */
    List<PolicyRef> policies() {
        return policies;
    }

    /**
     * Get the system prompt.
     */
    String systemPrompt() {
        return systemPrompt;
    }

    /**
     * Get the workspace reference.
     */
    WorkspaceRef workspace() {
        return workspace;
    }
}

package io.github.regalpine.agentforge.sdk;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * Fluent builder for defining and
 * registering agents.
 * AF-ENHANCE-01 §14: AgentBuilder.
 *
 * <p>Usage:
 * <pre>{@code
 * Agent agent = AgentForge.agent("researcher")
 *     .model("openai", "gpt-4")
 *     .skill(researchSkill)
 *     .tool(searchTool)
 *     .memory("USER")
 *     .policy(safetyPolicy)
 *     .workspace(execWorkspace)
 *     .systemPrompt("You are a researcher.")
 *     .build();
 * }</pre>
 *
 * <p>This builder supports both legacy
 * AgentDescriptor and new Agent interface.
 */
public final class AgentBuilder {

    private final String agentId;
    private final Map<String, Object> config =
        new LinkedHashMap<>();
    private final List<String> capabilities =
        new ArrayList<>();
    private final List<String> tools =
        new ArrayList<>();
    private final List<SkillRef> skills =
        new ArrayList<>();
    private final List<PolicyRef> policies =
        new ArrayList<>();
    private String modelProvider;
    private String modelName;
    private String memoryScope;
    private String description;
    private String systemPrompt;
    private WorkspaceRef workspace;
    private String version = "1.0.0";

    public AgentBuilder(String agentId) {
        this.agentId =
            Objects.requireNonNull(agentId, "agentId");
    }

    /**
     * Set the model provider and name.
     */
    public AgentBuilder model(
        String provider, String name
    ) {
        this.modelProvider = provider;
        this.modelName = name;
        return this;
    }

    /**
     * Add a capability reference.
     */
    public AgentBuilder capability(String capId) {
        capabilities.add(capId);
        return this;
    }

    /**
     * Add a tool reference.
     */
    public AgentBuilder tool(String toolId) {
        tools.add(toolId);
        return this;
    }

    /**
     * Add multiple tool references.
     */
    public AgentBuilder tools(String... toolIds) {
        Collections.addAll(tools, toolIds);
        return this;
    }

    /**
     * Add a skill reference.
     * AF-ENHANCE-01 §14.
     */
    public AgentBuilder skill(SkillRef skill) {
        skills.add(
            Objects.requireNonNull(skill, "skill")
        );
        return this;
    }

    /**
     * Add a skill by name.
     */
    public AgentBuilder skill(String skillName) {
        skills.add(SkillRef.of(skillName));
        return this;
    }

    /**
     * Add multiple skill references.
     */
    public AgentBuilder skills(SkillRef... refs) {
        Collections.addAll(skills, refs);
        return this;
    }

    /**
     * Set the memory scope.
     */
    public AgentBuilder memory(String scope) {
        this.memoryScope = scope;
        return this;
    }

    /**
     * Add a policy reference.
     * AF-ENHANCE-01 §14.
     */
    public AgentBuilder policy(PolicyRef policy) {
        policies.add(
            Objects.requireNonNull(policy, "policy")
        );
        return this;
    }

    /**
     * Add a policy by ID.
     */
    public AgentBuilder policy(String policyId) {
        policies.add(PolicyRef.of(policyId));
        return this;
    }

    /**
     * Set the workspace reference.
     * AF-ENHANCE-01 §14.
     */
    public AgentBuilder workspace(WorkspaceRef ws) {
        this.workspace = ws;
        return this;
    }

    /**
     * Set the workspace by ID.
     */
    public AgentBuilder workspace(String wsId) {
        this.workspace =
            WorkspaceRef.of(wsId);
        return this;
    }

    /**
     * Set the system prompt.
     * AF-ENHANCE-01 §14.
     */
    public AgentBuilder systemPrompt(String prompt) {
        this.systemPrompt = prompt;
        return this;
    }

    /**
     * Set the description.
     */
    public AgentBuilder description(String desc) {
        this.description = desc;
        return this;
    }

    /**
     * Set the agent version.
     */
    public AgentBuilder version(String version) {
        this.version = version;
        return this;
    }

    /**
     * Set a configuration property.
     */
    public AgentBuilder property(
        String key, Object value
    ) {
        config.put(key, value);
        return this;
    }

    /**
     * Build the agent descriptor (legacy).
     */
    public AgentDescriptor build() {
        return new AgentDescriptor(
            agentId,
            version,
            description != null
                ? description : "",
            Set.copyOf(
                new LinkedHashSet<>(capabilities)
            )
        );
    }

    /**
     * Build the first-class Agent.
     * AF-ENHANCE-01 §15.
     */
    public Agent buildAgent() {
        AgentMetadata metadata =
            AgentMetadata.builder(agentId)
                .name(agentId)
                .version(version)
                .description(
                    description != null
                        ? description : ""
                )
                .build();

        return new DefaultAgent(
            metadata,
            modelProvider,
            modelName,
            List.copyOf(skills),
            List.copyOf(tools),
            List.copyOf(policies),
            memoryScope,
            systemPrompt,
            workspace,
            Map.copyOf(config)
        );
    }

    /**
     * Get the agent ID.
     */
    public String agentId() {
        return agentId;
    }

    /**
     * Get the configured skills.
     */
    public List<SkillRef> skills() {
        return List.copyOf(skills);
    }

    /**
     * Get the configured policies.
     */
    public List<PolicyRef> policies() {
        return List.copyOf(policies);
    }
}

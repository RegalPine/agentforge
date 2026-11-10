package io.github.regalpine.agentforge.sdk;

import java.util.Objects;

/**
 * Top-level fluent builder for the
 * AgentForge SDK.
 * AF-ENHANCE-04 §3: unified entry point.
 *
 * <p>Usage:
 * <pre>{@code
 * AgentForge forge = FluentBuilder.create()
 *     .applicationId("my-app")
 *     .tenantId("default")
 *     .build();
 * }</pre>
 */
public final class FluentBuilder {

    private final AgentForgeConfig.Builder
        configBuilder = AgentForgeConfig.builder();

    private SecretProvider secretProvider;

    FluentBuilder() {}

    /**
     * Create a new fluent builder.
     */
    public static FluentBuilder create() {
        return new FluentBuilder();
    }

    /**
     * Set the application ID.
     */
    public FluentBuilder applicationId(String id) {
        configBuilder.applicationId(id);
        return this;
    }

    /**
     * Set the tenant ID.
     */
    public FluentBuilder tenantId(String id) {
        configBuilder.tenantId(id);
        return this;
    }

    /**
     * Set a secret provider for resolving
     * secrets from configuration.
     */
    public FluentBuilder secretProvider(
        SecretProvider provider
    ) {
        this.secretProvider = provider;
        return this;
    }

    /**
     * Start building an agent definition.
     */
    public AgentBuilder agent(String agentId) {
        return new AgentBuilder(agentId);
    }

    /**
     * Start building a graph definition.
     */
    public GraphBuilder graph(String graphId) {
        return new GraphBuilder(graphId);
    }

    /**
     * Load configuration from
     * agentforge.yaml.
     */
    public FluentBuilder loadYaml() {
        AgentForgeConfig yamlConfig =
            YamlConfigLoader.loadFromFile();

        if (yamlConfig.applicationId() != null) {
            configBuilder.applicationId(
                yamlConfig.applicationId()
            );
        }

        if (yamlConfig.tenantId() != null) {
            configBuilder.tenantId(
                yamlConfig.tenantId()
            );
        }

        return this;
    }

    /**
     * Build the AgentForge instance.
     */
    public AgentForge build() {
        AgentForgeConfig config =
            configBuilder.build();
        DefaultAgentForge forge =
            new DefaultAgentForge(config);
        return forge;
    }
}

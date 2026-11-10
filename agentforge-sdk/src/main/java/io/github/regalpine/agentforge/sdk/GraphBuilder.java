package io.github.regalpine.agentforge.sdk;

import java.util.*;

/**
 * Fluent builder for defining and
 * registering graphs.
 * AF-ENHANCE-04 §4: Graph API.
 *
 * <p>Usage:
 * <pre>{@code
 * GraphBuilder graph = AgentForge.builder()
 *     .graph("my-graph")
 *     .node("classify", "classifier")
 *     .node("respond", "responder")
 *     .edge("classify", "respond")
 *     .build();
 * }</pre>
 */
public final class GraphBuilder {

    private final String graphId;
    private String description;
    private final List<NodeDef> nodes =
        new ArrayList<>();
    private final List<EdgeDef> edges =
        new ArrayList<>();
    private final Map<String, Object> config =
        new LinkedHashMap<>();

    GraphBuilder(String graphId) {
        this.graphId =
            Objects.requireNonNull(graphId, "graphId");
    }

    /**
     * Add a node.
     */
    public GraphBuilder node(
        String nodeId, String capabilityId
    ) {
        nodes.add(new NodeDef(
            nodeId, capabilityId, Map.of()
        ));
        return this;
    }

    /**
     * Add a node with config.
     */
    public GraphBuilder node(
        String nodeId,
        String capabilityId,
        Map<String, Object> nodeConfig
    ) {
        nodes.add(new NodeDef(
            nodeId, capabilityId, nodeConfig
        ));
        return this;
    }

    /**
     * Add an edge.
     */
    public GraphBuilder edge(
        String from, String to
    ) {
        edges.add(new EdgeDef(from, to, null));
        return this;
    }

    /**
     * Add a conditional edge.
     */
    public GraphBuilder edge(
        String from,
        String to,
        String condition
    ) {
        edges.add(new EdgeDef(
            from, to, condition
        ));
        return this;
    }

    /**
     * Set the description.
     */
    public GraphBuilder description(String desc) {
        this.description = desc;
        return this;
    }

    /**
     * Set a configuration property.
     */
    public GraphBuilder property(
        String key, Object value
    ) {
        config.put(key, value);
        return this;
    }

    /**
     * Build the graph descriptor.
     */
    public GraphDescriptor build() {
        return new GraphDescriptor(
            graphId,
            description != null
                ? description : "",
            List.copyOf(nodes),
            List.copyOf(edges),
            Map.copyOf(config)
        );
    }

    /**
     * Get the graph ID.
     */
    public String graphId() {
        return graphId;
    }

    // --- Inner records ---

    /**
     * Node definition.
     */
    public record NodeDef(
        String nodeId,
        String capabilityId,
        Map<String, Object> config
    ) {}

    /**
     * Edge definition.
     */
    public record EdgeDef(
        String from,
        String to,
        String condition
    ) {}
}

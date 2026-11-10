package io.github.regalpine.agentforge.graph;

import io.github.regalpine.agentforge.agent.Agent;
import io.github.regalpine.agentforge.capability.Capability;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;

import java.util.ArrayList;
import java.util.List;

public final class GraphBuilder {

    private final EntityId id;
    private final Version version;
    private final String name;

    private final List<NodeDefinition> nodes =
        new ArrayList<>();

    private final List<EdgeDefinition> edges =
        new ArrayList<>();

    private GraphBuilder(
        EntityId id,
        Version version,
        String name
    ) {
        this.id = id;
        this.version = version;
        this.name = name;
    }

    public static GraphBuilder graph(
        String id,
        String version,
        String name
    ) {
        return new GraphBuilder(
            new EntityId(id),
            new Version(version),
            name
        );
    }

    public GraphBuilder start(String id) {
        nodes.add(
            new NodeDefinition(
                new EntityId(id),
                NodeType.START,
                null,
                null
            )
        );

        return this;
    }

    public GraphBuilder end(String id) {
        nodes.add(
            new NodeDefinition(
                new EntityId(id),
                NodeType.END,
                null,
                null
            )
        );

        return this;
    }

    public GraphBuilder capability(
        String id,
        Capability capability
    ) {
        nodes.add(
            new NodeDefinition(
                new EntityId(id),
                NodeType.CAPABILITY,
                new Reference(
                    capability.id(),
                    capability.version()
                ),
                null
            )
        );

        return this;
    }

    public GraphBuilder agent(
        String id,
        Agent agent
    ) {
        nodes.add(
            new NodeDefinition(
                new EntityId(id),
                NodeType.AGENT,
                new Reference(
                    agent.id(),
                    agent.version()
                ),
                null
            )
        );

        return this;
    }

    public GraphBuilder connect(
        String source,
        String target
    ) {
        String edgeId =
            source + "-to-" + target;

        edges.add(
            new EdgeDefinition(
                new EntityId(edgeId),
                new EntityId(source),
                new EntityId(target),
                EdgeType.NORMAL,
                null
            )
        );

        return this;
    }

    public GraphDefinition build() {
        return new GraphDefinition(
            id,
            version,
            name,
            null,
            null,
            null,
            nodes,
            edges,
            null,
            null,
            null,
            null
        );
    }
}

package io.github.regalpine.agentforge.graph;

import java.util.HashSet;
import java.util.Set;

public final class BasicGraphValidator {

    public void validate(
        GraphDefinition graph
    ) {

        if (graph.nodes().isEmpty()) {
            throw new IllegalArgumentException(
                "Graph must contain nodes"
            );
        }

        Set<String> nodeIds =
            new HashSet<>();

        int startCount = 0;
        int endCount = 0;

        for (NodeDefinition node :
            graph.nodes()) {

            if (!nodeIds.add(
                node.id().value()
            )) {
                throw new IllegalArgumentException(
                    "Duplicate node id: "
                        + node.id()
                );
            }

            if (node.type() == NodeType.START) {
                startCount++;
            }

            if (node.type() == NodeType.END) {
                endCount++;
            }

            if (node.type() == NodeType.CAPABILITY &&
                node.reference() == null) {

                throw new IllegalArgumentException(
                    "CAPABILITY node must reference "
                        + "a Capability"
                );
            }

            if (node.type() == NodeType.AGENT &&
                node.reference() == null) {

                throw new IllegalArgumentException(
                    "AGENT node must reference "
                        + "an Agent"
                );
            }
        }

        if (startCount != 1) {
            throw new IllegalArgumentException(
                "Graph must contain exactly one START"
            );
        }

        if (endCount != 1) {
            throw new IllegalArgumentException(
                "Graph must contain exactly one END"
            );
        }

        Set<String> edgeIds =
            new HashSet<>();

        for (EdgeDefinition edge :
            graph.edges()) {

            if (!edgeIds.add(
                edge.id().value()
            )) {
                throw new IllegalArgumentException(
                    "Duplicate edge id: "
                        + edge.id()
                );
            }

            if (!nodeIds.contains(
                edge.source().value()
            )) {
                throw new IllegalArgumentException(
                    "Unknown edge source: "
                        + edge.source()
                );
            }

            if (!nodeIds.contains(
                edge.target().value()
            )) {
                throw new IllegalArgumentException(
                    "Unknown edge target: "
                        + edge.target()
                );
            }
        }
    }
}

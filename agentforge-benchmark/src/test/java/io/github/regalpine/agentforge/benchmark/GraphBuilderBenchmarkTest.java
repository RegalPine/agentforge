package io.github.regalpine.agentforge.benchmark;

import io.github.regalpine.agentforge.graph.EdgeDefinition;
import io.github.regalpine.agentforge.graph.EdgeType;
import io.github.regalpine.agentforge.graph.GraphBuilder;
import io.github.regalpine.agentforge.graph.GraphDefinition;
import io.github.regalpine.agentforge.graph.NodeDefinition;
import io.github.regalpine.agentforge.graph.NodeType;
import io.github.regalpine.agentforge.kernel.EntityId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@org.junit.jupiter.api.Tag("benchmark")
class GraphBuilderBenchmarkTest {

    @Test
    void shouldBuildLargeGraphWithinTimeBudget() {
        int nodeCount = 100;

        long start = System.nanoTime();
        int iterations = 500;

        for (int iter = 0; iter < iterations; iter++) {
            GraphBuilder builder = GraphBuilder
                .graph("bench-large", "1.0.0", "Large Graph")
                .start("start");

            for (int i = 0; i < nodeCount; i++) {
                builder.start("node-" + i);
            }

            builder.end("end");

            builder.connect("start", "node-0");
            for (int i = 0; i < nodeCount - 1; i++) {
                builder.connect("node-" + i, "node-" + (i + 1));
            }
            builder.connect("node-" + (nodeCount - 1), "end");

            GraphDefinition graph = builder.build();
            assertEquals(nodeCount + 2, graph.nodes().size());
            assertEquals(nodeCount + 1, graph.edges().size());
        }

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        double avgMicros = (double) elapsedMs * 1000.0 / iterations;

        System.out.printf(
            "[BENCH] Large graph (%d nodes) build: %d iterations in %d ms (%.1f µs/iter)%n",
            nodeCount, iterations, elapsedMs, avgMicros
        );

        assertTrue(elapsedMs < 30_000,
            "500 builds should complete within 30s");
    }

    @Test
    void shouldMeasureGraphDefinitionImmutability() {
        GraphDefinition graph = GraphBuilder
            .graph("bench-immutable", "1.0.0", "Immutable Test")
            .start("start")
            .end("end")
            .connect("start", "end")
            .build();

        long start = System.nanoTime();
        int iterations = 100_000;

        for (int i = 0; i < iterations; i++) {
            assertNotNull(graph.nodes());
            assertNotNull(graph.edges());
            assertEquals(2, graph.nodes().size());
            assertEquals(1, graph.edges().size());
        }

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        System.out.printf(
            "[BENCH] Immutable access: %d iterations in %d ms%n",
            iterations, elapsedMs
        );

        assertTrue(elapsedMs < 10_000,
            "100K immutable accesses should complete within 10s");
    }
}

package io.github.regalpine.agentforge.server;

import io.github.regalpine.agentforge.event.ExecutionId;
import io.github.regalpine.agentforge.event.ExecutionEvent;

import io.github.regalpine.agentforge.compiler.RegistrySnapshot;
import io.github.regalpine.agentforge.graph.GraphBuilder;
import io.github.regalpine.agentforge.graph.GraphDefinition;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Schema;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.capability.Capability;
import io.github.regalpine.agentforge.capability.CapabilityDefinition;
import io.github.regalpine.agentforge.runtime.InMemoryExecutionPlanRegistry;
import io.github.regalpine.agentforge.server.api.*;
import io.github.regalpine.agentforge.server.health.*;
import io.github.regalpine.agentforge.server.idempotency.IdempotencyStore;
import io.github.regalpine.agentforge.server.service.*;
import io.github.regalpine.agentforge.server.sse.SseEventBuffer;
import io.github.regalpine.agentforge.runtime.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ServerModuleTest {

    private static final Schema ANY_SCHEMA =
        () -> "any";

    private static final Reference CALC_REF =
        new Reference(
            new EntityId("calc"),
            new Version("1.0.0")
        );

    private static final CapabilityDefinition CALC_DEF =
        new CapabilityDefinition(
            new EntityId("calc"),
            new Version("1.0.0"),
            "calc",
            ANY_SCHEMA,
            ANY_SCHEMA,
            null
        );

    private static final RegistrySnapshot SNAPSHOT =
        new RegistrySnapshot(
            Map.of(),
            Map.of(CALC_REF, CALC_DEF),
            Map.of()
        );

    private static final Capability CALC =
        new Capability() {
            @Override public EntityId id() { return new EntityId("calc"); }
            @Override public Version version() { return new Version("1.0.0"); }
            @Override public String name() { return "calc"; }
            @Override public Schema inputSchema() { return ANY_SCHEMA; }
            @Override public Schema outputSchema() { return ANY_SCHEMA; }
        };

    private GraphDefinition buildTestGraph(
        String graphId,
        String version
    ) {
        return GraphBuilder
            .graph(graphId, version, "test")
            .start("start")
            .capability("calc-node", CALC)
            .end("end")
            .connect("start", "calc-node")
            .connect("calc-node", "end")
            .build();
    }

    @Test
    void shouldPublishGraphAndFindPlan() {
        var graphService = new GraphService();

        var graph = buildTestGraph("test-graph", "1.0.0");

        var snapshot = SNAPSHOT;

        graphService.publish(graph, snapshot);

        assertTrue(
            graphService.findPlan(
                "test-graph",
                "1.0.0"
            ).isPresent()
        );
    }

    @Test
    void shouldReturnEmptyForUnknownGraph() {
        var graphService = new GraphService();

        assertFalse(
            graphService.findPlan(
                "unknown",
                "1.0.0"
            ).isPresent()
        );
    }

    @Test
    void shouldSubmitExecution() {
        var graphService = new GraphService();
        var planRegistry =
            new InMemoryExecutionPlanRegistry();
        var idempotencyStore = new IdempotencyStore();

        var execService =
            new ExecutionService(
                graphService,
                planRegistry,
                idempotencyStore
            );

        var graph = buildTestGraph("g1", "1.0.0");

        graphService.publish(
            "tenant-a", graph,
            SNAPSHOT
        );

        ExecutionResponse response =
            execService.submit(
                "tenant-a",
                new ExecutionSubmitRequest(
                    "g1",
                    "1.0.0",
                    Map.of("key", "value")
                ),
                null
            );

        assertNotNull(response.executionId());
        assertEquals("CREATED", response.status());
        assertEquals("g1", response.graphId());
    }

    @Test
    void shouldHandleIdempotentSubmit() {
        var graphService = new GraphService();
        var planRegistry =
            new InMemoryExecutionPlanRegistry();
        var idempotencyStore = new IdempotencyStore();

        var execService =
            new ExecutionService(
                graphService,
                planRegistry,
                idempotencyStore
            );

        var graph = buildTestGraph("g2", "1.0.0");

        graphService.publish(
            "tenant-a", graph,
            SNAPSHOT
        );

        var request =
            new ExecutionSubmitRequest(
                "g2",
                "1.0.0",
                Map.of()
            );

        ExecutionResponse first =
            execService.submit(
                "tenant-a",
                request,
                "idem-key-1"
            );

        ExecutionResponse second =
            execService.submit(
                "tenant-a",
                request,
                "idem-key-1"
            );

        assertEquals(
            first.executionId(),
            second.executionId()
        );
    }

    @Test
    void sameGraphIdAndVersionAreIsolatedInService() {
        var graphs = new GraphService();
        var graph = buildTestGraph("shared", "1.0.0");
        graphs.publish("tenant-a", graph, SNAPSHOT);
        assertTrue(graphs.findGraph("tenant-a", "shared", "1.0.0").isPresent());
        assertTrue(graphs.findGraph("tenant-b", "shared", "1.0.0").isEmpty());
        assertTrue(graphs.findPlan("tenant-b", "shared", "1.0.0").isEmpty());
        var executions = new ExecutionService(graphs, new InMemoryExecutionPlanRegistry(), new IdempotencyStore());
        assertThrows(SecurityException.class, () -> executions.submit("tenant-b",
            new ExecutionSubmitRequest("shared", "1.0.0", Map.of()), "key"));
        graphs.publish("tenant-b", graph, SNAPSHOT);
        assertTrue(graphs.findPlan("tenant-a", "shared", "1.0.0").isPresent());
        assertTrue(graphs.findPlan("tenant-b", "shared", "1.0.0").isPresent());
    }

    @Test
    void serviceRejectsMissingPermissionBeforePublishingOrSubmitting() {
        var graphs = new GraphService();
        var identity = new io.github.regalpine.agentforge.security.Identity("viewer", "test", Set.of(), Set.of(), Map.of());
        var tenant = new io.github.regalpine.agentforge.security.TenantContext("tenant-a", "viewer", Set.of());
        var context = new io.github.regalpine.agentforge.security.SecurityContext(identity, tenant, Instant.now());
        assertThrows(SecurityException.class, () -> graphs.publish(context, buildTestGraph("g", "1.0.0"), SNAPSHOT));
        assertTrue(graphs.findPlan("tenant-a", "g", "1.0.0").isEmpty());
        var executions = new ExecutionService(graphs, new InMemoryExecutionPlanRegistry(), new IdempotencyStore());
        assertThrows(SecurityException.class, () -> executions.submit(context,
            new ExecutionSubmitRequest("g", "1.0.0", Map.of()), null));
    }

    @Test
    void shouldReportLiveness() {
        var aggregator =
            new HealthAggregator(List.of());

        assertEquals(
            "UP",
            aggregator.liveness().status()
        );
    }

    @Test
    void shouldReportReadinessAllUp() {
        var aggregator =
            new HealthAggregator(
                List.of(
                    () -> HealthStatus.healthy("db"),
                    () -> HealthStatus.healthy("cache")
                )
            );

        assertEquals(
            "UP",
            aggregator.readiness().status()
        );
    }

    @Test
    void shouldReportReadinessDown() {
        var aggregator =
            new HealthAggregator(
                List.of(
                    () -> HealthStatus.healthy("db"),
                    () -> HealthStatus.unhealthy(
                        "cache",
                        "connection refused"
                    )
                )
            );

        assertEquals(
            "DOWN",
            aggregator.readiness().status()
        );
    }

    @Test
    void shouldBufferSseEvents() {
        var buffer = new SseEventBuffer();

        var execId = new ExecutionId("exec-1");

        var event1 =
            new ExecutionEvent(
                execId,
                1,
                Instant.now(),
                "NODE_STARTED",
                Map.of("node", "start")
            );

        var event2 =
            new ExecutionEvent(
                execId,
                2,
                Instant.now(),
                "NODE_COMPLETED",
                Map.of("node", "start")
            );

        buffer.add(event1);
        buffer.add(event2);

        var all = buffer.allEvents(execId);
        assertEquals(2, all.size());

        var afterOne =
            buffer.eventsAfter(execId, 1);
        assertEquals(1, afterOne.size());
        assertEquals(
            2,
            afterOne.get(0).sequence()
        );
    }

    @Test
    void shouldReturnEmptyForUnknownExecution() {
        var buffer = new SseEventBuffer();

        var events =
            buffer.allEvents(
                new ExecutionId("unknown")
            );

        assertTrue(events.isEmpty());
    }

    @Test
    void shouldMapApiErrorToStatusCode() {
        var error =
            new ApiError(
                "AF-SEC-001",
                "Unauthorized",
                "req-1",
                false
            );

        var response = ServerResponse.error(error);
        assertEquals(401, response.statusCode());
    }

    @Test
    void shouldMapNotFoundError() {
        var error =
            new ApiError(
                "AF-SRV-404",
                "Not found",
                "req-2",
                false
            );

        var response = ServerResponse.error(error);
        assertEquals(404, response.statusCode());
    }
}

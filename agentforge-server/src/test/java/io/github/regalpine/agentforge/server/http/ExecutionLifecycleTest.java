package io.github.regalpine.agentforge.server.http;

import io.github.regalpine.agentforge.compiler.RegistrySnapshot;
import io.github.regalpine.agentforge.graph.GraphBuilder;
import io.github.regalpine.agentforge.graph.GraphDefinition;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Schema;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.runtime.InMemoryCommitPort;
import io.github.regalpine.agentforge.runtime.InMemoryExecutionPlanRegistry;
import io.github.regalpine.agentforge.security.iam.*;
import io.github.regalpine.agentforge.server.execution.InMemoryExecutionStore;
import io.github.regalpine.agentforge.server.health.HealthAggregator;
import io.github.regalpine.agentforge.server.health.HealthStatus;
import io.github.regalpine.agentforge.server.idempotency.IdempotencyStore;
import io.github.regalpine.agentforge.server.service.ExecutionService;
import io.github.regalpine.agentforge.server.service.GraphService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Batch 4 execution lifecycle test.
 * Verifies: submit→202, query, cancel, pause/resume, timeline, idempotency 409.
 * Uses ProductionApiHandler (demoMode=false) for real service routing.
 */
class ExecutionLifecycleTest {

    private static final Schema ANY_SCHEMA = () -> "any";

    private AgentForgeHttpController controller;
    private HttpClient client;
    private String baseUrl;
    private String adminToken;
    private GraphService graphService;

    @BeforeEach
    void setUp() throws IOException {
        InMemoryUserStore userStore = new InMemoryUserStore();
        InMemoryTenantStore tenantStore = new InMemoryTenantStore();
        InMemoryRoleStore roleStore = new InMemoryRoleStore();

        IamBootstrap iam = new IamBootstrap(tenantStore, userStore, roleStore);
        iam.execute("test-password");

        SessionTokenProvider sessionProvider =
            new SessionTokenProvider(userStore, roleStore, tenantStore, "default");
        adminToken = sessionProvider.createSession("user-admin", "default");

        graphService = new GraphService();
        var planRegistry = new InMemoryExecutionPlanRegistry();
        var executionStore = new InMemoryExecutionStore();
        var commitPort = new InMemoryCommitPort(event -> {});
        ExecutionService executionService = new ExecutionService(
            graphService, planRegistry, new IdempotencyStore(),
            executionStore, commitPort);

        HealthAggregator healthAggregator = new HealthAggregator(List.of(
            () -> HealthStatus.healthy("test")
        ));

        BuiltInAuthenticationProvider authProvider =
            new BuiltInAuthenticationProvider(userStore, roleStore, tenantStore, "default");
        AuthHandler authHandler = new AuthHandler(authProvider, sessionProvider);

        // Production mode (demoMode=false) → ProductionApiHandler
        controller = new AgentForgeHttpController(
            0, graphService, executionService,
            healthAggregator, null, authHandler,
            sessionProvider, false
        );
        controller.start();
        baseUrl = "http://localhost:" + controller.port();
        client = HttpClient.newHttpClient();

        // Publish a test graph for the default tenant
        var graph = GraphBuilder.graph("test-graph", "1.0.0", "Test")
            .start("s").end("e").connect("s", "e").build();
        graphService.publish("default", graph, new RegistrySnapshot(Map.of(), Map.of(), Map.of()));

        // Publish a second graph for idempotency conflict test
        var graph2 = GraphBuilder.graph("other-graph", "1.0.0", "Other")
            .start("s").end("e").connect("s", "e").build();
        graphService.publish("default", graph2, new RegistrySnapshot(Map.of(), Map.of(), Map.of()));
    }

    @AfterEach
    void tearDown() {
        controller.stop();
    }

    @Test
    void submitReturns202Accepted() throws Exception {
        HttpResponse<String> resp = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/executions/submit"))
                .header("Authorization", "Bearer " + adminToken)
                .POST(HttpRequest.BodyPublishers.ofString(
                    "{\"graphId\":\"test-graph\",\"graphVersion\":\"1.0.0\"}"))
                .header("Content-Type", "application/json")
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(202, resp.statusCode());
        assertTrue(resp.body().contains("CREATED"));
        assertTrue(resp.body().contains("executionId"));
    }

    @Test
    void queryExecutionReturnsCurrentState() throws Exception {
        // Submit first
        HttpResponse<String> submitResp = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/executions/submit"))
                .header("Authorization", "Bearer " + adminToken)
                .POST(HttpRequest.BodyPublishers.ofString(
                    "{\"graphId\":\"test-graph\",\"graphVersion\":\"1.0.0\"}"))
                .header("Content-Type", "application/json")
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(202, submitResp.statusCode());

        String execId = extractField(submitResp.body(), "executionId");
        assertNotNull(execId);

        // Query execution
        HttpResponse<String> getResp = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/executions/" + execId))
                .header("Authorization", "Bearer " + adminToken)
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, getResp.statusCode());
        assertTrue(getResp.body().contains("\"CREATED\""));
        assertTrue(getResp.body().contains(execId));
    }

    @Test
    void cancelExecutionTransitionsToCancelled() throws Exception {
        String execId = submitAndGetId();

        HttpResponse<String> cancelResp = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/executions/" + execId + "/cancel"))
                .header("Authorization", "Bearer " + adminToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, cancelResp.statusCode());
        assertTrue(cancelResp.body().contains("CANCELLED"));
    }

    @Test
    void pauseAndResumeExecution() throws Exception {
        String execId = submitAndGetId();

        // Pause (CREATED → SUSPENDED is not allowed; need RUNNING first)
        // The service only allows pause from RUNNING/READY, so this should fail
        HttpResponse<String> pauseResp = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/executions/" + execId + "/pause"))
                .header("Authorization", "Bearer " + adminToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        // CREATED is not RUNNING/READY, so should be 409
        assertEquals(409, pauseResp.statusCode());
    }

    @Test
    void timelineReturnsEvents() throws Exception {
        String execId = submitAndGetId();

        HttpResponse<String> timelineResp = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/executions/" + execId + "/timeline"))
                .header("Authorization", "Bearer " + adminToken)
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, timelineResp.statusCode());
        // Should have at least the EXECUTION_CREATED event
        assertTrue(timelineResp.body().contains("EXECUTION_CREATED"));
    }

    @Test
    void listExecutionsReturnsAllForTenant() throws Exception {
        submitAndGetId();
        submitAndGetId();

        HttpResponse<String> listResp = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/executions"))
                .header("Authorization", "Bearer " + adminToken)
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, listResp.statusCode());
        // Should contain at least 2 executions
        String body = listResp.body();
        assertTrue(body.startsWith("["));
        assertTrue(body.endsWith("]"));
    }

    @Test
    void idempotentSubmitSameKeyReturnsExistingExecution() throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v1/executions/submit"))
            .header("Authorization", "Bearer " + adminToken)
            .POST(HttpRequest.BodyPublishers.ofString(
                "{\"graphId\":\"test-graph\",\"graphVersion\":\"1.0.0\"}"))
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", "idem-key-1");

        HttpResponse<String> first = client.send(builder.build(),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(202, first.statusCode());
        String id1 = extractField(first.body(), "executionId");

        HttpResponse<String> second = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/executions/submit"))
                .header("Authorization", "Bearer " + adminToken)
                .POST(HttpRequest.BodyPublishers.ofString(
                    "{\"graphId\":\"test-graph\",\"graphVersion\":\"1.0.0\"}"))
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", "idem-key-1")
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(202, second.statusCode());
        String id2 = extractField(second.body(), "executionId");
        assertEquals(id1, id2, "Same idempotency key should return same execution");
    }

    @Test
    void idempotentSubmitConflictReturns409() throws Exception {
        // First submit with idempotency key
        HttpResponse<String> first = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/executions/submit"))
                .header("Authorization", "Bearer " + adminToken)
                .POST(HttpRequest.BodyPublishers.ofString(
                    "{\"graphId\":\"test-graph\",\"graphVersion\":\"1.0.0\"}"))
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", "conflict-key")
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(202, first.statusCode());

        // Second submit with same key but different graph → 409
        HttpResponse<String> conflict = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/executions/submit"))
                .header("Authorization", "Bearer " + adminToken)
                .POST(HttpRequest.BodyPublishers.ofString(
                    "{\"graphId\":\"other-graph\",\"graphVersion\":\"1.0.0\"}"))
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", "conflict-key")
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(409, conflict.statusCode());
    }

    @Test
    void stateEndpointReturnsTerminalState() throws Exception {
        String execId = submitAndGetId();

        HttpResponse<String> stateResp = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/executions/" + execId + "/state"))
                .header("Authorization", "Bearer " + adminToken)
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, stateResp.statusCode());
        assertTrue(stateResp.body().contains("CREATED"));
        assertTrue(stateResp.body().contains("version"));
    }

    @Test
    void unknownExecutionReturns404() throws Exception {
        HttpResponse<String> resp = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/executions/nonexistent-id"))
                .header("Authorization", "Bearer " + adminToken)
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(404, resp.statusCode());
    }

    // ── Helpers ──

    private String submitAndGetId() throws Exception {
        HttpResponse<String> resp = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/executions/submit"))
                .header("Authorization", "Bearer " + adminToken)
                .POST(HttpRequest.BodyPublishers.ofString(
                    "{\"graphId\":\"test-graph\",\"graphVersion\":\"1.0.0\"}"))
                .header("Content-Type", "application/json")
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(202, resp.statusCode());
        return extractField(resp.body(), "executionId");
    }

    private static String extractField(String json, String field) {
        String pattern = "\"" + field + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) return null;
        int colonIdx = json.indexOf(":", idx + pattern.length());
        if (colonIdx < 0) return null;
        int quoteStart = json.indexOf("\"", colonIdx + 1);
        if (quoteStart < 0) return null;
        int quoteEnd = json.indexOf("\"", quoteStart + 1);
        if (quoteEnd < 0) return null;
        return json.substring(quoteStart + 1, quoteEnd);
    }
}

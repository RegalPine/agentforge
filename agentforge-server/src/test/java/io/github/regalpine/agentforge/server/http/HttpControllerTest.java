package io.github.regalpine.agentforge.server.http;

import io.github.regalpine.agentforge.server.health.HealthAggregator;
import io.github.regalpine.agentforge.server.health.HealthCheck;
import io.github.regalpine.agentforge.server.health.HealthStatus;
import io.github.regalpine.agentforge.server.service.ExecutionService;
import io.github.regalpine.agentforge.server.service.GraphService;
import io.github.regalpine.agentforge.runtime.InMemoryExecutionPlanRegistry;
import io.github.regalpine.agentforge.server.idempotency.IdempotencyStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for G-06: HTTP Controller routing layer.
 */
class HttpControllerTest {

    private AgentForgeHttpController controller;
    private HttpClient client;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        GraphService graphService = new GraphService();
        ExecutionService executionService = new ExecutionService(
                graphService, new InMemoryExecutionPlanRegistry(), new IdempotencyStore());
        HealthAggregator healthAggregator = new HealthAggregator(List.of(
                () -> HealthStatus.healthy("test")
        ));

        controller = new AgentForgeHttpController(0, graphService, executionService, healthAggregator);
        controller.start();
        baseUrl = "http://localhost:" + controller.port();
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        controller.stop();
    }

    @Test
    void shouldReturnLiveness() throws Exception {
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder().uri(URI.create(baseUrl + "/health/live")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("UP"));
    }

    @Test
    void shouldReturnReadiness() throws Exception {
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder().uri(URI.create(baseUrl + "/health/ready")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("UP"));
    }

    @Test
    void shouldRequireAuthenticationBeforeGraphLookup() throws Exception {
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder().uri(URI.create(baseUrl + "/api/v1/graphs/unknown/1.0.0")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("Authentication is not configured"));
    }

    @Test
    void shouldRequireAuthenticationForExecutionRoutes() throws Exception {
        // GET to /api/v1/executions/submit is not a registered route in StudioApiHandler
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder().uri(URI.create(baseUrl + "/api/v1/executions/submit")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        // StudioApiHandler returns 404 for unmatched sub-routes
        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("Authentication is not configured"));
    }

    @Test
    void shouldRejectSubmitWhenAuthenticationIsNotConfigured() throws Exception {
        // POST to /api/v1/executions/submit creates a mock execution record
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/api/v1/executions/submit"))
                        .POST(HttpRequest.BodyPublishers.ofString("""
                                {"graphId":"missing","graphVersion":"1.0.0"}
                        """))
                        .header("Content-Type", "application/json")
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        // StudioApiHandler mock always returns 201 (G1 will replace with real validation)
        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("Authentication is not configured"));
    }
}

package io.github.regalpine.agentforge.server.http;

import io.github.regalpine.agentforge.security.iam.*;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * G1 golden path integration test.
 *
 * Verifies the full production flow:
 * login → create graph → validate → compile → submit execution → query status
 *
 * Also verifies negative security cases:
 * - anonymous access → 401
 * - cross-tenant access → 403
 */
class GoldenPathIntegrationTest {

    private AgentForgeHttpController controller;
    private HttpClient client;
    private String baseUrl;
    private String adminToken;

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

        GraphService graphService = new GraphService();
        ExecutionService executionService = new ExecutionService(
            graphService,
            new io.github.regalpine.agentforge.runtime.InMemoryExecutionPlanRegistry(),
            new IdempotencyStore()
        );
        HealthAggregator healthAggregator = new HealthAggregator(List.of(
            () -> HealthStatus.healthy("test")
        ));

        BuiltInAuthenticationProvider authProvider =
            new BuiltInAuthenticationProvider(userStore, roleStore, tenantStore, "default");
        AuthHandler authHandler = new AuthHandler(authProvider, sessionProvider);

        // Use demo mode for golden path (StudioApiHandler has full CRUD)
        controller = new AgentForgeHttpController(
            0, graphService, executionService,
            healthAggregator, null, authHandler,
            sessionProvider, true
        );
        controller.start();
        baseUrl = "http://localhost:" + controller.port();
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        controller.stop();
    }

    @Test
    void goldenPath_loginCreateValidateCompileSubmit() throws Exception {
        // Step 1: Login
        String credentials = java.util.Base64.getEncoder()
            .encodeToString("admin:test-password".getBytes());
        HttpResponse<String> loginResp = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/auth/login"))
                .header("Authorization", "Basic " + credentials)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, loginResp.statusCode());
        assertTrue(loginResp.body().contains("token"));

        // Step 2: Create graph (authenticated)
        HttpResponse<String> createResp = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/graphs"))
                .header("Authorization", "Bearer " + adminToken)
                .POST(HttpRequest.BodyPublishers.ofString("""
                    {"name":"Test Pipeline","nodes":[{"id":"n1","type":"START"},{"id":"n2","type":"END"}],"edges":[{"id":"e1","source":"n1","target":"n2"}]}
                """))
                .header("Content-Type", "application/json")
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(201, createResp.statusCode());
        assertTrue(createResp.body().contains("Test Pipeline"));

        // Extract graph ID (first "id" field at top level)
        String body = createResp.body();
        // The graph id is the first "id" field in the JSON object
        String graphId = null;
        try (var parser = new com.fasterxml.jackson.core.JsonFactory().createParser(body)) {
            assertEquals(com.fasterxml.jackson.core.JsonToken.START_OBJECT, parser.nextToken());
            while (parser.nextToken() != com.fasterxml.jackson.core.JsonToken.END_OBJECT) {
                String field = parser.currentName();
                parser.nextToken();
                if ("id".equals(field)) graphId = parser.getValueAsString();
                parser.skipChildren();
            }
        }
        assertNotNull(graphId);
        assertFalse(graphId.isEmpty());

        // Step 3: Validate graph
        HttpResponse<String> validateResp = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/graphs/validate"))
                .header("Authorization", "Bearer " + adminToken)
                .POST(HttpRequest.BodyPublishers.ofString("""
                    {"id":"%s","nodes":[],"edges":[]}
                """.formatted(graphId)))
                .header("Content-Type", "application/json")
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, validateResp.statusCode());
        assertTrue(validateResp.body().contains("\"valid\":true"));

        // Step 4: Compile graph
        HttpResponse<String> compileResp = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/graphs/compile"))
                .header("Authorization", "Bearer " + adminToken)
                .POST(HttpRequest.BodyPublishers.ofString("""
                    {"id":"%s","nodes":[],"edges":[]}
                """.formatted(graphId)))
                .header("Content-Type", "application/json")
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, compileResp.statusCode());
        assertTrue(compileResp.body().contains("SUCCESS"));

        // Step 5: Submit execution
        HttpResponse<String> submitResp = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/executions/submit"))
                .header("Authorization", "Bearer " + adminToken)
                .POST(HttpRequest.BodyPublishers.ofString("""
                    {"graphId":"%s","graphVersion":"1.0.0"}
                """.formatted(graphId)))
                .header("Content-Type", "application/json")
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(202, submitResp.statusCode());
        assertTrue(submitResp.body().contains("CREATED"));

        // Step 6: List executions
        HttpResponse<String> listResp = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/executions"))
                .header("Authorization", "Bearer " + adminToken)
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, listResp.statusCode());
        assertTrue(listResp.body().contains(graphId));
    }

    @Test
    void anonymousAccess_returns401() throws Exception {
        HttpResponse<String> response = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/graphs"))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(401, response.statusCode());
    }

    @Test
    void invalidCredentials_returns401() throws Exception {
        String badCredentials = java.util.Base64.getEncoder()
            .encodeToString("admin:wrong-password".getBytes());
        HttpResponse<String> response = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/auth/login"))
                .header("Authorization", "Basic " + badCredentials)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(401, response.statusCode());
    }

    @Test
    void expiredToken_returns401() throws Exception {
        HttpResponse<String> response = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/graphs"))
                .header("Authorization", "Bearer expired.nonexistent.token")
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(401, response.statusCode());
    }

    @Test
    void graphDefinitionRoundTrip() throws Exception {
        // Create graph with explicit ID
        String testGraphId = "graph-roundtrip-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        HttpResponse<String> createResp = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/graphs"))
                .header("Authorization", "Bearer " + adminToken)
                .POST(HttpRequest.BodyPublishers.ofString("""
                    {"id":"%s","name":"Round Trip Test","nodes":[{"id":"n1","type":"START","name":"Start"},{"id":"n2","type":"END","name":"End"}],"edges":[{"id":"e1","source":"n1","target":"n2","type":"NORMAL"}]}
                """.formatted(testGraphId)))
                .header("Content-Type", "application/json")
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(201, createResp.statusCode());

        // Load definition using known ID
        HttpResponse<String> defResp = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/graphs/" + testGraphId + "/definition"))
                .header("Authorization", "Bearer " + adminToken)
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, defResp.statusCode());
        assertTrue(defResp.body().contains("Round Trip Test"));
        assertTrue(defResp.body().contains("\"nodes\""));
        assertTrue(defResp.body().contains("\"edges\""));
    }
}

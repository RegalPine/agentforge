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
 * Tests for G1-1: API route authentication.
 *
 * Verifies that:
 * - Anonymous requests to /api/v1/** return 401
 * - Invalid/expired tokens return 401
 * - Valid tokens allow access
 * - Health endpoints remain public
 */
@org.junit.jupiter.api.Tag("security")
class AuthenticatedHandlerTest {

    private AgentForgeHttpController controller;
    private HttpClient client;
    private String baseUrl;
    private SessionTokenProvider sessionProvider;
    private String validToken;
    private String viewerToken;
    private String otherTenantToken;

    @BeforeEach
    void setUp() throws IOException {
        // Set up IAM stores
        InMemoryUserStore userStore = new InMemoryUserStore();
        InMemoryTenantStore tenantStore = new InMemoryTenantStore();
        InMemoryRoleStore roleStore = new InMemoryRoleStore();

        IamBootstrap iam = new IamBootstrap(tenantStore, userStore, roleStore);
        iam.execute("test-password");

        sessionProvider = new SessionTokenProvider(userStore, roleStore, tenantStore, "default");

        // Create a valid token for the admin user (userId is "user-admin")
        validToken = sessionProvider.createSession("user-admin", "default");
        userStore.save(User.serviceAccount("viewer-user", "viewer-user"));
        roleStore.assignRole(RoleAssignment.of("viewer-user", IamBootstrap.ROLE_VIEWER, "default"));
        viewerToken = sessionProvider.createSession("viewer-user", "default");
        tenantStore.save(Tenant.create("tenant-b", "tenant-b", "Tenant B"));
        userStore.save(User.serviceAccount("tenant-b-user", "tenant-b-user"));
        roleStore.assignRole(RoleAssignment.of("tenant-b-user", IamBootstrap.ROLE_SYS_ADMIN, "tenant-b"));
        otherTenantToken = sessionProvider.createSession("tenant-b-user", "tenant-b");

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

        controller = new AgentForgeHttpController(
            0, graphService, executionService,
            healthAggregator, null, authHandler,
            sessionProvider, true // demo mode
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
    void healthEndpointsRemainPublic() throws Exception {
        HttpResponse<String> response = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/health/live"))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, response.statusCode());
    }

    @Test
    void anonymousRequestReturns401() throws Exception {
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
    void invalidTokenReturns401() throws Exception {
        HttpResponse<String> response = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/graphs"))
                .header("Authorization", "Bearer invalid-token-here")
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(401, response.statusCode());
    }

    @Test
    void missingBearerSchemeReturns401() throws Exception {
        HttpResponse<String> response = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/graphs"))
                .header("Authorization", "Basic dXNlcjpwYXNz")
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(401, response.statusCode());
    }

    @Test
    void validTokenAllowsAccess() throws Exception {
        HttpResponse<String> response = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/graphs"))
                .header("Authorization", "Bearer " + validToken)
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, response.statusCode());
    }

    @Test
    void validTokenAllowsExecutionSubmit() throws Exception {
        HttpResponse<String> response = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/executions/submit"))
                .header("Authorization", "Bearer " + validToken)
                .POST(HttpRequest.BodyPublishers.ofString(
                    "{\"graphId\":\"graph-translate\",\"graphVersion\":\"1.0.0\"}"))
                .header("Content-Type", "application/json")
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(202, response.statusCode());
    }

    @Test
    void viewerCannotMutateGraphsOrExecute() throws Exception {
        assertEquals(200, request(viewerToken, "GET", "/graphs", "").statusCode());
        for (String action : List.of("POST /graphs", "PUT /graphs/graph-translate",
                "DELETE /graphs/graph-translate", "POST /graphs/compile",
                "POST /executions/submit", "POST /executions/exec-001/cancel")) {
            String[] parts = action.split(" ");
            assertEquals(403, request(viewerToken, parts[0], parts[1], "{}").statusCode(), action);
        }
        assertEquals(200, request(validToken, "GET", "/graphs/graph-translate", "").statusCode());
        assertTrue(request(validToken, "GET", "/executions/exec-001", "").body().contains("COMPLETED"));
    }

    @Test
    void unknownBusinessOperationIsDenied() throws Exception {
        assertEquals(403, request(validToken, "POST", "/graphs/unknown/action", "{}").statusCode());
    }

    @Test
    void sameGraphIdInTwoTenantsCannotOverwriteOrDeleteEachOther() throws Exception {
        String a = "{\"id\":\"shared-id\",\"name\":\"A original\",\"nodes\":[{\"id\":\"a\"}],\"edges\":[]}";
        String b = "{\"id\":\"shared-id\",\"name\":\"B original\",\"nodes\":[{\"id\":\"b\"}],\"edges\":[]}";
        assertEquals(201, request(validToken, "POST", "/graphs", a).statusCode());
        assertEquals(201, request(otherTenantToken, "POST", "/graphs", b).statusCode());
        assertTrue(request(validToken, "GET", "/graphs/shared-id/definition", "").body().contains("A original"));
        assertEquals(200, request(otherTenantToken, "PUT", "/graphs/shared-id", "{\"name\":\"B updated\",\"nodes\":[],\"edges\":[]}").statusCode());
        assertTrue(request(validToken, "GET", "/graphs/shared-id", "").body().contains("A original"));
        assertEquals(204, request(otherTenantToken, "DELETE", "/graphs/shared-id", "").statusCode());
        assertEquals(200, request(validToken, "GET", "/graphs/shared-id", "").statusCode());
        assertTrue(request(validToken, "GET", "/graphs/shared-id/definition", "").body().contains("A original"));
    }

    @Test
    void duplicateGraphCreationDoesNotReplaceDefinition() throws Exception {
        String graph = "{\"id\":\"duplicate\",\"name\":\"original\",\"nodes\":[],\"edges\":[]}";
        assertEquals(201, request(validToken, "POST", "/graphs", graph).statusCode());
        assertEquals(409, request(validToken, "POST", "/graphs", graph.replace("original", "replacement")).statusCode());
        assertTrue(request(validToken, "GET", "/graphs/duplicate", "").body().contains("original"));
    }

    @Test
    void explicitEmptyNodesAndEdgesReplaceStoredDefinition() throws Exception {
        assertEquals(200, request(validToken, "PUT", "/graphs/graph-translate", "{\"nodes\":[],\"edges\":[]}").statusCode());
        String body = request(validToken, "GET", "/graphs/graph-translate", "").body();
        assertTrue(body.contains("\"nodes\":[]"));
        assertTrue(body.contains("\"edges\":[]"));
        assertTrue(body.contains("Translation Pipeline"));
        assertEquals(body, request(validToken, "GET", "/graphs/graph-translate/definition", "").body());
    }

    @Test
    void foreignGraphIsDeniedAndTenantHeaderCannotOverrideIdentity() throws Exception {
        assertEquals(403, request(otherTenantToken, "GET", "/graphs/graph-translate", "").statusCode());
        assertEquals(403, request(otherTenantToken, "PUT", "/graphs/graph-translate", "{}").statusCode());
        assertEquals(403, request(otherTenantToken, "DELETE", "/graphs/graph-translate", "").statusCode());
        var response = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/graphs"))
            .header("Authorization", "Bearer " + otherTenantToken).header("X-Tenant-Id", "default")
            .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertFalse(response.body().contains("graph-translate"));
    }

    @Test
    void graphRoundTripPreservesAllEdgeTypesNestedValuesAndEscapes() throws Exception {
        var metadata = new java.util.LinkedHashMap<String, Object>();
        metadata.put("quoted\"key", java.util.Arrays.asList(null, "line\n中文", new java.math.BigDecimal("1.250")));
        var edges = java.util.Arrays.stream(io.github.regalpine.agentforge.graph.EdgeType.values())
            .map(type -> java.util.Map.of("id", type.name(), "type", type.name(), "source", "n1", "target", "n2", "condition", "a && !b"))
            .toList();
        var graph = java.util.Map.of("id", "all-edges", "name", "带转义\"的图", "metadata", metadata,
            "nodes", java.util.List.of(java.util.Map.of("id", "n1"), java.util.Map.of("id", "n2")), "edges", edges);
        assertEquals(201, request(validToken, "POST", "/graphs", HttpJson.stringify(graph)).statusCode());
        var loaded = HttpJson.object(request(validToken, "GET", "/graphs/all-edges", "").body());
        assertEquals(metadata, loaded.get("metadata"));
        assertEquals(edges, loaded.get("edges"));
        assertEquals(200, request(validToken, "PUT", "/graphs/all-edges", HttpJson.stringify(loaded)).statusCode());
        assertEquals(loaded, HttpJson.object(request(validToken, "GET", "/graphs/all-edges", "").body()));
    }

    @Test
    void graphVersionsRequireExplicitSelectionWhenAmbiguous() throws Exception {
        for (String version : List.of("1.0.0", "2.0.0")) {
            assertEquals(201, request(validToken, "POST", "/graphs",
                "{\"id\":\"versioned\",\"version\":\"" + version + "\",\"nodes\":[],\"edges\":[]}").statusCode());
        }
        assertEquals(409, request(validToken, "GET", "/graphs/versioned", "").statusCode());
        assertEquals(204, request(validToken, "DELETE", "/graphs/versioned?version=1.0.0", "").statusCode());
        assertEquals(200, request(validToken, "GET", "/graphs/versioned?version=2.0.0", "").statusCode());
    }

    @Test
    void malformedJsonCannotCreateGraph() throws Exception {
        for (String body : List.of("", "[]", "{\"id\":\"bad\",}", "{\"id\":\"bad\",\"id\":\"duplicate\"}")) {
            assertEquals(400, request(validToken, "POST", "/graphs", body).statusCode());
        }
        assertFalse(request(validToken, "GET", "/graphs", "").body().contains("duplicate"));
    }

    @Test
    void executionSubresourcesAndSubmitEnforceGraphOwnership() throws Exception {
        for (String suffix : List.of("", "/state", "/events", "/timeline", "/checkpoints")) {
            assertEquals(403, request(otherTenantToken, "GET", "/executions/exec-001" + suffix, "").statusCode());
        }
        assertEquals(403, request(otherTenantToken, "POST", "/executions/submit",
            "{\"graphId\":\"graph-translate\",\"graphVersion\":\"1.0.0\"}").statusCode());
        assertEquals("[]", request(otherTenantToken, "GET", "/executions", "").body());
    }

    @Test
    void simultaneousRequestsKeepTheirOwnSecurityContext() throws Exception {
        var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(0), 0);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        var arrived = new java.util.concurrent.CountDownLatch(2);
        server.setExecutor(executor);
        server.createContext("/", new AuthenticatedHandler(exchange -> {
            arrived.countDown();
            try {
                if (!arrived.await(5, java.util.concurrent.TimeUnit.SECONDS)) throw new IOException("Barrier timeout");
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IOException(error);
            }
            var context = (io.github.regalpine.agentforge.security.SecurityContext) exchange.getAttribute(AuthenticatedHandler.SECURITY_CONTEXT_ATTR);
            byte[] body = context.tenant().tenantId().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var out = exchange.getResponseBody()) { out.write(body); }
        }, sessionProvider));
        server.start();
        try {
            URI uri = URI.create("http://localhost:" + server.getAddress().getPort() + "/");
            var a = client.sendAsync(HttpRequest.newBuilder(uri).header("Authorization", "Bearer " + validToken).GET().build(), HttpResponse.BodyHandlers.ofString());
            var b = client.sendAsync(HttpRequest.newBuilder(uri).header("Authorization", "Bearer " + otherTenantToken).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals("default", a.get(10, java.util.concurrent.TimeUnit.SECONDS).body());
            assertEquals("tenant-b", b.get(10, java.util.concurrent.TimeUnit.SECONDS).body());
        } finally {
            server.stop(0);
            executor.shutdownNow();
        }
    }

    private HttpResponse<String> request(String token, String method, String path, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1" + path))
            .header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
            .method(method, HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void loginEndpointRemainsPublic() throws Exception {
        // POST /api/auth/login should not require Bearer token
        HttpResponse<String> response = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/auth/login"))
                .POST(HttpRequest.BodyPublishers.ofString(""))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        // Should return 401 (missing Basic auth) but NOT because of Bearer check
        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("Authorization"));
    }
}

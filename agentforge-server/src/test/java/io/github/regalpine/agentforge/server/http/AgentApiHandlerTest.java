package io.github.regalpine.agentforge.server.http;

import io.github.regalpine.agentforge.agent.AgentDefinition;
import io.github.regalpine.agentforge.agent.AgentRegistrar;
import io.github.regalpine.agentforge.agent.RegistrationOptions;
import io.github.regalpine.agentforge.event.InMemoryEventPublisher;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.server.agent.AgentForgeAgentRegistrar;
import io.github.regalpine.agentforge.server.health.HealthAggregator;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HTTP endpoint contract tests for
 * {@link AgentApiHandler} via
 * {@link AgentForgeHttpController}.
 */
class AgentApiHandlerTest {

    private AgentForgeHttpController controller;
    private AgentRegistrar registrar;
    private HttpClient client;
    private String base;

    @BeforeEach
    void setUp() throws IOException {
        var events = new InMemoryEventPublisher();
        registrar = new AgentForgeAgentRegistrar(events);

        GraphService gs = new GraphService();
        ExecutionService es = new ExecutionService(
            gs, new InMemoryExecutionPlanRegistry(),
            new IdempotencyStore());
        HealthAggregator ha = new HealthAggregator(
            List.of(() -> HealthStatus.healthy("test")));

        controller = new AgentForgeHttpController(
            0, gs, es, ha, null, null, null, true,
            registrar);
        controller.start();
        base = "http://localhost:" + controller.port();
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        controller.stop();
    }

    private AgentDefinition def(String id, String ver) {
        return new AgentDefinition(
            new EntityId(id), new Version(ver),
            "agent-" + id, Set.of());
    }

    // ── GET /api/v1/agents ──

    @Test
    void shouldReturnEmptyList() throws Exception {
        var resp = get("/api/v1/agents");
        assertEquals(200, resp.statusCode());
        assertEquals("[]", resp.body());
    }

    @Test
    void shouldReturnRegisteredAgents() throws Exception {
        registrar.register(def("x1", "1.0.0"));
        registrar.register(def("x2", "2.0.0"));

        var resp = get("/api/v1/agents");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("x1"));
        assertTrue(resp.body().contains("x2"));
    }

    // ── GET /api/v1/agents/summaries ──

    @Test
    void shouldReturnSummaries() throws Exception {
        registrar.registerAndStart(
            def("s1", "1.0.0"),
            RegistrationOptions.defaults());

        var resp = get("/api/v1/agents/summaries");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("s1"));
        assertTrue(resp.body().contains("ACTIVE"));
    }

    // ── GET /api/v1/agents/{id}/overview ──

    @Test
    void shouldReturnOverview() throws Exception {
        registrar.registerAndStart(
            def("o1", "1.0.0"),
            RegistrationOptions.defaults());

        var resp = get("/api/v1/agents/o1/overview");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("metrics"));
        assertTrue(resp.body().contains("status"));
    }

    @Test
    void shouldReturn404ForMissingOverview() throws Exception {
        var resp = get("/api/v1/agents/missing/overview");
        assertEquals(404, resp.statusCode());
        assertTrue(resp.body().contains("not found"));
    }

    // ── GET /api/v1/agents/{id}/health ──

    @Test
    void shouldReturnHealth() throws Exception {
        registrar.register(def("h1", "1.0.0"));
        var resp = get("/api/v1/agents/h1/health");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("UP"));
    }

    // ── GET /api/v1/agents/{id}/metrics ──

    @Test
    void shouldReturnMetrics() throws Exception {
        registrar.register(def("m1", "1.0.0"));
        var resp = get("/api/v1/agents/m1/metrics");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("invocations"));
    }

    // ── POST /api/v1/agents ──

    @Test
    void shouldRegisterViaPost() throws Exception {
        var resp = post("/api/v1/agents",
            """
            {"id":"p1","version":"1.0.0","name":"posted"}
            """);
        assertEquals(201, resp.statusCode());
        assertTrue(resp.body().contains("p1"));
    }

    @Test
    void shouldRejectPostWithMissingFields() throws Exception {
        var resp = post("/api/v1/agents",
            """
            {"id":"p2"}
            """);
        assertEquals(400, resp.statusCode());
    }

    @Test
    void shouldRejectDuplicateRegistration() throws Exception {
        registrar.register(def("dup", "1.0.0"));
        var resp = post("/api/v1/agents",
            """
            {"id":"dup","version":"1.0.0","name":"dup"}
            """);
        assertEquals(409, resp.statusCode());
    }

    // ── POST /api/v1/agents/{id}/start|stop|drain ──

    @Test
    void shouldStartAgent() throws Exception {
        registrar.register(def("lc1", "1.0.0"));
        var resp = post("/api/v1/agents/lc1/start", "");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("start"));
    }

    @Test
    void shouldStopAgent() throws Exception {
        registrar.registerAndStart(
            def("lc2", "1.0.0"),
            RegistrationOptions.defaults());
        var resp = post("/api/v1/agents/lc2/stop", "");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("stop"));
    }

    @Test
    void shouldReturn404ForUnknownAgentStart() throws Exception {
        var resp = post("/api/v1/agents/nope/start", "");
        assertEquals(404, resp.statusCode());
    }

    // ── DELETE /api/v1/agents/{id}/{version} ──

    @Test
    void shouldUnregisterAgent() throws Exception {
        registrar.register(def("del", "1.0.0"));
        var resp = delete("/api/v1/agents/del/1.0.0");
        assertEquals(204, resp.statusCode());
        assertTrue(registrar.list().isEmpty());
    }

    // ── GET /api/v1/agents/{id}/definition ──

    @Test
    void shouldReturnDefinition() throws Exception {
        registrar.register(def("d1", "1.0.0"));
        var resp = get("/api/v1/agents/d1/definition");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("d1"));
    }

    // ── GET /api/v1/agents/{id}/versions ──

    @Test
    void shouldReturnVersions() throws Exception {
        registrar.register(def("v1", "1.0.0"));
        registrar.register(def("v1", "2.0.0"));
        var resp = get("/api/v1/agents/v1/versions");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("1.0.0"));
        assertTrue(resp.body().contains("2.0.0"));
    }

    // ── Unknown route ──

    @Test
    void shouldReturn404ForUnknownRoute() throws Exception {
        var resp = get("/api/v1/agents/x/y/z");
        assertEquals(404, resp.statusCode());
    }

    // ── helpers ──

    private HttpResponse<String> get(String path)
        throws Exception {
        return client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(base + path))
                .GET().build(),
            HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(
        String path, String body
    ) throws Exception {
        return client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(base + path))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build(),
            HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> delete(String path)
        throws Exception {
        return client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(base + path))
                .DELETE().build(),
            HttpResponse.BodyHandlers.ofString());
    }
}

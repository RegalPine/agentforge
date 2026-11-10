package io.github.regalpine.agentforge.server.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.github.regalpine.agentforge.security.iam.SessionTokenProvider;
import io.github.regalpine.agentforge.server.api.ExecutionResponse;
import io.github.regalpine.agentforge.server.api.ExecutionSubmitRequest;
import io.github.regalpine.agentforge.server.health.HealthAggregator;
import io.github.regalpine.agentforge.server.health.HealthStatus;
import io.github.regalpine.agentforge.server.service.ExecutionService;
import io.github.regalpine.agentforge.server.service.GraphService;
import io.github.regalpine.agentforge.server.service.SkillService;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * Lightweight HTTP controller that maps REST endpoints to the AgentForge service layer.
 * Uses JDK built-in {@link com.sun.net.httpserver.HttpServer} — no framework dependency.
 *
 * <p>G-06: HTTP Controller routing layer.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>GET  /health/live     — Liveness probe</li>
 *   <li>GET  /health/ready    — Readiness probe</li>
 *   <li>POST /api/v1/graphs/{graphId}/{version}/validate — Validate graph</li>
 *   <li>POST /api/v1/graphs/{graphId}/{version}/compile  — Compile graph</li>
 *   <li>POST /api/v1/executions/submit — Submit execution</li>
 *   <li>GET  /api/v1/executions/{executionId} — Get execution status</li>
 * </ul>
 */
public final class AgentForgeHttpController {

    private final HttpServer server;
    private final java.util.concurrent.ExecutorService executor;
    private final GraphService graphService;
    private final ExecutionService executionService;
    private final HealthAggregator healthAggregator;
    private final SkillService skillService;
    private final AuthHandler authHandler;
    private final SessionTokenProvider sessionProvider;
    private final boolean demoMode;
    private final io.github.regalpine.agentforge.agent.AgentRegistrar agentRegistrar;

    public AgentForgeHttpController(
        int port,
        GraphService graphService,
        ExecutionService executionService,
        HealthAggregator healthAggregator
    ) throws IOException {
        this(port, graphService, executionService,
            healthAggregator, null);
    }

    public AgentForgeHttpController(
        int port,
        GraphService graphService,
        ExecutionService executionService,
        HealthAggregator healthAggregator,
        SkillService skillService
    ) throws IOException {
        this(port, graphService, executionService,
            healthAggregator, skillService, null);
    }

    public AgentForgeHttpController(
        int port,
        GraphService graphService,
        ExecutionService executionService,
        HealthAggregator healthAggregator,
        SkillService skillService,
        AuthHandler authHandler
    ) throws IOException {
        this(port, graphService, executionService,
            healthAggregator, skillService, authHandler, null, true);
    }

    /**
     * Full constructor with auth and demo/production mode control.
     *
     * @param sessionProvider token validator for API auth (null = no auth)
     * @param demoMode true to use StudioApiHandler mock data, false for real services
     */
    public AgentForgeHttpController(
        int port,
        GraphService graphService,
        ExecutionService executionService,
        HealthAggregator healthAggregator,
        SkillService skillService,
        AuthHandler authHandler,
        SessionTokenProvider sessionProvider,
        boolean demoMode
    ) throws IOException {
        this(port, graphService, executionService,
            healthAggregator, skillService, authHandler,
            sessionProvider, demoMode, null);
    }

    /**
     * Full constructor with Agent registrar.
     *
     * <p>When {@code agentRegistrar} is non-null,
     * {@code /api/v1/agents} is served by
     * {@link AgentApiHandler} backed by real
     * platform state; otherwise the Studio
     * mock handler is used.
     */
    public AgentForgeHttpController(
        int port,
        GraphService graphService,
        ExecutionService executionService,
        HealthAggregator healthAggregator,
        SkillService skillService,
        AuthHandler authHandler,
        SessionTokenProvider sessionProvider,
        boolean demoMode,
        io.github.regalpine.agentforge.agent.AgentRegistrar agentRegistrar
    ) throws IOException {
        this.graphService = graphService;
        this.executionService = executionService;
        this.healthAggregator = healthAggregator;
        this.skillService = skillService;
        this.authHandler = authHandler;
        this.sessionProvider = sessionProvider;
        this.demoMode = demoMode;
        this.agentRegistrar = agentRegistrar;

        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.executor = Executors.newCachedThreadPool();
        this.server.setExecutor(executor);

        registerRoutes();
    }

    private void registerRoutes() {
        // Health endpoints (public, no auth)
        server.createContext("/health/live", new LivenessHandler());
        server.createContext("/health/ready", new ReadinessHandler());

        // Auth endpoint (public, handled by AuthHandler)
        if (authHandler != null) {
            server.createContext("/api/auth", authHandler);
        }

        // Studio API - authenticated when sessionProvider is present
        HttpHandler studioHandler = demoMode
            ? new StudioApiHandler()
            : new ProductionApiHandler(graphService, executionService);

        HttpHandler apiHandler = sessionProvider != null
            ? new AuthenticatedHandler(studioHandler, sessionProvider, ApiPermissions::required)
            : exchange -> sendJson(exchange, 401, "{\"error\":\"Authentication is not configured\"}");

        server.createContext("/api/v1/graphs", apiHandler);
        server.createContext("/api/v1/executions", apiHandler);

        // Agent API — dedicated handler when registrar attached
        if (agentRegistrar != null) {
            server.createContext("/api/v1/agents",
                new AgentApiHandler(agentRegistrar));
        } else {
            server.createContext("/api/v1/agents", apiHandler);
        }

        server.createContext("/api/v1/skills", apiHandler);
        server.createContext("/api/v1/capabilities", apiHandler);
        server.createContext("/api/v1/memories", apiHandler);
        server.createContext("/api/v1/knowledge", apiHandler);
        server.createContext("/api/v1/mcp", apiHandler);
        server.createContext("/api/v1/governance", apiHandler);
        server.createContext("/api/v1/evaluations", apiHandler);
        server.createContext("/api/v1/observability", apiHandler);
        server.createContext("/api/v1/human-approvals", apiHandler);
        server.createContext("/api/v1/search", apiHandler);
        server.createContext("/api/v1/diff", apiHandler);
        server.createContext("/api/v1/versions", apiHandler);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
        executor.shutdownNow();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    // ── Health Handlers ──

    private class LivenessHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendJson(exchange, 200, """
                {"status":"UP"}
            """);
        }
    }

    private class ReadinessHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            HealthStatus status = healthAggregator.readiness();
            int code = "UP".equals(status.status()) ? 200 : 503;
            sendJson(exchange, code, """
                {"status":"%s","component":"%s"}
            """.formatted(status.status(), status.component()));
        }
    }

    // ── Execution Handlers ──

    private class SubmitExecutionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendJson(exchange, 405, """
                    {"error":"Method not allowed"}
                """);
                return;
            }

            try {
                String body = readBody(exchange);
                String tenantId = exchange.getRequestHeaders().getFirst("X-Tenant-Id");
                String idempotencyKey = exchange.getRequestHeaders().getFirst("Idempotency-Key");

                // Simple JSON parsing for the request
                String graphId = extractJsonField(body, "graphId");
                String graphVersion = extractJsonField(body, "graphVersion");

                ExecutionSubmitRequest request = new ExecutionSubmitRequest(graphId, graphVersion, null);
                ExecutionResponse response = executionService.submit(tenantId, request, idempotencyKey);

                sendJson(exchange, 202, """
                    {"executionId":"%s","graphId":"%s","graphVersion":"%s","status":"%s","planFingerprint":"%s"}
                """.formatted(
                        response.executionId(), response.graphId(), response.graphVersion(),
                        response.status(), response.planFingerprint() != null ? response.planFingerprint() : ""
                ));
            } catch (IllegalStateException e) {
                sendJson(exchange, 404, """
                    {"error":"%s"}
                """.formatted(e.getMessage()));
            } catch (Exception e) {
                sendJson(exchange, 500, """
                    {"error":"Internal server error"}
                """);
            }
        }
    }

    // ── Graph Handlers ──

    private class GraphHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if ("GET".equals(method) && path.matches("/api/v1/graphs/[^/]+/[^/]+")) {
                // GET /api/v1/graphs/{graphId}/{version}
                String[] parts = path.split("/");
                String graphId = parts[4];
                String version = parts[5];

                var graphOpt = graphService.findGraph(graphId, version);
                if (graphOpt.isPresent()) {
                    sendJson(exchange, 200, """
                        {"graphId":"%s","version":"%s","name":"%s"}
                    """.formatted(graphId, version, graphOpt.get().name()));
                } else {
                    sendJson(exchange, 404, """
                        {"error":"Graph not found"}
                    """);
                }
            } else {
                sendJson(exchange, 404, """
                    {"error":"Not found"}
                """);
            }
        }
    }

    // ── Utilities ──

    private static void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            is.transferTo(bos);
            return bos.toString(StandardCharsets.UTF_8);
        }
    }

    private static String extractJsonField(String json, String field) {
        String pattern = "\"" + field + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) return "";
        int colonIdx = json.indexOf(":", idx + pattern.length());
        if (colonIdx < 0) return "";
        int quoteStart = json.indexOf("\"", colonIdx + 1);
        if (quoteStart < 0) return "";
        int quoteEnd = json.indexOf("\"", quoteStart + 1);
        if (quoteEnd < 0) return "";
        return json.substring(quoteStart + 1, quoteEnd);
    }
}

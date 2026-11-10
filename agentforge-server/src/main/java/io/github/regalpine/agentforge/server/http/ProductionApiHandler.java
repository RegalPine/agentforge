package io.github.regalpine.agentforge.server.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.regalpine.agentforge.event.ExecutionEvent;
import io.github.regalpine.agentforge.graph.EdgeDefinition;
import io.github.regalpine.agentforge.graph.EdgeType;
import io.github.regalpine.agentforge.graph.GraphDefinition;
import io.github.regalpine.agentforge.graph.NodeDefinition;
import io.github.regalpine.agentforge.graph.NodeType;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.security.SecurityContext;
import io.github.regalpine.agentforge.server.execution.ExecutionStore.ExecutionRecord;
import io.github.regalpine.agentforge.server.service.ExecutionService;
import io.github.regalpine.agentforge.server.service.GraphService;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Production API handler that delegates to real services.
 *
 * <p>Unlike {@link StudioApiHandler} which uses in-memory demo data,
 * this handler routes requests through the actual service layer
 * (GraphService, ExecutionService, etc.).
 *
 * <p>Reads tenant context from the security context attribute set by
 * {@link AuthenticatedHandler}.
 *
 * <p>Batch 4: Full HTTP execution lifecycle — create, read, cancel, pause,
 * resume, terminal state and event timeline.
 */
public final class ProductionApiHandler implements HttpHandler {

    private final GraphService graphService;
    private final ExecutionService executionService;

    public ProductionApiHandler(
        GraphService graphService,
        ExecutionService executionService
    ) {
        this.graphService = graphService;
        this.executionService = executionService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        SecurityContext context = (SecurityContext) exchange.getAttribute(AuthenticatedHandler.SECURITY_CONTEXT_ATTR);
        if (context == null || context.tenant() == null) {
            sendJson(exchange, 401, "{\"error\":\"Authentication required\"}");
            return;
        }
        String tenantId = context.tenant().tenantId();

        try {
            if (path.startsWith("/api/v1/graphs")) {
                handleGraphs(exchange, method, path, tenantId);
            } else if (path.startsWith("/api/v1/executions")) {
                handleExecutions(exchange, method, path, tenantId, context);
            } else {
                sendJson(exchange, 404, "{\"error\":\"Not found\"}");
            }
        } catch (SecurityException e) {
            AuthenticatedHandler.auditDenied(context, "SERVICE_ACCESS_DENIED");
            sendJson(exchange, 403, "{\"error\":\"Access denied\"}");
        } catch (ExecutionService.IdempotencyConflictException e) {
            sendJson(exchange, 409, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
        } catch (IllegalStateException e) {
            sendJson(exchange, 404, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
        } catch (Exception e) {
            sendJson(exchange, 500, "{\"error\":\"Internal server error\"}");
        }
    }

    private void handleGraphs(
        HttpExchange exchange, String method, String path, String tenantId
    ) throws IOException {
        // POST /api/v1/graphs/validate
        if ("POST".equals(method) && path.equals("/api/v1/graphs/validate")) {
            String body = readBody(exchange);
            String graphId = extractJsonField(body, "id");
            if (graphId == null || graphId.isBlank()) {
                sendJson(exchange, 400,
                    "{\"valid\":false,\"errors\":[\"Graph id is required\"],\"warnings\":[]}");
                return;
            }
            sendJson(exchange, 200,
                "{\"valid\":true,\"errors\":[],\"warnings\":[]}");
        }
        // POST /api/v1/graphs/compile
        else if ("POST".equals(method) && path.equals("/api/v1/graphs/compile")) {
            String body = readBody(exchange);
            String graphId = extractJsonField(body, "id");
            if (graphId == null || graphId.isBlank()) {
                sendJson(exchange, 400,
                    "{\"status\":\"FAILURE\",\"errors\":[\"Graph id is required\"]}");
                return;
            }
            try {
                String version = extractJsonField(body, "version") != null ? extractJsonField(body, "version") : "1.0.0";
                var graphOpt = graphService.findGraph(tenantId, graphId, version);
                if (graphOpt.isEmpty()) {
                    sendJson(exchange, 404,
                        "{\"status\":\"FAILURE\",\"errors\":[\"Graph not found: " + esc(graphId) + "\"]}");
                    return;
                }
                graphService.publish(tenantId, graphOpt.get(), io.github.regalpine.agentforge.compiler.RegistrySnapshot.empty());
                var planOpt = graphService.findPlan(tenantId, graphId, version);
                String fingerprint = planOpt.map(p -> p.fingerprint()).orElse("unknown");
                sendJson(exchange, 200,
                    "{\"status\":\"SUCCESS\",\"executionPlan\":\"plan-" + tenantId + "-" + esc(graphId) + "\",\"fingerprint\":\"" + fingerprint + "\"}");
            } catch (Exception e) {
                sendJson(exchange, 200,
                    "{\"status\":\"FAILURE\",\"errors\":[\"" + esc(e.getMessage()) + "\"]}");
            }
        }
        // POST /api/v1/graphs — create a new graph definition
        else if ("POST".equals(method) && path.equals("/api/v1/graphs")) {
            String body = readBody(exchange);
            try {
                Map<String, Object> graphMap = HttpJson.object(body);
                GraphDefinition graph = parseGraphDefinition(graphMap);
                graphService.storeGraph(tenantId, graph);
                // Return the created graph summary
                Map<String, Object> summary = new HashMap<>();
                summary.put("id", graph.id().value());
                summary.put("version", graph.version().value());
                summary.put("name", graph.name());
                summary.put("description", graph.description() != null ? graph.description() : "");
                summary.put("status", "DRAFT");
                summary.put("nodeCount", graph.nodes().size());
                summary.put("edgeCount", graph.edges().size());
                summary.put("createdAt", Instant.now().toString());
                summary.put("tenantId", tenantId);
                sendJson(exchange, 201, HttpJson.stringify(summary));
            } catch (IllegalStateException e) {
                sendJson(exchange, 409, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            } catch (IllegalArgumentException e) {
                sendJson(exchange, 400, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }
        // GET /api/v1/graphs — list all graphs for tenant
        else if ("GET".equals(method) && path.equals("/api/v1/graphs")) {
            List<Map<String, Object>> graphs = graphService.listGraphs(tenantId);
            sendJson(exchange, 200, HttpJson.stringify(graphs));
        }
        // GET /api/v1/graphs/{id} — get specific graph definition
        else if ("GET".equals(method) && path.startsWith("/api/v1/graphs/")) {
            String graphId = path.substring("/api/v1/graphs/".length());
            // Remove trailing slashes or sub-paths
            if (graphId.contains("/")) {
                graphId = graphId.substring(0, graphId.indexOf("/"));
            }
            // Get version from query parameter
            String version = extractQueryParam(exchange, "version");
            if (version == null) version = "1.0.0";
            var graphOpt = graphService.findGraph(tenantId, graphId, version);
            if (graphOpt.isEmpty()) {
                sendJson(exchange, 404, "{\"error\":\"Graph not found: " + esc(graphId) + "\"}");
                return;
            }
            // Convert GraphDefinition to JSON-serializable map
            Map<String, Object> result = graphDefinitionToMap(graphOpt.get());
            sendJson(exchange, 200, HttpJson.stringify(result));
        }
        else {
            sendJson(exchange, 404, "{\"error\":\"Not found\"}");
        }
    }

    private void handleExecutions(
        HttpExchange exchange, String method, String path,
        String tenantId, SecurityContext context
    ) throws IOException {
        // POST /api/v1/executions/submit
        if ("POST".equals(method) && path.equals("/api/v1/executions/submit")) {
            handleSubmit(exchange, tenantId, context);
            return;
        }

        // GET /api/v1/executions — list
        if ("GET".equals(method) && path.equals("/api/v1/executions")) {
            handleList(exchange, tenantId, context);
            return;
        }

        // Execution-specific routes: /api/v1/executions/{id}[/action]
        String suffix = path.substring("/api/v1/executions/".length());
        String[] parts = suffix.split("/", 2);
        String executionId = parts[0];
        String action = parts.length > 1 ? parts[1] : null;

        if (executionId.isBlank()) {
            sendJson(exchange, 404, "{\"error\":\"Not found\"}");
            return;
        }

        if (action == null && "GET".equals(method)) {
            // GET /api/v1/executions/{id} — query status
            handleGet(exchange, tenantId, executionId, context);
        } else if ("state".equals(action) && "GET".equals(method)) {
            // GET /api/v1/executions/{id}/state — terminal state
            handleGetState(exchange, tenantId, executionId, context);
        } else if ("timeline".equals(action) && "GET".equals(method)) {
            // GET /api/v1/executions/{id}/timeline — event timeline
            handleTimeline(exchange, tenantId, executionId, context);
        } else if ("cancel".equals(action) && "POST".equals(method)) {
            // POST /api/v1/executions/{id}/cancel
            handleCancel(exchange, tenantId, executionId, context);
        } else if ("pause".equals(action) && "POST".equals(method)) {
            // POST /api/v1/executions/{id}/pause
            handlePause(exchange, tenantId, executionId, context);
        } else if ("resume".equals(action) && "POST".equals(method)) {
            // POST /api/v1/executions/{id}/resume
            handleResume(exchange, tenantId, executionId, context);
        } else {
            sendJson(exchange, 404, "{\"error\":\"Not found\"}");
        }
    }

    private void handleSubmit(HttpExchange exchange, String tenantId, SecurityContext context)
        throws IOException {
        String body = readBody(exchange);
        String graphId = extractJsonField(body, "graphId");
        String graphVersion = extractJsonField(body, "graphVersion");

        if (graphId == null || graphId.isBlank()) {
            sendJson(exchange, 400, "{\"error\":\"graphId is required\"}");
            return;
        }

        var request = new io.github.regalpine.agentforge.server.api.ExecutionSubmitRequest(
            graphId, graphVersion, null);
        String idempotencyKey = exchange.getRequestHeaders().getFirst("Idempotency-Key");

        var response = executionService.submit(context, request, idempotencyKey);

        // Batch 4: submit returns 202 Accepted (transaction saved, execution pending)
        sendJson(exchange, 202, """
            {"executionId":"%s","graphId":"%s","graphVersion":"%s","status":"%s","planFingerprint":"%s"}
            """.formatted(
                response.executionId(),
                response.graphId(),
                response.graphVersion(),
                response.status(),
                response.planFingerprint() != null ? response.planFingerprint() : ""
            ));
    }

    private void handleGet(HttpExchange exchange, String tenantId, String executionId,
                           SecurityContext context) throws IOException {
        var record = executionService.find(tenantId, executionId);
        if (record.isEmpty()) {
            sendJson(exchange, 404, "{\"error\":\"Execution not found: " + esc(executionId) + "\"}");
            return;
        }
        sendJson(exchange, 200, recordToJson(record.get()));
    }

    private void handleList(HttpExchange exchange, String tenantId, SecurityContext context)
        throws IOException {
        List<ExecutionRecord> records = executionService.list(tenantId);
        String json = records.stream()
            .map(r -> recordToJson(r))
            .collect(Collectors.joining(",", "[", "]"));
        sendJson(exchange, 200, json);
    }

    private void handleGetState(HttpExchange exchange, String tenantId, String executionId,
                                SecurityContext context) throws IOException {
        var record = executionService.find(tenantId, executionId);
        if (record.isEmpty()) {
            sendJson(exchange, 404, "{\"error\":\"Execution not found: " + esc(executionId) + "\"}");
            return;
        }
        var r = record.get();
        sendJson(exchange, 200, """
            {"executionId":"%s","status":"%s","version":%d,"graphId":"%s","graphVersion":"%s","planFingerprint":"%s","createdAt":"%s","updatedAt":"%s"}
            """.formatted(
                r.executionId(), r.status().name(), r.version(),
                r.graphId(), r.graphVersion(),
                r.planFingerprint() != null ? r.planFingerprint() : "",
                r.createdAt(), r.updatedAt()
            ));
    }

    private void handleTimeline(HttpExchange exchange, String tenantId, String executionId,
                                SecurityContext context) throws IOException {
        List<ExecutionEvent> events = executionService.timeline(tenantId, executionId);
        String json = events.stream()
            .map(e -> """
                {"sequence":%d,"type":"%s","timestamp":"%s","payload":{}}
                """.formatted(e.sequence(), e.type(), e.timestamp()))
            .collect(Collectors.joining(",", "[", "]"));
        sendJson(exchange, 200, json);
    }

    private void handleCancel(HttpExchange exchange, String tenantId, String executionId,
                              SecurityContext context) throws IOException {
        try {
            var record = executionService.cancel(tenantId, executionId);
            sendJson(exchange, 200, recordToJson(record));
        } catch (IllegalStateException e) {
            sendJson(exchange, 409, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    private void handlePause(HttpExchange exchange, String tenantId, String executionId,
                             SecurityContext context) throws IOException {
        try {
            var record = executionService.pause(tenantId, executionId);
            sendJson(exchange, 200, recordToJson(record));
        } catch (IllegalStateException e) {
            sendJson(exchange, 409, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    private void handleResume(HttpExchange exchange, String tenantId, String executionId,
                              SecurityContext context) throws IOException {
        try {
            var record = executionService.resume(tenantId, executionId);
            sendJson(exchange, 200, recordToJson(record));
        } catch (IllegalStateException e) {
            sendJson(exchange, 409, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    // ── JSON helpers ──

    private static String recordToJson(ExecutionRecord r) {
        return """
            {"executionId":"%s","graphId":"%s","graphVersion":"%s","status":"%s","version":%d,"planFingerprint":"%s","createdAt":"%s","updatedAt":"%s"}
            """.formatted(
                r.executionId(), r.graphId(), r.graphVersion(),
                r.status().name(), r.version(),
                r.planFingerprint() != null ? r.planFingerprint() : "",
                r.createdAt(), r.updatedAt()
            );
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (var is = exchange.getRequestBody();
             var bos = new java.io.ByteArrayOutputStream()) {
            is.transferTo(bos);
            return bos.toString(StandardCharsets.UTF_8);
        }
    }

    private static String extractJsonField(String json, String field) {
        if (json == null) return null;
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

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ── Graph parsing/serialization helpers ──

    @SuppressWarnings("unchecked")
    private static GraphDefinition parseGraphDefinition(Map<String, Object> map) {
        String id = (String) map.get("id");
        String version = (String) map.get("version");
        String name = (String) map.get("name");
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Graph id is required");
        if (version == null || version.isBlank()) version = "1.0.0";
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Graph name is required");

        String description = (String) map.get("description");
        Map<String, Object> inputs = map.get("inputs") instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
        Map<String, Object> outputs = map.get("outputs") instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
        Map<String, Object> policies = map.get("policies") instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
        Map<String, Object> metadata = map.get("metadata") instanceof Map<?, ?> m ? (Map<String, Object>) m : null;

        List<NodeDefinition> nodes = new ArrayList<>();
        if (map.get("nodes") instanceof List<?> nodeList) {
            for (Object n : nodeList) {
                if (n instanceof Map<?, ?> nodeMap) {
                    nodes.add(parseNodeDefinition((Map<String, Object>) nodeMap));
                }
            }
        }

        List<EdgeDefinition> edges = new ArrayList<>();
        if (map.get("edges") instanceof List<?> edgeList) {
            for (Object e : edgeList) {
                if (e instanceof Map<?, ?> edgeMap) {
                    edges.add(parseEdgeDefinition((Map<String, Object>) edgeMap));
                }
            }
        }

        return new GraphDefinition(
            new EntityId(id),
            new Version(version),
            name,
            description,
            inputs,
            outputs,
            nodes,
            edges,
            null, // bindings
            null, // expressions
            policies,
            metadata
        );
    }

    @SuppressWarnings("unchecked")
    private static NodeDefinition parseNodeDefinition(Map<String, Object> map) {
        String id = (String) map.get("id");
        String typeStr = (String) map.get("type");
        NodeType type = typeStr != null ? NodeType.valueOf(typeStr) : NodeType.ACTION;
        Map<String, Object> config = map.get("config") instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
        return new NodeDefinition(new EntityId(id), type, null, config);
    }

    private static EdgeDefinition parseEdgeDefinition(Map<String, Object> map) {
        String id = (String) map.get("id");
        String source = (String) map.get("source");
        String target = (String) map.get("target");
        String typeStr = (String) map.get("type");
        EdgeType type = typeStr != null ? EdgeType.valueOf(typeStr) : EdgeType.NORMAL;
        String condition = (String) map.get("condition");
        return new EdgeDefinition(
            new EntityId(id),
            new EntityId(source),
            new EntityId(target),
            type,
            condition
        );
    }

    private static Map<String, Object> graphDefinitionToMap(GraphDefinition g) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", g.id().value());
        result.put("version", g.version().value());
        result.put("name", g.name());
        result.put("description", g.description());
        result.put("inputs", g.inputs());
        result.put("outputs", g.outputs());
        result.put("policies", g.policies());
        result.put("metadata", g.metadata());

        List<Map<String, Object>> nodeList = new ArrayList<>();
        for (NodeDefinition n : g.nodes()) {
            Map<String, Object> nm = new HashMap<>();
            nm.put("id", n.id().value());
            nm.put("type", n.type().name());
            nm.put("config", n.config());
            nodeList.add(nm);
        }
        result.put("nodes", nodeList);

        List<Map<String, Object>> edgeList = new ArrayList<>();
        for (EdgeDefinition e : g.edges()) {
            Map<String, Object> em = new HashMap<>();
            em.put("id", e.id().value());
            em.put("source", e.source().value());
            em.put("target", e.target().value());
            em.put("type", e.type().name());
            em.put("condition", e.condition());
            edgeList.add(em);
        }
        result.put("edges", edgeList);
        result.put("nodeCount", g.nodes().size());
        result.put("edgeCount", g.edges().size());
        return result;
    }

    private static String extractQueryParam(HttpExchange exchange, String param) {
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null) return null;
        for (String entry : query.split("&")) {
            String[] pair = entry.split("=", 2);
            if (pair[0].equals(param) && pair.length == 2) {
                return java.net.URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}

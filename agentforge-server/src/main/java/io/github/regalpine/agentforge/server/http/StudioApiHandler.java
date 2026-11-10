package io.github.regalpine.agentforge.server.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.regalpine.agentforge.security.SecurityContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Studio API Handler - provides REST endpoints for all Studio pages.
 * Uses in-memory stores for demo/testing purposes.
 */
public final class StudioApiHandler implements HttpHandler {

    // ── In-memory stores ──
    private final List<Map<String, Object>> graphs = new CopyOnWriteArrayList<>();
    private record GraphKey(String tenantId, String id, String version) {}
    private final Map<GraphKey, Map<String, Object>> graphDefinitions = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> executions = new CopyOnWriteArrayList<>();
    private final List<Map<String, Object>> agents = new CopyOnWriteArrayList<>();
    private final List<Map<String, Object>> skills = new CopyOnWriteArrayList<>();
    private final List<Map<String, Object>> capabilities = new CopyOnWriteArrayList<>();
    private final List<Map<String, Object>> memoryRecords = new CopyOnWriteArrayList<>();
    private final List<Map<String, Object>> knowledgeSources = new CopyOnWriteArrayList<>();
    private final List<Map<String, Object>> mcpServers = new CopyOnWriteArrayList<>();
    private final List<Map<String, Object>> policies = new CopyOnWriteArrayList<>();
    private final List<Map<String, Object>> evalDatasets = new CopyOnWriteArrayList<>();

    public StudioApiHandler() {
        seedDemoData();
    }

    private void seedDemoData() {
        // Seed agents
        agents.add(agentMap("agent-translator", "Translator Agent", "v1.2.0", "ACTIVE", "Translates text between languages using LLM"));
        agents.add(agentMap("agent-summarizer", "Summarizer Agent", "v2.0.1", "ACTIVE", "Summarizes long documents"));
        agents.add(agentMap("agent-coder", "Code Assistant", "v1.0.0", "DRAFT", "Generates and reviews code"));

        // Seed capabilities
        capabilities.add(Map.of("id", "cap-llm-call", "name", "LLM Call", "type", "MODEL", "version", "1.0.0", "status", "ACTIVE", "provider", "openai"));
        capabilities.add(Map.of("id", "cap-http-fetch", "name", "HTTP Fetch", "type", "HTTP", "version", "1.1.0", "status", "ACTIVE", "provider", "http"));
        capabilities.add(Map.of("id", "cap-db-query", "name", "DB Query", "type", "JDBC", "version", "1.0.0", "status", "ACTIVE", "provider", "jdbc"));

        // Seed graphs
        graphs.add(new HashMap<>(Map.of("id", "graph-translate", "version", "1.0.0", "name", "Translation Pipeline", "status", "PUBLISHED", "nodeCount", 5, "edgeCount", 4, "createdAt", "2026-09-20T10:00:00Z", "tenantId", "default")));
        graphs.add(new HashMap<>(Map.of("id", "graph-rag", "version", "1.0.0", "name", "RAG Pipeline", "status", "PUBLISHED", "nodeCount", 8, "edgeCount", 7, "createdAt", "2026-09-22T14:30:00Z", "tenantId", "default")));
        graphs.add(new HashMap<>(Map.of("id", "graph-eval", "version", "0.1.0", "name", "Evaluation Flow", "status", "DRAFT", "nodeCount", 3, "edgeCount", 2, "createdAt", "2026-09-25T09:00:00Z", "tenantId", "default")));

        // Seed full graph definitions (nodes + edges)
        seedGraphDefinitions();

        // Seed executions
        executions.add(Map.of("id", "exec-001", "graphId", "graph-translate", "graphVersion", "1.0.0", "status", "COMPLETED", "tenantId", "default", "createdAt", "2026-09-26T10:00:00Z", "startedAt", "2026-09-26T10:00:01Z", "completedAt", "2026-09-26T10:00:05Z"));
        executions.add(Map.of("id", "exec-002", "graphId", "graph-rag", "graphVersion", "1.0.0", "status", "RUNNING", "tenantId", "default", "createdAt", "2026-09-27T08:00:00Z", "startedAt", "2026-09-27T08:00:01Z", "completedAt", ""));
        executions.add(Map.of("id", "exec-003", "graphId", "graph-translate", "graphVersion", "1.0.0", "status", "FAILED", "tenantId", "default", "createdAt", "2026-09-27T09:00:00Z", "startedAt", "2026-09-27T09:00:01Z", "completedAt", "2026-09-27T09:00:03Z"));

        // Seed skills
        skills.add(skillMap("skill-tokenizer", "Tokenizer", "1.0.0", "ACTIVE", "Tokenizes input text"));
        skills.add(skillMap("skill-embedder", "Text Embedder", "2.1.0", "ACTIVE", "Generates text embeddings"));
        skills.add(skillMap("skill-classifier", "Text Classifier", "1.3.0", "DEPRECATED", "Classifies text into categories"));

        // Seed memory records
        memoryRecords.add(Map.of("id", "mem-001", "type", "EPISODIC", "scope", "CONVERSATION", "key", "user-pref-lang", "content", "User prefers Chinese", "status", "ACTIVE", "importance", 0.9, "createdAt", "2026-09-25T10:00:00Z"));
        memoryRecords.add(Map.of("id", "mem-002", "type", "SEMANTIC", "scope", "AGENT", "key", "domain-knowledge", "content", "AgentForge is a Java agent framework", "status", "ACTIVE", "importance", 0.8, "createdAt", "2026-09-25T11:00:00Z"));

        // Seed knowledge sources
        knowledgeSources.add(Map.of("id", "ks-001", "name", "AgentForge Docs", "type", "FILE", "status", "READY", "documentCount", 25, "chunkCount", 150, "createdAt", "2026-09-20T10:00:00Z"));
        knowledgeSources.add(Map.of("id", "ks-002", "name", "API Reference", "type", "URL", "status", "INDEXING", "documentCount", 10, "chunkCount", 0, "createdAt", "2026-09-26T14:00:00Z"));

        // Seed MCP servers
        mcpServers.add(Map.of("id", "mcp-filesystem", "name", "Filesystem Server", "transport", "STDIO", "status", "CONNECTED", "toolCount", 5, "resourceCount", 3, "promptCount", 1, "endpoint", "npx @modelcontextprotocol/server-filesystem"));
        mcpServers.add(Map.of("id", "mcp-github", "name", "GitHub Server", "transport", "SSE", "status", "DISCONNECTED", "toolCount", 10, "resourceCount", 0, "promptCount", 2, "endpoint", "https://mcp.github.com/sse"));

        // Seed policies
        policies.add(Map.of("id", "pol-001", "name", "Max Token Limit", "type", "BUDGET", "status", "ACTIVE", "description", "Limits model calls to 4096 tokens"));
        policies.add(Map.of("id", "pol-002", "name", "Approved Models Only", "type", "ALLOWLIST", "status", "ACTIVE", "description", "Only gpt-4 and claude-3 allowed"));

        // Seed evaluation datasets
        evalDatasets.add(Map.of("id", "eval-ds-001", "name", "Translation Quality", "caseCount", 20, "createdAt", "2026-09-20T10:00:00Z"));
        evalDatasets.add(Map.of("id", "eval-ds-002", "name", "Summarization Accuracy", "caseCount", 15, "createdAt", "2026-09-22T14:00:00Z"));
    }

    private Map<String, Object> agentMap(String id, String name, String version, String status, String desc) {
        return new HashMap<>(Map.of(
            "id", id, "name", name, "version", version, "status", status,
            "description", desc, "createdAt", "2026-09-20T10:00:00Z",
            "capabilities", List.of(), "graphBindings", List.of()
        ));
    }

    private Map<String, Object> skillMap(String id, String name, String version, String state, String desc) {
        return new HashMap<>(Map.of(
            "id", id, "name", name, "version", version, "lifecycleState", state,
            "description", desc, "createdAt", "2026-09-20T10:00:00Z"
        ));
    }

    private void seedGraphDefinitions() {
        // graph-translate: Translation Pipeline (5 nodes, 4 edges)
        Map<String, Object> translateDef = new HashMap<>();
        translateDef.put("id", "graph-translate");
        translateDef.put("version", "1.0.0");
        translateDef.put("name", "Translation Pipeline");
        translateDef.put("description", "Translates input text using LLM with quality check and retry loop");
        translateDef.put("nodes", List.of(
            Map.of("id", "n1", "type", "START", "name", "Receive Input", "config", Map.of()),
            Map.of("id", "n2", "type", "LLM", "name", "Translate", "config", Map.of("model", "gpt-4", "prompt", "Translate to {{targetLang}}: {{input}}")),
            Map.of("id", "n3", "type", "CONDITION", "name", "Quality Check", "config", Map.of("expression", "score >= 0.8")),
            Map.of("id", "n4", "type", "END", "name", "Return Translation", "config", Map.of()),
            Map.of("id", "n5", "type", "AGENT", "name", "Refine", "config", Map.of(), "agent", Map.of("id", "agent-translator", "version", "v1.2.0"))
        ));
        translateDef.put("edges", List.of(
            Map.of("id", "e1", "source", "n1", "target", "n2", "type", "NORMAL"),
            Map.of("id", "e2", "source", "n2", "target", "n3", "type", "NORMAL"),
            Map.of("id", "e3", "source", "n3", "target", "n4", "type", "NORMAL", "condition", "pass"),
            Map.of("id", "e4", "source", "n3", "target", "n5", "type", "NORMAL", "condition", "retry")
        ));
        translateDef.put("positions", Map.of(
            "n1", Map.of("x", 300, "y", 50),
            "n2", Map.of("x", 300, "y", 180),
            "n3", Map.of("x", 300, "y", 320),
            "n4", Map.of("x", 150, "y", 460),
            "n5", Map.of("x", 450, "y", 460)
        ));
        translateDef.put("tenantId", "default");
        graphDefinitions.put(new GraphKey("default", "graph-translate", "1.0.0"), translateDef);

        // graph-rag: RAG Pipeline (8 nodes, 7 edges)
        Map<String, Object> ragDef = new HashMap<>();
        ragDef.put("id", "graph-rag");
        ragDef.put("version", "1.0.0");
        ragDef.put("name", "RAG Pipeline");
        ragDef.put("description", "Retrieval-Augmented Generation with vector search and LLM synthesis");
        ragDef.put("nodes", List.of(
            Map.of("id", "r1", "type", "START", "name", "User Query", "config", Map.of()),
            Map.of("id", "r2", "type", "CAPABILITY", "name", "Embed Query", "config", Map.of(), "capability", Map.of("id", "cap-llm-call", "version", "1.0.0")),
            Map.of("id", "r3", "type", "ACTION", "name", "Vector Search", "config", Map.of("topK", 5)),
            Map.of("id", "r4", "type", "PARALLEL", "name", "Fan-out", "config", Map.of()),
            Map.of("id", "r5", "type", "LLM", "name", "Summarize Chunk", "config", Map.of("model", "gpt-4")),
            Map.of("id", "r6", "type", "JOIN", "name", "Collect", "config", Map.of()),
            Map.of("id", "r7", "type", "LLM", "name", "Synthesize Answer", "config", Map.of("model", "gpt-4")),
            Map.of("id", "r8", "type", "END", "name", "Return Answer", "config", Map.of())
        ));
        ragDef.put("edges", List.of(
            Map.of("id", "re1", "source", "r1", "target", "r2", "type", "NORMAL"),
            Map.of("id", "re2", "source", "r2", "target", "r3", "type", "NORMAL"),
            Map.of("id", "re3", "source", "r3", "target", "r4", "type", "NORMAL"),
            Map.of("id", "re4", "source", "r4", "target", "r5", "type", "NORMAL"),
            Map.of("id", "re5", "source", "r5", "target", "r6", "type", "NORMAL"),
            Map.of("id", "re6", "source", "r6", "target", "r7", "type", "NORMAL"),
            Map.of("id", "re7", "source", "r7", "target", "r8", "type", "NORMAL")
        ));
        ragDef.put("positions", Map.of(
            "r1", Map.of("x", 300, "y", 30),
            "r2", Map.of("x", 300, "y", 140),
            "r3", Map.of("x", 300, "y", 250),
            "r4", Map.of("x", 300, "y", 360),
            "r5", Map.of("x", 300, "y", 470),
            "r6", Map.of("x", 300, "y", 580),
            "r7", Map.of("x", 300, "y", 690),
            "r8", Map.of("x", 300, "y", 800)
        ));
        ragDef.put("tenantId", "default");
        graphDefinitions.put(new GraphKey("default", "graph-rag", "1.0.0"), ragDef);

        // graph-eval: Evaluation Flow (3 nodes, 2 edges)
        Map<String, Object> evalDef = new HashMap<>();
        evalDef.put("id", "graph-eval");
        evalDef.put("version", "0.1.0");
        evalDef.put("name", "Evaluation Flow");
        evalDef.put("description", "Runs evaluation dataset against agent and compares results");
        evalDef.put("nodes", List.of(
            Map.of("id", "ev1", "type", "START", "name", "Load Dataset", "config", Map.of()),
            Map.of("id", "ev2", "type", "AGENT", "name", "Run Agent", "config", Map.of(), "agent", Map.of("id", "agent-translator", "version", "v1.2.0")),
            Map.of("id", "ev3", "type", "END", "name", "Compute Metrics", "config", Map.of())
        ));
        evalDef.put("edges", List.of(
            Map.of("id", "eve1", "source", "ev1", "target", "ev2", "type", "NORMAL"),
            Map.of("id", "eve2", "source", "ev2", "target", "ev3", "type", "NORMAL")
        ));
        evalDef.put("positions", Map.of(
            "ev1", Map.of("x", 300, "y", 50),
            "ev2", Map.of("x", 300, "y", 200),
            "ev3", Map.of("x", 300, "y", 350)
        ));
        evalDef.put("tenantId", "default");
        graphDefinitions.put(new GraphKey("default", "graph-eval", "0.1.0"), evalDef);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            // Route to appropriate handler
            if (path.startsWith("/api/v1/graphs")) handleGraphs(exchange, method, path);
            else if (path.startsWith("/api/v1/executions")) handleExecutions(exchange, method, path);
            else if (path.startsWith("/api/v1/agents")) handleAgents(exchange, method, path);
            else if (path.startsWith("/api/v1/skills")) handleSkills(exchange, method, path);
            else if (path.startsWith("/api/v1/capabilities")) handleCapabilities(exchange, method, path);
            else if (path.startsWith("/api/v1/memories")) handleMemories(exchange, method, path);
            else if (path.startsWith("/api/v1/knowledge")) handleKnowledge(exchange, method, path);
            else if (path.startsWith("/api/v1/mcp")) handleMcp(exchange, method, path);
            else if (path.startsWith("/api/v1/governance")) handleGovernance(exchange, method, path);
            else if (path.startsWith("/api/v1/evaluations")) handleEvaluations(exchange, method, path);
            else if (path.startsWith("/api/v1/observability")) handleObservability(exchange, method, path);
            else if (path.startsWith("/api/v1/human-approvals")) handleHumanApprovals(exchange, method, path);
            else if (path.startsWith("/api/v1/search")) handleSearch(exchange, method, path);
            else if (path.startsWith("/api/v1/diff")) handleDiff(exchange, method, path);
            else if (path.startsWith("/api/v1/versions")) handleVersions(exchange, method, path);
            else if (path.startsWith("/api/v1/services")) handleServices(exchange, method, path);
            else if (path.startsWith("/api/v1/control-plane")) handleControlPlane(exchange, method, path);
            else if (path.startsWith("/api/v1/evolution")) handleEvolution(exchange, method, path);
            else if (path.startsWith("/api/v1/debugger")) handleDebugger(exchange, method, path);
            else if (path.startsWith("/api/v1/workspaces")) handleWorkspaces(exchange, method, path);
            else if (path.startsWith("/api/v1/multimodal")) handleMultimodal(exchange, method, path);
            else sendJson(exchange, 404, "{\"error\":\"Not found\"}");
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, "{\"error\":\"Invalid request\"}");
        } catch (Exception e) {
            sendJson(exchange, 500, "{\"error\":\"Internal server error\"}");
        }
    }

    // ── Graphs ──

    private synchronized void handleGraphs(HttpExchange exchange, String method, String path) throws IOException {
        String tenantId = resolveTenantId(exchange);
        if (path.equals("/api/v1/graphs") && method.equals("GET")) {
            sendJsonOk(exchange, graphs.stream().filter(g -> tenantId.equals(g.get("tenantId"))).toList());
            return;
        }
        if (path.equals("/api/v1/graphs") && method.equals("POST")) {
            Map<String, Object> graph = parseJsonBody(exchange);
            graph.putIfAbsent("id", "graph-" + UUID.randomUUID());
            graph.putIfAbsent("version", "1.0.0");
            graph.putIfAbsent("status", "DRAFT");
            graph.putIfAbsent("createdAt", Instant.now().toString());
            graph.put("tenantId", tenantId);
            normalizeGraph(graph);
            GraphKey key = keyOf(graph);
            if (graphDefinitions.containsKey(key)) {
                sendJson(exchange, 409, "{\"error\":\"Graph version already exists\"}");
                return;
            }
            graphDefinitions.put(key, graph);
            graphs.add(graphSummary(graph));
            sendJson(exchange, 201, toJson(graph));
            return;
        }
        if (method.equals("POST") && path.equals("/api/v1/graphs/validate")) {
            sendJson(exchange, 200, "{\"valid\":true,\"errors\":[],\"warnings\":[]}");
            return;
        }
        if (method.equals("POST") && path.equals("/api/v1/graphs/compile")) {
            sendJson(exchange, 200, "{\"status\":\"SUCCESS\",\"executionPlan\":\"plan-mock\"}");
            return;
        }
        if (!path.matches("/api/v1/graphs/[^/]+(?:/definition)?")) {
            sendJson(exchange, 404, "{\"error\":\"Not found\"}");
            return;
        }
        String id = path.split("/")[4];
        String version = queryVersion(exchange);
        List<GraphKey> candidates = graphDefinitions.keySet().stream()
            .filter(k -> k.tenantId().equals(tenantId) && k.id().equals(id)
                && (version == null || k.version().equals(version))).toList();
        if (candidates.isEmpty()) {
            AuthenticatedHandler.auditDenied((SecurityContext) exchange.getAttribute(
                AuthenticatedHandler.SECURITY_CONTEXT_ATTR), "GRAPH_ACCESS_DENIED");
            sendJson(exchange, 403, "{\"error\":\"Graph not found or access denied\"}");
            return;
        }
        if (candidates.size() != 1) {
            sendJson(exchange, 409, "{\"error\":\"Specify graph version with ?version=\"}");
            return;
        }
        GraphKey key = candidates.get(0);
        switch (method) {
            case "GET" -> sendJsonOk(exchange, graphDefinitions.get(key));
            case "PUT" -> {
                Map<String, Object> patch = parseJsonBody(exchange);
                if (patch.containsKey("version") && !key.version().equals(patch.get("version"))) {
                    sendJson(exchange, 409, "{\"error\":\"Create a new graph version explicitly\"}");
                    return;
                }
                Map<String, Object> updated = new HashMap<>(graphDefinitions.get(key));
                updated.putAll(patch);
                updated.put("id", id);
                updated.put("version", key.version());
                updated.put("tenantId", tenantId);
                normalizeGraph(updated);
                graphDefinitions.put(key, updated);
                graphs.removeIf(g -> key.equals(keyOf(g)));
                graphs.add(graphSummary(updated));
                sendJsonOk(exchange, updated);
            }
            case "DELETE" -> {
                graphDefinitions.remove(key);
                graphs.removeIf(g -> key.equals(keyOf(g)));
                sendJson(exchange, 204, "");
            }
            default -> sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
        }
    }

    private static GraphKey keyOf(Map<String, Object> graph) {
        return new GraphKey((String) graph.get("tenantId"), (String) graph.get("id"), (String) graph.get("version"));
    }

    private static String queryVersion(HttpExchange exchange) {
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null) return null;
        for (String entry : query.split("&")) {
            String[] pair = entry.split("=", 2);
            if (pair[0].equals("version") && pair.length == 2) {
                return java.net.URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static void normalizeGraph(Map<String, Object> graph) {
        for (String field : List.of("id", "version")) {
            if (!(graph.get(field) instanceof String value) || value.isBlank()) {
                throw new IllegalArgumentException("Invalid graph " + field);
            }
        }
        graph.putIfAbsent("nodes", List.of());
        graph.putIfAbsent("edges", List.of());
        if (!(graph.get("nodes") instanceof List<?> nodes) || !(graph.get("edges") instanceof List<?> edges)) {
            throw new IllegalArgumentException("Graph nodes and edges must be arrays");
        }
        graph.put("nodeCount", nodes.size());
        graph.put("edgeCount", edges.size());
    }

    private static Map<String, Object> graphSummary(Map<String, Object> graph) {
        Map<String, Object> summary = new HashMap<>();
        for (String key : List.of("id", "version", "name", "status", "createdAt", "tenantId", "nodeCount", "edgeCount")) {
            if (graph.containsKey(key)) summary.put(key, graph.get(key));
        }
        return summary;
    }

    // ── Executions ──

    private synchronized void handleExecutions(HttpExchange exchange, String method, String path) throws IOException {
        String tenantId = resolveTenantId(exchange);
        if (path.startsWith("/api/v1/executions/") && !path.equals("/api/v1/executions/submit")) {
            String id = path.split("/")[4];
            if (executions.stream().noneMatch(e -> id.equals(e.get("id")) && tenantId.equals(e.get("tenantId")))) {
                AuthenticatedHandler.auditDenied((SecurityContext) exchange.getAttribute(
                    AuthenticatedHandler.SECURITY_CONTEXT_ATTR), "EXECUTION_ACCESS_DENIED");
                sendJson(exchange, 403, "{\"error\":\"Execution not found or access denied\"}");
                return;
            }
        }
        if (path.equals("/api/v1/executions") && "GET".equals(method)) {
            List<Map<String, Object>> filtered = executions.stream()
                .filter(e -> tenantId.equals(e.getOrDefault("tenantId", "default")))
                .toList();
            sendJson(exchange, 200, toJson(filtered));
        } else if (path.matches("/api/v1/executions/[^/]+$") && "GET".equals(method)) {
            String id = path.split("/")[4];
            executions.stream()
                .filter(e -> id.equals(e.get("id")) && tenantId.equals(e.getOrDefault("tenantId", "default")))
                .findFirst().ifPresentOrElse(
                    e -> sendJsonOk(exchange, e),
                    () -> sendJson(exchange, 404, "{\"error\":\"Execution not found\"}")
                );
        } else if (path.matches("/api/v1/executions/[^/]+/state$") && "GET".equals(method)) {
            sendJson(exchange, 200, "{\"currentState\":{},\"delta\":{}}");
        } else if (path.matches("/api/v1/executions/[^/]+/timeline$") && "GET".equals(method)) {
            sendJson(exchange, 200, "[]");
        } else if (path.matches("/api/v1/executions/[^/]+/agent-loop$") && "GET".equals(method)) {
            sendJson(exchange, 200, "{\"iterations\":[],\"budget\":{\"max\":10,\"used\":0}}");
        } else if (path.matches("/api/v1/executions/[^/]+/context$") && "GET".equals(method)) {
            sendJson(exchange, 200, "{\"segments\":[]}");
        } else if (path.matches("/api/v1/executions/[^/]+/trace$") && "GET".equals(method)) {
            sendJson(exchange, 200, "{\"spans\":[]}");
        } else if (path.matches("/api/v1/executions/[^/]+/tool-calls$") && "GET".equals(method)) {
            sendJson(exchange, 200, "[]");
        } else if (path.matches("/api/v1/executions/[^/]+/model-calls$") && "GET".equals(method)) {
            sendJson(exchange, 200, "[]");
        } else if (path.matches("/api/v1/executions/[^/]+/checkpoints$") && "GET".equals(method)) {
            sendJson(exchange, 200, "[]");
        } else if (path.matches("/api/v1/executions/[^/]+/events$") && "GET".equals(method)) {
            sendJson(exchange, 200, "[]");
        } else if (path.matches("/api/v1/executions/[^/]+/(pause|resume|cancel)$") && "POST".equals(method)) {
            String id = path.split("/")[4];
            String action = path.split("/")[5];
            // Verify tenant ownership before state change
            boolean owned = executions.stream()
                .anyMatch(e -> id.equals(e.get("id")) && tenantId.equals(e.getOrDefault("tenantId", "default")));
            if (!owned) {
                sendJson(exchange, 403, "{\"error\":\"Execution not found or access denied\"}");
                return;
            }
            String newStatus = switch (action) {
                case "pause" -> "PAUSED";
                case "resume" -> "RUNNING";
                case "cancel" -> "CANCELLED";
                default -> "UNKNOWN";
            };
            executions.stream().filter(e -> id.equals(e.get("id")) && tenantId.equals(e.get("tenantId"))).findFirst()
                .ifPresent(e -> {
                    Map<String, Object> updated = new HashMap<>(e);
                    updated.put("status", newStatus);
                    executions.remove(e);
                    executions.add(updated);
                });
            sendJson(exchange, 200, "{\"status\":\"" + newStatus + "\"}");
        } else if (path.equals("/api/v1/executions/submit") && "POST".equals(method)) {
            Map<String, Object> body = parseJsonBody(exchange);
            GraphKey graphKey = new GraphKey(tenantId, (String) body.get("graphId"),
                (String) body.getOrDefault("graphVersion", "1.0.0"));
            if (!graphDefinitions.containsKey(graphKey)) {
                AuthenticatedHandler.auditDenied((SecurityContext) exchange.getAttribute(
                    AuthenticatedHandler.SECURITY_CONTEXT_ATTR), "GRAPH_ACCESS_DENIED");
                sendJson(exchange, 403, "{\"error\":\"Graph not found or access denied\"}");
                return;
            }
            String execId = "exec-" + UUID.randomUUID();
            Map<String, Object> exec = new HashMap<>();
            exec.put("id", execId);
            exec.put("graphId", body.getOrDefault("graphId", ""));
            exec.put("graphVersion", body.getOrDefault("graphVersion", "1.0.0"));
            exec.put("status", "CREATED");
            exec.put("tenantId", resolveTenantId(exchange));
            exec.put("createdAt", Instant.now().toString());
            exec.put("startedAt", "");
            exec.put("completedAt", "");
            executions.add(exec);
            sendJson(exchange, 202, toJson(exec));
        } else {
            sendJson(exchange, 404, "{\"error\":\"Not found\"}");
        }
    }

    // ── Agents ──

    private void handleAgents(HttpExchange exchange, String method, String path) throws IOException {
        if (path.equals("/api/v1/agents") && "GET".equals(method)) {
            sendJson(exchange, 200, toJson(agents));
        } else if (path.equals("/api/v1/agents/summaries") && "GET".equals(method)) {
            List<Map<String, Object>> summaries = agents.stream().map(a -> Map.of(
                "id", a.get("id"), "name", a.get("name"), "version", a.get("version"),
                "status", a.get("status"), "description", a.getOrDefault("description", "")
            )).toList();
            sendJson(exchange, 200, toJson(summaries));
        } else if (path.matches("/api/v1/agents/[^/]+/overview$") && "GET".equals(method)) {
            String id = path.split("/")[4];
            agents.stream().filter(a -> id.equals(a.get("id"))).findFirst()
                .ifPresentOrElse(
                    a -> sendJson(exchange, 200, "{\"id\":\"" + id + "\",\"name\":\"" + a.get("name") + "\",\"description\":\"" + a.get("description") + "\",\"executionCount\":5,\"successRate\":0.95,\"avgLatency\":120}"),
                    () -> sendJson(exchange, 404, "{\"error\":\"Agent not found\"}")
                );
        } else if (path.matches("/api/v1/agents/[^/]+/versions$") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"version\":\"1.0.0\",\"status\":\"ACTIVE\",\"createdAt\":\"2026-09-20T10:00:00Z\"}]");
        } else if (path.matches("/api/v1/agents/[^/]+/definition$") && "GET".equals(method)) {
            String id = path.split("/")[4];
            sendJson(exchange, 200, "{\"id\":\"" + id + "\",\"type\":\"FUNCTIONAL\",\"capabilities\":[],\"graphBindings\":[]}");
        } else {
            sendJson(exchange, 404, "{\"error\":\"Not found\"}");
        }
    }

    // ── Skills ──

    private void handleSkills(HttpExchange exchange, String method, String path) throws IOException {
        if (path.equals("/api/v1/skills") && "GET".equals(method)) {
            sendJson(exchange, 200, toJson(skills));
        } else if (path.matches("/api/v1/skills/[^/]+/overview$") && "GET".equals(method)) {
            String id = path.split("/")[4];
            skills.stream().filter(s -> id.equals(s.get("id"))).findFirst()
                .ifPresentOrElse(
                    s -> sendJsonOk(exchange, s),
                    () -> sendJson(exchange, 404, "{\"error\":\"Skill not found\"}")
                );
        } else if (path.matches("/api/v1/skills/[^/]+/versions$") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"version\":\"1.0.0\",\"lifecycleState\":\"ACTIVE\"}]");
        } else if (path.matches("/api/v1/skills/[^/]+/contract$") && "GET".equals(method)) {
            sendJson(exchange, 200, "{\"inputSchema\":{\"type\":\"object\"},\"outputSchema\":{\"type\":\"object\"}}");
        } else if (path.matches("/api/v1/skills/[^/]+/dependencies$") && "GET".equals(method)) {
            sendJson(exchange, 200, "{\"dependencies\":[],\"dependents\":[]}");
        } else if (path.matches("/api/v1/skills/[^/]+/security$") && "GET".equals(method)) {
            sendJson(exchange, 200, "{\"riskLevel\":\"LOW\",\"findings\":[]}");
        } else if (path.matches("/api/v1/skills/[^/]+/evaluation$") && "GET".equals(method)) {
            sendJson(exchange, 200, "{\"lastRun\":null,\"results\":[]}");
        } else if (path.matches("/api/v1/skills/[^/]+/governance$") && "GET".equals(method)) {
            sendJson(exchange, 200, "{\"policies\":[],\"violations\":[]}");
        } else if (path.matches("/api/v1/skills/[^/]+/usage$") && "GET".equals(method)) {
            sendJson(exchange, 200, "{\"totalInvocations\":42,\"successRate\":0.98,\"avgLatency\":50}");
        } else if (path.matches("/api/v1/skills/[^/]+/executions$") && "GET".equals(method)) {
            sendJson(exchange, 200, "[]");
        } else if (path.equals("/api/v1/skills/invoke") && "POST".equals(method)) {
            sendJson(exchange, 200, "{\"status\":\"SUCCESS\",\"output\":{\"result\":\"mock\"},\"latency\":15}");
        } else if (path.matches("/api/v1/skills/[^/]+/versions/[^/]+/lifecycle$") && "PUT".equals(method)) {
            sendJson(exchange, 200, "{\"status\":\"TRANSITIONED\"}");
        } else {
            sendJson(exchange, 404, "{\"error\":\"Not found\"}");
        }
    }

    // ── Capabilities ──

    private void handleCapabilities(HttpExchange exchange, String method, String path) throws IOException {
        if (path.equals("/api/v1/capabilities") && "GET".equals(method)) {
            sendJson(exchange, 200, toJson(capabilities));
        } else if (path.equals("/api/v1/capabilities/summaries") && "GET".equals(method)) {
            sendJson(exchange, 200, toJson(capabilities));
        } else if (path.matches("/api/v1/capabilities/[^/]+/detail$") && "GET".equals(method)) {
            String id = path.split("/")[4];
            capabilities.stream().filter(c -> id.equals(c.get("id"))).findFirst()
                .ifPresentOrElse(
                    c -> sendJsonOk(exchange, c),
                    () -> sendJson(exchange, 404, "{\"error\":\"Capability not found\"}")
                );
        } else if (path.equals("/api/v1/capabilities/invoke") && "POST".equals(method)) {
            sendJson(exchange, 200, "{\"status\":\"SUCCESS\",\"output\":{\"result\":\"mock\"},\"latency\":10}");
        } else {
            sendJson(exchange, 404, "{\"error\":\"Not found\"}");
        }
    }

    // ── Memories ──

    private void handleMemories(HttpExchange exchange, String method, String path) throws IOException {
        if (path.equals("/api/v1/memories/query") && "POST".equals(method)) {
            sendJson(exchange, 200, toJson(memoryRecords));
        } else if (path.equals("/api/v1/memories/candidates") && "GET".equals(method)) {
            sendJson(exchange, 200, "[]");
        } else if (path.equals("/api/v1/memories/metrics") && "GET".equals(method)) {
            sendJson(exchange, 200, "{\"totalRecords\":" + memoryRecords.size() + ",\"activeRecords\":" + memoryRecords.size() + ",\"storageBytes\":1024}");
        } else if (path.matches("/api/v1/memories/[^/]+/audit$") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"action\":\"CREATED\",\"timestamp\":\"2026-09-25T10:00:00Z\",\"actor\":\"system\"}]");
        } else {
            sendJson(exchange, 404, "{\"error\":\"Not found\"}");
        }
    }

    // ── Knowledge ──

    private void handleKnowledge(HttpExchange exchange, String method, String path) throws IOException {
        if (path.equals("/api/v1/knowledge/sources") && "GET".equals(method)) {
            sendJson(exchange, 200, toJson(knowledgeSources));
        } else if (path.matches("/api/v1/knowledge/sources/[^/]+/documents$") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"id\":\"doc-001\",\"name\":\"readme.md\",\"status\":\"INDEXED\",\"chunkCount\":10}]");
        } else if (path.equals("/api/v1/knowledge/search") && "GET".equals(method)) {
            sendJson(exchange, 200, "[]");
        } else if (path.matches("/api/v1/knowledge/traces/[^/]+$") && "GET".equals(method)) {
            sendJson(exchange, 200, "{\"traceId\":\"trace-001\",\"stages\":[]}");
        } else if (path.matches("/api/v1/knowledge/citations/[^/]+$") && "GET".equals(method)) {
            sendJson(exchange, 200, "[]");
        } else {
            sendJson(exchange, 404, "{\"error\":\"Not found\"}");
        }
    }

    // ── MCP ──

    private void handleMcp(HttpExchange exchange, String method, String path) throws IOException {
        if (path.equals("/api/v1/mcp/servers") && "GET".equals(method)) {
            sendJson(exchange, 200, toJson(mcpServers));
        } else if (path.matches("/api/v1/mcp/servers/[^/]+$") && "GET".equals(method)) {
            String id = path.split("/")[5];
            mcpServers.stream().filter(s -> id.equals(s.get("id"))).findFirst()
                .ifPresentOrElse(
                    s -> sendJsonOk(exchange, s),
                    () -> sendJson(exchange, 404, "{\"error\":\"MCP server not found\"}")
                );
        } else if (path.matches("/api/v1/mcp/servers/[^/]+/tools$") && "GET".equals(method)) {
            String serverId = path.split("/")[5];
            sendJson(exchange, 200, "[{\"id\":\"tool-1\",\"name\":\"read_file\",\"description\":\"Read a file\",\"serverId\":\"" + serverId + "\",\"enabled\":true},{\"id\":\"tool-2\",\"name\":\"write_file\",\"description\":\"Write a file\",\"serverId\":\"" + serverId + "\",\"enabled\":true}]");
        } else if (path.matches("/api/v1/mcp/servers/[^/]+/resources$") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"id\":\"res-1\",\"name\":\"config.json\",\"uri\":\"file:///config.json\",\"serverId\":\"" + path.split("/")[5] + "\"}]");
        } else if (path.matches("/api/v1/mcp/servers/[^/]+/prompts$") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"id\":\"prompt-1\",\"name\":\"analyze\",\"description\":\"Analyze the input\",\"serverId\":\"" + path.split("/")[5] + "\",\"arguments\":[]}]");
        } else if (path.matches("/api/v1/mcp/servers/[^/]+/tools/[^/]+/(enable|disable)$") && "POST".equals(method)) {
            sendJson(exchange, 200, "{\"status\":\"updated\"}");
        } else {
            sendJson(exchange, 404, "{\"error\":\"Not found\"}");
        }
    }

    // ── Governance ──

    private void handleGovernance(HttpExchange exchange, String method, String path) throws IOException {
        if (path.equals("/api/v1/governance/policies") && "GET".equals(method)) {
            sendJson(exchange, 200, toJson(policies));
        } else if (path.equals("/api/v1/governance/decisions") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"id\":\"dec-001\",\"policyId\":\"pol-001\",\"action\":\"ALLOW\",\"reason\":\"Within budget\",\"timestamp\":\"2026-09-26T10:00:00Z\"}]");
        } else if (path.equals("/api/v1/governance/violations") && "GET".equals(method)) {
            sendJson(exchange, 200, "[]");
        } else if (path.equals("/api/v1/governance/approvals") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"id\":\"appr-001\",\"type\":\"DEPLOYMENT\",\"status\":\"PENDING\",\"requester\":\"admin\",\"createdAt\":\"2026-09-26T10:00:00Z\"}]");
        } else if (path.matches("/api/v1/governance/approvals/[^/]+/decide$") && "POST".equals(method)) {
            sendJson(exchange, 200, "{\"status\":\"DECIDED\"}");
        } else if (path.equals("/api/v1/governance/audit") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"id\":\"audit-001\",\"action\":\"POLICY_CREATED\",\"actor\":\"admin\",\"timestamp\":\"2026-09-20T10:00:00Z\",\"details\":\"Created policy pol-001\"}]");
        } else if (path.equals("/api/v1/governance/risk/summary") && "GET".equals(method)) {
            sendJson(exchange, 200, "{\"overallRisk\":\"LOW\",\"activePolicies\":" + policies.size() + ",\"openViolations\":0,\"pendingApprovals\":1}");
        } else {
            sendJson(exchange, 404, "{\"error\":\"Not found\"}");
        }
    }

    // ── Evaluations ──

    private void handleEvaluations(HttpExchange exchange, String method, String path) throws IOException {
        if (path.equals("/api/v1/evaluations/datasets") && "GET".equals(method)) {
            sendJson(exchange, 200, toJson(evalDatasets));
        } else if (path.matches("/api/v1/evaluations/datasets/[^/]+$") && "GET".equals(method)) {
            String id = path.split("/")[5];
            evalDatasets.stream().filter(d -> id.equals(d.get("id"))).findFirst()
                .ifPresentOrElse(
                    d -> sendJsonOk(exchange, d),
                    () -> sendJson(exchange, 404, "{\"error\":\"Dataset not found\"}")
                );
        } else if (path.matches("/api/v1/evaluations/datasets/[^/]+/cases$") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"id\":\"case-001\",\"input\":{\"text\":\"hello\"},\"expectedOutput\":{\"translation\":\"你好\"},\"status\":\"ACTIVE\"}]");
        } else if (path.equals("/api/v1/evaluations/runs") && "POST".equals(method)) {
            sendJson(exchange, 201, "{\"id\":\"run-001\",\"status\":\"RUNNING\",\"startedAt\":\"" + Instant.now() + "\"}");
        } else if (path.equals("/api/v1/evaluations/runs") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"id\":\"run-001\",\"datasetId\":\"eval-ds-001\",\"status\":\"COMPLETED\",\"startedAt\":\"2026-09-26T10:00:00Z\",\"completedAt\":\"2026-09-26T10:05:00Z\"}]");
        } else if (path.matches("/api/v1/evaluations/runs/[^/]+/report$") && "GET".equals(method)) {
            sendJson(exchange, 200, "{\"runId\":\"run-001\",\"totalCases\":20,\"passed\":18,\"failed\":2,\"score\":0.9}");
        } else if (path.equals("/api/v1/evaluations/regression") && "GET".equals(method)) {
            sendJson(exchange, 200, "{\"hasRegression\":false,\"changes\":[]}");
        } else {
            sendJson(exchange, 404, "{\"error\":\"Not found\"}");
        }
    }

    // ── Observability ──

    private void handleObservability(HttpExchange exchange, String method, String path) throws IOException {
        if (path.equals("/api/v1/observability/metrics") && "GET".equals(method)) {
            sendJson(exchange, 200, "{\"totalExecutions\":3,\"successRate\":0.33,\"avgLatency\":2500,\"errorRate\":0.33,\"activeExecutions\":1}");
        } else if (path.equals("/api/v1/observability/series") && "GET".equals(method)) {
            sendJson(exchange, 200, "{\"metric\":\"latency\",\"dataPoints\":[]}");
        } else if (path.equals("/api/v1/observability/models") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"model\":\"gpt-4\",\"provider\":\"openai\",\"totalCalls\":15,\"avgLatency\":1200,\"errorRate\":0.05}]");
        } else if (path.equals("/api/v1/observability/capabilities/health") && "GET".equals(method)) {
            sendJson(exchange, 200, toJson(capabilities.stream().map(c -> Map.of("id", c.get("id"), "name", c.get("name"), "status", "HEALTHY", "invocationCount", 10)).toList()));
        } else {
            sendJson(exchange, 404, "{\"error\":\"Not found\"}");
        }
    }

    // ── Human Approvals ──

    private void handleHumanApprovals(HttpExchange exchange, String method, String path) throws IOException {
        if (path.equals("/api/v1/human-approvals") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"id\":\"ha-001\",\"type\":\"DEPLOYMENT_APPROVAL\",\"status\":\"PENDING\",\"riskLevel\":\"MEDIUM\",\"description\":\"Deploy graph-translate v1.1\",\"createdAt\":\"2026-09-26T10:00:00Z\"}]");
        } else if (path.equals("/api/v1/human-approvals/summary") && "GET".equals(method)) {
            sendJson(exchange, 200, "{\"pending\":1,\"approved\":5,\"rejected\":0,\"total\":6}");
        } else if (path.matches("/api/v1/human-approvals/[^/]+/decide$") && "POST".equals(method)) {
            sendJson(exchange, 200, "{\"status\":\"DECIDED\"}");
        } else {
            sendJson(exchange, 404, "{\"error\":\"Not found\"}");
        }
    }

    // ── Search ──

    private void handleSearch(HttpExchange exchange, String method, String path) throws IOException {
        if (path.equals("/api/v1/search") && "POST".equals(method)) {
            Map<String, Object> body = parseJsonBody(exchange);
            String query = (String) body.getOrDefault("query", "");
            List<Map<String, Object>> results = new ArrayList<>();
            // Search across all stores
            for (Map<String, Object> g : graphs) {
                if (matchesQuery(g, query)) results.add(Map.of("type", "GRAPH", "id", g.get("id"), "name", g.getOrDefault("name", ""), "score", 0.9));
            }
            for (Map<String, Object> a : agents) {
                if (matchesQuery(a, query)) results.add(Map.of("type", "AGENT", "id", a.get("id"), "name", a.getOrDefault("name", ""), "score", 0.85));
            }
            for (Map<String, Object> s : skills) {
                if (matchesQuery(s, query)) results.add(Map.of("type", "SKILL", "id", s.get("id"), "name", s.getOrDefault("name", ""), "score", 0.8));
            }
            sendJson(exchange, 200, "{\"results\":" + toJson(results) + ",\"total\":" + results.size() + "}");
        } else {
            sendJson(exchange, 404, "{\"error\":\"Not found\"}");
        }
    }

    // ── Diff & Versions ──

    private void handleDiff(HttpExchange exchange, String method, String path) throws IOException {
        sendJson(exchange, 200, "{\"changes\":[],\"summary\":{\"added\":0,\"removed\":0,\"modified\":0,\"unchanged\":0}}");
    }

    private void handleVersions(HttpExchange exchange, String method, String path) throws IOException {
        if (path.matches("/api/v1/versions/[^/]+/[^/]+/release-gate$")) {
            sendJson(exchange, 200, "{\"status\":\"PASSED\",\"gates\":[{\"name\":\"Validation\",\"status\":\"PASSED\"},{\"name\":\"Evaluation\",\"status\":\"PASSED\"},{\"name\":\"Approval\",\"status\":\"PENDING\"}]}");
        } else {
            sendJson(exchange, 200, "[{\"version\":\"1.0.0\",\"status\":\"PUBLISHED\",\"createdAt\":\"2026-09-20T10:00:00Z\"}]");
        }
    }

    // ── Service Channel ──

    private void handleServices(HttpExchange exchange, String method, String path) throws IOException {
        if (path.equals("/api/v1/services") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"id\":\"svc-001\",\"name\":\"Translation Service\",\"protocol\":\"REST\",\"status\":\"ACTIVE\",\"sessions\":3},{\"id\":\"svc-002\",\"name\":\"RAG Service\",\"protocol\":\"GRPC\",\"status\":\"ACTIVE\",\"sessions\":1}]");
        } else if (path.equals("/api/v1/services/sessions") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"id\":\"sess-001\",\"serviceId\":\"svc-001\",\"status\":\"ACTIVE\",\"createdAt\":\"2026-09-27T08:00:00Z\"}]");
        } else if (path.equals("/api/v1/services/conversations") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"id\":\"conv-001\",\"sessionId\":\"sess-001\",\"messageCount\":5,\"createdAt\":\"2026-09-27T08:00:00Z\"}]");
        } else if (path.equals("/api/v1/services/health") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"serviceId\":\"svc-001\",\"status\":\"HEALTHY\",\"uptime\":\"72h\",\"requestCount\":150},{\"serviceId\":\"svc-002\",\"status\":\"HEALTHY\",\"uptime\":\"48h\",\"requestCount\":80}]");
        } else {
            sendJson(exchange, 404, "{\"error\":\"Not found\"}");
        }
    }

    // ── Control Plane ──

    private void handleControlPlane(HttpExchange exchange, String method, String path) throws IOException {
        if (path.equals("/api/v1/control-plane/deployments") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"id\":\"dep-001\",\"graphId\":\"graph-translate\",\"environment\":\"production\",\"status\":\"RUNNING\",\"replicas\":2,\"createdAt\":\"2026-09-20T10:00:00Z\"}]");
        } else if (path.equals("/api/v1/control-plane/instances") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"id\":\"inst-001\",\"deploymentId\":\"dep-001\",\"host\":\"node-1\",\"port\":8080,\"status\":\"RUNNING\",\"cpu\":\"15%\",\"memory\":\"256MB\"}]");
        } else if (path.equals("/api/v1/control-plane/environments") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"name\":\"production\",\"deployments\":1,\"status\":\"ACTIVE\"},{\"name\":\"staging\",\"deployments\":0,\"status\":\"ACTIVE\"}]");
        } else if (path.equals("/api/v1/control-plane/releases") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"id\":\"rel-001\",\"deploymentId\":\"dep-001\",\"version\":\"1.0.0\",\"status\":\"DEPLOYED\",\"releasedAt\":\"2026-09-20T10:00:00Z\"}]");
        } else if (path.equals("/api/v1/control-plane/quotas") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"resource\":\"executions\",\"limit\":1000,\"used\":42},{\"resource\":\"model_calls\",\"limit\":10000,\"used\":350}]");
        } else {
            sendJson(exchange, 404, "{\"error\":\"Not found\"}");
        }
    }

    // ── Evolution ──

    private void handleEvolution(HttpExchange exchange, String method, String path) throws IOException {
        if (path.equals("/api/v1/evolution/candidates") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"id\":\"cand-001\",\"graphId\":\"graph-translate\",\"change\":\"Updated LLM prompt\",\"status\":\"EVALUATING\",\"createdAt\":\"2026-09-26T10:00:00Z\"}]");
        } else if (path.equals("/api/v1/evolution/changes") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"id\":\"chg-001\",\"candidateId\":\"cand-001\",\"type\":\"PROMPT_UPDATE\",\"description\":\"Improved translation prompt\"}]");
        } else if (path.equals("/api/v1/evolution/evidence") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"id\":\"ev-001\",\"candidateId\":\"cand-001\",\"type\":\"EVALUATION\",\"score\":0.92}]");
        } else if (path.equals("/api/v1/evolution/problems") && "GET".equals(method)) {
            sendJson(exchange, 200, "[]");
        } else if (path.equals("/api/v1/evolution/validation") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"candidateId\":\"cand-001\",\"status\":\"PASSED\",\"checks\":[{\"name\":\"Syntax\",\"status\":\"PASSED\"},{\"name\":\"Evaluation\",\"status\":\"PASSED\"}]}]");
        } else if (path.equals("/api/v1/evolution/lineage") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"from\":\"graph-translate:v1.0.0\",\"to\":\"cand-001\",\"timestamp\":\"2026-09-26T10:00:00Z\"}]");
        } else if (path.equals("/api/v1/evolution/policy") && "GET".equals(method)) {
            sendJson(exchange, 200, "{\"autoEvolve\":false,\"minScore\":0.8,\"maxCandidates\":5}");
        } else {
            sendJson(exchange, 404, "{\"error\":\"Not found\"}");
        }
    }

    // ── Debugger ──

    private void handleDebugger(HttpExchange exchange, String method, String path) throws IOException {
        if (path.equals("/api/v1/debugger/sessions") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"id\":\"dbg-001\",\"executionId\":\"exec-002\",\"status\":\"PAUSED\",\"breakpoints\":2,\"createdAt\":\"2026-09-27T08:00:00Z\"}]");
        } else if (path.equals("/api/v1/debugger/breakpoints") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"id\":\"bp-001\",\"sessionId\":\"dbg-001\",\"nodeId\":\"r3\",\"condition\":\"topK > 3\",\"enabled\":true}]");
        } else if (path.equals("/api/v1/debugger/cursor") && "GET".equals(method)) {
            sendJson(exchange, 200, "{\"sessionId\":\"dbg-001\",\"currentNode\":\"r3\",\"state\":{}}");
        } else if (path.equals("/api/v1/debugger/events") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"type\":\"BREAKPOINT_HIT\",\"nodeId\":\"r3\",\"timestamp\":\"2026-09-27T08:01:00Z\"}]");
        } else {
            sendJson(exchange, 404, "{\"error\":\"Not found\"}");
        }
    }

    // ── Workspaces ──

    private void handleWorkspaces(HttpExchange exchange, String method, String path) throws IOException {
        if (path.equals("/api/v1/workspaces") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"id\":\"ws-001\",\"name\":\"Default Workspace\",\"scope\":\"GLOBAL\",\"status\":\"ACTIVE\",\"sandboxCount\":1,\"artifactCount\":3,\"createdAt\":\"2026-09-20T10:00:00Z\"}]");
        } else if (path.equals("/api/v1/workspaces/sandboxes") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"id\":\"sb-001\",\"workspaceId\":\"ws-001\",\"isolation\":\"PROCESS\",\"status\":\"RUNNING\",\"cpuLimit\":\"2.0\",\"memoryLimit\":\"512MB\",\"createdAt\":\"2026-09-27T08:00:00Z\"}]");
        } else if (path.equals("/api/v1/workspaces/artifacts") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"id\":\"art-001\",\"workspaceId\":\"ws-001\",\"name\":\"output.json\",\"type\":\"FILE\",\"size\":1024,\"createdAt\":\"2026-09-27T08:00:00Z\"}]");
        } else {
            sendJson(exchange, 404, "{\"error\":\"Not found\"}");
        }
    }

    // ── Multimodal ──

    private void handleMultimodal(HttpExchange exchange, String method, String path) throws IOException {
        if (path.equals("/api/v1/multimodal/content") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"id\":\"mc-001\",\"type\":\"TEXT\",\"mimeType\":\"text/plain\",\"size\":256}]");
        } else if (path.equals("/api/v1/multimodal/images") && "GET".equals(method)) {
            sendJson(exchange, 200, "[{\"id\":\"img-001\",\"name\":\"diagram.png\",\"mimeType\":\"image/png\",\"width\":800,\"height\":600}]");
        } else if (path.equals("/api/v1/multimodal/realtime") && "GET".equals(method)) {
            sendJson(exchange, 200, "{\"activeStreams\":0,\"maxStreams\":10}");
        } else if (path.equals("/api/v1/multimodal/events") && "GET".equals(method)) {
            sendJson(exchange, 200, "[]");
        } else {
            sendJson(exchange, 404, "{\"error\":\"Not found\"}");
        }
    }

    // ── Utilities ──

    private boolean matchesQuery(Map<String, Object> item, String query) {
        if (query == null || query.isEmpty()) return true;
        String q = query.toLowerCase();
        return item.values().stream().anyMatch(v -> v != null && v.toString().toLowerCase().contains(q));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonBody(HttpExchange exchange) {
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            return HttpJson.object(body);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    private void sendJsonOk(HttpExchange exchange, Object obj) {
        sendJson(exchange, 200, toJson(obj));
    }

    private String toJson(Object obj) {
        return HttpJson.stringify(obj);
    }

    private void sendJson(HttpExchange exchange, int status, String json) {
        try {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, status == 204 ? -1 : bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    private String resolveTenantId(HttpExchange exchange) {
        SecurityContext ctx = (SecurityContext) exchange.getAttribute(
            AuthenticatedHandler.SECURITY_CONTEXT_ATTR
        );
        if (ctx != null && ctx.tenant() != null) {
            return ctx.tenant().tenantId();
        }
        return "default";
    }
}

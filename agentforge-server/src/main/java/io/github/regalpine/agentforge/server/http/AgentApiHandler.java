package io.github.regalpine.agentforge.server.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.regalpine.agentforge.agent.AgentDefinition;
import io.github.regalpine.agentforge.agent.AgentHealth;
import io.github.regalpine.agentforge.agent.AgentLifecycleState;
import io.github.regalpine.agentforge.agent.AgentMetrics;
import io.github.regalpine.agentforge.agent.AgentRegistrar;
import io.github.regalpine.agentforge.agent.RegistrationOptions;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * HTTP handler for {@code /api/v1/agents}.
 *
 * <p>Exposes the {@link AgentRegistrar}
 * over REST so that Studio (and other
 * clients) can list, inspect, and
 * manage the lifecycle of registered
 * Agents.
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>GET  /api/v1/agents — list all</li>
 *   <li>GET  /api/v1/agents/summaries — lightweight list</li>
 *   <li>GET  /api/v1/agents/{id}/{version} — find one</li>
 *   <li>GET  /api/v1/agents/{id}/overview — health + metrics + def</li>
 *   <li>GET  /api/v1/agents/{id}/definition — definition</li>
 *   <li>GET  /api/v1/agents/{id}/versions — all versions of id</li>
 *   <li>GET  /api/v1/agents/{id}/health — health snapshot</li>
 *   <li>GET  /api/v1/agents/{id}/metrics — metrics snapshot</li>
 *   <li>POST /api/v1/agents — register new</li>
 *   <li>POST /api/v1/agents/{id}/start — start</li>
 *   <li>POST /api/v1/agents/{id}/stop — stop</li>
 *   <li>POST /api/v1/agents/{id}/drain — drain</li>
 *   <li>DELETE /api/v1/agents/{id}/{version} — unregister</li>
 * </ul>
 */
public final class AgentApiHandler implements HttpHandler {

    private final AgentRegistrar registrar;

    public AgentApiHandler(AgentRegistrar registrar) {
        this.registrar = java.util.Objects.requireNonNull(
            registrar, "registrar"
        );
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            route(exchange, path, method);
        } catch (IllegalStateException e) {
            sendJson(exchange, 409,
                errorJson(e.getMessage()));
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400,
                errorJson(e.getMessage()));
        } catch (NullPointerException e) {
            sendJson(exchange, 400,
                errorJson("Missing required field: "
                    + e.getMessage()));
        }
    }

    private void route(
        HttpExchange exchange, String path, String method
    ) throws IOException {
        // GET /api/v1/agents
        if (path.equals("/api/v1/agents")
            && "GET".equals(method)) {
            sendJson(exchange, 200,
                HttpJson.stringify(toAgentList()));
            return;
        }

        // GET /api/v1/agents/summaries
        if (path.equals("/api/v1/agents/summaries")
            && "GET".equals(method)) {
            sendJson(exchange, 200,
                HttpJson.stringify(toSummaryList()));
            return;
        }

        // POST /api/v1/agents
        if (path.equals("/api/v1/agents")
            && "POST".equals(method)) {
            AgentDefinition def = readDefinition(exchange);
            boolean autoStart = !"false".equalsIgnoreCase(
                exchange.getRequestHeaders()
                    .getFirst("X-Auto-Start"));
            registrar.register(def,
                RegistrationOptions.of(autoStart));
            sendJson(exchange, 201,
                HttpJson.stringify(toAgentMap(def)));
            return;
        }

        // POST /api/v1/agents/{id}/start|stop|drain
        if (path.matches("/api/v1/agents/[^/]+/(start|stop|drain)")
            && "POST".equals(method)) {
            String[] parts = path.split("/");
            String id = parts[4];
            String action = parts[5];
            Reference ref = findFirstRef(id);
            if (ref == null) {
                sendJson(exchange, 404,
                    errorJson("Agent not found: " + id));
                return;
            }
            switch (action) {
                case "start" -> registrar.start(ref);
                case "stop"  -> registrar.stop(ref);
                case "drain" -> registrar.drain(ref);
                default -> {
                    sendJson(exchange, 404,
                        errorJson("Unknown action: " + action));
                    return;
                }
            }
            sendJson(exchange, 200,
                "{\"status\":\"" + action + "\"}");
            return;
        }

        // DELETE /api/v1/agents/{id}/{version}
        if (path.matches("/api/v1/agents/[^/]+/[^/]+")
            && "DELETE".equals(method)) {
            String[] parts = path.split("/");
            Reference ref = new Reference(
                new EntityId(parts[4]),
                new Version(parts[5])
            );
            registrar.unregister(ref);
            sendJson(exchange, 204, "");
            return;
        }

        // GET /api/v1/agents/{id}/overview
        if (path.matches("/api/v1/agents/[^/]+/overview")
            && "GET".equals(method)) {
            String id = path.split("/")[4];
            Reference ref = findFirstRef(id);
            if (ref == null) {
                sendJson(exchange, 404,
                    errorJson("Agent not found: " + id));
                return;
            }
            sendJson(exchange, 200,
                HttpJson.stringify(toOverview(ref)));
            return;
        }

        // GET /api/v1/agents/{id}/definition
        if (path.matches("/api/v1/agents/[^/]+/definition")
            && "GET".equals(method)) {
            String id = path.split("/")[4];
            Reference ref = findFirstRef(id);
            if (ref == null) {
                sendJson(exchange, 404,
                    errorJson("Agent not found: " + id));
                return;
            }
            Optional<AgentDefinition> def =
                registrar.find(ref);
            if (def.isEmpty()) {
                sendJson(exchange, 404,
                    errorJson("Agent not found: " + id));
                return;
            }
            sendJson(exchange, 200,
                HttpJson.stringify(toDefinitionMap(def.get())));
            return;
        }

        // GET /api/v1/agents/{id}/versions
        if (path.matches("/api/v1/agents/[^/]+/versions")
            && "GET".equals(method)) {
            String id = path.split("/")[4];
            List<Map<String, Object>> versions = new ArrayList<>();
            for (AgentDefinition a : registrar.list()) {
                if (a.id().value().equals(id)) {
                    Reference vr = new Reference(a.id(), a.version());
                    Map<String, Object> vm = new LinkedHashMap<>();
                    vm.put("version", a.version().value());
                    vm.put("status", stateOf(vr));
                    vm.put("createdAt", java.time.Instant.now().toString());
                    versions.add(vm);
                }
            }
            sendJson(exchange, 200,
                HttpJson.stringify(versions));
            return;
        }

        // GET /api/v1/agents/{id}/health
        if (path.matches("/api/v1/agents/[^/]+/health")
            && "GET".equals(method)) {
            String id = path.split("/")[4];
            Reference ref = findFirstRef(id);
            if (ref == null) {
                sendJson(exchange, 404,
                    errorJson("Agent not found: " + id));
                return;
            }
            sendJson(exchange, 200,
                HttpJson.stringify(toHealthMap(registrar.health(ref))));
            return;
        }

        // GET /api/v1/agents/{id}/metrics
        if (path.matches("/api/v1/agents/[^/]+/metrics")
            && "GET".equals(method)) {
            String id = path.split("/")[4];
            Reference ref = findFirstRef(id);
            if (ref == null) {
                sendJson(exchange, 404,
                    errorJson("Agent not found: " + id));
                return;
            }
            sendJson(exchange, 200,
                HttpJson.stringify(toMetricsMap(registrar.metrics(ref))));
            return;
        }

        // GET /api/v1/agents/{id}/{version}
        if (path.matches("/api/v1/agents/[^/]+/[^/]+")
            && "GET".equals(method)) {
            String[] parts = path.split("/");
            Reference ref = new Reference(
                new EntityId(parts[4]),
                new Version(parts[5])
            );
            Optional<AgentDefinition> def =
                registrar.find(ref);
            if (def.isEmpty()) {
                sendJson(exchange, 404,
                    errorJson("Agent not found"));
                return;
            }
            sendJson(exchange, 200,
                HttpJson.stringify(toAgentMap(def.get())));
            return;
        }

        sendJson(exchange, 404,
            errorJson("Not found: " + method + " " + path));
    }

    // ── mapping helpers ──

    private List<Map<String, Object>> toAgentList() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (AgentDefinition a : registrar.list()) {
            result.add(toAgentMap(a));
        }
        return result;
    }

    private List<Map<String, Object>> toSummaryList() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (AgentDefinition a : registrar.list()) {
            Reference r = new Reference(a.id(), a.version());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.id().value());
            m.put("name", a.name());
            m.put("version", a.version().value());
            m.put("status", lifecycleToStatus(r));
            m.put("description", "");
            m.put("graphCount", 0);
            result.add(m);
        }
        return result;
    }

    private Map<String, Object> toAgentMap(AgentDefinition a) {
        Reference r = new Reference(a.id(), a.version());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.id().value());
        m.put("name", a.name());
        m.put("version", a.version().value());
        m.put("status", lifecycleToStatus(r));
        m.put("description", "");
        m.put("capabilities", a.capabilities().stream()
            .map(c -> Map.of(
                "id", c.id().value(),
                "version", c.version().value()
            ))
            .toList());
        return m;
    }

    private Map<String, Object> toDefinitionMap(
        AgentDefinition a
    ) {
        Reference r = new Reference(a.id(), a.version());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.id().value());
        m.put("name", a.name());
        m.put("version", a.version().value());
        m.put("status", lifecycleToStatus(r));
        m.put("identity", Map.of(
            "agentId", a.id().value(),
            "name", a.name()
        ));
        m.put("contract", Map.of());
        m.put("model", Map.of());
        return m;
    }

    private Map<String, Object> toOverview(Reference r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.id().value());
        m.put("version", r.version().value());
        m.put("status", lifecycleToStatus(r));
        m.put("name", "");
        m.put("description", "");
        registrar.find(r).ifPresent(def -> {
            m.put("name", def.name());
            m.put("capabilities", def.capabilities().stream()
                .map(c -> Map.of(
                    "id", c.id().value(),
                    "version", c.version().value()
                ))
                .toList());
        });
        m.put("metrics", toMetricsMap(registrar.metrics(r)));
        return m;
    }

    private Map<String, Object> toHealthMap(AgentHealth h) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", h.status().name());
        Map<String, Object> comps = new LinkedHashMap<>();
        h.components().forEach((k, v) ->
            comps.put(k, Map.of(
                "name", v.name(),
                "status", v.status().name(),
                "detail", v.detail() != null ? v.detail() : ""
            ))
        );
        m.put("components", comps);
        return m;
    }

    private Map<String, Object> toMetricsMap(AgentMetrics mIn) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("invocations", mIn.invocations());
        m.put("failures", mIn.failures());
        m.put("successRate", mIn.successRate());
        m.put("p50Millis", mIn.p50().toMillis());
        m.put("p99Millis", mIn.p99().toMillis());
        return m;
    }

    /**
     * Map internal lifecycle state to
     * the OpenAPI {@code status} field
     * expected by Studio.
     */
    private String lifecycleToStatus(Reference r) {
        AgentLifecycleState s =
            registrar.lifecycleState(r);
        if (s == null) return "DRAFT";
        return switch (s) {
            case ACTIVE -> "ACTIVE";
            case REGISTERED, STARTING -> "DRAFT";
            case DRAINING, STOPPED -> "DEPRECATED";
            case FAILED -> "RETIRED";
        };
    }

    private String stateOf(Reference r) {
        return lifecycleToStatus(r);
    }

    private Reference findFirstRef(String id) {
        for (AgentDefinition a : registrar.list()) {
            if (a.id().value().equals(id)) {
                return new Reference(a.id(), a.version());
            }
        }
        return null;
    }

    // ── request body parsing ──

    private AgentDefinition readDefinition(
        HttpExchange exchange
    ) throws IOException {
        String body;
        try (InputStream is = exchange.getRequestBody()) {
            body = new String(is.readAllBytes(),
                StandardCharsets.UTF_8);
        }
        Map<String, Object> map = HttpJson.object(body);
        String id = (String) map.get("id");
        String version = (String) map.get("version");
        String name = (String) map.get("name");
        if (id == null || version == null || name == null) {
            throw new IllegalArgumentException(
                "id, version, name are required"
            );
        }
        return new AgentDefinition(
            new EntityId(id),
            new Version(version),
            name,
            Set.of()
        );
    }

    // ── helpers ──

    private static String errorJson(String message) {
        String safe = message == null ? "" : message;
        safe = safe.replace("\"", "\\\"");
        return "{\"error\":\"" + safe + "\"}";
    }

    private static void sendJson(
        HttpExchange exchange, int code, String body
    ) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        if (code == 204) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }
        exchange.getResponseHeaders().set(
            "Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}

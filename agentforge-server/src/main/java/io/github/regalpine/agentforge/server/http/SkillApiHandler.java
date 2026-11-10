package io.github.regalpine.agentforge.server.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.regalpine.agentforge.kernel.SkillDescriptor;
import io.github.regalpine.agentforge.skill.governance.SkillLifecycleState;
import io.github.regalpine.agentforge.skill.governance.SkillResult;
import io.github.regalpine.agentforge.server.service.SkillService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Skill REST API Handler.
 * AF-SKILL-04 §44-§49: Skill REST API.
 * AF-AD-15 §11.2: Server Skill API.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>GET  /api/v1/skills — List all</li>
 *   <li>GET  /api/v1/skills/{id} — Get one</li>
 *   <li>GET  /api/v1/skills/{id}/versions — Get versions</li>
 *   <li>PUT  /api/v1/skills/{id}/versions/{v}/lifecycle — Transition</li>
 *   <li>POST /api/v1/skills/{id}/invoke — Invoke</li>
 * </ul>
 *
 * <p>N: Tenant isolation via X-Tenant-Id
 * header. RBAC via Authorization header.
 */
public final class SkillApiHandler implements HttpHandler {

    private final SkillService skillService;

    public SkillApiHandler(SkillService skillService) {
        this.skillService = skillService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if (path.equals("/api/v1/skills")
                && "GET".equals(method)
            ) {
                handleListSkills(exchange);
            } else if (path.matches(
                "/api/v1/skills/[^/]+/versions/[^/]+/lifecycle"
            ) && "PUT".equals(method)) {
                handleLifecycle(exchange);
            } else if (path.matches(
                "/api/v1/skills/[^/]+/invoke"
            ) && "POST".equals(method)) {
                handleInvoke(exchange);
            } else if (path.matches(
                "/api/v1/skills/[^/]+/versions"
            ) && "GET".equals(method)) {
                handleVersions(exchange);
            } else if (path.matches(
                "/api/v1/skills/[^/]+"
            ) && "GET".equals(method)) {
                handleGetSkill(exchange);
            } else {
                sendJson(exchange, 404,
                    "{\"error\":\"Not found\"}"
                );
            }
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400,
                "{\"error\":\"" + escape(e.getMessage()) + "\"}"
            );
        } catch (IllegalStateException e) {
            sendJson(exchange, 409,
                "{\"error\":\"" + escape(e.getMessage()) + "\"}"
            );
        } catch (Exception e) {
            sendJson(exchange, 500,
                "{\"error\":\"Internal server error\"}"
            );
        }
    }

    // ── GET /api/v1/skills ─────

    private void handleListSkills(
        HttpExchange exchange
    ) throws IOException {
        List<SkillDescriptor> skills =
            skillService.listSkills();

        StringBuilder json = new StringBuilder();
        json.append("{\"skills\":[");

        for (int i = 0; i < skills.size(); i++) {
            if (i > 0) json.append(",");
            appendDescriptor(json, skills.get(i));
        }

        json.append("],\"total\":")
            .append(skills.size())
            .append("}");

        sendJson(exchange, 200, json.toString());
    }

    // ── GET /api/v1/skills/{id} ─

    private void handleGetSkill(
        HttpExchange exchange
    ) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String skillId = path.split("/")[4];

        Optional<SkillDescriptor> opt =
            skillService.getSkill(skillId);

        if (opt.isPresent()) {
            StringBuilder json = new StringBuilder();
            appendDescriptor(json, opt.get());
            sendJson(exchange, 200, json.toString());
        } else {
            sendJson(exchange, 404,
                "{\"error\":\"Skill not found: "
                    + escape(skillId) + "\"}"
            );
        }
    }

    // ── GET /api/v1/skills/{id}/versions ─

    private void handleVersions(
        HttpExchange exchange
    ) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String skillId = path.split("/")[4];

        List<SkillDescriptor> versions =
            skillService.getSkillVersions(skillId);

        StringBuilder json = new StringBuilder();
        json.append("{\"skillId\":\"")
            .append(escape(skillId))
            .append("\",\"versions\":[");

        for (int i = 0; i < versions.size(); i++) {
            if (i > 0) json.append(",");
            appendDescriptor(json, versions.get(i));
        }

        json.append("]}");
        sendJson(exchange, 200, json.toString());
    }

    // ── PUT lifecycle ───────────

    private void handleLifecycle(
        HttpExchange exchange
    ) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        String skillId = parts[4];
        String version = parts[6];

        String body = readBody(exchange);
        String targetState = extractField(body, "state");

        if (targetState == null || targetState.isBlank()) {
            sendJson(exchange, 400,
                "{\"error\":\"Missing 'state' field\"}"
            );
            return;
        }

        SkillLifecycleState target =
            SkillLifecycleState.valueOf(targetState);

        skillService.transition(
            skillId, version, target
        );

        sendJson(exchange, 200,
            "{\"skillId\":\"" + escape(skillId)
                + "\",\"version\":\"" + escape(version)
                + "\",\"state\":\"" + target.name()
                + "\"}"
        );
    }

    // ── POST invoke ─────────────

    private void handleInvoke(
        HttpExchange exchange
    ) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        String skillId = parts[4];

        String tenantId = exchange.getRequestHeaders()
            .getFirst("X-Tenant-Id");

        String body = readBody(exchange);
        String version = extractField(body, "version");
        if (version == null || version.isBlank()) {
            version = "latest";
        }

        // Simple input extraction
        String inputJson = extractField(body, "input");

        CompletionStage<SkillResult> stage =
            skillService.invoke(
                skillId, version,
                java.util.Map.of(),
                tenantId
            );

        SkillResult result = stage
            .toCompletableFuture()
            .join();

        String status = result.isSuccess()
            ? "SUCCESS" : "FAILED";

        sendJson(exchange,
            result.isSuccess() ? 200 : 500,
            "{\"skillId\":\"" + escape(skillId)
                + "\",\"version\":\"" + escape(version)
                + "\",\"status\":\"" + status
                + "\",\"errorCode\":"
                + (result.errorCode() != null
                    ? "\"" + result.errorCode().name() + "\""
                    : "null")
                + ",\"errorMessage\":"
                + (result.errorMessage() != null
                    ? "\"" + escape(result.errorMessage()) + "\""
                    : "null")
                + "}"
        );
    }

    // ── Utilities ───────────────

    private static void appendDescriptor(
        StringBuilder json,
        SkillDescriptor d
    ) {
        json.append("{")
            .append("\"skillId\":\"")
            .append(escape(d.skillId().value())).append("\",")
            .append("\"name\":\"")
            .append(escape(d.name())).append("\",")
            .append("\"version\":\"")
            .append(escape(d.version().value())).append("\",")
            .append("\"goal\":\"")
            .append(escape(d.goal())).append("\",")
            .append("\"strategy\":\"")
            .append(d.strategy().name()).append("\",")
            .append("\"owner\":\"")
            .append(escape(d.owner())).append("\",")
            .append("\"status\":\"")
            .append(escape(d.status())).append("\"")
            .append("}");
    }

    private static void sendJson(
        HttpExchange exchange,
        int statusCode,
        String json
    ) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
            "Content-Type", "application/json"
        );
        exchange.sendResponseHeaders(
            statusCode, bytes.length
        );
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readBody(
        HttpExchange exchange
    ) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             java.io.ByteArrayOutputStream bos =
                 new java.io.ByteArrayOutputStream()) {
            is.transferTo(bos);
            return bos.toString(StandardCharsets.UTF_8);
        }
    }

    private static String extractField(
        String json, String field
    ) {
        if (json == null) return null;
        String pattern = "\"" + field + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) return null;
        int colonIdx = json.indexOf(
            ":", idx + pattern.length()
        );
        if (colonIdx < 0) return null;
        int quoteStart = json.indexOf("\"", colonIdx + 1);
        if (quoteStart < 0) return null;
        int quoteEnd = json.indexOf("\"", quoteStart + 1);
        if (quoteEnd < 0) return null;
        return json.substring(quoteStart + 1, quoteEnd);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }
}

package io.github.regalpine.agentforge.server.http;

import com.sun.net.httpserver.HttpExchange;
import java.util.Set;

/** 业务路由的显式权限表；没有匹配项时拒绝，不按前缀猜测写权限。 */
final class ApiPermissions {
    private ApiPermissions() {}

    static Set<String> required(HttpExchange exchange) {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/api/v1/graphs")) {
            return switch (method) {
                case "GET" -> Set.of("graph:read");
                case "POST" -> Set.of("graph:create");
                default -> null;
            };
        }
        if (method.equals("POST") && path.matches("/api/v1/graphs/(?:[^/]+/[^/]+/)?(validate|compile)")) {
            return Set.of(path.endsWith("/compile") ? "graph:publish" : "graph:read");
        }
        if (path.matches("/api/v1/graphs/[^/]+")) {
            return switch (method) {
                case "GET" -> Set.of("graph:read");
                case "PUT" -> Set.of("graph:update");
                case "DELETE" -> Set.of("graph:delete");
                default -> null;
            };
        }
        if (method.equals("GET") && path.matches("/api/v1/graphs/[^/]+/definition")) {
            return Set.of("graph:read");
        }
        if (method.equals("POST") && path.equals("/api/v1/executions/submit")) {
            return Set.of("execution:create");
        }
        if (method.equals("GET") && path.matches("/api/v1/executions(?:/[^/]+(?:/(state|timeline|agent-loop|context|trace|tool-calls|model-calls|checkpoints|events))?)?")) {
            return Set.of("execution:read");
        }
        if (method.equals("POST") && path.matches("/api/v1/executions/[^/]+/(pause|resume|cancel)")) {
            return Set.of("execution:" + path.substring(path.lastIndexOf('/') + 1));
        }
        if (method.equals("GET") && path.matches("/api/v1/executions/[^/]+/events/stream")) {
            return Set.of("execution:read");
        }
        if (method.equals("POST") && (path.equals("/api/v1/search")
                || path.equals("/api/v1/memories/query")
                || path.matches("/api/v1/(human-approvals|governance/approvals)/[^/]+/decide"))) {
            return Set.of("admin:system");
        }
        // 尚未建立细粒度授权的外围演示资源仅向系统管理员开放。
        if (method.equals("GET") && path.matches("/api/v1/(agents|skills|capabilities|memories|knowledge|mcp|governance|evaluations|observability|human-approvals|search|diff|versions)(/[^/]+)*")) {
            return Set.of("admin:system");
        }
        return null;
    }
}

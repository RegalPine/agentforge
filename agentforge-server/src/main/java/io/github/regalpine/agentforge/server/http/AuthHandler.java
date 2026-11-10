package io.github.regalpine.agentforge.server.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.regalpine.agentforge.security.*;
import io.github.regalpine.agentforge.security.iam.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 认证 API 处理器.
 *
 * <p>POST /api/auth/login — Basic auth → session token
 */
public final class AuthHandler implements HttpHandler {

    private final BuiltInAuthenticationProvider authProvider;
    private final SessionTokenProvider sessionProvider;

    public AuthHandler(
        BuiltInAuthenticationProvider authProvider,
        SessionTokenProvider sessionProvider
    ) {
        this.authProvider = authProvider;
        this.sessionProvider = sessionProvider;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if ("POST".equals(method) && "/api/auth/login".equals(path)) {
            handleLogin(exchange);
        } else if ("POST".equals(method) && "/api/auth/logout".equals(path)) {
            handleLogout(exchange);
        } else {
            sendJson(exchange, 404, "{\"error\":\"Not found\"}");
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders()
            .getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            sendJson(exchange, 401,
                "{\"message\":\"Missing or invalid Authorization header\"}");
            return;
        }

        AuthenticationRequest request =
            new AuthenticationRequest(authHeader);
        AuthenticationResult result = authProvider.authenticate(request);

        if (result instanceof AuthenticationSuccess success) {
            String subjectId = success.securityContext()
                .identity().subjectId();
            String tenantId = success.securityContext()
                .tenant().tenantId();

            String token = sessionProvider.createSession(
                subjectId, tenantId);

            // Build user response
            var identity = success.securityContext().identity();
            Set<String> roles = identity.roles();
            Set<String> perms = identity.permissions();

            StringBuilder rolesJson = new StringBuilder("[");
            int i = 0;
            for (String r : roles) {
                if (i++ > 0) rolesJson.append(",");
                rolesJson.append("\"").append(esc(r)).append("\"");
            }
            rolesJson.append("]");

            StringBuilder permsJson = new StringBuilder("[");
            i = 0;
            for (String p : perms) {
                if (i++ > 0) permsJson.append(",");
                permsJson.append("\"").append(esc(p)).append("\"");
            }
            permsJson.append("]");

            String json = """
                {"token":"%s","user":{"userId":"%s","username":"%s","displayName":"%s","tenantId":"%s","roles":%s,"permissions":%s}}
                """.formatted(
                token,
                subjectId,
                esc(subjectId),
                esc(subjectId),
                tenantId,
                rolesJson,
                permsJson
            );

            sendJson(exchange, 200, json);
        } else if (result instanceof AuthenticationFailure failure) {
            sendJson(exchange, 401,
                "{\"message\":\"" + esc(failure.message()) + "\"}");
        } else {
            sendJson(exchange, 500,
                "{\"message\":\"Internal error\"}");
        }
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders()
            .getFirst("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            sessionProvider.invalidate(token);
        }

        sendJson(exchange, 200, "{\"message\":\"Logged out\"}");
    }

    private static void sendJson(
        HttpExchange exchange, int code, String json
    ) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders()
            .set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"");
    }
}

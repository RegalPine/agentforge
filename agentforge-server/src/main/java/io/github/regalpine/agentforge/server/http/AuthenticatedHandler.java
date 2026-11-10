package io.github.regalpine.agentforge.server.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.regalpine.agentforge.security.*;
import io.github.regalpine.agentforge.security.iam.SessionTokenProvider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.Objects;
import java.util.function.Function;
import io.github.regalpine.agentforge.security.auth.PermissionBasedAuthorizer;

/**
 * Authentication/Authorization filter for API routes.
 *
 * <p>Wraps a delegate {@link HttpHandler} and enforces:
 * <ul>
 *   <li>Bearer token validation via {@link SessionTokenProvider}</li>
 *   <li>Security context (identity, tenant) stored as exchange attributes</li>
 *   <li>401 for missing/invalid tokens</li>
 *   <li>403 for missing required permissions</li>
 * </ul>
 *
 * <p>Downstream handlers retrieve the security context via:
 * <pre>{@code
 * SecurityContext ctx = (SecurityContext) exchange.getAttribute("securityContext");
 * }</pre>
 */
public final class AuthenticatedHandler implements HttpHandler {

    /** Exchange attribute key for the resolved security context. */
    public static final String SECURITY_CONTEXT_ATTR = "securityContext";

    private final HttpHandler delegate;
    private final SessionTokenProvider sessionProvider;
    private final Function<HttpExchange, Set<String>> permissions;
    private final PermissionBasedAuthorizer authorizer = new PermissionBasedAuthorizer();

    /**
     * Create an authenticated handler that requires no specific permissions
     * (any authenticated user is allowed).
     */
    public AuthenticatedHandler(
        HttpHandler delegate,
        SessionTokenProvider sessionProvider
    ) {
        this(delegate, sessionProvider, Set.of());
    }

    /**
     * Create an authenticated handler that requires all of the given permissions.
     */
    public AuthenticatedHandler(
        HttpHandler delegate,
        SessionTokenProvider sessionProvider,
        Set<String> requiredPermissions
    ) {
        this(delegate, sessionProvider, exchange -> Set.copyOf(requiredPermissions));
    }

    AuthenticatedHandler(HttpHandler delegate, SessionTokenProvider sessionProvider,
                         Function<HttpExchange, Set<String>> permissions) {
        this.delegate = Objects.requireNonNull(delegate);
        this.sessionProvider = Objects.requireNonNull(sessionProvider);
        this.permissions = Objects.requireNonNull(permissions);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange = new RequestScopedExchange(exchange);
        String authHeader = exchange.getRequestHeaders()
            .getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendJson(exchange, 401,
                "{\"error\":\"Missing or invalid Authorization header\"}");
            return;
        }

        String token = authHeader.substring(7);
        AuthenticationResult result = sessionProvider.validate(token);

        if (result instanceof AuthenticationSuccess success) {
            SecurityContext ctx = success.securityContext();

            Set<String> requiredPermissions = permissions.apply(exchange);
            if (requiredPermissions == null) {
                auditDenied(ctx, "UNKNOWN_OPERATION");
                sendJson(exchange, 403, "{\"error\":\"Operation not allowed\"}");
                return;
            }
            // Check required permissions
            if (!requiredPermissions.isEmpty()) {
                for (String required : requiredPermissions) {
                    if (!authorizer.authorize(new AuthorizationRequest(ctx.identity(),
                            ctx.tenant().tenantId(), "api", null, required))) {
                        auditDenied(ctx, "MISSING_PERMISSION");
                        sendJson(exchange, 403,
                            "{\"error\":\"Insufficient permissions: " + required + "\"}");
                        return;
                    }
                }
            }

            // Store security context for downstream handlers
            exchange.setAttribute(SECURITY_CONTEXT_ATTR, ctx);
            delegate.handle(exchange);
        } else if (result instanceof AuthenticationFailure failure) {
            sendJson(exchange, 401,
                "{\"error\":\"" + esc(failure.message()) + "\"}");
        } else {
            sendJson(exchange, 500,
                "{\"error\":\"Internal authentication error\"}");
        }
    }

    static void auditDenied(SecurityContext context, String reason) {
        if (context == null) return;
        // 不记录 token、正文或外部租户的对象信息。
        System.getLogger("agentforge.security.audit").log(System.Logger.Level.WARNING,
            "authorization_denied tenant={0} subject={1} reason={2}",
            context.tenant().tenantId().replaceAll("[\\r\\n\\t]", "_"),
            String.valueOf(context.identity().subjectId()).replaceAll("[\\r\\n\\t]", "_"), reason);
    }

    private static void sendJson(
        HttpExchange exchange, int code, String json
    ) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders()
            .set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length);
        try (var os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"");
    }
}

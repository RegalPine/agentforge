package io.github.regalpine.agentforge.capability.http;

import io.github.regalpine.agentforge.capability.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * HTTP capability adapter.
 * AF-ENHANCE-02 §8-10: invokes
 * external HTTP/REST endpoints as
 * capabilities using JDK HttpClient.
 *
 * <p>Security pipeline:
 * <pre>
 * URL Parse → SSRF Guard → Build Request
 * → Apply Auth → Send → Map Response
 * </pre>
 */
public final class HttpCapabilityAdapter
    implements CapabilityAdapter {

    private final HttpCapabilityDescriptor descriptor;
    private final SsrfGuard ssrfGuard;
    private final HttpClient httpClient;

    private volatile AdapterHealthStatus healthStatus =
        AdapterHealthStatus.UNKNOWN;

    public HttpCapabilityAdapter(
        HttpCapabilityDescriptor descriptor
    ) {
        this(descriptor, null, null);
    }

    public HttpCapabilityAdapter(
        HttpCapabilityDescriptor descriptor,
        SsrfGuard ssrfGuard,
        HttpClient httpClient
    ) {
        Objects.requireNonNull(
            descriptor, "descriptor"
        );

        this.descriptor = descriptor;

        this.ssrfGuard =
            ssrfGuard != null
                ? ssrfGuard
                : new SsrfGuard(
                    descriptor.allowedHosts()
                  );

        this.httpClient =
            httpClient != null
                ? httpClient
                : HttpClient.newBuilder()
                    .connectTimeout(
                        descriptor.timeout()
                    )
                    .build();
    }

    @Override
    public AdapterMetadata metadata() {
        return new AdapterMetadata(
            "http-adapter-"
                + descriptor.capabilityId(),
            "http",
            "1.0.0",
            "HTTP Adapter: "
                + descriptor.name(),
            descriptor.description(),
            Set.of(
                SideEffectClassification.READ_ONLY,
                SideEffectClassification.WRITE,
                SideEffectClassification.EXTERNAL_SIDE_EFFECT
            ),
            Map.of(),
            descriptor.metadata()
        );
    }

    @Override
    public CapabilityResult execute(
        CapabilityInvocation invocation
    ) {
        try {
            // 1. Build URI
            URI requestUri =
                descriptor.resolveUri();

            // 2. SSRF validation
            ssrfGuard.validate(requestUri);

            // 3. Build request
            HttpRequest.Builder reqBuilder =
                HttpRequest.newBuilder()
                    .uri(requestUri)
                    .timeout(descriptor.timeout());

            // 4. Apply default headers
            for (var entry :
                descriptor.defaultHeaders().entrySet()
            ) {
                reqBuilder.header(
                    entry.getKey(),
                    entry.getValue()
                );
            }

            reqBuilder.header(
                "Content-Type",
                "application/json"
            );

            reqBuilder.header(
                "Accept",
                "application/json"
            );

            // 5. Apply auth
            applyAuth(reqBuilder);

            // 6. Set method + body
            HttpMethod method =
                descriptor.method();

            String body = buildRequestBody(
                invocation
            );

            switch (method) {
                case GET ->
                    reqBuilder.GET();

                case DELETE ->
                    reqBuilder.DELETE();

                case HEAD ->
                    reqBuilder.method(
                        "HEAD",
                        HttpRequest.BodyPublishers.noBody()
                    );

                case POST ->
                    reqBuilder.POST(
                        body != null
                            ? HttpRequest.BodyPublishers
                                .ofString(body)
                            : HttpRequest.BodyPublishers
                                .noBody()
                    );

                case PUT ->
                    reqBuilder.PUT(
                        body != null
                            ? HttpRequest.BodyPublishers
                                .ofString(body)
                            : HttpRequest.BodyPublishers
                                .noBody()
                    );

                case PATCH ->
                    reqBuilder.method(
                        "PATCH",
                        body != null
                            ? HttpRequest.BodyPublishers
                                .ofString(body)
                            : HttpRequest.BodyPublishers
                                .noBody()
                    );

                case OPTIONS ->
                    reqBuilder.method(
                        "OPTIONS",
                        HttpRequest.BodyPublishers.noBody()
                    );
            }

            HttpRequest request =
                reqBuilder.build();

            // 7. Send request
            HttpResponse<String> response =
                httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );

            // 8. Map response
            healthStatus =
                AdapterHealthStatus.AVAILABLE;

            int statusCode = response.statusCode();

            Map<String, Object> output =
                new LinkedHashMap<>();

            output.put("statusCode", statusCode);
            output.put("body", response.body());

            Map<String, String> respHeaders =
                new LinkedHashMap<>();

            response.headers().map().forEach(
                (key, values) -> {
                    if (!values.isEmpty()) {
                        respHeaders.put(
                            key,
                            values.get(0)
                        );
                    }
                }
            );

            output.put("headers", respHeaders);

            if (statusCode >= 200
                && statusCode < 300
            ) {
                return new CapabilitySuccess(output);
            }

            return new CapabilityFailure(
                "HTTP_" + statusCode,
                "HTTP request failed with status "
                    + statusCode,
                statusCode >= 500
            );

        } catch (SsrfGuard.SsrfViolationException e) {
            healthStatus =
                AdapterHealthStatus.AVAILABLE;

            return new CapabilityFailure(
                "SSRF_BLOCKED",
                e.getMessage(),
                false
            );

        } catch (Exception e) {
            healthStatus =
                AdapterHealthStatus.DEGRADED;

            return new CapabilityFailure(
                "HTTP_ERROR",
                e.getMessage(),
                true
            );
        }
    }

    @Override
    public AdapterHealth health() {
        return new AdapterHealth(
            healthStatus,
            Instant.now(),
            null,
            Map.of(
                "baseUrl",
                descriptor.baseUrl().toString(),
                "method",
                descriptor.method().name()
            )
        );
    }

    private void applyAuth(
        HttpRequest.Builder builder
    ) {
        HttpAuthConfig auth =
            descriptor.authConfig();

        if (auth == null
            || auth.type() == HttpAuthType.NONE
        ) {
            return;
        }

        // Auth credentials are resolved
        // externally via CredentialProvider.
        // Here we set the header structure
        // with a placeholder that would be
        // replaced at runtime.
        switch (auth.type()) {
            case BEARER_TOKEN ->
                builder.header(
                    "Authorization",
                    "Bearer ${credential:"
                        + auth.credentialRef() + "}"
                );

            case API_KEY -> {
                String headerName =
                    auth.properties().getOrDefault(
                        "headerName",
                        "X-API-Key"
                    );

                builder.header(
                    headerName,
                    "${credential:"
                        + auth.credentialRef() + "}"
                );
            }

            case BASIC ->
                builder.header(
                    "Authorization",
                    "Basic ${credential:"
                        + auth.credentialRef() + "}"
                );

            default -> { /* NONE — no-op */ }
        }
    }

    private String buildRequestBody(
        CapabilityInvocation invocation
    ) {
        Map<String, Object> args =
            invocation.arguments();

        if (args == null || args.isEmpty()) {
            return null;
        }

        // Minimal JSON serialisation
        // (no Jackson dependency)
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;

        for (var entry : args.entrySet()) {
            if (!first) {
                sb.append(",");
            }

            first = false;

            sb.append("\"")
              .append(escapeJson(entry.getKey()))
              .append("\":");

            sb.append(
                serialiseValue(entry.getValue())
            );
        }

        sb.append("}");

        return sb.toString();
    }

    private String serialiseValue(
        Object value
    ) {
        if (value == null) {
            return "null";
        }

        if (value instanceof Number
            || value instanceof Boolean
        ) {
            return value.toString();
        }

        return "\""
            + escapeJson(value.toString())
            + "\"";
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}

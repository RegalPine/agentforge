package io.github.regalpine.agentforge.provider.http;

import io.github.regalpine.agentforge.capability.*;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class HttpProvider
    implements CapabilityProvider {

    private final HttpClient httpClient;
    private final SsrfValidator ssrfValidator;
    private final Duration defaultTimeout;

    public HttpProvider() {
        this(
            HttpClient.newBuilder()
                .followRedirects(
                    HttpClient.Redirect.NEVER
                )
                .connectTimeout(
                    Duration.ofSeconds(10)
                )
                .build(),
            new SsrfValidator(),
            Duration.ofSeconds(30)
        );
    }

    public HttpProvider(
        HttpClient httpClient,
        SsrfValidator ssrfValidator,
        Duration defaultTimeout
    ) {
        this.httpClient = httpClient;
        this.ssrfValidator = ssrfValidator;
        this.defaultTimeout = defaultTimeout;
    }

    @Override
    public ProviderDescriptor descriptor() {
        return new ProviderDescriptor(
            "http",
            new Version("1.0.0"),
            Set.of(
                new Reference(
                    new EntityId("http.request"),
                    new Version("1.0.0")
                )
            ),
            Map.of("type", "reference")
        );
    }

    @Override
    public CapabilityResult invoke(
        CapabilityInvocation invocation
    ) {
        try {
            String method = getString(
                invocation, "method", "GET"
            );

            String url = getString(
                invocation, "url", null
            );

            if (url == null || url.isBlank()) {
                return new CapabilityFailure(
                    "AF-HTTP-001",
                    "Missing 'url' argument",
                    false
                );
            }

            URI uri = URI.create(url);

            ssrfValidator.validate(uri);

            HttpRequest.Builder reqBuilder =
                HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(defaultTimeout);

            String body = getString(
                invocation, "body", null
            );

            switch (method.toUpperCase()) {
                case "GET" ->
                    reqBuilder.GET();
                case "POST" ->
                    reqBuilder.POST(
                        body == null
                            ? HttpRequest.BodyPublishers
                                .noBody()
                            : HttpRequest.BodyPublishers
                                .ofString(body)
                    );
                case "PUT" ->
                    reqBuilder.PUT(
                        body == null
                            ? HttpRequest.BodyPublishers
                                .noBody()
                            : HttpRequest.BodyPublishers
                                .ofString(body)
                    );
                case "DELETE" ->
                    reqBuilder.DELETE();
                default -> {
                    return new CapabilityFailure(
                        "AF-HTTP-002",
                        "Unsupported method: "
                            + method,
                        false
                    );
                }
            }

            HttpResponse<String> response =
                httpClient.send(
                    reqBuilder.build(),
                    HttpResponse.BodyHandlers
                        .ofString()
                );

            Map<String, Object> output =
                new HashMap<>();

            output.put(
                "status",
                response.statusCode()
            );

            output.put(
                "body",
                response.body()
            );

            return new CapabilitySuccess(output);

        } catch (SecurityException e) {
            return new CapabilityFailure(
                "AF-HTTP-003",
                "SSRF blocked: "
                    + e.getMessage(),
                false
            );
        } catch (IllegalArgumentException e) {
            return new CapabilityFailure(
                "AF-HTTP-004",
                "Invalid request: "
                    + e.getMessage(),
                false
            );
        } catch (Exception e) {
            return new CapabilityFailure(
                "AF-HTTP-005",
                "HTTP error: "
                    + e.getMessage(),
                true
            );
        }
    }

    private static String getString(
        CapabilityInvocation invocation,
        String key,
        String defaultValue
    ) {
        Object value =
            invocation.arguments().get(key);

        if (value == null) {
            return defaultValue;
        }

        return value.toString();
    }
}

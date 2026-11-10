package io.github.regalpine.agentforge.capability.http;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * HTTP capability descriptor.
 * AF-ENHANCE-02 §8: describes an
 * HTTP endpoint that can be invoked
 * as a capability.
 */
public record HttpCapabilityDescriptor(
    String capabilityId,
    String name,
    String description,
    URI baseUrl,
    String path,
    HttpMethod method,
    Map<String, String> defaultHeaders,
    HttpAuthConfig authConfig,
    Duration timeout,
    int maxRetries,
    Set<String> allowedHosts,
    Map<String, Object> bodySchema,
    Map<String, Object> metadata
) {

    public HttpCapabilityDescriptor {
        Objects.requireNonNull(capabilityId, "capabilityId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(baseUrl, "baseUrl");
        Objects.requireNonNull(method, "method");

        if (capabilityId.isBlank()) {
            throw new IllegalArgumentException(
                "capabilityId cannot be blank"
            );
        }

        path =
            path == null || path.isBlank()
                ? "/"
                : path;

        defaultHeaders =
            defaultHeaders == null
                ? Map.of()
                : Map.copyOf(defaultHeaders);

        timeout =
            timeout == null
                ? Duration.ofSeconds(30)
                : timeout;

        if (maxRetries < 0) {
            maxRetries = 0;
        }

        allowedHosts =
            allowedHosts == null
                ? Set.of()
                : Set.copyOf(allowedHosts);

        bodySchema =
            bodySchema == null
                ? Map.of()
                : Map.copyOf(bodySchema);

        metadata =
            metadata == null
                ? Map.of()
                : Map.copyOf(metadata);
    }

    /**
     * Build the full request URI.
     */
    public URI resolveUri() {
        String base = baseUrl.toString();

        if (base.endsWith("/") && path.startsWith("/")) {
            return URI.create(base + path.substring(1));
        }

        if (!base.endsWith("/") && !path.startsWith("/")) {
            return URI.create(base + "/" + path);
        }

        return URI.create(base + path);
    }
}

package io.github.regalpine.agentforge.provider.mcp;

import java.util.Collection;
import java.util.Map;

/**
 * External capability provider SPI.
 * AE-10 §66: ExternalCapabilityProvider.
 *
 * <p>Allows new external protocols
 * (MCP, OpenAPI, gRPC, A2A, custom)
 * to be plugged in without modifying
 * the capability core model.
 */
public interface ExternalCapabilityProvider {

    /**
     * Provider type identifier
     * (e.g. "mcp", "openapi", "grpc").
     */
    String providerType();

    /**
     * Discover available capabilities
     * from the external source.
     */
    Collection<ExternalCapabilityDescriptor> discover(
        ProviderContext context
    );

    /**
     * Context passed to discovery.
     */
    record ProviderContext(
        String serverId,
        String endpoint,
        Map<String, Object> configuration
    ) {

        public ProviderContext {
            configuration =
                configuration == null
                    ? Map.of()
                    : Map.copyOf(configuration);
        }
    }
}

package io.github.regalpine.agentforge.provider.mcp;

import io.github.regalpine.agentforge.capability.*;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * MCP capability adapter bridge.
 * AF-ENHANCE-02 §17: wraps the
 * existing McpProvider into the
 * unified CapabilityAdapter SPI so
 * that MCP tools can participate
 * in the Invocation Pipeline
 * alongside HTTP, JDBC and File
 * adapters.
 *
 * <p>Delegates actual invocation to
 * McpProvider (which already handles
 * circuit breaker, error mapping,
 * and tool invocation).
 */
public final class McpCapabilityAdapter
    implements CapabilityAdapter {

    private final McpProvider mcpProvider;
    private final String adapterId;

    public McpCapabilityAdapter(
        McpProvider mcpProvider
    ) {
        Objects.requireNonNull(
            mcpProvider, "mcpProvider"
        );

        this.mcpProvider = mcpProvider;

        this.adapterId = "mcp-adapter-"
            + mcpProvider.descriptor().providerId();
    }

    @Override
    public AdapterMetadata metadata() {
        ProviderDescriptor desc =
            mcpProvider.descriptor();

        return new AdapterMetadata(
            adapterId,
            "mcp",
            desc.version().value(),
            "MCP Adapter: "
                + desc.providerId(),
            "MCP protocol capability adapter",
            Set.of(
                SideEffectClassification.READ_ONLY,
                SideEffectClassification.WRITE,
                SideEffectClassification.EXTERNAL_SIDE_EFFECT
            ),
            Map.of(),
            desc.metadata()
        );
    }

    @Override
    public CapabilityResult execute(
        CapabilityInvocation invocation
    ) {
        return mcpProvider.invoke(invocation);
    }

    @Override
    public AdapterHealth health() {
        CircuitBreaker.State cbState =
            mcpProvider.circuitBreakerState();

        AdapterHealthStatus status =
            switch (cbState) {
                case CLOSED ->
                    AdapterHealthStatus.AVAILABLE;
                case HALF_OPEN ->
                    AdapterHealthStatus.DEGRADED;
                case OPEN ->
                    AdapterHealthStatus.UNAVAILABLE;
            };

        McpConnection conn =
            mcpProvider.connection();

        return new AdapterHealth(
            status,
            Instant.now(),
            null,
            Map.of(
                "circuitBreaker",
                cbState.name(),
                "connectionStatus",
                conn.status().name()
            )
        );
    }

    @Override
    public void close() {
        // McpProvider manages its own
        // connection lifecycle. The
        // adapter bridge does not own
        // the connection.
    }
}

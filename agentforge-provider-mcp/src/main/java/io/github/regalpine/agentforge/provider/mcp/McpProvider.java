package io.github.regalpine.agentforge.provider.mcp;

import io.github.regalpine.agentforge.capability.*;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Enhanced MCP provider.
 * AE-10 §29: MCP Invocation.
 *
 * <p>Integrates circuit breaker, error
 * mapping, and capability discovery
 * into the existing CapabilityProvider
 * contract.
 */
public final class McpProvider
    implements CapabilityProvider {

    private final McpClientAdapter adapter;
    private final String providerId;
    private final CircuitBreaker circuitBreaker;

    public McpProvider(
        String providerId,
        McpClientAdapter adapter
    ) {
        this(providerId, adapter, CircuitBreaker.defaults());
    }

    public McpProvider(
        String providerId,
        McpClientAdapter adapter,
        CircuitBreaker circuitBreaker
    ) {
        this.providerId = providerId;
        this.adapter = adapter;
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public ProviderDescriptor descriptor() {
        Set<Reference> capabilities =
            new HashSet<>();

        for (McpToolDescriptor tool :
            adapter.discoverTools()
        ) {
            capabilities.add(
                new Reference(
                    new EntityId(
                        providerId + "."
                            + tool.toolName()
                    ),
                    new Version("1.0.0")
                )
            );
        }

        return new ProviderDescriptor(
            providerId,
            new Version("1.0.0"),
            Set.copyOf(capabilities),
            Map.of(
                "type", "mcp",
                "server",
                adapter.descriptor().serverId()
            )
        );
    }

    @Override
    public CapabilityResult invoke(
        CapabilityInvocation invocation
    ) {
        // Check circuit breaker
        if (!circuitBreaker.allowRequest()) {
            return new CapabilityFailure(
                CapabilityErrorType.CONNECTION_ERROR.name(),
                "Circuit breaker is OPEN for "
                    + providerId,
                false
            );
        }

        String capId =
            invocation.capabilityId().value();

        String toolName =
            capId.startsWith(providerId + ".")
                ? capId.substring(
                    providerId.length() + 1
                )
                : capId;

        McpToolInvocation toolInvocation =
            new McpToolInvocation(
                toolName,
                invocation.arguments()
            );

        McpToolResult result;

        try {
            result = adapter.invoke(toolInvocation);
        } catch (Exception e) {
            CapabilityErrorType errorType =
                McpErrorMapper.mapError(e.getMessage());

            circuitBreaker.recordFailure();

            return new CapabilityFailure(
                errorType.name(),
                "MCP invocation error: "
                    + e.getMessage(),
                McpErrorMapper.isRetryable(errorType)
            );
        }

        if (result.success()) {
            circuitBreaker.recordSuccess();

            return new CapabilitySuccess(
                result.output()
            );
        }

        // Map error
        CapabilityErrorType errorType =
            McpErrorMapper.mapError(result.error());

        circuitBreaker.recordFailure();

        return new CapabilityFailure(
            errorType.name(),
            "MCP tool failed: " + result.error(),
            McpErrorMapper.isRetryable(errorType)
        );
    }

    /**
     * Discover external capabilities from
     * the MCP server.
     * AE-10 §11-16.
     */
    public Collection<ExternalCapabilityDescriptor>
        discoverCapabilities()
    {
        return adapter.discoverCapabilities();
    }

    /**
     * Get the current circuit breaker state.
     */
    public CircuitBreaker.State circuitBreakerState() {
        return circuitBreaker.state();
    }

    /**
     * Get the underlying connection.
     */
    public McpConnection connection() {
        return adapter.connection();
    }
}

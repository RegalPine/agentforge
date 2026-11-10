package io.github.regalpine.agentforge.provider.mcp;

import io.github.regalpine.agentforge.capability.*;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class McpProviderTest {

    @Test
    void shouldDiscoverTools() {
        var adapter = new StubMcpAdapter();

        McpProvider provider =
            new McpProvider("mcp", adapter);

        var desc = provider.descriptor();

        assertEquals(2, desc.capabilities().size());
    }

    @Test
    void shouldInvokeToolSuccessfully() {
        var adapter = new StubMcpAdapter();

        McpProvider provider =
            new McpProvider("mcp", adapter);

        CapabilityInvocation invocation =
            new CapabilityInvocation(
                "inv-001",
                new EntityId(
                    "mcp.search_customer"
                ),
                new Version("1.0.0"),
                Map.of("query", "John"),
                null
            );

        var result = provider.invoke(invocation);

        assertInstanceOf(
            CapabilitySuccess.class,
            result
        );
    }

    @Test
    void shouldReturnMappedErrorOnToolFailure() {
        var adapter = new StubMcpAdapter() {
            @Override
            public McpToolResult invoke(
                McpToolInvocation invocation
            ) {
                return new McpToolResult(
                    false,
                    Map.of(),
                    "Connection refused"
                );
            }
        };

        McpProvider provider =
            new McpProvider("mcp", adapter);

        CapabilityInvocation invocation =
            new CapabilityInvocation(
                "inv-002",
                new EntityId(
                    "mcp.search_customer"
                ),
                new Version("1.0.0"),
                Map.of("query", "test"),
                null
            );

        var result = provider.invoke(invocation);

        assertInstanceOf(
            CapabilityFailure.class,
            result
        );

        var failure = (CapabilityFailure) result;

        assertEquals(
            CapabilityErrorType.CONNECTION_ERROR.name(),
            failure.code()
        );

        assertTrue(failure.retryable());
    }

    @Test
    void shouldOpenCircuitBreakerOnFailures() {
        var adapter = new StubMcpAdapter() {
            @Override
            public McpToolResult invoke(
                McpToolInvocation invocation
            ) {
                return new McpToolResult(
                    false, Map.of(),
                    "Connection refused"
                );
            }
        };

        var cb = new CircuitBreaker(
            3, Duration.ofSeconds(60)
        );

        McpProvider provider =
            new McpProvider("mcp", adapter, cb);

        for (int i = 0; i < 3; i++) {
            provider.invoke(
                new CapabilityInvocation(
                    "inv-" + i,
                    new EntityId("mcp.tool"),
                    new Version("1.0.0"),
                    Map.of(),
                    null
                )
            );
        }

        assertEquals(
            CircuitBreaker.State.OPEN,
            provider.circuitBreakerState()
        );

        // Next invocation should be
        // rejected by circuit breaker
        var result = provider.invoke(
            new CapabilityInvocation(
                "inv-blocked",
                new EntityId("mcp.tool"),
                new Version("1.0.0"),
                Map.of(),
                null
            )
        );

        assertInstanceOf(
            CapabilityFailure.class,
            result
        );

        assertTrue(
            ((CapabilityFailure) result)
                .message().contains("Circuit breaker")
        );
    }

    @Test
    void shouldDiscoverCapabilities() {
        var adapter = new StubMcpAdapter();

        McpProvider provider =
            new McpProvider("mcp", adapter);

        Collection<ExternalCapabilityDescriptor> caps =
            provider.discoverCapabilities();

        assertEquals(2, caps.size());

        var first = caps.iterator().next();

        assertEquals(
            CapabilityRegistrationStatus.DISCOVERED,
            first.registrationStatus()
        );

        assertTrue(
            first.identityUri().startsWith("mcp://")
        );
    }

    @Test
    void shouldGetConnection() {
        var adapter = new StubMcpAdapter();

        McpProvider provider =
            new McpProvider("mcp", adapter);

        McpConnection conn =
            provider.connection();

        assertNotNull(conn);

        assertEquals(
            McpConnectionStatus.READY,
            conn.status()
        );
    }

    // --- Stub adapter ---

    private static class StubMcpAdapter
        implements McpClientAdapter {

        private final McpConnection connection;

        StubMcpAdapter() {
            this.connection = new McpConnection(
                "conn-1",
                "stub-server",
                "stdio://local",
                McpTransportType.STDIO,
                McpConnectionStatus.READY,
                null,
                Map.of()
            );
        }

        @Override
        public McpServerDescriptor descriptor() {
            return new McpServerDescriptor(
                "stub-server",
                "2024-11-05",
                Map.of()
            );
        }

        @Override
        public McpConnection connection() {
            return connection;
        }

        @Override
        public McpConnection connect() {
            return connection;
        }

        @Override
        public void disconnect() {
            // no-op
        }

        @Override
        public List<McpToolDescriptor> discoverTools() {
            return List.of(
                new McpToolDescriptor(
                    "search_customer",
                    "Search for a customer",
                    Map.of()
                ),
                new McpToolDescriptor(
                    "get_order",
                    "Get an order",
                    Map.of()
                )
            );
        }

        @Override
        public McpToolResult invoke(
            McpToolInvocation invocation
        ) {
            return new McpToolResult(
                true,
                Map.of(
                    "tool",
                    invocation.toolName(),
                    "result",
                    "ok"
                ),
                null
            );
        }
    }
}

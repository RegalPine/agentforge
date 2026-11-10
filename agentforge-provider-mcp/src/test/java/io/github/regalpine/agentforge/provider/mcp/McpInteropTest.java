package io.github.regalpine.agentforge.provider.mcp;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MCP interoperability types.
 * AF-ENHANCE-10 §4-21, §66-69.
 */
class McpInteropTest {

    // --- McpServerId ---

    @Test
    void serverId_basic() {
        var id = new McpServerId("srv-1");
        assertEquals("srv-1", id.value());
        assertEquals("srv-1", id.toString());
    }

    @Test
    void serverId_rejectsNull() {
        assertThrows(
            NullPointerException.class,
            () -> new McpServerId(null)
        );
    }

    @Test
    void serverId_rejectsBlank() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new McpServerId("  ")
        );
    }

    // --- McpServerInfo ---

    @Test
    void serverInfo_defaults() {
        var id = new McpServerId("srv");
        var info = new McpServerInfo(
            id, "TestServer",
            null, null, null
        );

        assertEquals("srv", info.serverId().value());
        assertEquals("TestServer", info.name());
        assertEquals("0.0.0", info.version());
        assertEquals("2025-06-18", info.protocolVersion());
        assertTrue(info.metadata().isEmpty());
    }

    // --- McpResourceDefinition ---

    @Test
    void resourceDefinition_basic() {
        var res = new McpResourceDefinition(
            "file:///data.csv",
            "Data File",
            "A CSV file",
            "text/csv",
            null
        );

        assertEquals("file:///data.csv", res.uri());
        assertEquals("Data File", res.name());
        assertEquals("text/csv", res.mimeType());
    }

    @Test
    void resourceDefinition_defaults() {
        var res = new McpResourceDefinition(
            "urn:test", "Test",
            null, null, null
        );

        assertEquals("", res.description());
        assertEquals(
            "application/octet-stream",
            res.mimeType()
        );
    }

    // --- McpPromptDefinition ---

    @Test
    void promptDefinition_basic() {
        var arg = new McpPromptDefinition.McpPromptArgument(
            "topic", "The topic", true
        );

        var prompt = new McpPromptDefinition(
            "analyze", "Analyze prompt",
            java.util.List.of(arg), null
        );

        assertEquals("analyze", prompt.name());
        assertEquals(1, prompt.arguments().size());
        assertTrue(prompt.arguments().get(0).required());
    }

    @Test
    void promptDefinition_defaults() {
        var prompt = new McpPromptDefinition(
            "test", null, null, null
        );

        assertEquals("", prompt.description());
        assertTrue(prompt.arguments().isEmpty());
    }

    // --- McpServerTrust ---

    @Test
    void serverTrust_values() {
        assertEquals(4, McpServerTrust.values().length);
        assertNotNull(McpServerTrust.TRUSTED);
        assertNotNull(McpServerTrust.UNTRUSTED);
    }

    // --- McpLifecycle ---

    @Test
    void lifecycle_transitions() {
        assertTrue(
            McpLifecycle.REGISTERED.canTransitionTo(
                McpLifecycle.CONNECTING
            )
        );
        assertTrue(
            McpLifecycle.CONNECTING.canTransitionTo(
                McpLifecycle.CONNECTED
            )
        );
        assertTrue(
            McpLifecycle.CONNECTED.canTransitionTo(
                McpLifecycle.DISCOVERING
            )
        );
        assertTrue(
            McpLifecycle.DISCOVERING.canTransitionTo(
                McpLifecycle.READY
            )
        );
        assertFalse(
            McpLifecycle.READY.canTransitionTo(
                McpLifecycle.CONNECTING
            )
        );
        assertFalse(
            McpLifecycle.REVOKED.canTransitionTo(
                McpLifecycle.REGISTERED
            )
        );
    }

    @Test
    void lifecycle_disableFromAny() {
        assertTrue(
            McpLifecycle.REGISTERED.canTransitionTo(
                McpLifecycle.DISABLED
            )
        );
        assertTrue(
            McpLifecycle.READY.canTransitionTo(
                McpLifecycle.DISABLED
            )
        );
    }

    // --- McpErrorCode ---

    @Test
    void errorCode_fromString() {
        assertEquals(
            McpErrorCode.MCP_TIMEOUT,
            McpErrorCode.fromString("timeout")
        );
        assertEquals(
            McpErrorCode.MCP_CONNECTION_FAILED,
            McpErrorCode.fromString("connection refused")
        );
        assertEquals(
            McpErrorCode.MCP_TOOL_NOT_FOUND,
            McpErrorCode.fromString("tool not found")
        );
        assertEquals(
            McpErrorCode.MCP_PERMISSION_DENIED,
            McpErrorCode.fromString("permission denied")
        );
        assertEquals(
            McpErrorCode.MCP_PROTOCOL_ERROR,
            McpErrorCode.fromString(null)
        );
    }

    // --- McpServerDefinition ---

    @Test
    void serverDefinition_builder() {
        var def = McpServerDefinition.builder()
            .serverId("research")
            .endpoint("http://mcp.example.com")
            .transport(McpTransportType.STREAMABLE_HTTP)
            .credentialReference("cred-1")
            .trust(McpServerTrust.VERIFIED)
            .policyReference("policy-1")
            .metadata(Map.of("env", "prod"))
            .build();

        assertEquals("research", def.serverId().value());
        assertEquals(
            "http://mcp.example.com",
            def.endpoint()
        );
        assertEquals(
            McpServerTrust.VERIFIED,
            def.trust()
        );
    }

    @Test
    void serverDefinition_defaults() {
        var def = McpServerDefinition.of(
            "srv", "http://localhost",
            McpTransportType.STDIO
        );

        assertEquals(
            McpServerTrust.UNTRUSTED,
            def.trust()
        );
        assertEquals("", def.credentialReference());
    }

    // --- McpServerRegistry ---

    @Test
    void registry_registerAndGet() {
        var registry =
            new InMemoryMcpServerRegistry();

        var def = McpServerDefinition.of(
            "srv-1", "http://localhost:8080",
            McpTransportType.STREAMABLE_HTTP
        );

        registry.register(def);

        assertTrue(registry.get(def.serverId()).isPresent());
        assertEquals(1, registry.list().size());
    }

    @Test
    void registry_disable() {
        var registry =
            new InMemoryMcpServerRegistry();

        var def = McpServerDefinition.of(
            "srv-1", "http://localhost",
            McpTransportType.STREAMABLE_HTTP
        );

        registry.register(def);
        registry.disable(new McpServerId("srv-1"));

        var result = registry.get(
            new McpServerId("srv-1")
        );

        assertTrue(result.isPresent());
        assertEquals(
            McpServerTrust.REVOKED,
            result.get().trust()
        );
    }

    @Test
    void registry_revoke() {
        var registry =
            new InMemoryMcpServerRegistry();

        var def = McpServerDefinition.of(
            "srv-1", "http://localhost",
            McpTransportType.STREAMABLE_HTTP
        );

        registry.register(def);
        registry.revoke(new McpServerId("srv-1"));

        assertTrue(registry.get(
            new McpServerId("srv-1")
        ).isEmpty());
    }

    // --- ProtocolVersion ---

    @Test
    void protocolVersion_basic() {
        var v = ProtocolVersion.mcp("2025-06-18");
        assertEquals("2025-06-18", v.value());
        assertEquals("2025-06-18", v.toString());
    }

    @Test
    void protocolVersion_rejectsNull() {
        assertThrows(
            NullPointerException.class,
            () -> new ProtocolVersion(null)
        );
    }

    // --- ProtocolCapabilities ---

    @Test
    void capabilities_full() {
        var caps = ProtocolCapabilities.full();
        assertTrue(caps.discovery());
        assertTrue(caps.invocation());
        assertTrue(caps.streaming());
        assertTrue(caps.authentication());
    }

    @Test
    void capabilities_minimal() {
        var caps = ProtocolCapabilities.minimal();
        assertTrue(caps.discovery());
        assertTrue(caps.invocation());
        assertFalse(caps.streaming());
        assertFalse(caps.authentication());
    }

    @Test
    void capabilities_extensions() {
        var caps = new ProtocolCapabilities(
            true, true, false, false,
            false, false,
            Set.of("batch", "async")
        );

        assertTrue(caps.hasExtension("batch"));
        assertFalse(caps.hasExtension("streaming"));
    }

    // --- ProtocolContext ---

    @Test
    void context_basic() {
        var ctx = ProtocolContext.of(
            "tenant-1",
            "exec-1",
            "trace-1"
        );

        assertEquals("tenant-1", ctx.tenantId());
        assertEquals("exec-1", ctx.executionId());
        assertEquals("trace-1", ctx.traceId());
    }

    @Test
    void context_defaults() {
        var ctx = ProtocolContext.of("t1");
        assertEquals("t1", ctx.tenantId());
        assertEquals("", ctx.executionId());
        assertTrue(ctx.attributes().isEmpty());
    }
}

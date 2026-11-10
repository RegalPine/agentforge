package io.github.regalpine.agentforge.a2a;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for A2A interoperability types.
 * AF-ENHANCE-10 §27-§52.
 */
class A2AInteropTest {

    private InMemoryExternalAgentRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new InMemoryExternalAgentRegistry();
    }

    // --- ExternalAgentReference ---

    @Test
    void reference_basic() {
        var ref = new ExternalAgentReference(
            "ns", "agent-b", "2.0.0",
            URI.create("http://agent-b.example.com")
        );

        assertEquals("ns", ref.namespace());
        assertEquals("agent-b", ref.name());
        assertEquals("2.0.0", ref.version());
        assertEquals("ns:agent-b", ref.qualifiedName());
    }

    @Test
    void reference_defaults() {
        var ref = ExternalAgentReference.of(
            "ns", "agent",
            URI.create("http://localhost")
        );

        assertEquals("1.0.0", ref.version());
    }

    // --- AgentCard ---

    @Test
    void card_builder() {
        var card = AgentCard.builder()
            .agentId("research-agent")
            .name("Research Agent")
            .description("Research capabilities")
            .version("1.0.0")
            .endpoint("http://research.example.com")
            .capabilities(Set.of(
                "research", "analysis"
            ))
            .trust(ExternalAgentTrust.VERIFIED)
            .metadata(Map.of("region", "us"))
            .build();

        assertEquals("research-agent", card.agentId());
        assertEquals("Research Agent", card.name());
        assertTrue(card.hasCapability("research"));
        assertFalse(card.hasCapability("coding"));
        assertEquals(
            ExternalAgentTrust.VERIFIED,
            card.trust()
        );
    }

    @Test
    void card_defaults() {
        var card = new AgentCard(
            "a1", "Agent", null,
            null, "http://localhost",
            null, null, null
        );

        assertEquals("", card.description());
        assertEquals("1.0.0", card.version());
        assertTrue(card.capabilities().isEmpty());
        assertEquals(
            ExternalAgentTrust.UNTRUSTED,
            card.trust()
        );
    }

    // --- ExternalAgentTrust ---

    @Test
    void trust_values() {
        assertEquals(4, ExternalAgentTrust.values().length);
        assertNotNull(ExternalAgentTrust.TRUSTED);
        assertNotNull(ExternalAgentTrust.REVOKED);
    }

    // --- A2ATaskStatus ---

    @Test
    void taskStatus_terminal() {
        assertFalse(A2ATaskStatus.SUBMITTED.isTerminal());
        assertFalse(A2ATaskStatus.RUNNING.isTerminal());
        assertTrue(A2ATaskStatus.COMPLETED.isTerminal());
        assertTrue(A2ATaskStatus.FAILED.isTerminal());
        assertTrue(A2ATaskStatus.CANCELLED.isTerminal());
        assertTrue(A2ATaskStatus.TIMED_OUT.isTerminal());
    }

    // --- A2ATask ---

    @Test
    void task_withStatus() {
        var task = new A2ATask(
            "t1", "agent-a",
            A2ATaskStatus.SUBMITTED,
            null, null, null,
            null, null, null
        );

        assertEquals(
            A2ATaskStatus.SUBMITTED,
            task.status()
        );

        var updated = task.withStatus(
            A2ATaskStatus.RUNNING
        );

        assertEquals(
            A2ATaskStatus.RUNNING,
            updated.status()
        );
        assertEquals("t1", updated.taskId());
    }

    @Test
    void task_withMessage() {
        var task = new A2ATask(
            "t1", "agent-a",
            A2ATaskStatus.RUNNING,
            null, null, null,
            null, null, null
        );

        var msg = A2AMessage.text("m1", "Hello");
        var updated = task.withMessage(msg);

        assertEquals(1, updated.messages().size());
        assertEquals("Hello", updated.messages().get(0).payload());
    }

    @Test
    void task_withArtifact() {
        var task = new A2ATask(
            "t1", "agent-a",
            A2ATaskStatus.COMPLETED,
            null, null, null,
            null, null, null
        );

        var artifact = ArtifactReference.of(
            "art-1", "text/plain"
        );
        var updated = task.withArtifact(artifact);

        assertEquals(1, updated.artifacts().size());
        assertEquals("art-1", updated.artifacts().get(0).artifactId());
    }

    // --- A2AMessage ---

    @Test
    void message_text() {
        var msg = A2AMessage.text("m1", "Hello");
        assertEquals("m1", msg.messageId());
        assertEquals("text", msg.type());
        assertEquals("Hello", msg.payload());
    }

    @Test
    void message_structured() {
        var data = Map.<String, Object>of(
            "key", "value"
        );
        var msg = A2AMessage.structured("m2", data);
        assertEquals("structured", msg.type());
    }

    // --- A2ARequest ---

    @Test
    void request_basic() {
        var msg = A2AMessage.text("m1", "Do something");
        var req = A2ARequest.of("r1", msg);

        assertEquals("r1", req.requestId());
        assertEquals("m1", req.message().messageId());
        assertEquals("", req.correlationId());
    }

    // --- A2AEvent ---

    @Test
    void event_basic() {
        var evt = A2AEvent.of(
            "t1", A2AEventType.TASK_CREATED
        );

        assertEquals("t1", evt.taskId());
        assertEquals(
            A2AEventType.TASK_CREATED,
            evt.type()
        );
        assertNotNull(evt.eventId());
        assertNotNull(evt.timestamp());
    }

    @Test
    void event_withData() {
        var evt = A2AEvent.of(
            "t1", A2AEventType.TASK_MESSAGE,
            "some data"
        );

        assertEquals("some data", evt.data());
    }

    // --- ArtifactReference ---

    @Test
    void artifact_basic() {
        var art = ArtifactReference.of(
            "art-1", "text/plain"
        );

        assertEquals("art-1", art.artifactId());
        assertEquals("text/plain", art.contentType());
        assertEquals(0, art.size());
    }

    @Test
    void artifact_withDigest() {
        var art = ArtifactReference.of(
            "art-1",
            "sha256:abc123",
            "application/pdf",
            1024
        );

        assertEquals("sha256:abc123", art.digest());
        assertEquals(1024, art.size());
    }

    // --- A2AErrorCode ---

    @Test
    void errorCode_fromString() {
        assertEquals(
            A2AErrorCode.A2A_AGENT_NOT_FOUND,
            A2AErrorCode.fromString("agent not found")
        );
        assertEquals(
            A2AErrorCode.A2A_TASK_TIMEOUT,
            A2AErrorCode.fromString("task timeout")
        );
        assertEquals(
            A2AErrorCode.A2A_RECURSION_DETECTED,
            A2AErrorCode.fromString("recursion loop detected")
        );
        assertEquals(
            A2AErrorCode.A2A_PROTOCOL_ERROR,
            A2AErrorCode.fromString(null)
        );
    }

    // --- AgentCapabilityQuery ---

    @Test
    void query_matches() {
        var query = AgentCapabilityQuery.of(
            Set.of("research")
        );

        var cardA = AgentCard.builder()
            .agentId("a")
            .name("Agent A")
            .endpoint("http://a.com")
            .capabilities(Set.of("research", "analysis"))
            .build();

        var cardB = AgentCard.builder()
            .agentId("b")
            .name("Agent B")
            .endpoint("http://b.com")
            .capabilities(Set.of("coding"))
            .build();

        assertTrue(query.matches(cardA));
        assertFalse(query.matches(cardB));
    }

    @Test
    void query_nullCard() {
        var query = AgentCapabilityQuery.of(
            Set.of("research")
        );

        assertFalse(query.matches(null));
    }

    // --- ExternalAgentRegistry ---

    @Test
    void registry_registerAndGet() {
        var card = AgentCard.builder()
            .agentId("agent-a")
            .name("Agent A")
            .endpoint("http://a.com")
            .capabilities(Set.of("research"))
            .build();

        registry.register(card);

        assertTrue(registry.get("agent-a").isPresent());
        assertEquals(1, registry.list().size());
    }

    @Test
    void registry_findByCapability() {
        registry.register(AgentCard.builder()
            .agentId("a")
            .name("A")
            .endpoint("http://a.com")
            .capabilities(Set.of("research"))
            .build());

        registry.register(AgentCard.builder()
            .agentId("b")
            .name("B")
            .endpoint("http://b.com")
            .capabilities(Set.of("coding"))
            .build());

        var results = registry.findByCapability(
            "research"
        );

        assertEquals(1, results.size());
        assertEquals("a", results.get(0).agentId());
    }

    @Test
    void registry_unregister() {
        registry.register(AgentCard.builder()
            .agentId("a")
            .name("A")
            .endpoint("http://a.com")
            .build());

        registry.unregister("a");

        assertTrue(registry.get("a").isEmpty());
    }

    // --- ExternalAgentResolver ---

    @Test
    void resolver_byCapability() {
        registry.register(AgentCard.builder()
            .agentId("a")
            .name("A")
            .endpoint("http://a.com")
            .capabilities(Set.of("research"))
            .trust(ExternalAgentTrust.VERIFIED)
            .build());

        registry.register(AgentCard.builder()
            .agentId("b")
            .name("B")
            .endpoint("http://b.com")
            .capabilities(Set.of("research"))
            .trust(ExternalAgentTrust.REVOKED)
            .build());

        var resolver =
            new DefaultExternalAgentResolver(registry);

        var query = AgentCapabilityQuery.of(
            Set.of("research")
        );

        var results = resolver.resolve(query);

        // Only non-revoked agent
        assertEquals(1, results.size());
        assertEquals("a", results.get(0).agentId());
    }

    @Test
    void resolver_byReference() {
        registry.register(AgentCard.builder()
            .agentId("agent-b")
            .name("Agent B")
            .endpoint("http://b.com")
            .build());

        var resolver =
            new DefaultExternalAgentResolver(registry);

        var ref = new ExternalAgentReference(
            "ns", "agent-b", null,
            URI.create("http://b.com")
        );

        var card = resolver.resolve(ref);
        assertNotNull(card);
        assertEquals("agent-b", card.agentId());
    }

    // --- A2AClient ---

    @Test
    void client_submitTask() {
        registry.register(AgentCard.builder()
            .agentId("agent-b")
            .name("Agent B")
            .endpoint("http://b.com")
            .build());

        var client = new InMemoryA2AClient(registry);

        var ref = new ExternalAgentReference(
            "ns", "agent-b", null,
            URI.create("http://b.com")
        );

        var msg = A2AMessage.text("m1", "Do research");
        var req = A2ARequest.of("r1", msg);

        var task = client.submit(ref, req);

        assertNotNull(task);
        assertEquals(
            A2ATaskStatus.SUBMITTED,
            task.status()
        );
        assertEquals("agent-b", task.agentId());
        assertEquals(1, task.messages().size());
    }

    @Test
    void client_agentNotFound() {
        var client = new InMemoryA2AClient(registry);

        var ref = new ExternalAgentReference(
            "ns", "unknown", null,
            URI.create("http://unknown.com")
        );

        var msg = A2AMessage.text("m1", "Hello");
        var req = A2ARequest.of("r1", msg);

        assertThrows(
            A2AException.class,
            () -> client.submit(ref, req)
        );
    }

    @Test
    void client_revokedAgent() {
        registry.register(AgentCard.builder()
            .agentId("revoked-agent")
            .name("Revoked")
            .endpoint("http://r.com")
            .trust(ExternalAgentTrust.REVOKED)
            .build());

        var client = new InMemoryA2AClient(registry);

        var ref = new ExternalAgentReference(
            "ns", "revoked-agent", null,
            URI.create("http://r.com")
        );

        var msg = A2AMessage.text("m1", "Hello");
        var req = A2ARequest.of("r1", msg);

        assertThrows(
            A2AException.class,
            () -> client.submit(ref, req)
        );
    }

    @Test
    void client_cancelTask() {
        registry.register(AgentCard.builder()
            .agentId("agent-b")
            .name("B")
            .endpoint("http://b.com")
            .build());

        var client = new InMemoryA2AClient(registry);

        var ref = new ExternalAgentReference(
            "ns", "agent-b", null,
            URI.create("http://b.com")
        );

        var msg = A2AMessage.text("m1", "Work");
        var req = A2ARequest.of("r1", msg);

        var task = client.submit(ref, req);
        client.cancel(task.taskId());

        var updated = client.getTask(task.taskId());
        assertEquals(
            A2ATaskStatus.CANCELLED,
            updated.status()
        );
    }

    @Test
    void client_stream() {
        registry.register(AgentCard.builder()
            .agentId("agent-b")
            .name("B")
            .endpoint("http://b.com")
            .build());

        var client = new InMemoryA2AClient(registry);

        var ref = new ExternalAgentReference(
            "ns", "agent-b", null,
            URI.create("http://b.com")
        );

        var msg = A2AMessage.text("m1", "Work");
        var req = A2ARequest.of("r1", msg);

        var task = client.submit(ref, req);

        var events = client.stream(task.taskId());
        assertFalse(events.isEmpty());
        assertEquals(
            A2AEventType.TASK_CREATED,
            events.get(0).type()
        );
    }

    @Test
    void client_updateStatus() {
        registry.register(AgentCard.builder()
            .agentId("agent-b")
            .name("B")
            .endpoint("http://b.com")
            .build());

        var client = new InMemoryA2AClient(registry);

        var ref = new ExternalAgentReference(
            "ns", "agent-b", null,
            URI.create("http://b.com")
        );

        var msg = A2AMessage.text("m1", "Work");
        var req = A2ARequest.of("r1", msg);

        var task = client.submit(ref, req);
        client.updateTaskStatus(
            task.taskId(),
            A2ATaskStatus.COMPLETED
        );

        var updated = client.getTask(task.taskId());
        assertEquals(
            A2ATaskStatus.COMPLETED,
            updated.status()
        );
    }

    // --- A2AException ---

    @Test
    void exception_basic() {
        var ex = new A2AException(
            "Not found",
            A2AErrorCode.A2A_AGENT_NOT_FOUND
        );

        assertEquals(
            A2AErrorCode.A2A_AGENT_NOT_FOUND,
            ex.errorCode()
        );
        assertEquals("Not found", ex.getMessage());
    }
}

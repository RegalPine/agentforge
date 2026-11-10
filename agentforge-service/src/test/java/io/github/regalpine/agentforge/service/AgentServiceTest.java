package io.github.regalpine.agentforge.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Agent Service types.
 * AF-ENHANCE-11.
 */
class AgentServiceTest {

    private InMemoryAgentServiceRegistry registry;
    private InMemorySessionStore sessionStore;
    private InMemoryConversationStore convStore;

    @BeforeEach
    void setUp() {
        registry = new InMemoryAgentServiceRegistry();
        sessionStore = new InMemorySessionStore();
        convStore = new InMemoryConversationStore();
    }

    // --- AgentServiceId ---

    @Test
    void serviceId_basic() {
        var id = new AgentServiceId(
            "research", "assistant", "1.0"
        );

        assertEquals("research", id.namespace());
        assertEquals("assistant", id.name());
        assertEquals("1.0", id.version());
        assertEquals(
            "research:assistant",
            id.qualifiedName()
        );
    }

    @Test
    void serviceId_defaultsVersion() {
        var id = new AgentServiceId(
            "ns", "name", null
        );

        assertEquals("1.0.0", id.version());
    }

    @Test
    void serviceId_rejectsNull() {
        assertThrows(
            NullPointerException.class,
            () -> new AgentServiceId(null, "n", "v")
        );
    }

    @Test
    void serviceId_rejectsBlank() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new AgentServiceId(" ", "n", "v")
        );
    }

    // --- AgentServiceMetadata ---

    @Test
    void metadata_defaults() {
        var m = AgentServiceMetadata.defaults();

        assertFalse(
            m.authenticationRequired()
        );
        assertTrue(
            m.supportedChannels()
                .contains("HTTP")
        );
    }

    // --- AgentRequest ---

    @Test
    void request_basic() {
        var req = AgentRequest.of(
            "req-1", "hello"
        );

        assertEquals("req-1", req.requestId());
        assertEquals("hello", req.input());
        assertTrue(
            req.metadata().isEmpty()
        );
    }

    @Test
    void request_rejectsNull() {
        assertThrows(
            NullPointerException.class,
            () -> AgentRequest.of(null, "x")
        );
    }

    // --- AgentResponse ---

    @Test
    void response_basic() {
        var resp = AgentResponse.of(
            "resp-1", "exec-1", "output"
        );

        assertEquals("resp-1", resp.responseId());
        assertEquals("exec-1", resp.executionId());
        assertEquals("output", resp.output());
    }

    // --- AgentEvent ---

    @Test
    void event_basic() {
        var evt = AgentEvent.of(
            "message.delta",
            "exec-1",
            "Hello"
        );

        assertEquals("message.delta", evt.type());
        assertEquals("exec-1", evt.executionId());
        assertNotNull(evt.timestamp());
    }

    // --- Session ---

    @Test
    void session_create() {
        var s = Session.create(
            "s1", "tenant-1"
        );

        assertEquals("s1", s.sessionId());
        assertEquals("tenant-1", s.tenantId());
        assertEquals(
            SessionStatus.CREATED,
            s.status()
        );
        assertTrue(s.status().isActive());
    }

    @Test
    void session_activate() {
        var s = Session.create("s1", "t1");
        var active = s.activate();

        assertEquals(
            SessionStatus.ACTIVE,
            active.status()
        );
        assertTrue(active.status().isActive());
    }

    @Test
    void session_close() {
        var s = Session.create("s1", "t1");
        var closed = s.close();

        assertEquals(
            SessionStatus.CLOSED,
            closed.status()
        );
        assertTrue(closed.status().isTerminal());
    }

    // --- SessionStatus ---

    @Test
    void sessionStatus_terminal() {
        assertFalse(
            SessionStatus.CREATED.isTerminal()
        );
        assertFalse(
            SessionStatus.ACTIVE.isTerminal()
        );
        assertTrue(
            SessionStatus.EXPIRED.isTerminal()
        );
        assertTrue(
            SessionStatus.CLOSED.isTerminal()
        );
        assertTrue(
            SessionStatus.REVOKED.isTerminal()
        );
    }

    // --- Conversation ---

    @Test
    void conversation_create() {
        var c = Conversation.create(
            "conv-1", "s1"
        );

        assertEquals("conv-1", c.conversationId());
        assertEquals("s1", c.sessionId());
        assertTrue(c.messages().isEmpty());
    }

    @Test
    void conversation_append() {
        var c = Conversation.create(
            "conv-1", "s1"
        );

        var msg = ConversationMessage.user(
            "m1", "Hello"
        );

        var updated = c.append(msg);

        assertEquals(1, updated.messages().size());
        assertEquals(
            "Hello",
            updated.messages().get(0).content()
        );
        assertEquals(
            MessageRole.USER,
            updated.messages().get(0).role()
        );
    }

    // --- ConversationMessage ---

    @Test
    void message_user() {
        var m = ConversationMessage.user(
            "m1", "Hi"
        );

        assertEquals(MessageRole.USER, m.role());
    }

    @Test
    void message_assistant() {
        var m = ConversationMessage.assistant(
            "m2", "Hello"
        );

        assertEquals(
            MessageRole.ASSISTANT,
            m.role()
        );
    }

    // --- AgentServiceDefinition ---

    @Test
    void definition_basic() {
        var id = new AgentServiceId(
            "ns", "svc", "1.0"
        );

        var def = AgentServiceDefinition.of(
            id, "agent-ref"
        );

        assertEquals(id, def.serviceId());
        assertEquals(
            "agent-ref",
            def.agentReference()
        );
        assertEquals(
            ServiceHealth.STARTING,
            def.health()
        );
    }

    // --- ServiceHealth ---

    @Test
    void serviceHealth_values() {
        assertEquals(
            6,
            ServiceHealth.values().length
        );
    }

    // --- ChannelType ---

    @Test
    void channelType_values() {
        assertEquals(
            7,
            ChannelType.values().length
        );
    }

    // --- AgentChannelContext ---

    @Test
    void channelContext_basic() {
        var ctx = AgentChannelContext.of(
            "tenant-1"
        );

        assertEquals(
            "tenant-1",
            ctx.tenantId()
        );
        assertTrue(
            ctx.actorId().isEmpty()
        );
    }

    // --- Registry ---

    @Test
    void registry_registerAndGet() {
        var id = new AgentServiceId(
            "ns", "svc", "1.0"
        );
        var def = AgentServiceDefinition.of(
            id, "agent-1"
        );

        registry.register(def);

        var found = registry.get(id);
        assertTrue(found.isPresent());
        assertEquals(
            "agent-1",
            found.get().agentReference()
        );
    }

    @Test
    void registry_list() {
        var id1 = new AgentServiceId(
            "ns", "a", "1.0"
        );
        var id2 = new AgentServiceId(
            "ns", "b", "1.0"
        );

        registry.register(
            AgentServiceDefinition.of(id1, "a1")
        );
        registry.register(
            AgentServiceDefinition.of(id2, "a2")
        );

        assertEquals(2, registry.list().size());
    }

    @Test
    void registry_disable() {
        var id = new AgentServiceId(
            "ns", "svc", "1.0"
        );
        var def = AgentServiceDefinition.of(
            id, "agent-1"
        );

        registry.register(def);
        registry.disable(id);

        var found = registry.get(id);
        assertTrue(found.isPresent());
        assertEquals(
            ServiceHealth.STOPPED,
            found.get().health()
        );
    }

    @Test
    void registry_unregister() {
        var id = new AgentServiceId(
            "ns", "svc", "1.0"
        );
        var def = AgentServiceDefinition.of(
            id, "agent-1"
        );

        registry.register(def);
        registry.unregister(id);

        assertTrue(registry.get(id).isEmpty());
    }

    // --- SessionStore ---

    @Test
    void sessionStore_createAndGet() {
        var s = sessionStore.create(
            "s1", "t1"
        );

        assertEquals("s1", s.sessionId());

        var found = sessionStore.get("s1");
        assertTrue(found.isPresent());
    }

    @Test
    void sessionStore_close() {
        sessionStore.create("s1", "t1");
        sessionStore.close("s1");

        var found = sessionStore.get("s1");
        assertTrue(found.isPresent());
        assertEquals(
            SessionStatus.CLOSED,
            found.get().status()
        );
    }

    @Test
    void sessionStore_delete() {
        sessionStore.create("s1", "t1");
        sessionStore.delete("s1");

        assertTrue(sessionStore.get("s1").isEmpty());
    }

    // --- ConversationStore ---

    @Test
    void convStore_createAndGet() {
        var c = convStore.create(
            "conv-1", "s1"
        );

        assertEquals("conv-1", c.conversationId());

        var found = convStore.get("conv-1");
        assertTrue(found.isPresent());
    }

    @Test
    void convStore_append() {
        convStore.create("conv-1", "s1");

        var msg = ConversationMessage.user(
            "m1", "Hello"
        );

        convStore.append("conv-1", msg);

        var found = convStore.get("conv-1");
        assertTrue(found.isPresent());
        assertEquals(
            1,
            found.get().messages().size()
        );
    }
}

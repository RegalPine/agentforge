package io.github.regalpine.agentforge.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deep boundary / immutability /
 * edge-case tests for service module.
 * Complements AgentServiceTest.
 */
class AgentServiceBoundaryTest {

    private InMemoryAgentServiceRegistry registry;
    private InMemorySessionStore sessionStore;
    private InMemoryConversationStore convStore;

    @BeforeEach
    void setUp() {
        registry = new InMemoryAgentServiceRegistry();
        sessionStore = new InMemorySessionStore();
        convStore = new InMemoryConversationStore();
    }

    // ===== AgentServiceId =====

    @Test
    void serviceId_rejectsBlankName() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new AgentServiceId("ns", " ", "1.0")
        );
    }

    @Test
    void serviceId_rejectsNullName() {
        assertThrows(
            NullPointerException.class,
            () -> new AgentServiceId("ns", null, "1.0")
        );
    }

    @Test
    void serviceId_toStringIncludesVersion() {
        var id = new AgentServiceId(
            "ns", "svc", "2.0.0"
        );
        assertEquals("ns:svc:2.0.0", id.toString());
    }

    @Test
    void serviceId_equality() {
        var a = new AgentServiceId(
            "ns", "svc", "1.0"
        );
        var b = new AgentServiceId(
            "ns", "svc", "1.0"
        );
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void serviceId_inequalityByVersion() {
        var a = new AgentServiceId(
            "ns", "svc", "1.0"
        );
        var b = new AgentServiceId(
            "ns", "svc", "2.0"
        );
        assertNotEquals(a, b);
    }

    // ===== AgentServiceDefinition =====

    @Test
    void definition_nullMetadataDefaultsToDefault() {
        var id = new AgentServiceId(
            "ns", "svc", "1.0"
        );
        var def = new AgentServiceDefinition(
            id, "agent-ref",
            null, null, null, null
        );

        assertNotNull(def.metadata());
        assertEquals("", def.policyReference());
        assertEquals(
            ServiceHealth.STARTING,
            def.health()
        );
        assertTrue(def.extensions().isEmpty());
    }

    @Test
    void definition_defensiveCopyExtensions() {
        var id = new AgentServiceId(
            "ns", "svc", "1.0"
        );
        Map<String, Object> ext = new HashMap<>();
        ext.put("key", "val");

        var def = new AgentServiceDefinition(
            id, "agent-ref",
            null, null, null, ext
        );

        ext.put("key2", "val2");
        assertEquals(1, def.extensions().size());
    }

    @Test
    void definition_unmodifiableExtensions() {
        var def = AgentServiceDefinition.of(
            new AgentServiceId("ns", "s", "1"),
            "agent"
        );

        assertThrows(
            UnsupportedOperationException.class,
            () -> def.extensions().put("k", "v")
        );
    }

    @Test
    void definition_rejectsNullServiceId() {
        assertThrows(
            NullPointerException.class,
            () -> AgentServiceDefinition.of(
                null, "agent"
            )
        );
    }

    @Test
    void definition_rejectsNullAgentRef() {
        assertThrows(
            NullPointerException.class,
            () -> AgentServiceDefinition.of(
                new AgentServiceId("ns", "s", "1"),
                null
            )
        );
    }

    // ===== AgentServiceMetadata =====

    @Test
    void metadata_rejectsNullChannels() {
        assertThrows(
            NullPointerException.class,
            () -> new AgentServiceMetadata(
                "desc", null, false, "", null
            )
        );
    }

    @Test
    void metadata_defensiveCopyExtensions() {
        Map<String, Object> ext = new HashMap<>();
        ext.put("a", "b");

        var m = new AgentServiceMetadata(
            "desc", Set.of("HTTP"),
            false, "", ext
        );

        ext.put("c", "d");
        assertEquals(1, m.extensions().size());
    }

    @Test
    void metadata_nullDescriptionDefaultsToEmpty() {
        var m = new AgentServiceMetadata(
            null, Set.of(),
            false, null, null
        );

        assertEquals("", m.description());
        assertEquals("", m.policyReference());
        assertTrue(m.extensions().isEmpty());
    }

    // ===== AgentRequest =====

    @Test
    void request_nullInputRejected() {
        assertThrows(
            NullPointerException.class,
            () -> AgentRequest.of("r1", null)
        );
    }

    @Test
    void request_fullConstructor() {
        var req = new AgentRequest(
            "r1", "input",
            "sess-ref", "conv-ref",
            "idemp-key",
            Map.of("k", "v")
        );

        assertEquals("r1", req.requestId());
        assertEquals("sess-ref", req.sessionReference());
        assertEquals("conv-ref", req.conversationReference());
        assertEquals("idemp-key", req.idempotencyKey());
        assertEquals("v", req.metadata().get("k"));
    }

    @Test
    void request_nullOptionalsDefaultToEmpty() {
        var req = new AgentRequest(
            "r1", "input",
            null, null, null, null
        );

        assertEquals("", req.sessionReference());
        assertEquals("", req.conversationReference());
        assertEquals("", req.idempotencyKey());
        assertTrue(req.metadata().isEmpty());
    }

    @Test
    void request_defensiveCopyMetadata() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("a", "b");

        var req = new AgentRequest(
            "r1", "input",
            null, null, null, meta
        );

        meta.put("c", "d");
        assertEquals(1, req.metadata().size());
    }

    @Test
    void request_unmodifiableMetadata() {
        var req = AgentRequest.of("r1", "in");
        assertThrows(
            UnsupportedOperationException.class,
            () -> req.metadata().put("k", "v")
        );
    }

    // ===== AgentResponse =====

    @Test
    void response_nullOutputDefaultsToEmpty() {
        var resp = new AgentResponse(
            "resp-1", "exec-1",
            null, null
        );

        assertEquals("", resp.output());
        assertTrue(resp.metadata().isEmpty());
    }

    @Test
    void response_defensiveCopyMetadata() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("a", "b");

        var resp = new AgentResponse(
            "r1", "e1", "out", meta
        );

        meta.put("c", "d");
        assertEquals(1, resp.metadata().size());
    }

    @Test
    void response_unmodifiableMetadata() {
        var resp = AgentResponse.of(
            "r1", "e1", "out"
        );
        assertThrows(
            UnsupportedOperationException.class,
            () -> resp.metadata().put("k", "v")
        );
    }

    @Test
    void response_rejectsNullResponseId() {
        assertThrows(
            NullPointerException.class,
            () -> AgentResponse.of(null, "e1", "o")
        );
    }

    @Test
    void response_rejectsNullExecutionId() {
        assertThrows(
            NullPointerException.class,
            () -> AgentResponse.of("r1", null, "o")
        );
    }

    // ===== AgentEvent =====

    @Test
    void event_nullExecutionIdDefaultsToEmpty() {
        var evt = new AgentEvent(
            "e1", "type", null,
            null, null, null
        );

        assertEquals("", evt.executionId());
        assertNotNull(evt.timestamp());
        assertTrue(evt.metadata().isEmpty());
    }

    @Test
    void event_defensiveCopyMetadata() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("a", "b");

        var evt = new AgentEvent(
            "e1", "type", "exec",
            "data", null, meta
        );

        meta.put("c", "d");
        assertEquals(1, evt.metadata().size());
    }

    @Test
    void event_rejectsNullEventId() {
        assertThrows(
            NullPointerException.class,
            () -> new AgentEvent(
                null, "type", "",
                null, null, null
            )
        );
    }

    @Test
    void event_rejectsNullType() {
        assertThrows(
            NullPointerException.class,
            () -> new AgentEvent(
                "e1", null, "",
                null, null, null
            )
        );
    }

    // ===== Session =====

    @Test
    void session_nullActorIdDefaultsToEmpty() {
        var s = new Session(
            "s1", "t1", null,
            SessionStatus.CREATED,
            null, null, null, null
        );

        assertEquals("", s.actorId());
    }

    @Test
    void session_defensiveCopyContext() {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("a", "b");

        var s = new Session(
            "s1", "t1", "",
            SessionStatus.CREATED,
            null, null, null, ctx
        );

        ctx.put("c", "d");
        assertEquals(1, s.context().size());
    }

    @Test
    void session_unmodifiableContext() {
        var s = Session.create("s1", "t1");
        assertThrows(
            UnsupportedOperationException.class,
            () -> s.context().put("k", "v")
        );
    }

    @Test
    void session_rejectsNullSessionId() {
        assertThrows(
            NullPointerException.class,
            () -> new Session(
                null, "t1", "",
                SessionStatus.CREATED,
                null, null, null, null
            )
        );
    }

    @Test
    void session_rejectsNullTenantId() {
        assertThrows(
            NullPointerException.class,
            () -> new Session(
                "s1", null, "",
                SessionStatus.CREATED,
                null, null, null, null
            )
        );
    }

    @Test
    void session_rejectsNullStatus() {
        assertThrows(
            NullPointerException.class,
            () -> new Session(
                "s1", "t1", "",
                null, null, null, null, null
            )
        );
    }

    @Test
    void session_activatePreservesId() {
        var s = Session.create("s1", "t1");
        var active = s.activate();

        assertEquals("s1", active.sessionId());
        assertEquals("t1", active.tenantId());
    }

    @Test
    void session_closePreservesId() {
        var s = Session.create("s1", "t1");
        var closed = s.close();

        assertEquals("s1", closed.sessionId());
        assertEquals("t1", closed.tenantId());
    }

    // ===== SessionStatus =====

    @Test
    void sessionStatus_isActive() {
        assertTrue(SessionStatus.CREATED.isActive());
        assertTrue(SessionStatus.ACTIVE.isActive());
        assertFalse(SessionStatus.IDLE.isActive());
        assertFalse(SessionStatus.EXPIRED.isActive());
        assertFalse(SessionStatus.CLOSED.isActive());
        assertFalse(SessionStatus.INVALID.isActive());
        assertFalse(SessionStatus.REVOKED.isActive());
    }

    @Test
    void sessionStatus_isTerminalAll() {
        assertFalse(SessionStatus.CREATED.isTerminal());
        assertFalse(SessionStatus.ACTIVE.isTerminal());
        assertFalse(SessionStatus.IDLE.isTerminal());
        assertTrue(SessionStatus.EXPIRED.isTerminal());
        assertTrue(SessionStatus.CLOSED.isTerminal());
        assertTrue(SessionStatus.INVALID.isTerminal());
        assertTrue(SessionStatus.REVOKED.isTerminal());
    }

    // ===== Conversation =====

    @Test
    void conversation_defensiveCopyMessages() {
        var msg = ConversationMessage.user(
            "m1", "Hi"
        );
        var list = new java.util.ArrayList<>(
            List.of(msg)
        );

        var c = new Conversation(
            "c1", "s1", list,
            null, null, null
        );

        list.add(
            ConversationMessage.user("m2", "x")
        );
        assertEquals(1, c.messages().size());
    }

    @Test
    void conversation_unmodifiableMessages() {
        var c = Conversation.create("c1", "s1");
        assertThrows(
            UnsupportedOperationException.class,
            () -> c.messages().add(
                ConversationMessage.user("m", "x")
            )
        );
    }

    @Test
    void conversation_defensiveCopyMetadata() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("a", "b");

        var c = new Conversation(
            "c1", "s1", null,
            null, null, meta
        );

        meta.put("c", "d");
        assertEquals(1, c.metadata().size());
    }

    @Test
    void conversation_rejectsNullId() {
        assertThrows(
            NullPointerException.class,
            () -> Conversation.create(null, "s1")
        );
    }

    @Test
    void conversation_rejectsNullSessionId() {
        assertThrows(
            NullPointerException.class,
            () -> Conversation.create("c1", null)
        );
    }

    @Test
    void conversation_appendMultiple() {
        var c = Conversation.create("c1", "s1");

        c = c.append(
            ConversationMessage.user("m1", "Hi")
        );
        c = c.append(
            ConversationMessage.assistant("m2", "Hello")
        );
        c = c.append(
            ConversationMessage.user("m3", "How?")
        );

        assertEquals(3, c.messages().size());
        assertEquals(
            MessageRole.ASSISTANT,
            c.messages().get(1).role()
        );
    }

    // ===== ConversationMessage =====

    @Test
    void message_rejectsNullId() {
        assertThrows(
            NullPointerException.class,
            () -> ConversationMessage.user(
                null, "Hi"
            )
        );
    }

    @Test
    void message_rejectsNullContent() {
        assertThrows(
            NullPointerException.class,
            () -> ConversationMessage.user(
                "m1", null
            )
        );
    }

    @Test
    void message_rejectsNullRole() {
        assertThrows(
            NullPointerException.class,
            () -> new ConversationMessage(
                "m1", null, "content",
                null, null
            )
        );
    }

    @Test
    void message_defensiveCopyMetadata() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("a", "b");

        var m = new ConversationMessage(
            "m1", MessageRole.USER,
            "content", null, meta
        );

        meta.put("c", "d");
        assertEquals(1, m.metadata().size());
    }

    @Test
    void message_unmodifiableMetadata() {
        var m = ConversationMessage.user(
            "m1", "Hi"
        );
        assertThrows(
            UnsupportedOperationException.class,
            () -> m.metadata().put("k", "v")
        );
    }

    // ===== AgentChannelContext =====

    @Test
    void channelContext_fullConstructor() {
        var sid = new AgentServiceId(
            "ns", "svc", "1.0"
        );
        var ctx = new AgentChannelContext(
            "t1", "actor", "app",
            sid, Map.of("k", "v")
        );

        assertEquals("t1", ctx.tenantId());
        assertEquals("actor", ctx.actorId());
        assertEquals("app", ctx.applicationId());
        assertEquals(sid, ctx.serviceId());
        assertEquals("v", ctx.attributes().get("k"));
    }

    @Test
    void channelContext_nullOptionalsDefault() {
        var ctx = new AgentChannelContext(
            "t1", null, null,
            null, null
        );

        assertEquals("", ctx.actorId());
        assertEquals("", ctx.applicationId());
        assertNull(ctx.serviceId());
        assertTrue(ctx.attributes().isEmpty());
    }

    @Test
    void channelContext_rejectsNullTenant() {
        assertThrows(
            NullPointerException.class,
            () -> AgentChannelContext.of(null)
        );
    }

    @Test
    void channelContext_defensiveCopyAttributes() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("a", "b");

        var ctx = new AgentChannelContext(
            "t1", "", "", null, attrs
        );

        attrs.put("c", "d");
        assertEquals(1, ctx.attributes().size());
    }

    @Test
    void channelContext_unmodifiableAttributes() {
        var ctx = AgentChannelContext.of("t1");
        assertThrows(
            UnsupportedOperationException.class,
            () -> ctx.attributes().put("k", "v")
        );
    }

    // ===== InMemorySessionStore =====

    @Test
    void sessionStore_rejectsNullSessionId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> sessionStore.create(null, "t1")
        );
    }

    @Test
    void sessionStore_rejectsNullTenantId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> sessionStore.create("s1", null)
        );
    }

    @Test
    void sessionStore_getNullReturnsEmpty() {
        assertTrue(
            sessionStore.get(null).isEmpty()
        );
    }

    @Test
    void sessionStore_getUnknownReturnsEmpty() {
        assertTrue(
            sessionStore.get("ghost").isEmpty()
        );
    }

    @Test
    void sessionStore_updateReplacesSession() {
        sessionStore.create("s1", "t1");
        var active = Session.create("s1", "t1")
            .activate();

        sessionStore.update(active);

        var found = sessionStore.get("s1")
            .orElseThrow();
        assertEquals(
            SessionStatus.ACTIVE,
            found.status()
        );
    }

    @Test
    void sessionStore_updateRejectsNull() {
        assertThrows(
            IllegalArgumentException.class,
            () -> sessionStore.update(null)
        );
    }

    @Test
    void sessionStore_closeNonExistentIsNoop() {
        assertDoesNotThrow(
            () -> sessionStore.close("ghost")
        );
    }

    @Test
    void sessionStore_deleteNonExistentIsNoop() {
        assertDoesNotThrow(
            () -> sessionStore.delete("ghost")
        );
    }

    // ===== InMemoryConversationStore =====

    @Test
    void convStore_rejectsNullId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> convStore.create(null, "s1")
        );
    }

    @Test
    void convStore_getNullReturnsEmpty() {
        assertTrue(
            convStore.get(null).isEmpty()
        );
    }

    @Test
    void convStore_appendToNonExistentIsNoop() {
        assertDoesNotThrow(
            () -> convStore.append(
                "ghost",
                ConversationMessage.user("m", "x")
            )
        );
    }

    @Test
    void convStore_appendMultipleMessages() {
        convStore.create("c1", "s1");

        convStore.append("c1",
            ConversationMessage.user("m1", "Hi"));
        convStore.append("c1",
            ConversationMessage.assistant("m2", "Hey"));

        var found = convStore.get("c1")
            .orElseThrow();
        assertEquals(2, found.messages().size());
    }

    // ===== InMemoryAgentServiceRegistry =====

    @Test
    void registry_rejectsNullDefinition() {
        assertThrows(
            IllegalArgumentException.class,
            () -> registry.register(null)
        );
    }

    @Test
    void registry_getNullReturnsEmpty() {
        assertTrue(
            registry.get(null).isEmpty()
        );
    }

    @Test
    void registry_disableNonExistentIsNoop() {
        var id = new AgentServiceId(
            "ns", "ghost", "1.0"
        );
        assertDoesNotThrow(
            () -> registry.disable(id)
        );
    }

    @Test
    void registry_unregisterNonExistentIsNoop() {
        var id = new AgentServiceId(
            "ns", "ghost", "1.0"
        );
        assertDoesNotThrow(
            () -> registry.unregister(id)
        );
    }

    @Test
    void registry_listReturnsDefensiveCopy() {
        registry.register(
            AgentServiceDefinition.of(
                new AgentServiceId("ns", "a", "1"),
                "agent"
            )
        );

        var list = registry.list();
        assertThrows(
            UnsupportedOperationException.class,
            () -> list.add(null)
        );
    }

    @Test
    void registry_overwriteOnReregister() {
        var id = new AgentServiceId(
            "ns", "svc", "1.0"
        );

        registry.register(
            AgentServiceDefinition.of(id, "a1")
        );
        registry.register(
            AgentServiceDefinition.of(id, "a2")
        );

        var found = registry.get(id).orElseThrow();
        assertEquals("a2", found.agentReference());
        assertEquals(1, registry.list().size());
    }
}

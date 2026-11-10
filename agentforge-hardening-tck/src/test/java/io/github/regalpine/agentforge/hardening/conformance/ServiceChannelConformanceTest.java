package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.service.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * conformance-service-channel:
 * AgentService / Session / Channel.
 * AF-AD-16 §8.2, T-X12.11.
 */
class ServiceChannelConformanceTest {

    // ── AgentServiceId ──

    @Test
    void afSvc001_serviceIdCreation() {
        var id = new AgentServiceId(
            "prod", "research", "1.0.0"
        );
        assertEquals("prod",
            id.namespace());
        assertEquals("research",
            id.name());
        assertEquals("1.0.0",
            id.version());
        assertEquals(
            "prod:research",
            id.qualifiedName()
        );
    }

    @Test
    void afSvc002_serviceIdDefaultVersion() {
        var id = new AgentServiceId(
            "ns", "svc", null
        );
        assertEquals("1.0.0",
            id.version());
    }

    // ── Session ──

    @Test
    void afSvc003_sessionLifecycle() {
        var s = Session.create(
            "s1", "tenant-1"
        );
        assertEquals(
            SessionStatus.CREATED,
            s.status()
        );

        var active = s.activate();
        assertEquals(
            SessionStatus.ACTIVE,
            active.status()
        );

        var closed = active.close();
        assertEquals(
            SessionStatus.CLOSED,
            closed.status()
        );
    }

    @Test
    void afSvc004_sessionStatusSeven() {
        assertEquals(
            7, SessionStatus.values().length
        );
        assertTrue(
            SessionStatus.ACTIVE
                .isActive()
        );
        assertTrue(
            SessionStatus.CLOSED
                .isTerminal()
        );
    }

    // ── Conversation ──

    @Test
    void afSvc005_conversationCreation() {
        var c = Conversation.create(
            "conv-1", "s1"
        );
        assertEquals("conv-1",
            c.conversationId());
        assertEquals("s1",
            c.sessionId());
        assertTrue(
            c.messages().isEmpty()
        );
    }

    @Test
    void afSvc006_conversationAppend() {
        var c = Conversation.create(
            "conv-1", "s1"
        );
        var msg = ConversationMessage.user(
            "m1", "hello"
        );
        var updated = c.append(msg);
        assertEquals(1,
            updated.messages().size());
        assertEquals(
            MessageRole.USER,
            updated.messages().get(0).role()
        );
    }

    // ── MessageRole ──

    @Test
    void afSvc007_sixMessageRoles() {
        assertEquals(
            6, MessageRole.values().length
        );
        assertNotNull(MessageRole.USER);
        assertNotNull(
            MessageRole.ASSISTANT
        );
        assertNotNull(
            MessageRole.SYSTEM
        );
        assertNotNull(
            MessageRole.DEVELOPER
        );
        assertNotNull(
            MessageRole.TOOL
        );
        assertNotNull(
            MessageRole.AGENT
        );
    }

    // ── ChannelType ──

    @Test
    void afSvc008_sevenChannelTypes() {
        assertEquals(
            7, ChannelType.values().length
        );
        assertNotNull(ChannelType.HTTP);
        assertNotNull(ChannelType.SSE);
        assertNotNull(
            ChannelType.WEBSOCKET
        );
        assertNotNull(ChannelType.CLI);
        assertNotNull(
            ChannelType.WEBHOOK
        );
        assertNotNull(ChannelType.SDK);
        assertNotNull(ChannelType.MQ);
    }

    // ── AgentChannelContext ──

    @Test
    void afSvc009_channelContext() {
        var ctx = AgentChannelContext.of(
            "t1"
        );
        assertEquals("t1",
            ctx.tenantId());
    }

    // ── AgentRequest/Response ──

    @Test
    void afSvc010_requestResponse() {
        var req = AgentRequest.of(
            "req-1", "hello"
        );
        assertEquals("req-1",
            req.requestId());
        assertEquals("hello",
            req.input());

        var resp = AgentResponse.of(
            "resp-1", "exec-1", "world"
        );
        assertEquals("resp-1",
            resp.responseId());
        assertEquals("world",
            resp.output());
    }

    // ── ServiceHealth ──

    @Test
    void afSvc011_serviceHealth() {
        assertTrue(
            ServiceHealth.values().length
                >= 4
        );
        assertNotNull(
            ServiceHealth.READY
        );
        assertNotNull(
            ServiceHealth.UNAVAILABLE
        );
    }

    // ── Registry ──

    @Test
    void afSvc012_inMemoryRegistry() {
        var reg =
            new InMemoryAgentServiceRegistry();
        assertNotNull(reg);
        assertTrue(reg.list().isEmpty());
    }
}

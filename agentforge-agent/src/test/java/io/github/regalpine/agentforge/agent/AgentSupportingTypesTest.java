package io.github.regalpine.agentforge.agent;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.skill.governance.SkillGovernanceProfile;
import io.github.regalpine.agentforge.skill.governance.SkillRiskLevel;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the supporting record types:
 * {@link AgentHealth}, {@link AgentMetrics},
 * {@link RegistrationOptions},
 * {@link RegistrationHandle},
 * {@link AgentEventListener}.
 */
class AgentSupportingTypesTest {

    private Reference ref() {
        return new Reference(
            new EntityId("a"), new Version("1.0.0")
        );
    }

    // ── AgentHealth ──

    @Test
    void shouldBuildAgentHealth() {
        AgentHealth h = new AgentHealth(
            ref(),
            AgentHealth.AgentHealthStatus.UP,
            Map.of("loop",
                AgentHealth.AgentComponentHealth.up("loop"))
        );
        assertEquals(AgentHealth.AgentHealthStatus.UP, h.status());
        assertEquals(1, h.components().size());
    }

    @Test
    void shouldDefensivelyCopyHealthComponents() {
        Map<String, AgentHealth.AgentComponentHealth> mutable =
            new java.util.HashMap<>();
        mutable.put("x",
            AgentHealth.AgentComponentHealth.up("x"));

        AgentHealth h = new AgentHealth(
            ref(),
            AgentHealth.AgentHealthStatus.UP,
            mutable
        );

        assertThrows(
            UnsupportedOperationException.class,
            () -> h.components().put("y",
                AgentHealth.AgentComponentHealth.up("y"))
        );
    }

    @Test
    void shouldDefaultHealthComponentsToEmpty() {
        AgentHealth h = new AgentHealth(
            ref(),
            AgentHealth.AgentHealthStatus.DOWN,
            null
        );
        assertNotNull(h.components());
        assertTrue(h.components().isEmpty());
    }

    @Test
    void shouldRejectNullHealthReference() {
        assertThrows(
            NullPointerException.class,
            () -> new AgentHealth(
                null,
                AgentHealth.AgentHealthStatus.UP,
                Map.of()
            )
        );
    }

    @Test
    void shouldRejectNullHealthStatus() {
        assertThrows(
            NullPointerException.class,
            () -> new AgentHealth(ref(), null, Map.of())
        );
    }

    @Test
    void shouldBuildComponentHealthDown() {
        var c = AgentHealth.AgentComponentHealth.down(
            "db", "connection refused"
        );
        assertEquals(
            AgentHealth.AgentHealthStatus.DOWN, c.status()
        );
        assertEquals("connection refused", c.detail());
    }

    // ── AgentMetrics ──

    @Test
    void shouldBuildEmptyMetrics() {
        AgentMetrics m = AgentMetrics.empty(ref());
        assertEquals(0L, m.invocations());
        assertEquals(0L, m.failures());
        assertEquals(Duration.ZERO, m.p50());
        assertEquals(Duration.ZERO, m.p99());
    }

    @Test
    void shouldComputeSuccessRate() {
        AgentMetrics m = new AgentMetrics(
            ref(), 100L, 3L,
            Duration.ofMillis(50),
            Duration.ofMillis(200)
        );
        assertEquals(0.97, m.successRate(), 0.0001);
    }

    @Test
    void shouldReturnFullSuccessRateWhenZeroInvocations() {
        AgentMetrics m = AgentMetrics.empty(ref());
        assertEquals(1.0, m.successRate());
    }

    @Test
    void shouldRejectNegativeInvocations() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new AgentMetrics(
                ref(), -1L, 0L,
                Duration.ZERO, Duration.ZERO
            )
        );
    }

    @Test
    void shouldRejectNegativeFailures() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new AgentMetrics(
                ref(), 0L, -1L,
                Duration.ZERO, Duration.ZERO
            )
        );
    }

    @Test
    void shouldDefaultNullDurationsToZero() {
        AgentMetrics m = new AgentMetrics(
            ref(), 0L, 0L, null, null
        );
        assertEquals(Duration.ZERO, m.p50());
        assertEquals(Duration.ZERO, m.p99());
    }

    // ── RegistrationOptions ──

    @Test
    void shouldBuildDefaultOptions() {
        RegistrationOptions o =
            RegistrationOptions.defaults();
        assertTrue(o.autoStart());
        assertNull(o.governance());
        assertNull(o.tenantId());
        assertTrue(o.tags().isEmpty());
    }

    @Test
    void shouldBuildOptionsWithAutoStartFalse() {
        RegistrationOptions o =
            RegistrationOptions.of(false);
        assertFalse(o.autoStart());
    }

    @Test
    void shouldBuildOptionsViaBuilder() {
        SkillGovernanceProfile gov =
            new SkillGovernanceProfile(
                "owner", "domain",
                SkillRiskLevel.HIGH,
                Set.of(), Set.of(), null
            );

        RegistrationOptions o =
            RegistrationOptions.builder()
                .governance(gov)
                .tenantId("tenant-x")
                .tag("env", "prod")
                .tag("team", "ai")
                .autoStart(false)
                .build();

        assertEquals(gov, o.governance());
        assertEquals("tenant-x", o.tenantId());
        assertFalse(o.autoStart());
        assertEquals("prod", o.tags().get("env"));
        assertEquals("ai", o.tags().get("team"));
    }

    @Test
    void shouldDefensivelyCopyOptionsTags() {
        Map<String, String> mutable = new java.util.HashMap<>();
        mutable.put("k", "v");
        RegistrationOptions o =
            new RegistrationOptions(null, mutable, null, true);

        assertThrows(
            UnsupportedOperationException.class,
            () -> o.tags().put("k2", "v2")
        );
    }

    @Test
    void shouldDefaultNullTagsToEmpty() {
        RegistrationOptions o =
            new RegistrationOptions(null, null, null, true);
        assertNotNull(o.tags());
        assertTrue(o.tags().isEmpty());
    }

    // ── RegistrationHandle ──

    @Test
    void shouldBuildRegistrationHandle() {
        RegistrationHandle h =
            new RegistrationHandle(ref());
        assertEquals(ref(), h.reference());
    }

    @Test
    void shouldRejectNullHandleReference() {
        assertThrows(
            NullPointerException.class,
            () -> new RegistrationHandle(null)
        );
    }

    // ── AgentEventListener ──

    @Test
    void shouldBuildEventWithPayload() {
        var event =
            AgentEventListener.AgentRegistrationEvent.of(
                AgentEventListener.AgentEventKind.STARTED,
                ref()
            );
        assertEquals(
            AgentEventListener.AgentEventKind.STARTED,
            event.kind()
        );
        assertEquals(ref(), event.reference());
        assertNotNull(event.timestamp());
        assertNotNull(event.payload());
    }

    @Test
    void shouldDefensivelyCopyEventPayload() {
        var event =
            new AgentEventListener.AgentRegistrationEvent(
                AgentEventListener.AgentEventKind.INVOKED,
                ref(),
                java.time.Instant.now(),
                new java.util.HashMap<>(Map.of("k", "v"))
            );
        assertThrows(
            UnsupportedOperationException.class,
            () -> event.payload().put("k2", "v2")
        );
    }

    @Test
    void shouldDefaultNullEventPayloadToEmpty() {
        var event =
            new AgentEventListener.AgentRegistrationEvent(
                AgentEventListener.AgentEventKind.INVOKED,
                ref(),
                java.time.Instant.now(),
                null
            );
        assertNotNull(event.payload());
        assertTrue(event.payload().isEmpty());
    }

    @Test
    void shouldRejectNullEventKind() {
        assertThrows(
            NullPointerException.class,
            () -> new AgentEventListener.AgentRegistrationEvent(
                null, ref(),
                java.time.Instant.now(), Map.of()
            )
        );
    }

    @Test
    void shouldInvokeListener() {
        var received = new java.util.concurrent.atomic.AtomicReference<
            AgentEventListener.AgentRegistrationEvent>();
        AgentEventListener listener = received::set;

        var event =
            AgentEventListener.AgentRegistrationEvent.of(
                AgentEventListener.AgentEventKind.STOPPED,
                ref()
            );
        listener.onEvent(event);

        assertNotNull(received.get());
        assertEquals(
            AgentEventListener.AgentEventKind.STOPPED,
            received.get().kind()
        );
    }
}

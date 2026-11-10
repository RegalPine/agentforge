package io.github.regalpine.agentforge.agent;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.skill.governance.SkillGovernanceProfile;
import io.github.regalpine.agentforge.skill.governance.SkillRiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Behavioral tests for {@link InMemoryAgentRegistrar}
 * and the {@link AgentRegistrar} SPI contract.
 *
 * <p>Emphasis on negative behavior:
 * duplicate registration, illegal
 * transitions, operations on unknown
 * references, and listener isolation.
 */
class InMemoryAgentRegistrarTest {

    private InMemoryAgentRegistrar registrar;

    @BeforeEach
    void setUp() {
        registrar = new InMemoryAgentRegistrar();
    }

    // ── helpers ──

    private AgentDefinition agent(
        String id, String version, String name
    ) {
        return new AgentDefinition(
            new EntityId(id),
            new Version(version),
            name,
            Set.of()
        );
    }

    private Reference ref(String id, String version) {
        return new Reference(
            new EntityId(id),
            new Version(version)
        );
    }

    // ── positive path ──

    @Test
    void shouldRegisterAndAutoStart() {
        AgentDefinition def = agent(
            "agent.a", "1.0.0", "alpha"
        );

        registrar.register(def, RegistrationOptions.defaults());

        assertEquals(
            AgentLifecycleState.ACTIVE,
            registrar.lifecycleState(ref("agent.a", "1.0.0"))
        );
        assertTrue(
            registrar.find(ref("agent.a", "1.0.0")).isPresent()
        );
    }

    @Test
    void shouldRegisterWithoutAutoStart() {
        AgentDefinition def = agent(
            "agent.b", "1.0.0", "beta"
        );

        registrar.register(
            def,
            RegistrationOptions.of(false)
        );

        assertEquals(
            AgentLifecycleState.REGISTERED,
            registrar.lifecycleState(ref("agent.b", "1.0.0"))
        );
    }

    @Test
    void shouldTransitionThroughLifecycle() {
        AgentDefinition def = agent(
            "agent.life", "1.0.0", "lifecycle"
        );
        registrar.register(def, RegistrationOptions.of(false));
        Reference r = ref("agent.life", "1.0.0");

        assertEquals(AgentLifecycleState.REGISTERED,
            registrar.lifecycleState(r));

        registrar.start(r);
        assertEquals(AgentLifecycleState.ACTIVE,
            registrar.lifecycleState(r));

        registrar.drain(r);
        assertEquals(AgentLifecycleState.DRAINING,
            registrar.lifecycleState(r));

        registrar.stop(r);
        assertEquals(AgentLifecycleState.STOPPED,
            registrar.lifecycleState(r));
    }

    @Test
    void shouldRestartAfterStopped() {
        AgentDefinition def = agent(
            "agent.restart", "1.0.0", "r"
        );
        registrar.register(def);
        Reference r = ref("agent.restart", "1.0.0");

        registrar.stop(r);
        assertEquals(AgentLifecycleState.STOPPED,
            registrar.lifecycleState(r));

        // STOPPED → REGISTERED → STARTING → ACTIVE
        registrar.start(r);
        assertEquals(AgentLifecycleState.ACTIVE,
            registrar.lifecycleState(r));
    }

    @Test
    void shouldBindGovernance() {
        AgentDefinition def = agent(
            "agent.gov", "1.0.0", "g"
        );
        registrar.register(def);
        Reference r = ref("agent.gov", "1.0.0");

        SkillGovernanceProfile profile =
            new SkillGovernanceProfile(
                "owner@example",
                "platform",
                SkillRiskLevel.HIGH,
                Set.of("admin"),
                Set.of("reviewer"),
                "policy-1"
            );

        registrar.bindGovernance(r, profile);
        assertEquals(profile, registrar.governanceOf(r));
    }

    @Test
    void shouldBindGovernanceViaRegistrationOptions() {
        SkillGovernanceProfile profile =
            new SkillGovernanceProfile(
                "owner@example", "platform",
                SkillRiskLevel.LOW,
                Set.of(), Set.of(), null
            );

        AgentDefinition def = agent(
            "agent.opt", "1.0.0", "opt"
        );
        registrar.register(
            def,
            RegistrationOptions.builder()
                .governance(profile)
                .build()
        );

        assertEquals(
            profile,
            registrar.governanceOf(ref("agent.opt", "1.0.0"))
        );
    }

    @Test
    void shouldReturnHealthSnapshot() {
        AgentDefinition def = agent(
            "agent.h", "1.0.0", "h"
        );
        registrar.register(def);
        Reference r = ref("agent.h", "1.0.0");

        AgentHealth health = registrar.health(r);
        assertEquals(
            AgentHealth.AgentHealthStatus.UP,
            health.status()
        );
        assertTrue(health.components().containsKey("lifecycle"));
    }

    @Test
    void shouldReturnDegradedHealthWhenDraining() {
        AgentDefinition def = agent(
            "agent.d", "1.0.0", "d"
        );
        registrar.register(def);
        Reference r = ref("agent.d", "1.0.0");
        registrar.drain(r);

        assertEquals(
            AgentHealth.AgentHealthStatus.DEGRADED,
            registrar.health(r).status()
        );
    }

    @Test
    void shouldReturnEmptyMetricsForNewAgent() {
        AgentDefinition def = agent(
            "agent.m", "1.0.0", "m"
        );
        registrar.register(def);

        AgentMetrics metrics = registrar.metrics(
            ref("agent.m", "1.0.0")
        );
        assertEquals(0L, metrics.invocations());
        assertEquals(0L, metrics.failures());
        assertEquals(1.0, metrics.successRate());
    }

    @Test
    void shouldEmitEventsToSubscribedListener() {
        AgentDefinition def = agent(
            "agent.ev", "1.0.0", "ev"
        );
        registrar.register(def, RegistrationOptions.of(false));
        Reference r = ref("agent.ev", "1.0.0");

        List<AgentEventListener.AgentEventKind> received =
            new ArrayList<>();
        registrar.subscribe(r, e -> received.add(e.kind()));

        registrar.start(r);
        registrar.drain(r);
        registrar.stop(r);

        assertTrue(received.contains(
            AgentEventListener.AgentEventKind.STARTED));
        assertTrue(received.contains(
            AgentEventListener.AgentEventKind.DRAINED));
        assertTrue(received.contains(
            AgentEventListener.AgentEventKind.STOPPED));
    }

    @Test
    void shouldIsolateListenerExceptions() {
        AgentDefinition def = agent(
            "agent.ex", "1.0.0", "ex"
        );
        registrar.register(def);
        Reference r = ref("agent.ex", "1.0.0");

        AtomicInteger goodCount = new AtomicInteger();
        registrar.subscribe(r, e -> {
            throw new RuntimeException("boom");
        });
        registrar.subscribe(r, e -> goodCount.incrementAndGet());

        // Second listener must still be called
        registrar.stop(r);
        assertEquals(1, goodCount.get());
    }

    // ── negative behavior ──

    @Test
    void shouldRejectNullAgentRegistration() {
        assertThrows(
            NullPointerException.class,
            () -> registrar.register(
                null, RegistrationOptions.defaults()
            )
        );
    }

    @Test
    void shouldRejectNullOptionsRegistration() {
        AgentDefinition def = agent(
            "agent.n", "1.0.0", "n"
        );
        assertThrows(
            NullPointerException.class,
            () -> registrar.register(def, null)
        );
    }

    @Test
    void shouldRejectDuplicateRegistration() {
        AgentDefinition def = agent(
            "agent.dup", "1.0.0", "dup"
        );
        registrar.register(def);

        assertThrows(
            IllegalStateException.class,
            () -> registrar.register(def)
        );
    }

    @Test
    void shouldAllowSameIdDifferentVersion() {
        registrar.register(agent("agent.mv", "1.0.0", "v1"));
        registrar.register(agent("agent.mv", "2.0.0", "v2"));

        assertEquals(2, registrar.list().size());
    }

    @Test
    void shouldFailStartOfUnknownReference() {
        assertThrows(
            IllegalStateException.class,
            () -> registrar.start(
                ref("ghost", "1.0.0")
            )
        );
    }

    @Test
    void shouldFailIllegalTransition() {
        AgentDefinition def = agent(
            "agent.ill", "1.0.0", "ill"
        );
        // autoStart → ACTIVE
        registrar.register(def);
        Reference r = ref("agent.ill", "1.0.0");
        assertEquals(AgentLifecycleState.ACTIVE,
            registrar.lifecycleState(r));

        // ACTIVE → REGISTERED is illegal; there is no
        // public registrar method to force that exact
        // transition, but AgentLifecycleState itself
        // must reject it.
        assertThrows(
            IllegalStateException.class,
            () -> AgentLifecycleState.ACTIVE
                .transitionTo(AgentLifecycleState.REGISTERED)
        );
    }

    @Test
    void shouldTolerateStopOfUnknownReference() {
        // Per contract: stop of unknown is a no-op
        assertDoesNotThrow(
            () -> registrar.stop(ref("ghost", "1.0.0"))
        );
    }

    @Test
    void shouldTolerateDrainOfUnknownReference() {
        assertDoesNotThrow(
            () -> registrar.drain(ref("ghost", "1.0.0"))
        );
    }

    @Test
    void shouldReturnNullLifecycleStateForUnknown() {
        assertNull(
            registrar.lifecycleState(ref("ghost", "1.0.0"))
        );
    }

    @Test
    void shouldReturnDownHealthForUnknownReference() {
        AgentHealth health = registrar.health(
            ref("ghost", "1.0.0")
        );
        assertEquals(
            AgentHealth.AgentHealthStatus.DOWN,
            health.status()
        );
    }

    @Test
    void shouldFailGovernanceBindingOnUnknown() {
        SkillGovernanceProfile profile =
            new SkillGovernanceProfile(
                "owner", "domain",
                SkillRiskLevel.LOW,
                Set.of(), Set.of(), null
            );

        assertThrows(
            IllegalStateException.class,
            () -> registrar.bindGovernance(
                ref("ghost", "1.0.0"), profile
            )
        );
    }

    @Test
    void shouldFailSubscribeOnUnknownReference() {
        assertThrows(
            IllegalStateException.class,
            () -> registrar.subscribe(
                ref("ghost", "1.0.0"),
                e -> {}
            )
        );
    }

    @Test
    void shouldRejectNullGovernanceBinding() {
        AgentDefinition def = agent(
            "agent.ng", "1.0.0", "ng"
        );
        registrar.register(def);
        Reference r = ref("agent.ng", "1.0.0");

        assertThrows(
            NullPointerException.class,
            () -> registrar.bindGovernance(r, null)
        );
    }

    @Test
    void shouldRejectNullListenerSubscription() {
        AgentDefinition def = agent(
            "agent.nl", "1.0.0", "nl"
        );
        registrar.register(def);
        Reference r = ref("agent.nl", "1.0.0");

        assertThrows(
            NullPointerException.class,
            () -> registrar.subscribe(r, null)
        );
    }

    @Test
    void shouldClearStateOnUnregister() {
        AgentDefinition def = agent(
            "agent.rm", "1.0.0", "rm"
        );
        registrar.register(def);
        Reference r = ref("agent.rm", "1.0.0");

        registrar.unregister(r);

        assertFalse(registrar.find(r).isPresent());
        assertNull(registrar.lifecycleState(r));
    }
}

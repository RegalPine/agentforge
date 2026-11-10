package io.github.regalpine.agentforge.multiagent;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.multiagent.contract.AgentContract;
import io.github.regalpine.agentforge.multiagent.contract.AgentExecutionPolicy;
import io.github.regalpine.agentforge.multiagent.contract.AgentTrustLevel;
import io.github.regalpine.agentforge.multiagent.reference.AgentResolutionException;
import io.github.regalpine.agentforge.multiagent.reference.InMemoryAgentResolver;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentContract tests — AE-11 §5, §12-13.
 * AF-MA-001, AF-MA-002.
 */
class AgentContractTest {

    @Test
    void shouldCreateMinimalContract() {
        var contract = AgentContract.of(
            "analysis-agent", "1.0.0"
        );
        assertEquals("analysis-agent",
            contract.agentId().value());
        assertEquals("1.0.0",
            contract.version().value());
        assertEquals(AgentTrustLevel.STANDARD,
            contract.trustLevel());
        assertNotNull(contract.executionPolicy());
    }

    @Test
    void shouldCreateFullContract() {
        var policy = new AgentExecutionPolicy(
            Duration.ofSeconds(30),
            3, 5,
            Set.of("tool-a", "tool-b"),
            50_000, 10, 20, 5
        );
        var contract = new AgentContract(
            new EntityId("research-agent"),
            new Version("2.0.0"),
            null, null,
            policy,
            AgentTrustLevel.VERIFIED,
            null, null
        );

        assertEquals("research-agent",
            contract.agentId().value());
        assertEquals(AgentTrustLevel.VERIFIED,
            contract.trustLevel());
        assertEquals(3,
            contract.executionPolicy().maxDelegationDepth());
        assertTrue(
            contract.executionPolicy()
                .isDelegationTargetAllowed("tool-a")
        );
        assertFalse(
            contract.executionPolicy()
                .isDelegationTargetAllowed("tool-c")
        );
    }

    @Test
    void shouldConvertToReference() {
        var contract = AgentContract.of("a1", "1.0.0");
        Reference ref = contract.toReference();
        assertEquals("a1", ref.id().value());
        assertEquals("1.0.0", ref.version().value());
    }

    @Test
    void shouldResolveAgent() {
        var resolver = new InMemoryAgentResolver();
        var contract = AgentContract.of("agent-a", "1.0.0");
        resolver.register(contract);

        var resolved = resolver.resolve(
            Reference.of("agent-a", "1.0.0")
        );
        assertEquals("agent-a", resolved.agentId().value());
        assertEquals(1, resolver.size());
    }

    @Test
    void shouldThrowOnUnresolvableAgent() {
        var resolver = new InMemoryAgentResolver();
        assertThrows(
            AgentResolutionException.class,
            () -> resolver.resolve(
                Reference.of("unknown", "1.0.0")
            )
        );
    }

    @Test
    void shouldRejectBlankAgentId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new EntityId("")
        );
    }

    @Test
    void shouldRejectInvalidVersion() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new Version("abc")
        );
    }

    @Test
    void shouldCreateDefaultExecutionPolicy() {
        var policy = AgentExecutionPolicy.defaults();
        assertEquals(5, policy.maxDelegationDepth());
        assertEquals(10, policy.maxChildren());
        assertTrue(policy.timeout().toSeconds() > 0);
        // Empty allowed = all allowed
        assertTrue(policy.isDelegationTargetAllowed("any"));
    }

    @Test
    void shouldCreateStrictExecutionPolicy() {
        var policy = AgentExecutionPolicy.strict();
        assertEquals(2, policy.maxDelegationDepth());
        assertEquals(3, policy.maxChildren());
    }

    @Test
    void shouldRejectNegativeDepth() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new AgentExecutionPolicy(
                Duration.ofSeconds(10),
                -1, 0, null, 0, 0, 0, 0
            )
        );
    }
}

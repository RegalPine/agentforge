package io.github.regalpine.agentforge.examples;

import io.github.regalpine.agentforge.agent.AgentDefinition;
import io.github.regalpine.agentforge.agent.AgentLifecycleState;
import io.github.regalpine.agentforge.agent.AgentProvider;
import io.github.regalpine.agentforge.agent.AgentRegistrar;
import io.github.regalpine.agentforge.agent.InMemoryAgentRegistrar;
import io.github.regalpine.agentforge.agent.RegistrationHandle;
import io.github.regalpine.agentforge.agent.RegistrationOptions;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Golden Path: Agent Registration via
 * ServiceLoader auto-discovery.
 *
 * <p>Demonstrates the complete workflow:
 * <ol>
 *   <li>ServiceLoader discovers
 *       {@link AgentProvider} implementations</li>
 *   <li>Providers are sorted by
 *       {@link AgentProvider#order()}</li>
 *   <li>Each Agent definition is
 *       registered and auto-started</li>
 *   <li>Lifecycle state is verifiable</li>
 * </ol>
 *
 * <p>The {@code SampleAgentProvider} in
 * {@code src/main/java} is discovered via
 * {@code META-INF/services/}.
 */
class GoldenPathAgentRegistrationTest {

    @Test
    void shouldDiscoverProvidersViaServiceLoader() {
        List<AgentProvider> providers = new ArrayList<>();
        ServiceLoader.load(AgentProvider.class)
            .forEach(providers::add);

        assertFalse(providers.isEmpty(),
            "ServiceLoader should find at least one AgentProvider");
    }

    @Test
    void shouldRegisterAndStartAllProvidedAgents() {
        AgentRegistrar registrar =
            new InMemoryAgentRegistrar();

        List<AgentProvider> providers = new ArrayList<>();
        ServiceLoader.load(AgentProvider.class)
            .forEach(providers::add);
        providers.sort(
            Comparator.comparingInt(AgentProvider::order));

        int totalRegistered = 0;
        for (AgentProvider provider : providers) {
            for (AgentDefinition agent : provider.provide()) {
                registrar.registerAndStart(
                    agent, provider.options(agent));
                totalRegistered++;
            }
        }

        assertEquals(totalRegistered,
            registrar.list().size(),
            "All provided Agents should be registered");

        // Verify all are ACTIVE (autoStart=true)
        for (AgentDefinition a : registrar.list()) {
            Reference ref = new Reference(
                a.id(), a.version());
            assertEquals(AgentLifecycleState.ACTIVE,
                registrar.lifecycleState(ref),
                a.name() + " should be ACTIVE");
        }
    }

    @Test
    void shouldRespectProviderOrdering() {
        List<AgentProvider> providers = new ArrayList<>();
        ServiceLoader.load(AgentProvider.class)
            .forEach(providers::add);
        providers.sort(
            Comparator.comparingInt(AgentProvider::order));

        for (int i = 0; i < providers.size() - 1; i++) {
            assertTrue(
                providers.get(i).order()
                    <= providers.get(i + 1).order(),
                "Providers should be sorted by order()"
            );
        }
    }

    @Test
    void shouldSupportLifecycleOperations() {
        AgentRegistrar registrar =
            new InMemoryAgentRegistrar();

        AgentDefinition agent = new AgentDefinition(
            new EntityId("lifecycle-test"),
            new Version("1.0.0"),
            "Lifecycle Test Agent",
            Set.of()
        );

        RegistrationHandle handle =
            registrar.registerAndStart(
                agent,
                RegistrationOptions.defaults());

        Reference ref = handle.reference();
        assertEquals(AgentLifecycleState.ACTIVE,
            registrar.lifecycleState(ref));

        // Drain
        registrar.drain(ref);
        assertEquals(AgentLifecycleState.DRAINING,
            registrar.lifecycleState(ref));

        // Stop
        registrar.stop(ref);
        assertEquals(AgentLifecycleState.STOPPED,
            registrar.lifecycleState(ref));

        // Restart
        registrar.start(ref);
        assertEquals(AgentLifecycleState.ACTIVE,
            registrar.lifecycleState(ref));

        // Unregister
        registrar.unregister(ref);
        assertNull(registrar.lifecycleState(ref));
        assertTrue(registrar.list().isEmpty());
    }

    @Test
    void shouldHandleEmptyProviderGracefully() {
        AgentRegistrar registrar =
            new InMemoryAgentRegistrar();

        AgentProvider empty = new AgentProvider() {
            @Override
            public List<AgentDefinition> provide() {
                return List.of();
            }
        };

        for (AgentDefinition agent : empty.provide()) {
            registrar.registerAndStart(
                agent, empty.options(agent));
        }

        assertTrue(registrar.list().isEmpty(),
            "Empty provider should not register any Agents");
    }
}

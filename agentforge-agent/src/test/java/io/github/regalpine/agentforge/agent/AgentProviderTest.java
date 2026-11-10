package io.github.regalpine.agentforge.agent;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AgentProvider} SPI
 * contract and its integration with
 * {@link AgentRegistrar}.
 *
 * <p>N: ServiceLoader discovery is
 * tested via direct instantiation
 * rather than META-INF/services in
 * this module — the server module
 * performs the full ServiceLoader
 * integration test.
 */
class AgentProviderTest {

    // ── helper provider impl ──

    static final class EmptyProvider
        implements AgentProvider {
        @Override
        public List<AgentDefinition> provide() {
            return List.of();
        }
    }

    static final class TwoAgentProvider
        implements AgentProvider {
        @Override
        public List<AgentDefinition> provide() {
            return List.of(
                new AgentDefinition(
                    new EntityId("p.one"),
                    new Version("1.0.0"),
                    "one", Set.of()
                ),
                new AgentDefinition(
                    new EntityId("p.two"),
                    new Version("1.0.0"),
                    "two", Set.of()
                )
            );
        }
    }

    static final class OrderedProvider
        implements AgentProvider {
        private final int order;
        OrderedProvider(int order) { this.order = order; }
        @Override
        public List<AgentDefinition> provide() {
            return List.of(
                new AgentDefinition(
                    new EntityId("p.ord-" + order),
                    new Version("1.0.0"),
                    "ord-" + order, Set.of()
                )
            );
        }
        @Override
        public int order() { return order; }
    }

    static final class CustomOptionsProvider
        implements AgentProvider {
        @Override
        public List<AgentDefinition> provide() {
            return List.of(
                new AgentDefinition(
                    new EntityId("p.custom"),
                    new Version("1.0.0"),
                    "custom", Set.of()
                )
            );
        }
        @Override
        public RegistrationOptions options(AgentDefinition a) {
            return RegistrationOptions.of(false);
        }
    }

    // ── tests ──

    @Test
    void shouldReturnEmptyListByDefault() {
        var provider = new EmptyProvider();
        assertNotNull(provider.provide());
        assertTrue(provider.provide().isEmpty());
    }

    @Test
    void shouldReturnDefaultOptions() {
        var provider = new EmptyProvider();
        AgentDefinition def = new AgentDefinition(
            new EntityId("x"), new Version("1.0.0"),
            "x", Set.of()
        );
        RegistrationOptions opts = provider.options(def);
        assertTrue(opts.autoStart());
        assertNull(opts.governance());
        assertTrue(opts.tags().isEmpty());
    }

    @Test
    void shouldReturnDefaultOrder() {
        assertEquals(100, new EmptyProvider().order());
    }

    @Test
    void shouldProvideMultipleAgents() {
        var provider = new TwoAgentProvider();
        assertEquals(2, provider.provide().size());
    }

    @Test
    void shouldRespectCustomOrder() {
        assertEquals(42, new OrderedProvider(42).order());
    }

    @Test
    void shouldRespectCustomOptions() {
        var provider = new CustomOptionsProvider();
        AgentDefinition def = provider.provide().get(0);
        assertFalse(provider.options(def).autoStart());
    }

    @Test
    void shouldRegisterAgentsFromProvider() {
        InMemoryAgentRegistrar registrar =
            new InMemoryAgentRegistrar();
        var provider = new TwoAgentProvider();

        for (AgentDefinition a : provider.provide()) {
            registrar.registerAndStart(a, provider.options(a));
        }

        assertEquals(2, registrar.list().size());
        assertEquals(
            AgentLifecycleState.ACTIVE,
            registrar.lifecycleState(
                new Reference(
                    new EntityId("p.one"),
                    new Version("1.0.0")
                )
            )
        );
    }

    @Test
    void shouldOrderProvidersBeforeRegistration() {
        List<AgentProvider> providers = new ArrayList<>();
        providers.add(new OrderedProvider(50));
        providers.add(new OrderedProvider(10));
        providers.add(new OrderedProvider(99));

        providers.sort(
            java.util.Comparator.comparingInt(AgentProvider::order)
        );

        assertEquals(10, providers.get(0).order());
        assertEquals(50, providers.get(1).order());
        assertEquals(99, providers.get(2).order());
    }

    @Test
    void shouldHandleProviderThatReturnsNullList() {
        AgentProvider weird = new AgentProvider() {
            @Override
            public List<AgentDefinition> provide() {
                return null;
            }
        };
        // Contract says non-null; caller must defend.
        // Here we just verify the provider itself returns null.
        assertNull(weird.provide());
    }
}

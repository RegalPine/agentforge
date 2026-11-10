package io.github.regalpine.agentforge.provider.spi;

import io.github.regalpine.agentforge.capability.*;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.provider.spi.cancellation.DefaultCancellationToken;
import io.github.regalpine.agentforge.provider.spi.registry.DefaultProviderRegistry;
import io.github.regalpine.agentforge.provider.spi.registry.ProviderRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProviderSpiTest {

    @Test
    void shouldRegisterAndFindProvider() {
        ProviderRegistry registry =
            new DefaultProviderRegistry();

        CapabilityProvider provider =
            createTestProvider(
                "test",
                "1.0.0"
            );

        registry.register(provider);

        var found = registry.find(
            "test",
            new Version("1.0.0")
        );

        assertTrue(found.isPresent());
        assertEquals(
            "test",
            found.get().descriptor().providerId()
        );
    }

    @Test
    void shouldRejectDuplicateRegistration() {
        ProviderRegistry registry =
            new DefaultProviderRegistry();

        CapabilityProvider provider =
            createTestProvider(
                "test",
                "1.0.0"
            );

        registry.register(provider);

        assertThrows(
            IllegalStateException.class,
            () -> registry.register(provider)
        );
    }

    @Test
    void shouldFindByCapability() {
        ProviderRegistry registry =
            new DefaultProviderRegistry();

        CapabilityProvider provider =
            createTestProvider(
                "calc",
                "1.0.0"
            );

        registry.register(provider);

        var found = registry.findByCapability(
            new Reference(
                new EntityId("calc.evaluate"),
                new Version("1.0.0")
            )
        );

        assertEquals(1, found.size());
    }

    @Test
    void shouldUnregisterProvider() {
        ProviderRegistry registry =
            new DefaultProviderRegistry();

        CapabilityProvider provider =
            createTestProvider(
                "test",
                "1.0.0"
            );

        registry.register(provider);
        registry.unregister(
            "test",
            new Version("1.0.0")
        );

        var found = registry.find(
            "test",
            new Version("1.0.0")
        );

        assertTrue(found.isEmpty());
    }

    @Test
    void shouldSupportCancellationToken() {
        var token =
            new DefaultCancellationToken();

        assertFalse(token.isCancelled());

        token.cancel();

        assertTrue(token.isCancelled());
        assertThrows(
            RuntimeException.class,
            token::throwIfCancelled
        );
    }

    private static CapabilityProvider createTestProvider(
        String providerId,
        String version
    ) {
        return new CapabilityProvider() {
            @Override
            public ProviderDescriptor descriptor() {
                return new ProviderDescriptor(
                    providerId,
                    new Version(version),
                    Set.of(
                        new Reference(
                            new EntityId(
                                providerId + ".evaluate"
                            ),
                            new Version(version)
                        )
                    ),
                    Map.of()
                );
            }

            @Override
            public CapabilityResult invoke(
                CapabilityInvocation invocation
            ) {
                return new CapabilitySuccess(
                    Map.of("result", "ok")
                );
            }
        };
    }
}

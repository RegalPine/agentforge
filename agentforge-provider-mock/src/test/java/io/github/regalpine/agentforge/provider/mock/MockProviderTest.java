package io.github.regalpine.agentforge.provider.mock;

import io.github.regalpine.agentforge.capability.*;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MockProviderTest {

    @Test
    void shouldEchoMessage() {
        MockProvider provider = new MockProvider();

        CapabilityInvocation invocation =
            new CapabilityInvocation(
                "inv-001",
                new EntityId("mock.echo"),
                new Version("1.0.0"),
                Map.of("message", "hello"),
                null
            );

        CapabilityResult result =
            provider.invoke(invocation);

        assertInstanceOf(
            CapabilitySuccess.class,
            result
        );

        CapabilitySuccess success =
            (CapabilitySuccess) result;

        assertEquals(
            "hello",
            success.output().get("message")
        );
    }

    @Test
    void shouldDeclareCorrectDescriptor() {
        MockProvider provider = new MockProvider();

        var desc = provider.descriptor();

        assertEquals("mock", desc.providerId());
        assertEquals(
            "1.0.0",
            desc.version().value()
        );
        assertEquals(1, desc.capabilities().size());
    }

    @Test
    void shouldHandleNullMessage() {
        MockProvider provider = new MockProvider();

        CapabilityInvocation invocation =
            new CapabilityInvocation(
                "inv-002",
                new EntityId("mock.echo"),
                new Version("1.0.0"),
                Map.of(),
                null
            );

        CapabilityResult result =
            provider.invoke(invocation);

        assertInstanceOf(
            CapabilitySuccess.class,
            result
        );

        assertTrue(
            ((CapabilitySuccess) result)
                .output().isEmpty()
        );
    }
}

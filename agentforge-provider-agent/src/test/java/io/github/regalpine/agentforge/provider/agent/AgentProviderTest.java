package io.github.regalpine.agentforge.provider.agent;

import io.github.regalpine.agentforge.capability.*;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AgentProviderTest {

    @Test
    void shouldInvokeAgent() {
        AgentProvider provider =
            new AgentProvider(
                invocation ->
                    new CapabilitySuccess(
                        Map.of(
                            "agentResult",
                            "ok"
                        )
                    )
            );

        CapabilityInvocation invocation =
            new CapabilityInvocation(
                "inv-001",
                new EntityId("agent.invoke"),
                new Version("1.0.0"),
                Map.of(
                    "agentId", "helper-agent",
                    "agentVersion", "1.0.0"
                ),
                null
            );

        var result = provider.invoke(invocation);

        assertInstanceOf(
            CapabilitySuccess.class,
            result
        );
    }

    @Test
    void shouldBlockRecursionLimit() {
        AgentProvider provider =
            new AgentProvider(
                invocation -> {
                    assertEquals(
                        8,
                        invocation.context()
                            .depth()
                    );

                    return new CapabilityFailure(
                        "AF-AGENT-002",
                        "Recursion limit",
                        false
                    );
                }
            );

        var ctx = new InvocationContext(
            "exec-001",
            "tenant-001",
            Map.of("depth", 8)
        );

        CapabilityInvocation invocation =
            new CapabilityInvocation(
                "inv-002",
                new EntityId("agent.invoke"),
                new Version("1.0.0"),
                Map.of(
                    "agentId", "deep-agent"
                ),
                ctx
            );

        var result = provider.invoke(invocation);

        assertInstanceOf(
            CapabilityFailure.class,
            result
        );

        assertEquals(
            "AF-AGENT-002",
            ((CapabilityFailure) result).code()
        );
    }

    @Test
    void shouldReturnFailureForMissingAgentId() {
        AgentProvider provider =
            new AgentProvider(inv -> null);

        CapabilityInvocation invocation =
            new CapabilityInvocation(
                "inv-003",
                new EntityId("agent.invoke"),
                new Version("1.0.0"),
                Map.of(),
                null
            );

        var result = provider.invoke(invocation);

        assertInstanceOf(
            CapabilityFailure.class,
            result
        );

        assertEquals(
            "AF-AGENT-001",
            ((CapabilityFailure) result).code()
        );
    }

    @Test
    void shouldDeclareCorrectDescriptor() {
        AgentProvider provider =
            new AgentProvider(inv -> null);

        var desc = provider.descriptor();

        assertEquals(
            "agent",
            desc.providerId()
        );
    }
}

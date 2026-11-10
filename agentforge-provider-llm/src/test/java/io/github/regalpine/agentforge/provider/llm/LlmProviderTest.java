package io.github.regalpine.agentforge.provider.llm;

import io.github.regalpine.agentforge.capability.*;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LlmProviderTest {

    @Test
    void shouldCompleteWithStub() {
        var response = new LlmResponse(
            "The answer is 42",
            "stub-model",
            Map.of(
                "promptTokens", 10,
                "completionTokens", 5
            )
        );

        StubLlmProvider provider =
            new StubLlmProvider(response);

        CapabilityInvocation invocation =
            new CapabilityInvocation(
                "inv-001",
                new EntityId("llm.complete"),
                new Version("1.0.0"),
                Map.of(
                    "prompt",
                    "What is the answer?"
                ),
                null
            );

        var result = provider.invoke(invocation);

        assertInstanceOf(
            CapabilitySuccess.class,
            result
        );

        var success = (CapabilitySuccess) result;

        assertEquals(
            "The answer is 42",
            success.output().get("content")
        );
    }

    @Test
    void shouldReturnFailureForMissingPrompt() {
        StubLlmProvider provider =
            new StubLlmProvider(
                new LlmResponse(
                    "", "m", Map.of()
                )
            );

        CapabilityInvocation invocation =
            new CapabilityInvocation(
                "inv-002",
                new EntityId("llm.complete"),
                new Version("1.0.0"),
                Map.of(),
                null
            );

        var result = provider.invoke(invocation);

        assertInstanceOf(
            CapabilityFailure.class,
            result
        );
    }

    @Test
    void shouldDeclareCorrectDescriptor() {
        StubLlmProvider provider =
            new StubLlmProvider(
                new LlmResponse(
                    "", "m", Map.of()
                )
            );

        var desc = provider.descriptor();

        assertEquals(
            "llm",
            desc.providerId()
        );
    }

    @Test
    void shouldProduceStructuredDecision() {
        var decision = new StructuredDecision(
            "INVOKE_CAPABILITY",
            "calculator.evaluate",
            "1.0.0",
            Map.of("expression", "10 + 20"),
            "User wants to calculate"
        );

        assertEquals(
            "INVOKE_CAPABILITY",
            decision.decision()
        );

        assertEquals(
            "calculator.evaluate",
            decision.capabilityId()
        );
    }
}

package io.github.regalpine.agentforge.provider.llm;

import io.github.regalpine.agentforge.capability.*;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class StubLlmProvider
    implements LlmProvider {

    private final LlmResponse stubResponse;

    public StubLlmProvider(
        LlmResponse stubResponse
    ) {
        this.stubResponse = stubResponse;
    }

    @Override
    public ProviderDescriptor descriptor() {
        return new ProviderDescriptor(
            "llm",
            new Version("1.0.0"),
            Set.of(
                new Reference(
                    new EntityId("llm.complete"),
                    new Version("1.0.0")
                )
            ),
            Map.of("type", "reference")
        );
    }

    @Override
    public CapabilityResult invoke(
        CapabilityInvocation invocation
    ) {
        Object promptObj =
            invocation.arguments()
                .get("prompt");

        if (promptObj == null) {
            return new CapabilityFailure(
                "AF-LLM-001",
                "Missing 'prompt' argument",
                false
            );
        }

        LlmRequest request =
            new LlmRequest(
                "stub-model",
                List.of(
                    new LlmMessage(
                        "user",
                        promptObj.toString()
                    )
                ),
                Map.of(),
                30000
            );

        LlmResponse response = complete(request);

        return new CapabilitySuccess(
            Map.of(
                "content", response.content(),
                "model", response.model(),
                "usage", response.usage()
            )
        );
    }

    @Override
    public LlmResponse complete(
        LlmRequest request
    ) {
        return stubResponse;
    }
}

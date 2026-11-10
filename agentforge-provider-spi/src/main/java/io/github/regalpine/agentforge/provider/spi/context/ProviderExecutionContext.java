package io.github.regalpine.agentforge.provider.spi.context;

import io.github.regalpine.agentforge.provider.spi.cancellation.CancellationToken;

import java.time.Instant;
import java.util.Map;

public record ProviderExecutionContext(
    String executionId,
    String nodeId,
    String invocationId,
    String tenantId,
    Instant deadline,
    CancellationToken cancellationToken,
    Map<String, Object> attributes
) {

    public ProviderExecutionContext {
        attributes =
            attributes == null
                ? Map.of()
                : Map.copyOf(attributes);
    }
}

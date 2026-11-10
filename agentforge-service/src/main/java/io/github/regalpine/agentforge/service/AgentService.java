package io.github.regalpine.agentforge.service;

import java.util.concurrent.CompletionStage;
import java.util.List;

/**
 * Agent Service interface.
 * AF-ENHANCE-11 §5: AgentService.
 *
 * <p>Service is the exposure
 * boundary for Agent. Not a
 * new Agent type.
 */
public interface AgentService {

    /**
     * Service identity.
     */
    AgentServiceId id();

    /**
     * Referenced agent.
     */
    String agentReference();

    /**
     * Service metadata.
     */
    AgentServiceMetadata metadata();

    /**
     * Invoke agent synchronously.
     */
    CompletionStage<AgentResponse> invoke(
        AgentRequest request
    );

    /**
     * Stream agent events.
     * AF-ENHANCE-11 §13: Streaming.
     */
    List<AgentEvent> stream(
        AgentRequest request
    );
}

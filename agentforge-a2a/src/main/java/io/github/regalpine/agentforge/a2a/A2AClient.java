package io.github.regalpine.agentforge.a2a;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * A2A client interface.
 * AF-ENHANCE-10 §30: A2A Invocation.
 *
 * <p>Provides task submission,
 * retrieval, streaming, and
 * cancellation.
 */
public interface A2AClient {

    /**
     * Submit a task to an
     * external agent.
     */
    A2ATask submit(
        ExternalAgentReference agent,
        A2ARequest request
    );

    /**
     * Get task status.
     */
    A2ATask getTask(String taskId);

    /**
     * Stream task events.
     * AF-ENHANCE-10 §39: Streaming.
     */
    List<A2AEvent> stream(String taskId);

    /**
     * Cancel a task.
     */
    void cancel(String taskId);
}

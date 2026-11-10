package io.github.regalpine.agentforge.multimodal;

import java.util.concurrent.CompletionStage;

/**
 * Realtime session.
 * AF-ENHANCE-13 §30: RealtimeSession.
 *
 * <p>Realtime Session ≠ Execution.
 * A Realtime Session can contain
 * multiple incremental Executions.
 */
public interface RealtimeSession {

    /**
     * Session identifier.
     */
    String id();

    /**
     * Current status.
     */
    RealtimeStatus status();

    /**
     * Transport type.
     */
    RealtimeTransport transport();

    /**
     * Connect to the session.
     */
    CompletionStage<Void> connect();

    /**
     * Send an event.
     */
    CompletionStage<Void> send(
        RealtimeEvent event
    );

    /**
     * Close the session.
     */
    CompletionStage<Void> close();
}

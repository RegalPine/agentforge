package io.github.regalpine.agentforge.multimodal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory realtime session.
 * AF-ENHANCE-13 §30.
 *
 * <p>Simulates realtime session
 * for testing. Real implementations
 * would use WebSocket/SSE.
 */
public final class InMemoryRealtimeSession
    implements RealtimeSession {

    private final String sessionId;

    private final RealtimeTransport transport;

    private volatile RealtimeStatus status;

    private final List<RealtimeEvent>
        events = new ArrayList<>();

    private final Map<String, Object>
        attributes = new ConcurrentHashMap<>();

    public InMemoryRealtimeSession(
        String sessionId,
        RealtimeTransport transport
    ) {
        if (sessionId == null) {
            throw new IllegalArgumentException(
                "sessionId must not be null"
            );
        }
        if (transport == null) {
            throw new IllegalArgumentException(
                "transport must not be null"
            );
        }

        this.sessionId = sessionId;
        this.transport = transport;
        this.status = RealtimeStatus.CREATED;
    }

    @Override
    public String id() {
        return sessionId;
    }

    @Override
    public RealtimeStatus status() {
        return status;
    }

    @Override
    public RealtimeTransport transport() {
        return transport;
    }

    @Override
    public CompletionStage<Void> connect() {
        status = RealtimeStatus.CONNECTED;

        return CompletableFuture
            .completedFuture(null);
    }

    @Override
    public CompletionStage<Void> send(
        RealtimeEvent event
    ) {
        if (event == null) {
            return CompletableFuture
                .failedFuture(
                    new IllegalArgumentException(
                        "event must not be null"
                    )
                );
        }

        synchronized (events) {
            events.add(event);
        }

        return CompletableFuture
            .completedFuture(null);
    }

    @Override
    public CompletionStage<Void> close() {
        status = RealtimeStatus.CLOSED;

        return CompletableFuture
            .completedFuture(null);
    }

    /**
     * Get all sent events.
     */
    public List<RealtimeEvent> events() {
        synchronized (events) {
            return List.copyOf(events);
        }
    }
}

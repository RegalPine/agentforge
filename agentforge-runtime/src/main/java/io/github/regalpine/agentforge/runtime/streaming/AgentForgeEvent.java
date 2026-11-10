package io.github.regalpine.agentforge.runtime.streaming;

import io.github.regalpine.agentforge.event.Execution;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Unified event model for AgentForge streaming.
 * <p>
 * All runtime events flow through a single event type with a
 * discriminator (EventType). This enables uniform handling
 * across SSE, WebSocket, and in-process event streams.
 * <p>
 * Events are organized in four layers:
 * <ol>
 *   <li>Execution — graph execution lifecycle</li>
 *   <li>Node — individual node lifecycle</li>
 *   <li>Agent — agent loop lifecycle</li>
 *   <li>Model — LLM model interaction</li>
 * </ol>
 */
public record AgentForgeEvent(
    EventType type,
    String executionId,
    String nodeId,
    String agentId,
    Instant timestamp,
    Map<String, Object> payload,
    long sequenceNumber
) {

    public AgentForgeEvent {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    /**
     * Create an execution-level event.
     */
    public static AgentForgeEvent execution(EventType type, String executionId) {
        return new AgentForgeEvent(type, executionId, null, null, Instant.now(), Map.of(), 0);
    }

    /**
     * Create an execution-level event with payload.
     */
    public static AgentForgeEvent execution(EventType type, String executionId, Map<String, Object> payload) {
        return new AgentForgeEvent(type, executionId, null, null, Instant.now(), payload, 0);
    }

    /**
     * Create a node-level event.
     */
    public static AgentForgeEvent node(EventType type, String executionId, String nodeId) {
        return new AgentForgeEvent(type, executionId, nodeId, null, Instant.now(), Map.of(), 0);
    }

    /**
     * Create a node-level event with payload.
     */
    public static AgentForgeEvent node(EventType type, String executionId, String nodeId, Map<String, Object> payload) {
        return new AgentForgeEvent(type, executionId, nodeId, null, Instant.now(), payload, 0);
    }

    /**
     * Create an agent-level event.
     */
    public static AgentForgeEvent agent(EventType type, String executionId, String agentId) {
        return new AgentForgeEvent(type, executionId, null, agentId, Instant.now(), Map.of(), 0);
    }

    /**
     * Create an agent-level event with payload.
     */
    public static AgentForgeEvent agent(EventType type, String executionId, String agentId, Map<String, Object> payload) {
        return new AgentForgeEvent(type, executionId, null, agentId, Instant.now(), payload, 0);
    }

    /**
     * Create a model-level event.
     */
    public static AgentForgeEvent model(EventType type, String executionId, Map<String, Object> payload) {
        return new AgentForgeEvent(type, executionId, null, null, Instant.now(), payload, 0);
    }

    /**
     * Create a copy with a sequence number.
     */
    public AgentForgeEvent withSequence(long seq) {
        return new AgentForgeEvent(type, executionId, nodeId, agentId, timestamp, payload, seq);
    }
}

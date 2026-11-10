package io.github.regalpine.agentforge.runtime.streaming;

import io.github.regalpine.agentforge.event.Execution;
/**
 * Event types for AgentForge streaming.
 * <p>
 * Organized in four layers:
 * <ul>
 *   <li>EXECUTION_* — graph execution lifecycle</li>
 *   <li>NODE_* — individual node lifecycle</li>
 *   <li>AGENT_* — agent loop lifecycle</li>
 *   <li>MODEL_* — LLM model interaction</li>
 * </ul>
 */
public enum EventType {

    // Execution layer
    EXECUTION_STARTED,
    EXECUTION_COMPLETED,
    EXECUTION_FAILED,
    EXECUTION_CANCELLED,
    EXECUTION_RESUMED,

    // Node layer
    NODE_STARTED,
    NODE_COMPLETED,
    NODE_FAILED,
    NODE_SKIPPED,
    NODE_WAITING,

    // Agent layer
    AGENT_LOOP_STARTED,
    AGENT_LOOP_STEP,
    AGENT_LOOP_COMPLETED,
    AGENT_DECISION,
    AGENT_OBSERVATION,
    AGENT_BUDGET_UPDATE,
    AGENT_STAGNATION_DETECTED,
    AGENT_POLICY_BLOCKED,

    // Model layer
    MODEL_REQUEST_STARTED,
    MODEL_RESPONSE_RECEIVED,
    MODEL_TOKEN_STREAM,
    MODEL_TOOL_CALL,
    MODEL_ERROR,

    // Streaming layer
    STREAM_STARTED,
    STREAM_CHUNK,
    STREAM_COMPLETED,
    STREAM_ERROR
}

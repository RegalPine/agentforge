package io.github.regalpine.agentforge.runtime.activation;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.event.ExecutionId;

import java.time.Instant;
import java.util.Map;

/**
 * A token produced by a branch within a join frame.
 * When all expected tokens are collected, the join node can proceed.
 *
 * <p>G-01: Token for fork/join persistence.
 */
public record FrameToken(
        String tokenId,
        ExecutionId executionId,
        String joinFrameId,
        EntityId branchNode,
        Map<String, Object> payload,
        Instant producedAt
) {

    public FrameToken {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}

package io.github.regalpine.agentforge.runtime.activation;

import io.github.regalpine.agentforge.event.ExecutionId;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link ActivationStore}.
 */
public final class InMemoryActivationStore implements ActivationStore {

    private final Map<String, Activation> activations = new ConcurrentHashMap<>();
    private final Map<String, FrameToken> tokens = new ConcurrentHashMap<>();
    private final Map<String, JoinFrame> joinFrames = new ConcurrentHashMap<>();

    @Override
    public void saveActivation(Activation activation) {
        activations.put(activation.activationId(), activation);
    }

    @Override
    public Optional<Activation> findActivation(String activationId) {
        return Optional.ofNullable(activations.get(activationId));
    }

    @Override
    public List<Activation> listByExecution(ExecutionId executionId) {
        return activations.values().stream()
                .filter(a -> a.executionId().equals(executionId))
                .toList();
    }

    @Override
    public void saveToken(FrameToken token) {
        tokens.put(token.tokenId(), token);
    }

    @Override
    public List<FrameToken> listTokensByFrame(String joinFrameId) {
        return tokens.values().stream()
                .filter(t -> t.joinFrameId().equals(joinFrameId))
                .toList();
    }

    @Override
    public void saveJoinFrame(JoinFrame frame) {
        joinFrames.put(frame.joinFrameId(), frame);
    }

    @Override
    public Optional<JoinFrame> findJoinFrame(String joinFrameId) {
        return Optional.ofNullable(joinFrames.get(joinFrameId));
    }
}

package io.github.regalpine.agentforge.runtime.activation;

import io.github.regalpine.agentforge.event.ExecutionId;

import java.util.List;
import java.util.Optional;

/**
 * SPI for persisting activations, tokens, and join frames.
 */
public interface ActivationStore {

    void saveActivation(Activation activation);

    Optional<Activation> findActivation(String activationId);

    List<Activation> listByExecution(ExecutionId executionId);

    void saveToken(FrameToken token);

    List<FrameToken> listTokensByFrame(String joinFrameId);

    void saveJoinFrame(JoinFrame frame);

    Optional<JoinFrame> findJoinFrame(String joinFrameId);
}

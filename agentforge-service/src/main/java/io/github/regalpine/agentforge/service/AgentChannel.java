package io.github.regalpine.agentforge.service;

import java.util.concurrent.CompletionStage;

/**
 * Agent Channel SPI.
 * AF-ENHANCE-11 §17: Channel SPI.
 *
 * <p>Channel is the communication
 * transport between Agent and
 * external consumers.
 * Channel ≠ Agent.
 */
public interface AgentChannel {

    /**
     * Channel identifier.
     */
    String id();

    /**
     * Channel type.
     */
    ChannelType type();

    /**
     * Start the channel.
     */
    CompletionStage<Void> start(
        AgentChannelContext context
    );

    /**
     * Stop the channel.
     */
    CompletionStage<Void> stop();
}

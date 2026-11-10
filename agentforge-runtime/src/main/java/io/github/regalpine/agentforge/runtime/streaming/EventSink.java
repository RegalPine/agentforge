package io.github.regalpine.agentforge.runtime.streaming;

/**
 * Event sink for publishing events to an event stream.
 * <p>
 * The EventSink is the producer side of the event system.
 * Runtime components use the sink to emit events during execution.
 */
public interface EventSink {

    /**
     * Publish an event to all subscribers.
     *
     * @param event the event to publish
     */
    void publish(AgentForgeEvent event);

    /**
     * Publish an event with automatic sequence numbering.
     *
     * @param event the event to publish
     * @return the assigned sequence number
     */
    long publishWithSequence(AgentForgeEvent event);
}

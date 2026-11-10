package io.github.regalpine.agentforge.rag.event;

/**
 * Knowledge event sink.
 * AE-09 §66: Knowledge Events — consumed by AE-04 Event Stream.
 */
public interface KnowledgeEventSink {

    /**
     * Emit a knowledge event.
     */
    void emit(KnowledgeEvent event);

    /**
     * A no-op event sink that discards all events.
     */
    KnowledgeEventSink NOOP = event -> {};
}

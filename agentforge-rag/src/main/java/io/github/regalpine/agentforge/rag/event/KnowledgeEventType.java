package io.github.regalpine.agentforge.rag.event;

/**
 * Knowledge event types.
 * AE-09 §66: Knowledge Events.
 */
public enum KnowledgeEventType {

    KNOWLEDGE_INGESTION_STARTED,

    DOCUMENT_PARSED,

    DOCUMENT_CHUNKED,

    DOCUMENT_INDEXED,

    KNOWLEDGE_RETRIEVAL_STARTED,

    KNOWLEDGE_RETRIEVAL_COMPLETED,

    KNOWLEDGE_ACCESS_DENIED,

    KNOWLEDGE_CONTEXT_CREATED,

    CITATION_VALIDATED,

    CITATION_VALIDATION_FAILED
}

package io.github.regalpine.agentforge.rag.model;

/**
 * Document lifecycle status.
 * AE-09 §10: Document Lifecycle.
 */
public enum DocumentStatus {

    DISCOVERED,

    INGESTING,

    PARSED,

    CHUNKED,

    INDEXED,

    ACTIVE,

    FAILED,

    ARCHIVED
}

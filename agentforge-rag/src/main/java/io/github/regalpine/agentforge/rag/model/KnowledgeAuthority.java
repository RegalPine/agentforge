package io.github.regalpine.agentforge.rag.model;

/**
 * Knowledge authority level.
 * AE-09 §44: Knowledge Authority.
 *
 * <p>Represents the governance authority level of a knowledge source,
 * not the truthfulness of its content.
 */
public enum KnowledgeAuthority {

    AUTHORITATIVE,

    TRUSTED,

    GENERAL,

    UNVERIFIED
}

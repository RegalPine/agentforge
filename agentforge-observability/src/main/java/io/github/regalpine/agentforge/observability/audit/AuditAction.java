package io.github.regalpine.agentforge.observability.audit;

/**
 * Audit action type.
 */
public enum AuditAction {

    AGENT_CREATE,
    AGENT_DELETE,
    CAPABILITY_INVOKE,
    GRAPH_COMPILE,
    EXECUTION_START,
    EXECUTION_STOP,
    MEMORY_WRITE,
    MEMORY_READ,
    KNOWLEDGE_INDEX,
    KNOWLEDGE_QUERY,
    POLICY_EVALUATE,
    CREDENTIAL_ACCESS,
    CONFIG_CHANGE,
    PROVIDER_CONNECT,
    DATA_EXPORT
}

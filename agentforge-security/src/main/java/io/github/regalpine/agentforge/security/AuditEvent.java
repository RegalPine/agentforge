package io.github.regalpine.agentforge.security;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Unified security audit event.
 * <p>
 * Records security-relevant operations for compliance and forensics.
 * Audit events must NOT contain secrets or sensitive data.
 * <p>
 * Reuses the AgentForge Event model — does not create a second event protocol.
 */
public record AuditEvent(
    String eventId,
    Instant timestamp,
    String tenantId,
    String principalId,
    AuditAction action,
    String resourceType,
    String resourceId,
    String executionId,
    AuditResult result,
    String reasonCode,
    String source,
    String traceId,
    Severity severity,
    Map<String, Object> metadata
) {

    public AuditEvent {
        Objects.requireNonNull(eventId, "eventId cannot be null");
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
        Objects.requireNonNull(action, "action cannot be null");
        Objects.requireNonNull(result, "result cannot be null");
        Objects.requireNonNull(severity, "severity cannot be null");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static AuditEvent success(String eventId, AuditAction action, String principalId, String tenantId) {
        return new AuditEvent(
            eventId, Instant.now(), tenantId, principalId, action,
            null, null, null, AuditResult.ALLOWED, null, null, null,
            Severity.INFO, Map.of()
        );
    }

    public static AuditEvent denied(String eventId, AuditAction action, String principalId, String tenantId, String reasonCode) {
        return new AuditEvent(
            eventId, Instant.now(), tenantId, principalId, action,
            null, null, null, AuditResult.DENIED, reasonCode, null, null,
            Severity.MEDIUM, Map.of()
        );
    }

    /**
     * Security audit actions.
     */
    public enum AuditAction {
        AUTHENTICATION_SUCCESS,
        AUTHENTICATION_FAILURE,
        AUTHORIZATION_CHECK,
        AUTHORIZATION_DENIED,
        CAPABILITY_INVOKED,
        CAPABILITY_DENIED,
        POLICY_CHECK,
        POLICY_DENIED,
        CREDENTIAL_ACCESS,
        SECRET_REDACTED,
        MCP_SERVER_BLOCKED,
        TOOL_BLOCKED,
        DATA_EGRESS_DENIED,
        MEMORY_ACCESS_DENIED,
        KNOWLEDGE_ACCESS_DENIED,
        AGENT_DELEGATION,
        AGENT_DELEGATION_DENIED,
        CHECKPOINT_ACCESS,
        CHECKPOINT_ACCESS_DENIED,
        DEPLOYMENT_DENIED,
        PROMPT_INJECTION_DETECTED
    }

    /**
     * Audit result.
     */
    public enum AuditResult {
        ALLOWED,
        DENIED,
        ERROR
    }

    /**
     * Security severity.
     */
    public enum Severity {
        INFO,
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}

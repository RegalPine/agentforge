package io.github.regalpine.agentforge.security;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Unified security execution chain.
 * <p>
 * Enforces the security pipeline at each critical point:
 * <pre>
 * AuthN → Tenant → AuthZ → Capability → Model → Tool → Audit
 * </pre>
 * <p>
 * The chain is composable — security checks can be added or removed.
 * All security events are recorded in the audit log.
 */
public final class SecurityExecutionChain {

    private final List<SecurityCheck> checks;
    private final AuditLog auditLog;
    private final SecretRedactor redactor;

    public SecurityExecutionChain(List<SecurityCheck> checks, AuditLog auditLog) {
        this.checks = checks == null ? List.of() : List.copyOf(checks);
        this.auditLog = auditLog != null ? auditLog : AuditLog.inMemory();
        this.redactor = new SecretRedactor();
    }

    /**
     * Execute all security checks for a given request.
     *
     * @param request the security request context
     * @return the result of the security chain
     */
    public SecurityChainResult execute(SecurityChainRequest request) {
        for (SecurityCheck check : checks) {
            SecurityCheckResult result = check.check(request);
            if (!result.allowed()) {
                AuditEvent event = AuditEvent.denied(
                    UUID.randomUUID().toString(),
                    mapToAuditAction(request),
                    request.principalId(),
                    request.tenantId(),
                    result.reasonCode()
                );
                auditLog.record(event);
                return SecurityChainResult.deny(result.reasonCode(), result.message());
            }
        }

        AuditEvent event = AuditEvent.success(
            UUID.randomUUID().toString(),
            mapToAuditAction(request),
            request.principalId(),
            request.tenantId()
        );
        auditLog.record(event);
        return SecurityChainResult.allow();
    }

    /**
     * Get the audit log.
     */
    public AuditLog auditLog() {
        return auditLog;
    }

    /**
     * Get the secret redactor.
     */
    public SecretRedactor redactor() {
        return redactor;
    }

    private AuditEvent.AuditAction mapToAuditAction(SecurityChainRequest request) {
        return switch (request.actionType()) {
            case "AUTHENTICATE" -> AuditEvent.AuditAction.AUTHENTICATION_SUCCESS;
            case "AUTHORIZE" -> AuditEvent.AuditAction.AUTHORIZATION_CHECK;
            case "INVOKE_CAPABILITY" -> AuditEvent.AuditAction.CAPABILITY_INVOKED;
            case "ACCESS_CREDENTIAL" -> AuditEvent.AuditAction.CREDENTIAL_ACCESS;
            case "DELEGATE_AGENT" -> AuditEvent.AuditAction.AGENT_DELEGATION;
            default -> AuditEvent.AuditAction.POLICY_CHECK;
        };
    }

    public static Builder builder() {
        return new Builder();
    }

    public static SecurityExecutionChain permissive() {
        return new SecurityExecutionChain(List.of(), AuditLog.inMemory());
    }

    /**
     * Security check SPI.
     */
    public interface SecurityCheck {
        SecurityCheckResult check(SecurityChainRequest request);
    }

    public static final class Builder {
        private final List<SecurityCheck> checks = new ArrayList<>();
        private AuditLog auditLog;

        public Builder addCheck(SecurityCheck check) {
            checks.add(check);
            return this;
        }

        public Builder auditLog(AuditLog auditLog) {
            this.auditLog = auditLog;
            return this;
        }

        public SecurityExecutionChain build() {
            return new SecurityExecutionChain(checks, auditLog);
        }
    }
}

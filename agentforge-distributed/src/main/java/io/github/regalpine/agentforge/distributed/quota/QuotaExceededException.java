package io.github.regalpine.agentforge.distributed.quota;

/**
 * QuotaExceededException — AF-RUNTIME-01 §24-25.
 *
 * Thrown when a tenant exceeds its quota.
 * Priority cannot bypass quota (§28).
 */
public class QuotaExceededException extends RuntimeException {

    private final String tenantId;
    private final String quotaType;

    public QuotaExceededException(
        String tenantId,
        String quotaType
    ) {
        super("Quota exceeded for tenant '"
            + tenantId + "': " + quotaType);
        this.tenantId = tenantId;
        this.quotaType = quotaType;
    }

    public String tenantId() {
        return tenantId;
    }

    public String quotaType() {
        return quotaType;
    }
}

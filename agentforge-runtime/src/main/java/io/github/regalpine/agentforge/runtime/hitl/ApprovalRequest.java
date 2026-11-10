package io.github.regalpine.agentforge.runtime.hitl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 审批请求。
 *
 * <p>一次正式的人工决策请求。
 * 对应 AF-ENHANCE-03 §11。</p>
 */
public final class ApprovalRequest {

    private final String id;
    private final String executionId;
    private final String traceId;
    private final String agentId;
    private final String policyId;
    private final RiskAssessment risk;
    private ApprovalContext context;
    private final ApprovalRequirement
        requirement;
    private ApprovalStatus status;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final List<ApprovalDecision>
        decisions;
    private long version;

    public ApprovalRequest(
        String id,
        String executionId,
        String traceId,
        String agentId,
        String policyId,
        RiskAssessment risk,
        ApprovalContext context,
        ApprovalRequirement requirement,
        ApprovalStatus status,
        Instant createdAt,
        Instant expiresAt,
        long version
    ) {
        Objects.requireNonNull(
            id, "id"
        );
        Objects.requireNonNull(
            executionId, "executionId"
        );
        Objects.requireNonNull(
            status, "status"
        );
        Objects.requireNonNull(
            createdAt, "createdAt"
        );
        Objects.requireNonNull(
            requirement, "requirement"
        );
        this.id = id;
        this.executionId = executionId;
        this.traceId = traceId;
        this.agentId = agentId;
        this.policyId = policyId;
        this.risk = risk;
        this.context = context;
        this.requirement = requirement;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.decisions =
            new ArrayList<>();
        this.version = version;
    }

    public String id() { return id; }
    public String executionId() {
        return executionId;
    }
    public String traceId() {
        return traceId;
    }
    public String agentId() {
        return agentId;
    }
    public String policyId() {
        return policyId;
    }
    public RiskAssessment risk() {
        return risk;
    }
    public ApprovalContext context() {
        return context;
    }
    public ApprovalRequirement
    requirement() {
        return requirement;
    }
    public ApprovalStatus status() {
        return status;
    }
    public Instant createdAt() {
        return createdAt;
    }
    public Instant expiresAt() {
        return expiresAt;
    }
    public List<ApprovalDecision>
    decisions() {
        return List.copyOf(decisions);
    }
    public long version() {
        return version;
    }

    /**
     * 状态转换（带乐观锁）。
     */
    public synchronized void
    transitionTo(
        ApprovalStatus newStatus,
        long expectedVersion
    ) {
        if (this.version
            != expectedVersion
        ) {
            throw new IllegalStateException(
                "Optimistic lock failed: "
                    + "expected "
                    + expectedVersion
                    + " but was "
                    + this.version
            );
        }
        if (!status.canTransitionTo(
            newStatus
        )) {
            throw new IllegalStateException(
                "Invalid transition: "
                    + status + " -> "
                    + newStatus
            );
        }
        this.status = newStatus;
        this.version++;
    }

    /**
     * 添加决策。
     */
    public synchronized void
    addDecision(
        ApprovalDecision decision
    ) {
        decisions.add(decision);
        this.version++;
    }

    /**
     * 更新上下文（MODIFY 后）。
     */
    public void updateContext(
        ApprovalContext ctx
    ) {
        this.context = ctx;
    }

    /**
     * 是否已过期。
     */
    public boolean isExpired() {
        if (expiresAt == null) {
            return false;
        }
        return Instant.now()
            .isAfter(expiresAt);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String id;
        private String executionId;
        private String traceId;
        private String agentId;
        private String policyId;
        private RiskAssessment risk;
        private ApprovalContext context;
        private ApprovalRequirement
            requirement;
        private ApprovalStatus status =
            ApprovalStatus.PENDING;
        private Instant createdAt;
        private Instant expiresAt;
        private long version = 1;

        Builder() {}

        public Builder id(String v) {
            this.id = v;
            return this;
        }

        public Builder executionId(
            String v
        ) {
            this.executionId = v;
            return this;
        }

        public Builder traceId(
            String v
        ) {
            this.traceId = v;
            return this;
        }

        public Builder agentId(
            String v
        ) {
            this.agentId = v;
            return this;
        }

        public Builder policyId(
            String v
        ) {
            this.policyId = v;
            return this;
        }

        public Builder risk(
            RiskAssessment v
        ) {
            this.risk = v;
            return this;
        }

        public Builder context(
            ApprovalContext v
        ) {
            this.context = v;
            return this;
        }

        public Builder requirement(
            ApprovalRequirement v
        ) {
            this.requirement = v;
            return this;
        }

        public Builder status(
            ApprovalStatus v
        ) {
            this.status = v;
            return this;
        }

        public Builder createdAt(
            Instant v
        ) {
            this.createdAt = v;
            return this;
        }

        public Builder expiresAt(
            Instant v
        ) {
            this.expiresAt = v;
            return this;
        }

        public Builder version(
            long v
        ) {
            this.version = v;
            return this;
        }

        public ApprovalRequest build() {
            if (id == null) {
                id = "apr-"
                    + UUID.randomUUID();
            }
            if (createdAt == null) {
                createdAt =
                    Instant.now();
            }
            return new ApprovalRequest(
                id, executionId,
                traceId, agentId,
                policyId, risk,
                context, requirement,
                status, createdAt,
                expiresAt, version
            );
        }
    }
}

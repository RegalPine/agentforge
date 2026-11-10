/**
 * Governance workspace viewer models.
 * AF-STUDIO-01 §40-41: Governance Workspace, Policy Decision Viewer.
 *
 * Maps to backend AF-SEC-01 Security/Governance types.
 */

// ── Policy ──

export type PolicyStatus = "ACTIVE" | "DRAFT" | "DISABLED" | "DEPRECATED";

export type PolicyEffect = "ALLOW" | "DENY" | "REQUIRE_APPROVAL" | "REDACT";

export interface PolicyView {
  readonly policyId: string;
  readonly name: string;
  readonly description: string;
  readonly version: string;
  readonly status: PolicyStatus;
  readonly scope: string;
  readonly rules: PolicyRuleView[];
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface PolicyRuleView {
  readonly ruleId: string;
  readonly name: string;
  readonly effect: PolicyEffect;
  readonly condition: string;
  readonly priority: number;
}

// ── Policy Decision ──

/**
 * PolicyDecisionView - a single policy evaluation result.
 * AF-STUDIO-01 §41: Policy Decision Viewer.
 */
export interface PolicyDecisionView {
  readonly decisionId: string;
  readonly policyId: string;
  readonly policyName: string;
  readonly effect: PolicyEffect;
  readonly reason: string;
  readonly actor: string;
  readonly tenantId: string;
  readonly resource: string;
  readonly resourceVersion: string;
  readonly timestamp: string;
  readonly requestId: string;
  readonly executionId?: string;
  readonly metadata: Record<string, unknown>;
}

// ── Violation ──

export type ViolationSeverity = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export interface PolicyViolationView {
  readonly violationId: string;
  readonly decisionId: string;
  readonly policyId: string;
  readonly severity: ViolationSeverity;
  readonly description: string;
  readonly actor: string;
  readonly timestamp: string;
  readonly resolved: boolean;
  readonly resolvedAt?: string;
  readonly resolvedBy?: string;
}

// ── Approval ──

export type ApprovalStatus = "PENDING" | "APPROVED" | "REJECTED" | "EXPIRED";

export interface ApprovalView {
  readonly approvalId: string;
  readonly action: string;
  readonly resource: string;
  readonly risk: "LOW" | "MEDIUM" | "HIGH";
  readonly reason: string;
  readonly status: ApprovalStatus;
  readonly requestedBy: string;
  readonly requestedAt: string;
  readonly decidedBy?: string;
  readonly decidedAt?: string;
  readonly decision?: string;
}

// ── Audit Entry ──

export interface AuditEntryView {
  readonly entryId: string;
  readonly actor: string;
  readonly tenantId: string;
  readonly action: string;
  readonly resource: string;
  readonly resourceVersion: string;
  readonly timestamp: string;
  readonly requestId: string;
  readonly result: "SUCCESS" | "FAILURE" | "DENIED";
  readonly policyDecisionId?: string;
  readonly metadata: Record<string, unknown>;
}

// ── Risk Summary ──

export interface RiskSummaryView {
  readonly totalViolations: number;
  readonly criticalViolations: number;
  readonly highViolations: number;
  readonly mediumViolations: number;
  readonly lowViolations: number;
  readonly unresolvedViolations: number;
  readonly pendingApprovals: number;
}

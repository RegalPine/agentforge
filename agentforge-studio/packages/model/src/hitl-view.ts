/**
 * Human-in-the-loop Viewer Models (AF-STUDIO-01 §42)
 *
 * When Execution enters WAITING_HUMAN:
 *   ┌─────────────────────────────┐
 *   │ Human Approval Required     │
 *   ├─────────────────────────────┤
 *   │ Action: payment.execute     │
 *   │ Risk: HIGH                  │
 *   │ Reason: Policy requirement  │
 *   │                             │
 *   │ [Approve]     [Reject]      │
 *   └─────────────────────────────┘
 *
 * Approve/Reject must go through: Authentication → Authorization → Policy → Audit
 */

export type HumanApprovalStatus =
  | "PENDING"
  | "APPROVED"
  | "REJECTED"
  | "EXPIRED"
  | "CANCELLED";

export type RiskLevel =
  | "LOW"
  | "MEDIUM"
  | "HIGH"
  | "CRITICAL";

export interface HumanApprovalRequest {
  readonly requestId: string;
  readonly executionId: string;
  readonly action: string;
  readonly risk: RiskLevel;
  readonly reason: string;
  readonly policy?: string;
  readonly actor?: string;
  readonly arguments?: Record<string, unknown>;
  readonly status: HumanApprovalStatus;
  readonly createdAt: string;
  readonly expiresAt?: string;
  readonly decidedAt?: string;
  readonly decidedBy?: string;
  readonly decisionReason?: string;
}

export interface HumanApprovalDecision {
  readonly requestId: string;
  readonly approved: boolean;
  readonly reason: string;
}

export interface HumanApprovalSummary {
  readonly totalPending: number;
  readonly totalApproved: number;
  readonly totalRejected: number;
  readonly totalExpired: number;
  readonly avgResponseTimeMs: number;
}

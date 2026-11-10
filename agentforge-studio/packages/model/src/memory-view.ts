/**
 * Memory viewer models.
 * AF-STUDIO-01 §31: Memory Viewer.
 *
 * Maps to backend AE-08 Memory types.
 * Studio must NOT create parallel semantic types.
 */

// ── Memory Type ──

export type MemoryType =
  | "CONVERSATION"
  | "FACT"
  | "PREFERENCE"
  | "TASK"
  | "EXPERIENCE"
  | "SUMMARY";

export type MemoryScope =
  | "EXECUTION"
  | "CONVERSATION"
  | "USER"
  | "AGENT"
  | "TENANT";

export type MemoryStatus =
  | "CANDIDATE"
  | "ACTIVE"
  | "EXPIRED"
  | "ARCHIVED"
  | "DELETED";

export type MemorySource =
  | "USER_EXPLICIT"
  | "AGENT_INFERRED"
  | "TOOL_RESULT"
  | "CAPABILITY_RESULT"
  | "SYSTEM"
  | "IMPORTED";

export type MemoryImportance =
  | "LOW"
  | "NORMAL"
  | "HIGH"
  | "CRITICAL";

// ── Memory Record ──

/**
 * MemoryRecordView - a persisted memory for display.
 */
export interface MemoryRecordView {
  readonly memoryId: string;
  readonly tenantId: string;
  readonly subjectId: string;
  readonly type: MemoryType;
  readonly scope: MemoryScope;
  readonly content: string;
  readonly status: MemoryStatus;
  readonly source: MemorySource;
  readonly confidence: number;
  readonly importance: MemoryImportance;
  readonly createdAt: string;
  readonly updatedAt: string;
  readonly expiresAt?: string;
  readonly version: number;
  readonly provenance: MemoryProvenanceView;
  readonly attributes: Record<string, unknown>;
}

/**
 * MemoryProvenanceView - why and where a memory exists.
 */
export interface MemoryProvenanceView {
  readonly sourceExecutionId: string;
  readonly sourceMessageId: string;
  readonly sourceCapabilityId: string;
  readonly createdBy: string;
}

// ── Memory Query ──

/**
 * MemoryQueryView - search parameters for memory retrieval.
 */
export interface MemoryQueryView {
  readonly subjectId: string;
  readonly scope: MemoryScope;
  readonly type?: MemoryType;
  readonly query?: string;
  readonly limit: number;
  readonly strategy: "EXACT" | "KEYWORD" | "SEMANTIC" | "HYBRID";
}

// ── Memory Candidate ──

/**
 * MemoryCandidateView - a pending memory awaiting policy.
 */
export interface MemoryCandidateView {
  readonly candidateId: string;
  readonly tenantId: string;
  readonly subjectId: string;
  readonly type: MemoryType;
  readonly scope: MemoryScope;
  readonly content: string;
  readonly source: MemorySource;
  readonly confidence: number;
  readonly importance: MemoryImportance;
  readonly status: "PENDING" | "APPROVED" | "REJECTED";
  readonly rejectionReason?: string;
}

// ── Memory Audit ──

/**
 * MemoryAuditEntry - audit trail for memory operations.
 */
export interface MemoryAuditEntry {
  readonly entryId: string;
  readonly memoryId: string;
  readonly action: "CREATED" | "UPDATED" | "EXPIRED" | "ARCHIVED" | "DELETED";
  readonly actor: string;
  readonly timestamp: string;
  readonly reason: string;
  readonly previousVersion: number;
  readonly newVersion: number;
}

// ── Memory Metrics ──

/**
 * MemoryMetricsView - observability counters.
 */
export interface MemoryMetricsView {
  readonly readTotal: number;
  readonly writeTotal: number;
  readonly hitTotal: number;
  readonly missTotal: number;
  readonly candidateTotal: number;
  readonly promotedTotal: number;
  readonly rejectedTotal: number;
  readonly expiredTotal: number;
  readonly conflictTotal: number;
}

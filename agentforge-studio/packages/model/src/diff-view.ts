/**
 * Diff Viewer Models (AF-STUDIO-01 §47-48)
 *
 * Diff Viewer:
 *   Agent Diff
 *   Graph Diff
 *   Capability Diff
 *   Policy Diff
 *   Prompt Diff
 *   Schema Diff
 *
 * Graph Diff:
 *   Added Node / Removed Node / Changed Edge / Changed Route /
 *   Changed Capability / Changed Configuration
 */

export type DiffEntityType =
  | "AGENT"
  | "GRAPH"
  | "CAPABILITY"
  | "POLICY"
  | "PROMPT"
  | "SCHEMA";

export type DiffChangeType =
  | "ADDED"
  | "REMOVED"
  | "MODIFIED"
  | "UNCHANGED";

export interface DiffResult {
  readonly entityType: DiffEntityType;
  readonly entityId: string;
  readonly leftVersion: string;
  readonly rightVersion: string;
  readonly changes: DiffChange[];
  readonly summary: DiffSummary;
}

export interface DiffChange {
  readonly path: string;
  readonly changeType: DiffChangeType;
  readonly leftValue?: string;
  readonly rightValue?: string;
  readonly description: string;
}

export interface DiffSummary {
  readonly added: number;
  readonly removed: number;
  readonly modified: number;
  readonly unchanged: number;
}

// ── Graph-specific Diff (AF-STUDIO-01 §48) ──

export interface GraphDiffResult extends DiffResult {
  readonly entityType: "GRAPH";
  readonly nodeChanges: GraphNodeChange[];
  readonly edgeChanges: GraphEdgeChange[];
}

export interface GraphNodeChange {
  readonly nodeId: string;
  readonly changeType: DiffChangeType;
  readonly leftNodeType?: string;
  readonly rightNodeType?: string;
  readonly configChanged: boolean;
}

export interface GraphEdgeChange {
  readonly edgeId: string;
  readonly changeType: DiffChangeType;
  readonly leftSource?: string;
  readonly leftTarget?: string;
  readonly rightSource?: string;
  readonly rightTarget?: string;
  readonly routeChanged: boolean;
}

// ── Version Management (AF-STUDIO-01 §45) ──

export type VersionLifecycleStatus =
  | "DRAFT"
  | "VALIDATED"
  | "TESTED"
  | "EVALUATED"
  | "APPROVED"
  | "RELEASED"
  | "DEPRECATED";

export interface VersionEntry {
  readonly entityType: DiffEntityType;
  readonly entityId: string;
  readonly version: string;
  readonly status: VersionLifecycleStatus;
  readonly createdAt: string;
  readonly updatedAt: string;
  readonly changelog?: string;
}

// ── Release Gate (AF-STUDIO-01 §46) ──

export type ReleaseGateStatus =
  | "PENDING"
  | "PASSED"
  | "FAILED"
  | "SKIPPED";

export interface ReleaseGateView {
  readonly gateId: string;
  readonly entityType: DiffEntityType;
  readonly entityId: string;
  readonly version: string;
  readonly gates: ReleaseGateStep[];
  readonly overallStatus: ReleaseGateStatus;
}

export interface ReleaseGateStep {
  readonly name: string;
  readonly status: ReleaseGateStatus;
  readonly message?: string;
  readonly completedAt?: string;
}

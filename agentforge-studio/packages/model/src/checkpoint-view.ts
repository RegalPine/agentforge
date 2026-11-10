/**
 * Checkpoint & Recovery Viewer Models (AF-STUDIO-01 §43-44)
 *
 * Checkpoint Viewer:
 *   Checkpoint
 *   ├── ID
 *   ├── Execution
 *   ├── Sequence
 *   ├── State Snapshot
 *   ├── Execution Snapshot
 *   ├── Graph Version
 *   ├── Plan Version
 *   ├── Checksum
 *   └── Timestamp
 *
 * Recovery UI:
 *   Execution Failed → Latest Checkpoint → Inspect → Recovery Decision → Resume
 */

export type CheckpointStatus =
  | "CREATED"
  | "VERIFIED"
  | "RESTORED"
  | "INVALID";

export interface CheckpointView {
  readonly checkpointId: string;
  readonly executionId: string;
  readonly sequence: number;
  readonly stateSnapshot: Record<string, unknown>;
  readonly executionSnapshot: ExecutionSnapshotView;
  readonly graphVersion: string;
  readonly planVersion: string;
  readonly checksum: string;
  readonly createdAt: string;
  readonly status: CheckpointStatus;
}

export interface ExecutionSnapshotView {
  readonly executionId: string;
  readonly currentNodeId?: string;
  readonly status: string;
  readonly attempt: number;
  readonly nodeStates: Record<string, NodeSnapshotView>;
}

export interface NodeSnapshotView {
  readonly nodeId: string;
  readonly status: string;
  readonly attempt: number;
  readonly lastResult?: Record<string, unknown>;
}

// ── Recovery (AF-STUDIO-01 §44) ──

export type RecoveryDecision =
  | "RESUME"
  | "RETRY_FROM_CHECKPOINT"
  | "RESTART"
  | "ABORT";

export interface RecoveryView {
  readonly executionId: string;
  readonly failedAt: string;
  readonly failureReason: string;
  readonly latestCheckpointId?: string;
  readonly latestCheckpointSequence: number;
  readonly availableDecisions: RecoveryDecision[];
  readonly recommendedDecision: RecoveryDecision;
}

export interface RecoveryRequest {
  readonly executionId: string;
  readonly checkpointId?: string;
  readonly decision: RecoveryDecision;
}

// ── State Conflict (AF-STUDIO-01 §29) ──

export type ConflictResolutionStrategy =
  | "FIRST_WRITE_WINS"
  | "LAST_WRITE_WINS"
  | "MERGE"
  | "FAIL";

export interface StateConflictView {
  readonly executionId: string;
  readonly nodeA: string;
  readonly nodeB: string;
  readonly stateDeltaA: Record<string, unknown>;
  readonly stateDeltaB: Record<string, unknown>;
  readonly strategy: ConflictResolutionStrategy;
  readonly resolution: string;
  readonly resolvedState: Record<string, unknown>;
}

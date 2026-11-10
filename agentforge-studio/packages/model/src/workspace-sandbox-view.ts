/**
 * Workspace & Sandbox viewer models.
 * AF-ENHANCE-07: Workspace / Sandbox / Artifact.
 *
 * Maps to backend agentforge-workspace types.
 */

// ── Workspace ──

/**
 * WorkspaceView - workspace overview.
 */
export interface WorkspaceView {
  readonly workspaceId: string;
  readonly name: string;
  readonly scope: WorkspaceScopeView;
  readonly status: WorkspaceStatusView;
  readonly sandboxCount: number;
  readonly artifactCount: number;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export type WorkspaceScopeView =
  | "AGENT"
  | "GRAPH"
  | "SESSION"
  | "TENANT"
  | "GLOBAL";

export type WorkspaceStatusView =
  | "ACTIVE"
  | "ARCHIVED"
  | "SUSPENDED"
  | "DELETED"
  | "INITIALIZING";

// ── Sandbox ──

/**
 * SandboxView - sandbox within a workspace.
 */
export interface SandboxView {
  readonly sandboxId: string;
  readonly workspaceId: string;
  readonly isolationLevel: SandboxIsolationView;
  readonly status: SandboxStatusView;
  readonly resourceLimits: SandboxResourceLimits;
  readonly createdAt: string;
  readonly expiresAt?: string;
}

export type SandboxIsolationView =
  | "PROCESS"
  | "CONTAINER"
  | "VM"
  | "NETWORK"
  | "FULL";

export type SandboxStatusView =
  | "CREATED"
  | "STARTING"
  | "RUNNING"
  | "PAUSED"
  | "STOPPING"
  | "STOPPED"
  | "FAILED"
  | "EXPIRED";

export interface SandboxResourceLimits {
  readonly maxCpu: number;
  readonly maxMemoryMb: number;
  readonly maxDiskMb: number;
  readonly maxNetworkBandwidthMbps: number;
  readonly timeoutSeconds: number;
}

// ── Artifact ──

/**
 * ArtifactView - artifact within a workspace.
 */
export interface ArtifactView {
  readonly artifactId: string;
  readonly workspaceId: string;
  readonly name: string;
  readonly type: ArtifactTypeView;
  readonly sizeBytes: number;
  readonly checksum: string;
  readonly createdAt: string;
  readonly createdBy: string;
  readonly metadata?: Record<string, string>;
}

export type ArtifactTypeView =
  | "FILE"
  | "IMAGE"
  | "DATA"
  | "MODEL_OUTPUT"
  | "LOG"
  | "CHECKPOINT"
  | "CONFIG"
  | "OTHER";

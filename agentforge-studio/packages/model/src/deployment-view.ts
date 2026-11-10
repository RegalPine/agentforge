/**
 * Control Plane / Deployment viewer models.
 * AF-ENHANCE-12 §2: Deployment / Environment / Release.
 *
 * Maps to backend agentforge-control-plane types.
 */

// ── Deployment ──

/**
 * DeploymentView - deployment overview.
 */
export interface DeploymentView {
  readonly deploymentId: string;
  readonly namespace: string;
  readonly name: string;
  readonly status: DeploymentStatusView;
  readonly strategy: DeploymentStrategyView;
  readonly instanceCount: number;
  readonly readyCount: number;
  readonly environment: string;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export type DeploymentStatusView =
  | "PENDING"
  | "PROVISIONING"
  | "RUNNING"
  | "DEGRADED"
  | "SCALING"
  | "ROLLING"
  | "STOPPED"
  | "FAILED";

export type DeploymentStrategyView =
  | "BLUE_GREEN"
  | "CANARY"
  | "ROLLING"
  | "RECREATE";

// ── Runtime Instance ──

/**
 * RuntimeInstanceView - individual runtime instance.
 */
export interface RuntimeInstanceView {
  readonly instanceId: string;
  readonly deploymentId: string;
  readonly status: InstanceStatusView;
  readonly endpoint: string;
  readonly region: string;
  readonly startedAt: string;
  readonly lastHeartbeat?: string;
  readonly resourceUsage: ResourceUsageView;
}

export type InstanceStatusView =
  | "STARTING"
  | "RUNNING"
  | "READY"
  | "BUSY"
  | "DRAINING"
  | "STOPPING"
  | "STOPPED"
  | "FAILED"
  | "UNKNOWN";

export interface ResourceUsageView {
  readonly cpuPercent: number;
  readonly memoryPercent: number;
  readonly heapUsedMb: number;
  readonly threadCount: number;
}

// ── Environment ──

/**
 * EnvironmentView - deployment environment.
 */
export interface EnvironmentView {
  readonly environmentId: string;
  readonly name: string;
  readonly type: EnvironmentTypeView;
  readonly deploymentCount: number;
  readonly totalInstances: number;
  readonly createdAt: string;
}

export type EnvironmentTypeView =
  | "DEVELOPMENT"
  | "STAGING"
  | "PRODUCTION"
  | "TEST";

// ── Release ──

/**
 * ReleaseView - release status.
 */
export interface ReleaseView {
  readonly releaseId: string;
  readonly deploymentId: string;
  readonly version: string;
  readonly status: ReleaseStatusView;
  readonly rolloutPercent: number;
  readonly startedAt: string;
  readonly completedAt?: string;
}

export type ReleaseStatusView =
  | "PLANNED"
  | "IN_PROGRESS"
  | "CANARY"
  | "MONITORING"
  | "COMPLETED"
  | "ROLLED_BACK"
  | "FAILED"
  | "CANCELLED";

// ── Resource Quota ──

/**
 * ResourceQuotaView - environment resource quota.
 */
export interface ResourceQuotaView {
  readonly environmentId: string;
  readonly maxInstances: number;
  readonly maxCpu: number;
  readonly maxMemoryMb: number;
  readonly usedInstances: number;
  readonly usedCpu: number;
  readonly usedMemoryMb: number;
}

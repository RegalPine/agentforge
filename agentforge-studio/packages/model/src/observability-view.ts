/**
 * Observability dashboard viewer models.
 * AF-STUDIO-01 §61-63: Observability Dashboard, Model Dashboard, Capability Health.
 *
 * Maps to backend AE-04 Event / Observability types.
 */

// ── Dashboard Metrics ──

/**
 * ObservabilityMetrics - core dashboard KPIs.
 * AF-STUDIO-01 §61: Observability Dashboard.
 */
export interface ObservabilityMetrics {
  readonly executionRate: number;
  readonly successRate: number;
  readonly failureRate: number;
  readonly p95LatencyMs: number;
  readonly modelLatencyMs: number;
  readonly toolLatencyMs: number;
  readonly tokenUsage: number;
  readonly cost: number;
  readonly retryCount: number;
  readonly timeoutCount: number;
  readonly policyDeniedCount: number;
  readonly humanApprovalCount: number;
  readonly period: TimeRange;
}

// ── Time Range ──

export interface TimeRange {
  readonly from: string;
  readonly to: string;
}

// ── Metric Series ──

/**
 * MetricSeries - time-series data point for charts.
 */
export interface MetricSeries {
  readonly metric: string;
  readonly dataPoints: MetricDataPoint[];
  readonly unit: string;
}

export interface MetricDataPoint {
  readonly timestamp: string;
  readonly value: number;
}

// ── Model Dashboard ──

/**
 * ModelDashboardView - per-model metrics.
 * AF-STUDIO-01 §62: Model Dashboard.
 */
export interface ModelDashboardView {
  readonly modelId: string;
  readonly provider: string;
  readonly version: string;
  readonly requestCount: number;
  readonly tokenUsage: number;
  readonly avgLatencyMs: number;
  readonly errorRate: number;
  readonly toolCallCount: number;
  readonly structuredOutputFailures: number;
  readonly cost: number;
}

// ── Capability Health ──

/**
 * CapabilityHealthView - per-capability health.
 * AF-STUDIO-01 §63: Capability Health.
 */
export interface CapabilityHealthView {
  readonly capabilityId: string;
  readonly name: string;
  readonly health: "HEALTHY" | "DEGRADED" | "UNAVAILABLE" | "UNKNOWN";
  readonly avgLatencyMs: number;
  readonly errorRate: number;
  readonly timeoutCount: number;
  readonly rateLimitCount: number;
  readonly circuitBreakerState: "CLOSED" | "OPEN" | "HALF_OPEN";
}

// ── Filter ──

/**
 * ObservabilityFilter - dashboard filter parameters.
 */
export interface ObservabilityFilter {
  readonly agentId?: string;
  readonly graphId?: string;
  readonly capabilityId?: string;
  readonly tenantId?: string;
  readonly modelId?: string;
  readonly timeRange: TimeRange;
}

// ── Multi-Agent Tree ──

/**
 * MultiAgentTreeView - execution tree for multi-agent.
 * AF-STUDIO-01 §36: Multi-Agent Viewer.
 */
export interface MultiAgentTreeView {
  readonly rootExecutionId: string;
  readonly rootAgentId: string;
  readonly rootAgentName: string;
  readonly children: MultiAgentTreeNode[];
}

export interface MultiAgentTreeNode {
  readonly executionId: string;
  readonly agentId: string;
  readonly agentName: string;
  readonly status: string;
  readonly delegationReason?: string;
  readonly budgetAllocated?: number;
  readonly budgetUsed?: number;
  readonly children: MultiAgentTreeNode[];
}

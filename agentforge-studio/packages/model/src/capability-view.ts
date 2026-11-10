/**
 * Capability Workspace Viewer Models (AF-STUDIO-01 §11-14)
 *
 * Capability Workspace:
 *   Capability
 *   ├── Overview
 *   ├── Schema
 *   ├── Provider
 *   ├── Security
 *   ├── Policy
 *   ├── Health
 *   ├── Versions
 *   ├── Invocations
 *   └── Audit
 */

// ── Capability Types (AF-STUDIO-01 §12) ──

export type CapabilityType =
  | "JAVA"
  | "HTTP"
  | "MCP"
  | "RETRIEVAL"
  | "MEMORY"
  | "AGENT"
  | "MODEL";

export type SideEffectClassification =
  | "READ_ONLY"
  | "WRITE"
  | "EXTERNAL_SIDE_EFFECT";

export type CapabilityHealthStatus =
  | "HEALTHY"
  | "DEGRADED"
  | "UNAVAILABLE"
  | "UNKNOWN";

export type CircuitBreakerState =
  | "CLOSED"
  | "OPEN"
  | "HALF_OPEN";

// ── Capability Overview (AF-STUDIO-01 §13) ──

export interface CapabilityDetailView {
  readonly capabilityId: string;
  readonly version: string;
  readonly type: CapabilityType;
  readonly description: string;
  readonly inputSchema?: Record<string, unknown>;
  readonly outputSchema?: Record<string, unknown>;
  readonly sideEffect: SideEffectClassification;
  readonly provider: string;
  readonly security: CapabilitySecurityView;
  readonly timeout: CapabilityTimeoutView;
  readonly retry: CapabilityRetryView;
  readonly policy: CapabilityPolicyView;
  readonly health: CapabilityHealthDetailView;
}

export interface CapabilitySecurityView {
  readonly requiredPermissions: string[];
  readonly authRequired: boolean;
  readonly credentialRef?: string;
  readonly credentialStatus?: string;
}

export interface CapabilityTimeoutView {
  readonly timeoutMs: number;
  readonly connectTimeoutMs: number;
}

export interface CapabilityRetryView {
  readonly maxRetries: number;
  readonly backoffMs: number;
  readonly maxBackoffMs: number;
}

export interface CapabilityPolicyView {
  readonly rateLimitPerMinute?: number;
  readonly idempotencyKeyRequired: boolean;
  readonly allowedTenants?: string[];
}

export interface CapabilityHealthDetailView {
  readonly status: CapabilityHealthStatus;
  readonly circuitBreakerState: CircuitBreakerState;
  readonly successRate: number;
  readonly avgLatencyMs: number;
  readonly errorCount: number;
  readonly timeoutCount: number;
  readonly rateLimitCount: number;
  readonly lastCheckedAt: string;
}

// ── Capability Invocation (AF-STUDIO-01 §14) ──

export interface CapabilityInvocationRequest {
  readonly capabilityId: string;
  readonly version: string;
  readonly input: Record<string, unknown>;
  readonly tenantId: string;
}

export interface CapabilityInvocationResult {
  readonly success: boolean;
  readonly output?: Record<string, unknown>;
  readonly error?: string;
  readonly latencyMs: number;
  readonly policyDecision: string;
  readonly auditRecorded: boolean;
}

// ── Capability Summary for list ──

export interface CapabilitySummaryView {
  readonly capabilityId: string;
  readonly version: string;
  readonly type: CapabilityType;
  readonly description: string;
  readonly provider: string;
  readonly sideEffect: SideEffectClassification;
  readonly health: CapabilityHealthStatus;
  readonly invocationCount: number;
}

// ── Capability Invocation Audit ──

export interface CapabilityInvocationAuditEntry {
  readonly invocationId: string;
  readonly capabilityId: string;
  readonly timestamp: string;
  readonly tenantId: string;
  readonly actor: string;
  readonly success: boolean;
  readonly latencyMs: number;
  readonly policyDecision: string;
}

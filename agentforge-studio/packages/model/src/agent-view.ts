/**
 * Agent Workspace Viewer Models (AF-STUDIO-01 §7-10)
 *
 * Agent Workspace:
 *   Agent
 *   ├── Overview
 *   ├── Definition
 *   ├── Model
 *   ├── Capabilities
 *   ├── Instructions
 *   ├── Memory
 *   ├── Knowledge
 *   ├── Governance
 *   ├── Versions
 *   ├── Tests
 *   ├── Evaluations
 *   └── Executions
 */

// ── Agent Version Lifecycle (AF-STUDIO-01 §45) ──

export type AgentVersionStatus =
  | "DRAFT"
  | "VALIDATED"
  | "TESTED"
  | "EVALUATED"
  | "APPROVED"
  | "RELEASED"
  | "DEPRECATED";

// ── Agent Overview (AF-STUDIO-01 §8) ──

export interface AgentOverview {
  readonly agentId: string;
  readonly version: string;
  readonly status: AgentVersionStatus;
  readonly description: string;
  readonly model: string;
  readonly capabilities: string[];
  readonly graphReferences: string[];
  readonly governanceStatus: string;
  readonly deploymentStatus: string;
  readonly evaluationStatus: string;
  readonly policyStatus: string;
  readonly inputSchema?: Record<string, unknown>;
  readonly outputSchema?: Record<string, unknown>;
}

// ── Agent Definition (AF-STUDIO-01 §9) ──

export interface AgentDefinitionView {
  readonly agentId: string;
  readonly version: string;
  readonly identity: AgentIdentityView;
  readonly contract: AgentContractView;
  readonly model: AgentModelView;
  readonly capabilityReferences: CapabilityReferenceView[];
  readonly memoryPolicy?: MemoryPolicyView;
  readonly knowledgePolicy?: KnowledgePolicyView;
  readonly executionPolicy?: ExecutionPolicyView;
  readonly governanceMetadata: Record<string, unknown>;
}

export interface AgentIdentityView {
  readonly agentId: string;
  readonly name: string;
  readonly description: string;
  readonly tenantId: string;
}

export interface AgentContractView {
  readonly inputSchema?: Record<string, unknown>;
  readonly outputSchema?: Record<string, unknown>;
}

export interface AgentModelView {
  readonly modelId: string;
  readonly provider: string;
  readonly version: string;
}

export interface CapabilityReferenceView {
  readonly capabilityId: string;
  readonly version: string;
  readonly required: boolean;
}

export interface MemoryPolicyView {
  readonly enabled: boolean;
  readonly maxRecords: number;
  readonly allowedTypes: string[];
  readonly allowedScopes: string[];
}

export interface KnowledgePolicyView {
  readonly enabled: boolean;
  readonly allowedSources: string[];
  readonly maxResults: number;
}

export interface ExecutionPolicyView {
  readonly maxIterations: number;
  readonly maxDurationMs: number;
  readonly maxTokens: number;
  readonly maxToolCalls: number;
  readonly budgetLimit?: number;
}

// ── Agent Version (AF-STUDIO-01 §10) ──

export interface AgentVersionView {
  readonly agentId: string;
  readonly version: string;
  readonly status: AgentVersionStatus;
  readonly createdAt: string;
  readonly updatedAt: string;
  readonly changelog?: string;
}

// ── Agent Summary for list ──

export interface AgentSummaryView {
  readonly agentId: string;
  readonly version: string;
  readonly name: string;
  readonly description: string;
  readonly status: AgentVersionStatus;
  readonly model: string;
  readonly capabilityCount: number;
  readonly latestExecutionAt?: string;
}

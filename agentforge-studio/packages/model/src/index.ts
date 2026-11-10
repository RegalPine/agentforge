// @agentforge/model - TypeScript model definitions
// Maps 1:1 to backend AgentForge Server/JSON contract.
// Studio must NOT create parallel semantic types.

export type { NodeType } from "./node-type.js";
export { ALL_NODE_TYPES } from "./node-type.js";

export type { EdgeType } from "./edge-type.js";
export { ALL_EDGE_TYPES } from "./edge-type.js";

export type { Reference } from "./reference.js";
export { createReference, referenceEquals, referenceToString } from "./reference.js";

export type { NodeDefinition } from "./node-definition.js";
export { createNode } from "./node-definition.js";

export type { EdgeDefinition } from "./edge-definition.js";
export { createEdge } from "./edge-definition.js";

export type { DataBinding, Expression } from "./binding.js";

export type { GraphDefinition } from "./graph-definition.js";
export { createGraphDefinition } from "./graph-definition.js";

export type {
  ExecutionStatus,
  Execution,
  ExecutionRequest,
  ExecutionHandle,
  ExecutionState,
  NodeState,
  ExecutionEvent,
  CompilationResult,
  CompilationDiagnostic,
  ValidationResult,
} from "./execution.js";

export type {
  ProviderDescriptor,
  CapabilityDescriptor,
  AgentDescriptor,
  JsonSchema,
  JsonSchemaProperty,
} from "./descriptors.js";

export type { CanvasState } from "./canvas-state.js";
export { createDefaultCanvasState } from "./canvas-state.js";

// ── Evaluation (AF-STUDIO-01 §37-39) ──

export type {
  EvaluationDataset,
  TestCaseStatus,
  EvaluationTestCase,
  CriteriaType,
  EvaluationCriteria,
  EvaluationRunStatus,
  EvaluationRun,
  CaseResultStatus,
  EvaluationCaseResult,
  CriteriaResultStatus,
  CriteriaResult,
  EvaluationReport,
  EnvironmentFingerprint,
  FindingSeverity,
  EvaluationFinding,
  RegressionComparison,
  RegressionChange,
  NumericRegressionChange,
} from "./evaluation.js";

// ── Memory Viewer (AF-STUDIO-01 §31) ──

export type {
  MemoryType,
  MemoryScope,
  MemoryStatus,
  MemorySource,
  MemoryImportance,
  MemoryRecordView,
  MemoryProvenanceView,
  MemoryQueryView,
  MemoryCandidateView,
  MemoryAuditEntry,
  MemoryMetricsView,
} from "./memory-view.js";

// ── Knowledge Viewer (AF-STUDIO-01 §32-33) ──

export type {
  KnowledgeSourceStatus,
  KnowledgeSourceView,
  KnowledgeDocumentView,
  KnowledgeChunkView,
  RetrievalRequestView,
  RetrievalResultView,
  RetrievalTraceView,
  RetrievalStageView,
  CitationView,
} from "./knowledge-view.js";

// ── MCP Viewer (AF-STUDIO-01 §34-35) ──

export type {
  McpTransportType,
  McpConnectionStatus,
  McpConnectionView,
  McpServerView,
  McpHealthStatus,
  McpToolView,
  McpToolRegistrationStatus,
  McpResourceView,
  McpPromptView,
  McpPromptArgument,
} from "./mcp-view.js";

// ── Governance Viewer (AF-STUDIO-01 §40-41) ──

export type {
  PolicyStatus,
  PolicyEffect,
  PolicyView,
  PolicyRuleView,
  PolicyDecisionView,
  ViolationSeverity,
  PolicyViolationView,
  ApprovalStatus,
  ApprovalView,
  AuditEntryView,
  RiskSummaryView,
} from "./governance-view.js";

// ── Agent Workspace (AF-STUDIO-01 §7-10) ──

export type {
  AgentVersionStatus,
  AgentOverview,
  AgentDefinitionView,
  AgentIdentityView,
  AgentContractView,
  AgentModelView,
  CapabilityReferenceView,
  MemoryPolicyView,
  KnowledgePolicyView,
  ExecutionPolicyView,
  AgentVersionView,
  AgentSummaryView,
} from "./agent-view.js";

// ── Capability Workspace (AF-STUDIO-01 §11-14) ──

export type {
  CapabilityType,
  SideEffectClassification,
  CapabilityHealthStatus,
  CircuitBreakerState,
  CapabilityDetailView,
  CapabilitySecurityView,
  CapabilityTimeoutView,
  CapabilityRetryView,
  CapabilityPolicyView,
  CapabilityHealthDetailView,
  CapabilityInvocationRequest,
  CapabilityInvocationResult,
  CapabilitySummaryView,
  CapabilityInvocationAuditEntry,
} from "./capability-view.js";

// ── Checkpoint & Recovery (AF-STUDIO-01 §43-44) ──

export type {
  CheckpointStatus,
  CheckpointView,
  ExecutionSnapshotView,
  NodeSnapshotView,
  RecoveryDecision,
  RecoveryView,
  RecoveryRequest,
  ConflictResolutionStrategy,
  StateConflictView,
} from "./checkpoint-view.js";

// ── Diff & Version (AF-STUDIO-01 §45-48) ──

export type {
  DiffEntityType,
  DiffChangeType,
  DiffResult,
  DiffChange,
  DiffSummary,
  GraphDiffResult,
  GraphNodeChange,
  GraphEdgeChange,
  VersionLifecycleStatus,
  VersionEntry,
  ReleaseGateStatus,
  ReleaseGateView,
  ReleaseGateStep,
} from "./diff-view.js";

// ── Search (AF-STUDIO-01 §49) ──

export type {
  SearchResultType,
  SearchRequest,
  SearchResult,
  SearchResponse,
} from "./search-view.js";

// ── Human-in-the-loop (AF-STUDIO-01 §42) ──

export type {
  HumanApprovalStatus,
  RiskLevel,
  HumanApprovalRequest,
  HumanApprovalDecision,
  HumanApprovalSummary,
} from "./hitl-view.js";

// ── Execution Workspace Enhanced (AF-STUDIO-01 §23-30) ──

export type {
  TimelineEvent,
  TimelineEventType,
  AgentLoopView,
  AgentLoopIteration,
  LoopBudgetView,
  DecisionAction,
  StructuredDecisionView,
  LoopActionView,
  ContextView,
  ContextSegmentView,
  ContextSegmentType,
  ExecutionTraceView,
  TraceSpan,
  TraceSpanType,
  ToolCallDetail,
  ModelCallDetail,
  TokenUsageView,
} from "./execution-view.js";

// ── Observability Viewer (AF-STUDIO-01 §61-63) ──

export type {
  ObservabilityMetrics,
  TimeRange,
  MetricSeries,
  MetricDataPoint,
  ModelDashboardView,
  CapabilityHealthView,
  ObservabilityFilter,
  MultiAgentTreeView,
  MultiAgentTreeNode,
} from "./observability-view.js";

// ── Skill Workspace (AF-STUDIO-01 §50-58) ──

export type {
  SkillLifecycleState,
  SkillRiskLevel,
  SkillExecutionStrategy,
  SkillSummaryView,
  SkillOverviewView,
  SkillContractView,
  SkillDependencyView,
  SkillDependencyNode,
  SkillSecurityView,
  SkillEvaluationView,
  SkillEvaluationResultView,
  SkillVersionView,
  SkillReleaseGateView,
  SkillGateCheckView,
  SkillGovernanceView,
  SkillAuditEntryView,
  SkillUsageView,
  SkillExecutionView,
  SkillInvocationRequest,
  SkillInvocationResult,
} from "./skill-view.js";

// ── Service Channel (AF-ENHANCE-11 §3) ──

export type {
  AgentServiceView,
  AgentServiceStatus,
  SessionView,
  SessionStatusView,
  ConversationView,
  ConversationMessageView,
  MessageRoleView,
  ChannelTypeView,
  ServiceHealthView,
} from "./service-view.js";

// ── Control Plane / Deployment (AF-ENHANCE-12 §2) ──

export type {
  DeploymentView,
  DeploymentStatusView,
  DeploymentStrategyView,
  RuntimeInstanceView,
  InstanceStatusView,
  ResourceUsageView,
  EnvironmentView,
  EnvironmentTypeView,
  ReleaseView,
  ReleaseStatusView,
  ResourceQuotaView,
} from "./deployment-view.js";

// ── Evolution (AF-ENHANCE-12 §4) ──

export type {
  EvolutionCandidateView,
  CandidateStatusView,
  EvolutionChangeView,
  ChangeScopeView,
  EvolutionEvidenceView,
  EvidenceTypeView,
  EvolutionProblemView,
  ProblemCategoryView,
  ProblemSeverityView,
  ValidationResultView,
  CandidateLineageView,
  LineageGenerationView,
  LineageNodeView,
  EvolutionPolicyView,
} from "./evolution-view.js";

// ── Debugger (AF-ENHANCE-04) ──

export type {
  DebugSessionView,
  DebugSessionStatusView,
  BreakpointView,
  BreakpointTypeView,
  ExecutionCursorView,
  CursorTypeView,
  StepCommandView,
  DebugEventView,
  DebugEventTypeView,
} from "./debugger-view.js";

// ── Workspace & Sandbox (AF-ENHANCE-07) ──

export type {
  WorkspaceView,
  WorkspaceScopeView,
  WorkspaceStatusView,
  SandboxView,
  SandboxIsolationView,
  SandboxStatusView,
  SandboxResourceLimits,
  ArtifactView,
  ArtifactTypeView,
} from "./workspace-sandbox-view.js";

// ── Multimodal (AF-ENHANCE-11 §3) ──

export type {
  ContentTypeView,
  ContentPartView,
  MultimodalContentView,
  ImageContentView,
  RealtimeSessionView,
  RealtimeStatusView,
  RealtimeTransportView,
  RealtimeEventView,
} from "./multimodal-view.js";

// ── Generated API Types (from OpenAPI spec) ──
// These types are auto-generated from docs/api/openapi.yaml.
// Regenerate with: pnpm --filter @agentforge/model generate:api

export type {
  paths as ApiPaths,
  components as ApiComponents,
  operations as ApiOperations,
  ApiSchemas,
} from "./generated/api-types.js";

import type {
  ProviderDescriptor,
  CapabilityDescriptor,
  AgentDescriptor,
  EvaluationDataset,
  EvaluationTestCase,
  EvaluationRun,
  EvaluationReport,
  RegressionComparison,
  MemoryRecordView,
  MemoryQueryView,
  MemoryCandidateView,
  MemoryAuditEntry,
  MemoryMetricsView,
  KnowledgeSourceView,
  KnowledgeDocumentView,
  RetrievalResultView,
  RetrievalTraceView,
  CitationView,
  McpServerView,
  McpToolView,
  McpResourceView,
  McpPromptView,
  PolicyView,
  PolicyDecisionView,
  PolicyViolationView,
  ApprovalView,
  AuditEntryView,
  RiskSummaryView,
  ObservabilityMetrics,
  MetricSeries,
  ModelDashboardView,
  CapabilityHealthView,
  ObservabilityFilter,
  MultiAgentTreeView,
  // Capability Workspace (AF-STUDIO-01 §11-14)
  CapabilitySummaryView,
  CapabilityDetailView,
  CapabilityInvocationRequest,
  CapabilityInvocationResult,
  CapabilityInvocationAuditEntry,
  // Checkpoint & Recovery (AF-STUDIO-01 §43-44)
  CheckpointView,
  RecoveryView,
  RecoveryRequest,
  StateConflictView,
  // Diff & Version (AF-STUDIO-01 §45-48)
  DiffResult,
  GraphDiffResult,
  VersionEntry,
  ReleaseGateView,
  // Search (AF-STUDIO-01 §49)
  SearchRequest,
  SearchResponse,
  // Human-in-the-loop (AF-STUDIO-01 §42)
  HumanApprovalRequest,
  HumanApprovalDecision,
  HumanApprovalSummary,
  // Execution Enhanced (AF-STUDIO-01 §23-30)
  TimelineEvent,
  AgentLoopView,
  ContextView,
  ExecutionTraceView,
  ToolCallDetail,
  ModelCallDetail,
} from "@agentforge/model";
// Generated API types from OpenAPI spec (docs/api/openapi.yaml).
// These provide compile-time verification that api-client method signatures
// match the backend API contract.
import type { ApiSchemas } from "@agentforge/model";
import type { HttpClient } from "./http-client.js";

/**
 * AgentForgeApiClient - typed client for the AgentForge Server REST API.
 *
 * All methods carry tenant context from HttpClient configuration.
 * Studio never sends raw ReactFlow data to the server.
 *
 * Type safety: Methods for Graphs, Executions, Agents, and Skills
 * reference types from the generated OpenAPI spec (ApiSchemas).
 * When the backend API contract changes, regenerate types with:
 *   pnpm --filter @agentforge/model generate:api
 */
export class AgentForgeApiClient {
  constructor(private readonly http: HttpClient) {}

  /** 尚未生成专用 DTO 的业务端点也必须使用统一认证传输。 */
  request<T>(method: "GET" | "POST" | "PUT" | "DELETE", path: string, body?: unknown): Promise<T> {
    if (!path.startsWith("/api/v1/")) throw new Error("仅允许业务 API 路径");
    return this.http.request<T>(method, path, body);
  }

  // ── Graph CRUD ──
  // Types verified against OpenAPI spec (ApiSchemas.GraphDefinition, ApiSchemas.GraphSummary)

  async listGraphs(): Promise<ApiSchemas["GraphSummary"][]> {
    return this.http.get<ApiSchemas["GraphSummary"][]>("/api/v1/graphs");
  }

  async getGraph(id: string, version?: string): Promise<ApiSchemas["GraphDefinition"]> {
    const query = version ? `?version=${encodeURIComponent(version)}` : "";
    return this.http.get<ApiSchemas["GraphDefinition"]>(`/api/v1/graphs/${id}${query}`);
  }

  async createGraph(graph: ApiSchemas["GraphDefinition"]): Promise<ApiSchemas["GraphDefinition"]> {
    return this.http.post<ApiSchemas["GraphDefinition"]>("/api/v1/graphs", graph);
  }

  async updateGraph(
    id: string,
    graph: ApiSchemas["GraphDefinition"],
    expectedRevision?: number,
  ): Promise<ApiSchemas["GraphDefinition"]> {
    const params = new URLSearchParams();
    if (expectedRevision != null) params.set("expectedRevision", String(expectedRevision));
    if (graph.version) params.set("version", graph.version);
    const query = params.size ? `?${params.toString()}` : "";
    return this.http.put<ApiSchemas["GraphDefinition"]>(
      `/api/v1/graphs/${id}${query}`,
      graph,
    );
  }

  async deleteGraph(id: string, version?: string): Promise<void> {
    const query = version ? `?version=${encodeURIComponent(version)}` : "";
    return this.http.delete(`/api/v1/graphs/${encodeURIComponent(id)}${query}`);
  }

  // ── Validation & Compilation ──

  async validateGraph(graph: ApiSchemas["GraphDefinition"]): Promise<ApiSchemas["ValidationResult"]> {
    return this.http.post<ApiSchemas["ValidationResult"]>(
      "/api/v1/graphs/validate",
      graph,
    );
  }

  async compileGraph(graph: ApiSchemas["GraphDefinition"]): Promise<ApiSchemas["CompilationResult"]> {
    return this.http.post<ApiSchemas["CompilationResult"]>(
      "/api/v1/graphs/compile",
      graph,
    );
  }

  // ── Execution ──
  // Types verified against OpenAPI spec (ApiSchemas.Execution*, ExecutionRequest, ExecutionHandle)

  async createExecution(
    request: ApiSchemas["ExecutionRequest"],
  ): Promise<ApiSchemas["ExecutionHandle"]> {
    return this.http.post<ApiSchemas["ExecutionHandle"]>(
      "/api/v1/executions",
      request,
    );
  }

  async getExecution(id: string): Promise<ApiSchemas["Execution"]> {
    return this.http.get<ApiSchemas["Execution"]>(`/api/v1/executions/${id}`);
  }

  async getExecutionState(id: string): Promise<ApiSchemas["ExecutionState"]> {
    return this.http.get<ApiSchemas["ExecutionState"]>(
      `/api/v1/executions/${id}/state`,
    );
  }

  async listExecutions(
    limit?: number,
    offset?: number,
  ): Promise<ApiSchemas["Execution"][]> {
    const params = new URLSearchParams();
    if (limit != null) params.set("limit", String(limit));
    if (offset != null) params.set("offset", String(offset));
    const query = params.toString();
    return this.http.get<ApiSchemas["Execution"][]>(
      `/api/v1/executions${query ? `?${query}` : ""}`,
    );
  }

  async pauseExecution(id: string): Promise<void> {
    return this.http.post(`/api/v1/executions/${id}/pause`, {});
  }

  async resumeExecution(id: string): Promise<void> {
    return this.http.post(`/api/v1/executions/${id}/resume`, {});
  }

  async cancelExecution(id: string): Promise<void> {
    return this.http.post(`/api/v1/executions/${id}/cancel`, {});
  }

  // ── Events ──

  async getExecutionEvents(
    executionId: string,
    after?: number,
  ): Promise<ApiSchemas["ExecutionEvent"][]> {
    const query = after != null ? `?after=${after}` : "";
    return this.http.get<ApiSchemas["ExecutionEvent"][]>(
      `/api/v1/executions/${executionId}/events${query}`,
    );
  }

  // ── Capabilities & Agents ──

  async listCapabilities(): Promise<CapabilityDescriptor[]> {
    return this.http.get<CapabilityDescriptor[]>("/api/v1/capabilities");
  }

  async listProviders(): Promise<ProviderDescriptor[]> {
    return this.http.get<ProviderDescriptor[]>("/api/v1/providers");
  }

  async listAgents(): Promise<AgentDescriptor[]> {
    return this.http.get<AgentDescriptor[]>("/api/v1/agents");
  }

  // ── Evaluation (AF-STUDIO-01 §37-39) ──

  async listEvaluationDatasets(): Promise<EvaluationDataset[]> {
    return this.http.get<EvaluationDataset[]>("/api/v1/evaluations/datasets");
  }

  async getEvaluationDataset(id: string): Promise<EvaluationDataset> {
    return this.http.get<EvaluationDataset>(`/api/v1/evaluations/datasets/${id}`);
  }

  async listTestCases(datasetId: string): Promise<EvaluationTestCase[]> {
    return this.http.get<EvaluationTestCase[]>(`/api/v1/evaluations/datasets/${datasetId}/cases`);
  }

  async createEvaluationRun(datasetId: string, agentId?: string, graphId?: string): Promise<EvaluationRun> {
    return this.http.post<EvaluationRun>("/api/v1/evaluations/runs", { datasetId, agentId, graphId });
  }

  async listEvaluationRuns(limit?: number): Promise<EvaluationRun[]> {
    const query = limit != null ? `?limit=${limit}` : "";
    return this.http.get<EvaluationRun[]>(`/api/v1/evaluations/runs${query}`);
  }

  async getEvaluationReport(runId: string): Promise<EvaluationReport> {
    return this.http.get<EvaluationReport>(`/api/v1/evaluations/runs/${runId}/report`);
  }

  async getRegressionComparison(currentRunId: string, baselineRunId: string): Promise<RegressionComparison> {
    return this.http.get<RegressionComparison>(`/api/v1/evaluations/regression?current=${encodeURIComponent(currentRunId)}&baseline=${encodeURIComponent(baselineRunId)}`);
  }

  // ── Memory (AF-STUDIO-01 §31) ──

  async queryMemories(query: MemoryQueryView): Promise<MemoryRecordView[]> {
    return this.http.post<MemoryRecordView[]>("/api/v1/memories/query", query);
  }

  async listMemoryCandidates(limit?: number): Promise<MemoryCandidateView[]> {
    const query = limit != null ? `?limit=${limit}` : "";
    return this.http.get<MemoryCandidateView[]>(`/api/v1/memories/candidates${query}`);
  }

  async getMemoryAudit(memoryId: string): Promise<MemoryAuditEntry[]> {
    return this.http.get<MemoryAuditEntry[]>(`/api/v1/memories/${memoryId}/audit`);
  }

  async getMemoryMetrics(): Promise<MemoryMetricsView> {
    return this.http.get<MemoryMetricsView>("/api/v1/memories/metrics");
  }

  // ── Knowledge / RAG (AF-STUDIO-01 §32-33) ──

  async listKnowledgeSources(): Promise<KnowledgeSourceView[]> {
    return this.http.get<KnowledgeSourceView[]>("/api/v1/knowledge/sources");
  }

  async listKnowledgeDocuments(sourceId: string): Promise<KnowledgeDocumentView[]> {
    return this.http.get<KnowledgeDocumentView[]>(`/api/v1/knowledge/sources/${sourceId}/documents`);
  }

  async searchKnowledge(query: string, topK?: number): Promise<RetrievalResultView[]> {
    const params = new URLSearchParams({ query });
    if (topK != null) params.set("topK", String(topK));
    return this.http.get<RetrievalResultView[]>(`/api/v1/knowledge/search?${params}`);
  }

  async getRetrievalTrace(traceId: string): Promise<RetrievalTraceView> {
    return this.http.get<RetrievalTraceView>(`/api/v1/knowledge/traces/${traceId}`);
  }

  async getCitations(executionId: string): Promise<CitationView[]> {
    return this.http.get<CitationView[]>(`/api/v1/knowledge/citations/${executionId}`);
  }

  // ── MCP (AF-STUDIO-01 §34-35) ──

  async listMcpServers(): Promise<McpServerView[]> {
    return this.http.get<McpServerView[]>("/api/v1/mcp/servers");
  }

  async getMcpServer(serverId: string): Promise<McpServerView> {
    return this.http.get<McpServerView>(`/api/v1/mcp/servers/${serverId}`);
  }

  async listMcpTools(serverId: string): Promise<McpToolView[]> {
    return this.http.get<McpToolView[]>(`/api/v1/mcp/servers/${serverId}/tools`);
  }

  async listMcpResources(serverId: string): Promise<McpResourceView[]> {
    return this.http.get<McpResourceView[]>(`/api/v1/mcp/servers/${serverId}/resources`);
  }

  async listMcpPrompts(serverId: string): Promise<McpPromptView[]> {
    return this.http.get<McpPromptView[]>(`/api/v1/mcp/servers/${serverId}/prompts`);
  }

  async enableMcpTool(serverId: string, toolId: string): Promise<void> {
    return this.http.post(`/api/v1/mcp/servers/${serverId}/tools/${toolId}/enable`, {});
  }

  async disableMcpTool(serverId: string, toolId: string): Promise<void> {
    return this.http.post(`/api/v1/mcp/servers/${serverId}/tools/${toolId}/disable`, {});
  }

  // ── Governance (AF-STUDIO-01 §40-41) ──

  async listPolicies(): Promise<PolicyView[]> {
    return this.http.get<PolicyView[]>("/api/v1/governance/policies");
  }

  async listPolicyDecisions(limit?: number): Promise<PolicyDecisionView[]> {
    const query = limit != null ? `?limit=${limit}` : "";
    return this.http.get<PolicyDecisionView[]>(`/api/v1/governance/decisions${query}`);
  }

  async listViolations(resolved?: boolean): Promise<PolicyViolationView[]> {
    const params = new URLSearchParams();
    if (resolved != null) params.set("resolved", String(resolved));
    const query = params.toString();
    return this.http.get<PolicyViolationView[]>(`/api/v1/governance/violations${query ? `?${query}` : ""}`);
  }

  async listApprovals(status?: string): Promise<ApprovalView[]> {
    const query = status ? `?status=${encodeURIComponent(status)}` : "";
    return this.http.get<ApprovalView[]>(`/api/v1/governance/approvals${query}`);
  }

  async decideApproval(approvalId: string, approved: boolean, reason: string): Promise<void> {
    return this.http.post(`/api/v1/governance/approvals/${approvalId}/decide`, { approved, reason });
  }

  async listAuditEntries(limit?: number): Promise<AuditEntryView[]> {
    const query = limit != null ? `?limit=${limit}` : "";
    return this.http.get<AuditEntryView[]>(`/api/v1/governance/audit${query}`);
  }

  async getRiskSummary(): Promise<RiskSummaryView> {
    return this.http.get<RiskSummaryView>("/api/v1/governance/risk/summary");
  }

  // ── Observability (AF-STUDIO-01 §61-63) ──

  async getObservabilityMetrics(filter?: ObservabilityFilter): Promise<ObservabilityMetrics> {
    const query = filter ? `?filter=${encodeURIComponent(JSON.stringify(filter))}` : "";
    return this.http.get<ObservabilityMetrics>(`/api/v1/observability/metrics${query}`);
  }

  async getMetricSeries(metric: string, filter?: ObservabilityFilter): Promise<MetricSeries> {
    const params = new URLSearchParams({ metric });
    if (filter) params.set("filter", JSON.stringify(filter));
    return this.http.get<MetricSeries>(`/api/v1/observability/series?${params}`);
  }

  async listModelDashboards(): Promise<ModelDashboardView[]> {
    return this.http.get<ModelDashboardView[]>("/api/v1/observability/models");
  }

  async listCapabilityHealth(): Promise<CapabilityHealthView[]> {
    return this.http.get<CapabilityHealthView[]>("/api/v1/observability/capabilities/health");
  }

  // ── Multi-Agent (AF-STUDIO-01 §36) ──

  async getMultiAgentTree(executionId: string): Promise<MultiAgentTreeView> {
    return this.http.get<MultiAgentTreeView>(`/api/v1/executions/${executionId}/agent-tree`);
  }

  // ── Agent Workspace (AF-STUDIO-01 §7-10) ──
  // Types verified against OpenAPI spec (ApiSchemas.Agent*)

  async listAgentSummaries(): Promise<ApiSchemas["AgentSummaryView"][]> {
    return this.http.get<ApiSchemas["AgentSummaryView"][]>("/api/v1/agents/summaries");
  }

  async getAgentOverview(agentId: string): Promise<ApiSchemas["AgentOverview"]> {
    return this.http.get<ApiSchemas["AgentOverview"]>(`/api/v1/agents/${agentId}/overview`);
  }

  async getAgentDefinition(agentId: string, version?: string): Promise<ApiSchemas["AgentDefinitionView"]> {
    const query = version ? `?version=${encodeURIComponent(version)}` : "";
    return this.http.get<ApiSchemas["AgentDefinitionView"]>(`/api/v1/agents/${agentId}/definition${query}`);
  }

  async listAgentVersions(agentId: string): Promise<ApiSchemas["AgentVersionView"][]> {
    return this.http.get<ApiSchemas["AgentVersionView"][]>(`/api/v1/agents/${agentId}/versions`);
  }

  // ── Capability Workspace (AF-STUDIO-01 §11-14) ──

  async listCapabilitySummaries(): Promise<CapabilitySummaryView[]> {
    return this.http.get<CapabilitySummaryView[]>("/api/v1/capabilities/summaries");
  }

  async getCapabilityDetail(capabilityId: string, version?: string): Promise<CapabilityDetailView> {
    const query = version ? `?version=${encodeURIComponent(version)}` : "";
    return this.http.get<CapabilityDetailView>(`/api/v1/capabilities/${capabilityId}/detail${query}`);
  }

  async invokeCapability(request: CapabilityInvocationRequest): Promise<CapabilityInvocationResult> {
    return this.http.post<CapabilityInvocationResult>("/api/v1/capabilities/invoke", request);
  }

  async listCapabilityInvocationAudits(capabilityId: string, limit?: number): Promise<CapabilityInvocationAuditEntry[]> {
    const query = limit != null ? `?limit=${limit}` : "";
    return this.http.get<CapabilityInvocationAuditEntry[]>(`/api/v1/capabilities/${capabilityId}/audit${query}`);
  }

  // ── Checkpoint & Recovery (AF-STUDIO-01 §43-44) ──

  async listCheckpoints(executionId: string): Promise<CheckpointView[]> {
    return this.http.get<CheckpointView[]>(`/api/v1/executions/${executionId}/checkpoints`);
  }

  async getCheckpoint(executionId: string, checkpointId: string): Promise<CheckpointView> {
    return this.http.get<CheckpointView>(`/api/v1/executions/${executionId}/checkpoints/${checkpointId}`);
  }

  async getRecoveryInfo(executionId: string): Promise<RecoveryView> {
    return this.http.get<RecoveryView>(`/api/v1/executions/${executionId}/recovery`);
  }

  async executeRecovery(request: RecoveryRequest): Promise<void> {
    return this.http.post("/api/v1/executions/recover", request);
  }

  async getStateConflict(executionId: string): Promise<StateConflictView> {
    return this.http.get<StateConflictView>(`/api/v1/executions/${executionId}/state-conflict`);
  }

  // ── Diff & Version (AF-STUDIO-01 §45-48) ──

  async getDiff(entityType: string, entityId: string, leftVersion: string, rightVersion: string): Promise<DiffResult> {
    const params = new URLSearchParams({ entityType, leftVersion, rightVersion });
    return this.http.get<DiffResult>(`/api/v1/diff/${entityId}?${params}`);
  }

  async getGraphDiff(graphId: string, leftVersion: string, rightVersion: string): Promise<GraphDiffResult> {
    const params = new URLSearchParams({ leftVersion, rightVersion });
    return this.http.get<GraphDiffResult>(`/api/v1/diff/graphs/${graphId}?${params}`);
  }

  async listVersions(entityType: string, entityId: string): Promise<VersionEntry[]> {
    return this.http.get<VersionEntry[]>(`/api/v1/versions/${entityType}/${entityId}`);
  }

  async getReleaseGate(entityType: string, entityId: string, version: string): Promise<ReleaseGateView> {
    const query = `?version=${encodeURIComponent(version)}`;
    return this.http.get<ReleaseGateView>(`/api/v1/versions/${entityType}/${entityId}/release-gate${query}`);
  }

  // ── Search (AF-STUDIO-01 §49) ──

  async search(request: SearchRequest): Promise<SearchResponse> {
    return this.http.post<SearchResponse>("/api/v1/search", request);
  }

  // ── Human-in-the-loop (AF-STUDIO-01 §42) ──

  async listHumanApprovals(status?: string): Promise<HumanApprovalRequest[]> {
    const query = status ? `?status=${encodeURIComponent(status)}` : "";
    return this.http.get<HumanApprovalRequest[]>(`/api/v1/human-approvals${query}`);
  }

  async decideHumanApproval(decision: HumanApprovalDecision): Promise<void> {
    return this.http.post(`/api/v1/human-approvals/${decision.requestId}/decide`, decision);
  }

  async getHumanApprovalSummary(): Promise<HumanApprovalSummary> {
    return this.http.get<HumanApprovalSummary>("/api/v1/human-approvals/summary");
  }

  // ── Execution Enhanced (AF-STUDIO-01 §23-30) ──

  async getExecutionTimeline(executionId: string): Promise<TimelineEvent[]> {
    return this.http.get<TimelineEvent[]>(`/api/v1/executions/${executionId}/timeline`);
  }

  async getAgentLoop(executionId: string): Promise<AgentLoopView> {
    return this.http.get<AgentLoopView>(`/api/v1/executions/${executionId}/agent-loop`);
  }

  async getExecutionContext(executionId: string): Promise<ContextView> {
    return this.http.get<ContextView>(`/api/v1/executions/${executionId}/context`);
  }

  async getExecutionTrace(executionId: string): Promise<ExecutionTraceView> {
    return this.http.get<ExecutionTraceView>(`/api/v1/executions/${executionId}/trace`);
  }

  async listToolCalls(executionId: string): Promise<ToolCallDetail[]> {
    return this.http.get<ToolCallDetail[]>(`/api/v1/executions/${executionId}/tool-calls`);
  }

  async listModelCalls(executionId: string): Promise<ModelCallDetail[]> {
    return this.http.get<ModelCallDetail[]>(`/api/v1/executions/${executionId}/model-calls`);
  }

  // ── Skill Workspace (AF-STUDIO-01 §50-58) ──
  // Types verified against OpenAPI spec (ApiSchemas.Skill*)

  async listSkillSummaries(): Promise<ApiSchemas["SkillSummaryView"][]> {
    return this.http.get<ApiSchemas["SkillSummaryView"][]>("/api/v1/skills");
  }

  async getSkillOverview(skillId: string): Promise<ApiSchemas["SkillOverviewView"]> {
    return this.http.get<ApiSchemas["SkillOverviewView"]>(`/api/v1/skills/${skillId}/overview`);
  }

  async getSkillContract(skillId: string, version?: string): Promise<ApiSchemas["SkillContractView"]> {
    const query = version ? `?version=${encodeURIComponent(version)}` : "";
    return this.http.get<ApiSchemas["SkillContractView"]>(`/api/v1/skills/${skillId}/contract${query}`);
  }

  async getSkillDependencies(skillId: string): Promise<ApiSchemas["SkillDependencyView"]> {
    return this.http.get<ApiSchemas["SkillDependencyView"]>(`/api/v1/skills/${skillId}/dependencies`);
  }

  async getSkillSecurity(skillId: string): Promise<ApiSchemas["SkillSecurityView"]> {
    return this.http.get<ApiSchemas["SkillSecurityView"]>(`/api/v1/skills/${skillId}/security`);
  }

  async getSkillEvaluation(skillId: string): Promise<ApiSchemas["SkillEvaluationView"]> {
    return this.http.get<ApiSchemas["SkillEvaluationView"]>(`/api/v1/skills/${skillId}/evaluation`);
  }

  async listSkillVersions(skillId: string): Promise<ApiSchemas["SkillVersionView"][]> {
    return this.http.get<ApiSchemas["SkillVersionView"][]>(`/api/v1/skills/${skillId}/versions`);
  }

  async getSkillGovernance(skillId: string): Promise<ApiSchemas["SkillGovernanceView"]> {
    return this.http.get<ApiSchemas["SkillGovernanceView"]>(`/api/v1/skills/${skillId}/governance`);
  }

  async getSkillUsage(skillId: string): Promise<ApiSchemas["SkillUsageView"]> {
    return this.http.get<ApiSchemas["SkillUsageView"]>(`/api/v1/skills/${skillId}/usage`);
  }

  async listSkillExecutions(skillId: string, limit?: number): Promise<ApiSchemas["SkillExecutionView"][]> {
    const query = limit != null ? `?limit=${limit}` : "";
    return this.http.get<ApiSchemas["SkillExecutionView"][]>(`/api/v1/skills/${skillId}/executions${query}`);
  }

  async invokeSkill(request: ApiSchemas["SkillInvocationRequest"]): Promise<ApiSchemas["SkillInvocationResult"]> {
    return this.http.post<ApiSchemas["SkillInvocationResult"]>("/api/v1/skills/invoke", request);
  }

  async transitionSkillLifecycle(skillId: string, version: string, targetState: string): Promise<void> {
    return this.http.put(`/api/v1/skills/${skillId}/versions/${version}/lifecycle`, { targetState });
  }
}

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useApiClient } from "./use-api-client.js";

/**
 * Agent workspace query hooks.
 * FE-05: Replaces manual data fetching in AgentWorkspacePage.
 */

export function useAgentSummaries() {
  const api = useApiClient();
  return useQuery({
    queryKey: ["agents", "summaries"],
    queryFn: () => api.listAgentSummaries(),
  });
}

export function useAgentOverview(agentId: string | undefined | null) {
  const api = useApiClient();
  return useQuery({
    queryKey: ["agents", agentId, "overview"],
    queryFn: () => api.getAgentOverview(agentId!),
    enabled: !!agentId,
  });
}

export function useAgentDefinition(agentId: string | undefined | null, version?: string) {
  const api = useApiClient();
  return useQuery({
    queryKey: ["agents", agentId, "definition", version],
    queryFn: () => api.getAgentDefinition(agentId!, version),
    enabled: !!agentId,
  });
}

export function useAgentVersions(agentId: string | undefined | null) {
  const api = useApiClient();
  return useQuery({
    queryKey: ["agents", agentId, "versions"],
    queryFn: () => api.listAgentVersions(agentId!),
    enabled: !!agentId,
  });
}

// ── Observability ──

export function useObservabilityMetrics() {
  const api = useApiClient();
  return useQuery({
    queryKey: ["observability", "metrics"],
    queryFn: () => api.getObservabilityMetrics(),
  });
}

export function useModelDashboards() {
  const api = useApiClient();
  return useQuery({
    queryKey: ["observability", "models"],
    queryFn: () => api.listModelDashboards(),
  });
}

export function useCapabilityHealth() {
  const api = useApiClient();
  return useQuery({
    queryKey: ["observability", "capabilities", "health"],
    queryFn: () => api.listCapabilityHealth(),
  });
}

// ── Human-in-the-Loop ──

export function useHumanApprovals(status?: string) {
  const api = useApiClient();
  return useQuery({
    queryKey: ["hitl", "approvals", status],
    queryFn: () => api.listHumanApprovals(status),
    refetchInterval: 10_000,
  });
}

export function useHumanApprovalSummary() {
  const api = useApiClient();
  return useQuery({
    queryKey: ["hitl", "summary"],
    queryFn: () => api.getHumanApprovalSummary(),
  });
}

export function useDecideHumanApproval() {
  const api = useApiClient();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (decision: Parameters<typeof api.decideHumanApproval>[0]) => api.decideHumanApproval(decision),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["hitl"] });
    },
  });
}

// ── Evaluation ──

export function useEvaluationDatasets() {
  const api = useApiClient();
  return useQuery({
    queryKey: ["evaluation", "datasets"],
    queryFn: () => api.listEvaluationDatasets(),
  });
}

export function useEvaluationRuns(limit?: number) {
  const api = useApiClient();
  return useQuery({
    queryKey: ["evaluation", "runs", limit],
    queryFn: () => api.listEvaluationRuns(limit),
  });
}

export function useEvaluationReport(runId: string | undefined) {
  const api = useApiClient();
  return useQuery({
    queryKey: ["evaluation", "runs", runId, "report"],
    queryFn: () => api.getEvaluationReport(runId!),
    enabled: !!runId,
  });
}

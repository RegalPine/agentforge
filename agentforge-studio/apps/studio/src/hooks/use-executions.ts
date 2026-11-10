import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useApiClient } from "./use-api-client.js";

/**
 * Execution query & mutation hooks.
 *
 * FE-05: Replaces manual useState + useEffect in ExecutionListPage/DetailPage.
 */

export function useExecutionList(limit?: number, offset?: number) {
  const api = useApiClient();
  return useQuery({
    queryKey: ["executions", limit, offset],
    queryFn: () => api.listExecutions(limit, offset),
  });
}

export function useExecution(id: string | undefined) {
  const api = useApiClient();
  return useQuery({
    queryKey: ["executions", id],
    queryFn: () => api.getExecution(id!),
    enabled: !!id,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status === "RUNNING" || status === "PENDING" ? 3_000 : false;
    },
  });
}

export function useExecutionState(id: string | undefined) {
  const api = useApiClient();
  return useQuery({
    queryKey: ["executions", id, "state"],
    queryFn: () => api.getExecutionState(id!),
    enabled: !!id,
  });
}

export function useExecutionEvents(id: string | undefined, after?: number) {
  const api = useApiClient();
  return useQuery({
    queryKey: ["executions", id, "events", after],
    queryFn: () => api.getExecutionEvents(id!, after),
    enabled: !!id,
    refetchInterval: 2_000,
  });
}

export function useExecutionTimeline(id: string | undefined) {
  const api = useApiClient();
  return useQuery({
    queryKey: ["executions", id, "timeline"],
    queryFn: () => api.getExecutionTimeline(id!),
    enabled: !!id,
  });
}

export function useExecutionTrace(id: string | undefined) {
  const api = useApiClient();
  return useQuery({
    queryKey: ["executions", id, "trace"],
    queryFn: () => api.getExecutionTrace(id!),
    enabled: !!id,
  });
}

export function useAgentLoop(id: string | undefined) {
  const api = useApiClient();
  return useQuery({
    queryKey: ["executions", id, "agent-loop"],
    queryFn: () => api.getAgentLoop(id!),
    enabled: !!id,
  });
}

export function useToolCalls(id: string | undefined) {
  const api = useApiClient();
  return useQuery({
    queryKey: ["executions", id, "tool-calls"],
    queryFn: () => api.listToolCalls(id!),
    enabled: !!id,
  });
}

export function useModelCalls(id: string | undefined) {
  const api = useApiClient();
  return useQuery({
    queryKey: ["executions", id, "model-calls"],
    queryFn: () => api.listModelCalls(id!),
    enabled: !!id,
  });
}

export function useExecutionContext(id: string | undefined) {
  const api = useApiClient();
  return useQuery({
    queryKey: ["executions", id, "context"],
    queryFn: () => api.getExecutionContext(id!),
    enabled: !!id,
  });
}

// ── Execution Actions ──

export function useCreateExecution() {
  const api = useApiClient();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (req: Parameters<typeof api.createExecution>[0]) => api.createExecution(req),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["executions"] }); },
  });
}

export function usePauseExecution() {
  const api = useApiClient();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.pauseExecution(id),
    onSuccess: (_, id) => { qc.invalidateQueries({ queryKey: ["executions", id] }); },
  });
}

export function useResumeExecution() {
  const api = useApiClient();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.resumeExecution(id),
    onSuccess: (_, id) => { qc.invalidateQueries({ queryKey: ["executions", id] }); },
  });
}

export function useCancelExecution() {
  const api = useApiClient();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.cancelExecution(id),
    onSuccess: (_, id) => { qc.invalidateQueries({ queryKey: ["executions", id] }); },
  });
}

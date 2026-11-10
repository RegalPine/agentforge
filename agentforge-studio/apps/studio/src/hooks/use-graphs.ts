import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useApiClient } from "./use-api-client.js";

/**
 * Graph query & mutation hooks.
 *
 * Uses TanStack Query for automatic caching, deduplication,
 * background refetching, and optimistic updates.
 *
 * FE-05: Replaces manual useState + useEffect in GraphListPage.
 */

export function useGraphList() {
  const api = useApiClient();
  return useQuery({
    queryKey: ["graphs"],
    queryFn: () => api.listGraphs(),
  });
}

export function useGraph(id: string | undefined, version?: string) {
  const api = useApiClient();
  return useQuery({
    queryKey: ["graphs", id, version],
    queryFn: () => api.getGraph(id!, version),
    enabled: !!id,
  });
}

export function useCreateGraph() {
  const api = useApiClient();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (graph: Parameters<typeof api.createGraph>[0]) => api.createGraph(graph),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["graphs"] }); },
  });
}

export function useUpdateGraph(id: string) {
  const api = useApiClient();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ graph, expectedRevision }: { graph: Parameters<typeof api.updateGraph>[1]; expectedRevision?: number }) =>
      api.updateGraph(id, graph, expectedRevision),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["graphs"] }); },
  });
}

export function useDeleteGraph() {
  const api = useApiClient();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.deleteGraph(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["graphs"] }); },
  });
}

export function useValidateGraph() {
  const api = useApiClient();
  return useMutation({
    mutationFn: (graph: Parameters<typeof api.validateGraph>[0]) => api.validateGraph(graph),
  });
}

export function useCompileGraph() {
  const api = useApiClient();
  return useMutation({
    mutationFn: (graph: Parameters<typeof api.compileGraph>[0]) => api.compileGraph(graph),
  });
}

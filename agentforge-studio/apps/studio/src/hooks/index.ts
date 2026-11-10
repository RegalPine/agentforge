/**
 * TanStack Query hooks for AgentForge Studio.
 *
 * FE-05: Centralized data-fetching layer.
 * All pages should use these hooks instead of manual useState + useEffect.
 *
 * Benefits:
 * - Automatic caching and deduplication
 * - Background refetching for real-time data
 * - Optimistic updates for mutations
 * - Query invalidation on data changes
 */

export { useApiClient, getApiClient } from "./use-api-client.js";
export {
  useGraphList,
  useGraph,
  useCreateGraph,
  useUpdateGraph,
  useDeleteGraph,
  useValidateGraph,
  useCompileGraph,
} from "./use-graphs.js";
export {
  useExecutionList,
  useExecution,
  useExecutionState,
  useExecutionEvents,
  useExecutionTimeline,
  useExecutionTrace,
  useAgentLoop,
  useToolCalls,
  useModelCalls,
  useExecutionContext,
  useCreateExecution,
  usePauseExecution,
  useResumeExecution,
  useCancelExecution,
} from "./use-executions.js";
export {
  useAgentSummaries,
  useAgentOverview,
  useAgentDefinition,
  useAgentVersions,
  useObservabilityMetrics,
  useModelDashboards,
  useCapabilityHealth,
  useHumanApprovals,
  useHumanApprovalSummary,
  useDecideHumanApproval,
  useEvaluationDatasets,
  useEvaluationRuns,
  useEvaluationReport,
} from "./use-workspace.js";

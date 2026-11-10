/**
 * ExecutionStatus - maps to backend ExecutionStatus enum.
 */
export type ExecutionStatus =
  | "CREATED"
  | "RUNNING"
  | "PAUSED"
  | "COMPLETED"
  | "FAILED"
  | "CANCELLED"
  | "UNKNOWN";

/**
 * Execution - maps to backend execution model.
 */
export interface Execution {
  id: string;
  graphId: string;
  graphVersion: string;
  planFingerprint?: string;
  status: ExecutionStatus;
  tenantId: string;
  created: string;
  started?: string;
  completed?: string;
  currentNodeId?: string;
  workerId?: string;
  attempt: number;
  inputs: Record<string, unknown>;
  outputs: Record<string, unknown>;
  metadata: Record<string, unknown>;
}

/**
 * ExecutionRequest - submit a new execution.
 */
export interface ExecutionRequest {
  graphId: string;
  graphVersion: string;
  tenantId: string;
  inputs: Record<string, unknown>;
  metadata?: Record<string, unknown>;
}

/**
 * ExecutionHandle - returned after execution submission.
 */
export interface ExecutionHandle {
  executionId: string;
  status: ExecutionStatus;
}

/**
 * ExecutionState - maps to backend durable state.
 */
export interface ExecutionState {
  executionId: string;
  stateVersion: number;
  data: Record<string, unknown>;
  nodeStates: Record<string, NodeState>;
}

/**
 * NodeState - per-node execution state.
 */
export interface NodeState {
  nodeId: string;
  status: string;
  attempt: number;
  lastResult?: Record<string, unknown>;
}

/**
 * ExecutionEvent - maps to backend event model.
 */
export interface ExecutionEvent {
  executionId: string;
  sequence: number;
  type: string;
  timestamp: string;
  nodeId?: string;
  data: Record<string, unknown>;
}

/**
 * CompilationResult - returned by compiler endpoint.
 */
export interface CompilationResult {
  status: "SUCCESS" | "FAILURE";
  planFingerprint?: string;
  nodeCount: number;
  edgeCount: number;
  diagnostics: CompilationDiagnostic[];
}

/**
 * CompilationDiagnostic - compiler feedback.
 */
export interface CompilationDiagnostic {
  severity: "ERROR" | "WARNING" | "INFO";
  code: string;
  message: string;
  nodeId?: string;
}

/**
 * Validation result.
 */
export interface ValidationResult {
  valid: boolean;
  diagnostics: CompilationDiagnostic[];
}

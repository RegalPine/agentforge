/**
 * Debugger viewer models.
 * AF-ENHANCE-04: DebugSession / Breakpoint / ExecutionCursor.
 *
 * Maps to backend agentforge-debugger types.
 */

// ── Debug Session ──

/**
 * DebugSessionView - active debug session.
 */
export interface DebugSessionView {
  readonly sessionId: string;
  readonly executionId: string;
  readonly status: DebugSessionStatusView;
  readonly breakpointCount: number;
  readonly createdAt: string;
  readonly attachedAt?: string;
  readonly detachedAt?: string;
}

export type DebugSessionStatusView =
  | "CREATED"
  | "ATTACHED"
  | "PAUSED"
  | "STEPPING"
  | "DETACHED"
  | "COMPLETED"
  | "FAILED";

// ── Breakpoint ──

/**
 * BreakpointView - breakpoint within a debug session.
 */
export interface BreakpointView {
  readonly breakpointId: string;
  readonly sessionId: string;
  readonly type: BreakpointTypeView;
  readonly targetNodeId?: string;
  readonly targetEdgeId?: string;
  readonly condition?: string;
  readonly enabled: boolean;
  readonly hitCount: number;
  readonly createdAt: string;
}

export type BreakpointTypeView =
  | "NODE_ENTRY"
  | "NODE_EXIT"
  | "EDGE_TRAVERSAL"
  | "TOOL_CALL"
  | "MODEL_CALL"
  | "POLICY_EVALUATION"
  | "HUMAN_APPROVAL"
  | "ERROR";

// ── Execution Cursor ──

/**
 * ExecutionCursorView - current execution position.
 */
export interface ExecutionCursorView {
  readonly eventId: string;
  readonly sequence: number;
  readonly type: CursorTypeView;
  readonly nodeId?: string;
  readonly state?: Record<string, unknown>;
}

export type CursorTypeView =
  | "NODE"
  | "EDGE"
  | "TOOL"
  | "MODEL"
  | "POLICY"
  | "EXECUTION"
  | "BREAKPOINT";

// ── Step Command ──

/**
 * StepCommandView - debugger step command.
 */
export type StepCommandView =
  | "STEP_OVER"
  | "STEP_INTO"
  | "STEP_OUT"
  | "CONTINUE"
  | "PAUSE"
  | "STOP";

// ── Debug Event ──

/**
 * DebugEventView - event emitted during debugging.
 */
export interface DebugEventView {
  readonly eventId: string;
  readonly sessionId: string;
  readonly type: DebugEventTypeView;
  readonly cursor: ExecutionCursorView;
  readonly payload: Record<string, unknown>;
  readonly timestamp: string;
}

export type DebugEventTypeView =
  | "BREAKPOINT_HIT"
  | "STEP_COMPLETED"
  | "SESSION_ATTACHED"
  | "SESSION_DETACHED"
  | "EXECUTION_PAUSED"
  | "EXECUTION_RESUMED"
  | "STATE_INSPECTED"
  | "ERROR_ENCOUNTERED";

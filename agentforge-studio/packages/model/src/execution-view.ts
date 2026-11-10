/**
 * Execution Workspace Enhanced Models (AF-STUDIO-01 §23-30)
 *
 * Execution Workspace:
 *   Execution
 *   ├── Overview
 *   ├── Timeline
 *   ├── Trace
 *   ├── State
 *   ├── Context
 *   ├── Events
 *   ├── Tool Calls
 *   ├── Model Calls
 *   ├── Memory
 *   ├── Knowledge
 *   ├── Policy
 *   ├── Checkpoints
 *   └── Errors
 *
 * Agent Loop Viewer (§26):
 *   Iteration → Observation → Decision → Action → Observation
 *
 * Decision Viewer (§27):
 *   Structured Decision with action/target/arguments/reasonCode
 */

// ── Timeline Event (AF-STUDIO-01 §24) ──

export interface TimelineEvent {
  readonly timestamp: string;
  readonly type: TimelineEventType;
  readonly description: string;
  readonly nodeId?: string;
  readonly detail?: Record<string, unknown>;
}

export type TimelineEventType =
  | "EXECUTION_STARTED"
  | "EXECUTION_COMPLETED"
  | "EXECUTION_FAILED"
  | "AGENT_STARTED"
  | "AGENT_COMPLETED"
  | "NODE_STARTED"
  | "NODE_COMPLETED"
  | "MODEL_CALL"
  | "TOOL_CALL"
  | "TOOL_COMPLETED"
  | "OBSERVATION_CREATED"
  | "STATE_UPDATED"
  | "POLICY_DECISION"
  | "CHECKPOINT_CREATED"
  | "HUMAN_APPROVAL_REQUESTED"
  | "HUMAN_APPROVAL_DECIDED";

// ── Agent Loop Viewer (AF-STUDIO-01 §26) ──

export interface AgentLoopView {
  readonly executionId: string;
  readonly iterations: AgentLoopIteration[];
  readonly totalIterations: number;
  readonly modelCalls: number;
  readonly toolCalls: number;
  readonly budget: LoopBudgetView;
  readonly stopCondition?: string;
}

export interface AgentLoopIteration {
  readonly iterationNumber: number;
  readonly observation?: string;
  readonly decision?: StructuredDecisionView;
  readonly action?: LoopActionView;
  readonly resultObservation?: string;
  readonly timestamp: string;
}

export interface LoopBudgetView {
  readonly maxIterations: number;
  readonly maxDurationMs: number;
  readonly maxTokens: number;
  readonly maxToolCalls: number;
  readonly consumedIterations: number;
  readonly consumedDurationMs: number;
  readonly consumedTokens: number;
  readonly consumedToolCalls: number;
}

// ── Decision Viewer (AF-STUDIO-01 §27) ──

export type DecisionAction =
  | "CALL_CAPABILITY"
  | "CALL_TOOL"
  | "DELEGATE_AGENT"
  | "FINISH"
  | "WAIT_HUMAN";

export interface StructuredDecisionView {
  readonly action: DecisionAction;
  readonly target?: string;
  readonly arguments?: Record<string, unknown>;
  readonly reasonCode?: string;
  readonly confidence?: number;
}

// ── Loop Action ──

export interface LoopActionView {
  readonly type: DecisionAction;
  readonly target: string;
  readonly arguments: Record<string, unknown>;
  readonly result?: Record<string, unknown>;
}

// ── Context Viewer (AF-STUDIO-01 §30) ──

export interface ContextView {
  readonly segments: ContextSegmentView[];
  readonly totalTokens: number;
  readonly tokenBudget: number;
  readonly truncatedSegments: string[];
}

export interface ContextSegmentView {
  readonly type: ContextSegmentType;
  readonly label: string;
  readonly tokenCount: number;
  readonly content: string;
  readonly truncated: boolean;
}

export type ContextSegmentType =
  | "SYSTEM_POLICY"
  | "CURRENT_INPUT"
  | "STATE"
  | "CAPABILITIES"
  | "OBSERVATIONS"
  | "MEMORY"
  | "KNOWLEDGE"
  | "HISTORY";

// ── Execution Trace (AF-STUDIO-01 §25) ──

export interface ExecutionTraceView {
  readonly executionId: string;
  readonly rootSpan: TraceSpan;
}

export interface TraceSpan {
  readonly spanId: string;
  readonly type: TraceSpanType;
  readonly name: string;
  readonly startTime: string;
  readonly endTime?: string;
  readonly status: string;
  readonly children: TraceSpan[];
  readonly metadata: Record<string, unknown>;
}

export type TraceSpanType =
  | "EXECUTION"
  | "AGENT"
  | "GRAPH"
  | "NODE"
  | "MODEL_CALL"
  | "TOOL_CALL"
  | "CAPABILITY"
  | "CHILD_EXECUTION";

// ── Tool Call / Model Call detail ──

export interface ToolCallDetail {
  readonly callId: string;
  readonly capabilityId: string;
  readonly input: Record<string, unknown>;
  readonly output?: Record<string, unknown>;
  readonly error?: string;
  readonly latencyMs: number;
  readonly policyDecision: string;
}

export interface ModelCallDetail {
  readonly callId: string;
  readonly modelId: string;
  readonly provider: string;
  readonly prompt?: string;
  readonly response?: string;
  readonly tokenUsage: TokenUsageView;
  readonly latencyMs: number;
  readonly structuredOutputFailure?: boolean;
}

export interface TokenUsageView {
  readonly promptTokens: number;
  readonly completionTokens: number;
  readonly totalTokens: number;
}

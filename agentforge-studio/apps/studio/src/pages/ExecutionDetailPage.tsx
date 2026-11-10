import { useState, useEffect, useRef, useCallback } from "react";
import {
  Button,
  Badge,
  InlineCode,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tabs,
  TabsList,
  TabsTrigger,
  TabsContent,
  StatusLight,
  ProgressBar,
  Separator,
} from "@agentforge/ui";
import { useParams } from "react-router-dom";
import type {
  ExecutionEvent,
  TimelineEvent,
  ToolCallDetail,
  ModelCallDetail,
  AgentLoopView,
  ContextView,
} from "@agentforge/model";
import type { ApiSchemas } from "@agentforge/model";
import { ExecutionEventSource } from "@agentforge/api-client";
import { useApiClient, getApiClientConfig } from "../hooks/use-api-client.js";
import {
  Pause,
  SkipForward,
  Save,
  XCircle,
  Clock,
  Zap,
  Bot,
  Wrench,
  Brain,
  Copy,
  ChevronDown,
  ChevronRight,
  Filter,
  Network,
} from "lucide-react";

type ExecTab = "overview" | "timeline" | "trace" | "state" | "context" | "agentloop" | "tools" | "models" | "events";

// ─── Mock DAG span data ──────────────────────────────────────────────────────

interface DagSpan {
  id: string;
  label: string;
  type: "ingress" | "agent" | "tool_call" | "policy" | "subgraph";
  status: "success" | "running" | "queued" | "error";
  duration: string;
  durationMs?: number;
  detail?: string;
  children?: DagSpan[];
  progress?: number;
}

const MOCK_DAG_SPANS: DagSpan[] = [
  {
    id: "ingress-1",
    label: "Ingress::WebhookIngress",
    type: "ingress",
    status: "success",
    duration: "18ms",
    durationMs: 18,
    detail: "Payload: event.support.triage  Offset: 0.00s",
  },
  {
    id: "agent-1",
    label: "secops-triage-agent",
    type: "agent",
    status: "running",
    duration: "1,420ms",
    durationMs: 1420,
    detail: "Claude 3.5 Sonnet  ·  Generating ReAct Thought #4  Prompt: 2,810 tok",
    progress: 65,
    children: [
      { id: "tool-1", label: "get_auth_token_digest()", type: "tool_call", status: "success", duration: "42ms", durationMs: 42, detail: "[Resolved]" },
      { id: "tool-2", label: 'query_vpc_flow_logs(ip="10.24.1.88")', type: "tool_call", status: "success", duration: "210ms", durationMs: 210, detail: "[Success]" },
      { id: "tool-3", label: "cap-ebpf-kernel-telemetry(pid=4192)", type: "tool_call", status: "running", duration: "Running (84ms)", durationMs: 84, progress: 40 },
    ],
  },
  {
    id: "policy-1",
    label: "mitigation-dispatch-policy",
    type: "policy",
    status: "queued",
    duration: "--",
    detail: "Awaiting kernel telemetry socket resolution",
  },
];

// ─── Sub-components ──────────────────────────────────────────────────────────

function DagSpanItem({ span, depth = 0 }: { span: DagSpan; depth?: number }) {
  const [expanded, setExpanded] = useState(true);
  const hasChildren = span.children && span.children.length > 0;

  const statusColor: Record<string, string> = {
    success: "text-success",
    running: "text-primary",
    queued: "text-outline",
    error: "text-error",
  };

  const statusBadge: Record<string, { label: string; className: string }> = {
    success: { label: span.type === "tool_call" ? "[Resolved]" : "200 OK", className: "bg-success/20 text-success border-success/30" },
    running: { label: "Running", className: "bg-primary/20 text-primary border-primary/30" },
    queued: { label: "QUEUED", className: "bg-surface-container-high text-outline border-outline-variant" },
    error: { label: "Error", className: "bg-error/20 text-error border-error/30" },
  };

  const badge = statusBadge[span.status];
  const typeIcons: Record<string, React.ReactNode> = {
    ingress: <Network className="h-3 w-3" />,
    agent: <Bot className="h-3 w-3" />,
    tool_call: <Wrench className="h-3 w-3" />,
    policy: <Brain className="h-3 w-3" />,
    subgraph: <Network className="h-3 w-3" />,
  };

  return (
    <div>
      <div
        className={`flex items-center gap-2 py-2 px-2 rounded hover:bg-surface-container-high/50 transition-colors cursor-pointer ${depth > 0 ? "ml-4" : ""}`}
        style={{ paddingLeft: `${depth * 16 + 8}px` }}
        onClick={() => hasChildren && setExpanded(!expanded)}
      >
        {hasChildren ? (
          expanded ? <ChevronDown className="h-3 w-3 text-outline shrink-0" /> : <ChevronRight className="h-3 w-3 text-outline shrink-0" />
        ) : (
          <span className="w-3 shrink-0" />
        )}
        <span className={statusColor[span.status]}>{typeIcons[span.type]}</span>
        <span className="text-[12px] font-code-sm text-on-surface flex-1 truncate">{span.label}</span>
        <Badge variant="outline" className={`text-[10px] shrink-0 ${badge.className}`}>{badge.label}</Badge>
        <span className="text-[11px] font-code-sm text-outline tabular-nums shrink-0 w-20 text-right">{span.duration}</span>
      </div>

      {span.progress !== undefined && span.status === "running" && (
        <div className="mx-2 mb-1" style={{ marginLeft: `${depth * 16 + 32}px` }}>
          <ProgressBar value={span.progress} color="primary" size="xs" />
        </div>
      )}

      {span.detail && (
        <p className="text-[10px] font-code-sm text-outline ml-8 mb-1">{span.detail}</p>
      )}

      {hasChildren && expanded && span.children!.map((child) => (
        <DagSpanItem key={child.id} span={child} depth={depth + 1} />
      ))}
    </div>
  );
}

function DagExecutionSpans() {
  return (
    <div className="rounded-lg border border-outline-variant bg-surface-container p-4 flex flex-col gap-2 flex-1 min-h-0">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Network className="h-4 w-4 text-primary" />
          <span className="text-[12px] font-label-md font-semibold text-on-surface uppercase tracking-wider">
            DAG Execution Spans
          </span>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-[11px] font-code-sm text-primary">TTFT: 112ms</span>
          <button className="p-1 rounded hover:bg-surface-container-high text-outline">
            <Filter className="h-3 w-3" />
          </button>
        </div>
      </div>
      <Separator />
      <div className="flex-1 overflow-auto space-y-0.5">
        {MOCK_DAG_SPANS.map((span) => (
          <DagSpanItem key={span.id} span={span} />
        ))}
      </div>
    </div>
  );
}

function NodeContextReAct() {
  const [viewMode, setViewMode] = useState<"json" | "ast">("json");

  const reactSteps = [
    {
      type: "THOUGHT" as const,
      content: "VPC flow logs show repeated SYN packets on target port 443 with anomalous non-standard payload flags from client internal node 10.24.1.88. Need to correlate active running kernel process with PID mapping.",
    },
    {
      type: "ACTION" as const,
      content: 'cap-ebpf-kernel-telemetry(pid=4192, sample_duration_ms=500, capture_syscalls=["sys_enter_connect", "sys_enter_execve"])',
    },
    {
      type: "OBSERVATION" as const,
      content: "Socket listening on live eBPF tap ringbuffer... (14 packets streamed)",
    },
  ];

  const mockJson = `{
  "execution_id": "exec-9f81b10a-ecb1-4190",
  "active_node": "secops-triage-agent",
  "state_snapshot": {
    "incident_priority": "P1-CRITICAL",
    "security_context": {
      "source_ip": "10.24.1.88",
      "target_port": 443,
      "anomaly_score": 0.942,
      "compromised_pid": 4192,
      "threat_vector": "lateral_movement.remote_socket_bind"
    },
    "mitigation_plan": [
      "quarantine_pod_network_policy",
      "revoke_service_account_token",
      "issue_pagerduty_incident"
    ],
    "is_interactive_checkpoint_reached": true
  }
}`;

  const typeColors: Record<string, string> = {
    THOUGHT: "text-primary",
    ACTION: "text-tertiary",
    OBSERVATION: "text-success",
  };

  return (
    <div className="rounded-lg border border-outline-variant bg-surface-container p-4 flex flex-col gap-3 flex-1 min-h-0">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Brain className="h-4 w-4 text-secondary" />
          <span className="text-[12px] font-label-md font-semibold text-on-surface uppercase tracking-wider">
            Node Context & ReAct Reasoner
          </span>
        </div>
        <div className="flex items-center gap-2">
          <Badge variant="outline" className="text-[10px]">secops-triage-agent</Badge>
          <div className="flex rounded border border-outline-variant overflow-hidden">
            <button
              className={`px-2 py-0.5 text-[10px] font-code-sm ${viewMode === "json" ? "bg-surface-container-high text-on-surface" : "bg-surface text-outline"}`}
              onClick={() => setViewMode("json")}
            >
              JSON
            </button>
            <button
              className={`px-2 py-0.5 text-[10px] font-code-sm ${viewMode === "ast" ? "bg-surface-container-high text-on-surface" : "bg-surface text-outline"}`}
              onClick={() => setViewMode("ast")}
            >
              AST VIEW
            </button>
          </div>
          <button className="p-1 rounded hover:bg-surface-container-high text-outline">
            <Copy className="h-3 w-3" />
          </button>
        </div>
      </div>

      {/* ReAct Chain */}
      <div className="space-y-2">
        <div className="flex items-center gap-1.5">
          <Brain className="h-3 w-3 text-secondary" />
          <span className="text-[11px] font-code-sm text-on-surface font-medium">Step 4: ReAct Agent Reasoning Chain</span>
          <span className="text-[10px] font-code-sm text-success ml-auto">Confidence: 98.4%</span>
        </div>

        {reactSteps.map((step, i) => (
          <div key={i} className="rounded border border-outline-variant bg-surface p-2.5">
            <span className={`text-[10px] font-code-sm font-semibold ${typeColors[step.type]}`}>
              [{step.type}]
            </span>
            <p className="text-[11px] font-body-md text-on-surface mt-1 leading-relaxed">{step.content}</p>
            {step.type === "OBSERVATION" && (
              <StatusLight status="running" size="sm" pulse className="ml-auto" />
            )}
          </div>
        ))}
      </div>

      {/* JSON View */}
      {viewMode === "json" && (
        <pre className="rounded border border-outline-variant bg-surface p-3 text-[11px] font-code-sm text-on-surface overflow-auto max-h-48 leading-relaxed">
          {mockJson.split("\n").map((line, i) => (
            <div key={i} className="flex">
              <span className="text-outline/50 w-6 shrink-0 text-right mr-3 select-none">{String(i + 1).padStart(2, "0")}</span>
              <span
                className={
                  line.includes('"execution_id"') || line.includes('"active_node"')
                    ? "text-primary"
                    : line.includes('"P1-CRITICAL"') || line.includes("0.942") || line.includes("4192")
                    ? "text-error"
                    : line.includes('"mitigation_plan"') || line.includes("quarantine") || line.includes("revoke") || line.includes("issue")
                    ? "text-tertiary"
                    : "text-on-surface"
                }
              >
                {line}
              </span>
            </div>
          ))}
        </pre>
      )}
    </div>
  );
}

// ─── Main Page ───────────────────────────────────────────────────────────────

export function ExecutionDetailPage() {
  const apiClient = useApiClient();
  const { executionId } = useParams<{ executionId: string }>();
  const [activeTab, setActiveTab] = useState<ExecTab>("trace");
  const [execution, setExecution] = useState<ApiSchemas["Execution"] | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  useEffect(() => {
    if (!executionId) return;
    setLoading(true);
    apiClient.getExecution(executionId)
      .then(setExecution)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [executionId, apiClient]);

  const handlePause = useCallback(async () => {
    if (!executionId) return;
    setActionLoading("pause");
    try { await apiClient.pauseExecution(executionId); } catch {}
    setActionLoading(null);
  }, [executionId, apiClient]);

  const handleCancel = useCallback(async () => {
    if (!executionId) return;
    setActionLoading("cancel");
    try { await apiClient.cancelExecution(executionId); } catch {}
    setActionLoading(null);
  }, [executionId, apiClient]);

  const isRunning = execution?.status === "RUNNING" || execution?.status === "PENDING";

  const tabs: { key: ExecTab; label: string; icon?: React.ReactNode }[] = [
    { key: "overview", label: "Overview", icon: <Network className="h-3 w-3" /> },
    { key: "timeline", label: "Timeline", icon: <Clock className="h-3 w-3" /> },
    { key: "trace", label: "Trace", icon: <Zap className="h-3 w-3" /> },
    { key: "state", label: "State" },
    { key: "context", label: "Context" },
    { key: "agentloop", label: "AgentLoop", icon: <Bot className="h-3 w-3" /> },
    { key: "tools", label: "Tools", icon: <Wrench className="h-3 w-3" /> },
    { key: "models", label: "Models", icon: <Brain className="h-3 w-3" /> },
    { key: "events", label: "Events (SSE Stream)" },
  ];

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center bg-surface">
        <span className="text-[13px] font-body-md text-outline">Loading execution...</span>
      </div>
    );
  }

  return (
    <div className="flex-1 flex flex-col overflow-hidden bg-surface">
      {/* Header metadata */}
      <div className="px-4 py-2 border-b border-outline-variant bg-surface-container-lowest shrink-0">
        <div className="flex items-center gap-3 flex-wrap">
          <div className="flex items-center gap-1.5">
            <span className="text-[11px] font-code-sm text-outline">ID</span>
            <code className="text-[12px] font-code-sm text-primary bg-surface px-1.5 py-0.5 rounded">
              #{executionId}
            </code>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="text-[11px] font-code-sm text-outline">GRAPH</span>
            <span className="text-[12px] font-body-md text-on-surface">
              {execution?.graphId ?? "Customer-Support-MultiAgent"}
            </span>
            <Badge variant="outline" className="text-[10px]">v2.4</Badge>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="text-[11px] font-code-sm text-outline">TENANT</span>
            <span className="text-[12px] font-body-md text-on-surface">{execution?.tenantId ?? "default-enterprise"}</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="text-[11px] font-code-sm text-outline">DURATION</span>
            <span className="text-[12px] font-code-sm text-on-surface flex items-center gap-1">
              <Clock className="h-3 w-3" />04m 12s
            </span>
          </div>
        </div>
        <div className="flex items-center gap-3 mt-1.5">
          <div className="flex items-center gap-1.5">
            <span className="text-[11px] font-code-sm text-outline">TOKENS</span>
            <span className="text-[12px] font-code-sm text-on-surface">4,210</span>
            <span className="text-[10px] font-code-sm text-outline">($0.042)</span>
          </div>
          <StatusLight status={isRunning ? "running" : "neutral"} pulse />
          <Badge className={isRunning ? "bg-primary/20 text-primary text-[10px]" : "bg-success/20 text-success text-[10px]"}>
            {execution?.status ?? "RUNNING"}
          </Badge>
        </div>
      </div>

      {/* Action buttons */}
      <div className="flex items-center gap-1.5 px-4 py-1.5 border-b border-outline-variant bg-surface-container-low/50 shrink-0">
        <Button variant="secondary" size="sm" className="h-6 text-[11px]" onClick={handlePause} disabled={!isRunning || actionLoading !== null}>
          <Pause className="h-3 w-3 mr-1" />Pause F6
        </Button>
        <Button variant="secondary" size="sm" className="h-6 text-[11px]" disabled>
          <SkipForward className="h-3 w-3 mr-1" />Step F10
        </Button>
        <Button variant="secondary" size="sm" className="h-6 text-[11px]">
          <Save className="h-3 w-3 mr-1" />Create Checkpoint
        </Button>
        <Button variant="destructive" size="sm" className="h-6 text-[11px] ml-auto" onClick={handleCancel} disabled={!isRunning || actionLoading !== null}>
          <XCircle className="h-3 w-3 mr-1" />Cancel Execution
        </Button>
      </div>

      {/* Tabs */}
      <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as ExecTab)} className="flex-1 flex flex-col overflow-hidden">
        <div className="px-2 pt-1 shrink-0">
          <TabsList className="h-7">
            {tabs.map((tab) => (
              <TabsTrigger key={tab.key} value={tab.key} className="text-[11px] gap-1">
                {tab.icon}{tab.label}
              </TabsTrigger>
            ))}
          </TabsList>
        </div>

        <div className="flex-1 overflow-hidden p-3">
          {/* Trace tab — two-column DAG + ReAct */}
          <TabsContent value="trace" className="mt-0 h-full">
            <div className="grid grid-cols-[1fr_1fr] gap-4 h-full">
              <DagExecutionSpans />
              <NodeContextReAct />
            </div>
          </TabsContent>

          {/* Overview tab */}
          <TabsContent value="overview" className="mt-0">
            {execution ? (
              <div className="rounded-lg border border-outline-variant bg-surface-container p-4">
                <Table>
                  <TableBody>
                    <TableRow><TableCell className="font-medium text-[12px]">Execution ID</TableCell><TableCell className="text-[12px] font-code-sm">{execution.id}</TableCell></TableRow>
                    <TableRow><TableCell className="font-medium text-[12px]">Status</TableCell><TableCell><Badge className="text-[10px]">{execution.status}</Badge></TableCell></TableRow>
                    <TableRow><TableCell className="font-medium text-[12px]">Graph ID</TableCell><TableCell><InlineCode>{execution.graphId}</InlineCode></TableCell></TableRow>
                    <TableRow><TableCell className="font-medium text-[12px]">Graph Version</TableCell><TableCell className="text-[12px]">{execution.graphVersion}</TableCell></TableRow>
                    <TableRow><TableCell className="font-medium text-[12px]">Tenant</TableCell><TableCell className="text-[12px]">{execution.tenantId}</TableCell></TableRow>
                    <TableRow><TableCell className="font-medium text-[12px]">Created</TableCell><TableCell className="text-[12px]">{execution.createdAt}</TableCell></TableRow>
                    <TableRow><TableCell className="font-medium text-[12px]">Started</TableCell><TableCell className="text-[12px]">{execution.startedAt || "-"}</TableCell></TableRow>
                    <TableRow><TableCell className="font-medium text-[12px]">Completed</TableCell><TableCell className="text-[12px]">{execution.completedAt || "-"}</TableCell></TableRow>
                    {execution.error && <TableRow><TableCell className="font-medium text-[12px] text-error">Error</TableCell><TableCell className="text-[12px] text-error">{execution.error}</TableCell></TableRow>}
                  </TableBody>
                </Table>
              </div>
            ) : (
              <span className="text-[12px] text-outline">Execution not found.</span>
            )}
          </TabsContent>

          {/* Timeline tab */}
          <TabsContent value="timeline" className="mt-0">
            <TimelinePanel executionId={executionId!} />
          </TabsContent>

          {/* State tab */}
          <TabsContent value="state" className="mt-0">
            <StatePanel executionId={executionId!} />
          </TabsContent>

          {/* Context tab */}
          <TabsContent value="context" className="mt-0">
            <ContextPanel executionId={executionId!} />
          </TabsContent>

          {/* AgentLoop tab */}
          <TabsContent value="agentloop" className="mt-0">
            <AgentLoopPanel executionId={executionId!} />
          </TabsContent>

          {/* Tools tab */}
          <TabsContent value="tools" className="mt-0">
            <ToolCallsPanel executionId={executionId!} />
          </TabsContent>

          {/* Models tab */}
          <TabsContent value="models" className="mt-0">
            <ModelCallsPanel executionId={executionId!} />
          </TabsContent>

          {/* Events tab */}
          <TabsContent value="events" className="mt-0">
            <EventsPanel executionId={executionId!} />
          </TabsContent>
        </div>
      </Tabs>
    </div>
  );
}

// ─── Reused sub-panels (kept from original, styled for IDE) ──────────────────

function TimelinePanel({ executionId }: { executionId: string }) {
  const apiClient = useApiClient();
  const [events, setEvents] = useState<TimelineEvent[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiClient.getExecutionTimeline(executionId)
      .then(setEvents)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [executionId, apiClient]);

  if (loading) return <span className="text-[12px] text-outline">Loading timeline...</span>;
  if (events.length === 0) return <span className="text-[12px] text-outline">No timeline events available yet.</span>;

  return (
    <div className="rounded-lg border border-outline-variant bg-surface-container p-4 space-y-2">
      <span className="text-[12px] font-label-md font-semibold text-on-surface uppercase tracking-wider">Execution Timeline</span>
      {events.map((evt, i) => (
        <div key={i} className="flex items-center gap-2 py-1 border-b border-outline-variant/50 last:border-0">
          <span className="text-[10px] font-code-sm text-outline w-20 shrink-0">{evt.timestamp ?? ""}</span>
          <Badge variant="outline" className="text-[10px]">{evt.type}</Badge>
          <span className="text-[11px] font-code-sm text-on-surface truncate">{JSON.stringify(evt)}</span>
        </div>
      ))}
    </div>
  );
}

function StatePanel({ executionId }: { executionId: string }) {
  const apiClient = useApiClient();
  const [state, setState] = useState<ApiSchemas["ExecutionState"] | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiClient.getExecutionState(executionId)
      .then(setState)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [executionId, apiClient]);

  if (loading) return <span className="text-[12px] text-outline">Loading state...</span>;
  if (!state) return <span className="text-[12px] text-outline">No state data available.</span>;

  return (
    <div className="rounded-lg border border-outline-variant bg-surface-container p-4">
      <span className="text-[12px] font-label-md font-semibold text-on-surface uppercase tracking-wider mb-3 block">Node States</span>
      <Table>
        <TableHeader>
          <TableRow><TableHead className="text-[10px]">Node ID</TableHead><TableHead className="text-[10px]">Status</TableHead><TableHead className="text-[10px]">Input</TableHead><TableHead className="text-[10px]">Output</TableHead></TableRow>
        </TableHeader>
        <TableBody>
          {state.nodeStates?.map((ns, i) => (
            <TableRow key={i}>
              <TableCell className="font-code-sm text-[11px]">{ns.nodeId ?? "-"}</TableCell>
              <TableCell><Badge variant="outline" className="text-[10px]">{ns.status ?? "-"}</Badge></TableCell>
              <TableCell><InlineCode className="text-[10px]">{ns.input ? JSON.stringify(ns.input) : "-"}</InlineCode></TableCell>
              <TableCell><InlineCode className="text-[10px]">{ns.output ? JSON.stringify(ns.output) : "-"}</InlineCode></TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}

function ContextPanel({ executionId }: { executionId: string }) {
  const apiClient = useApiClient();
  const [context, setContext] = useState<ContextView | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiClient.getExecutionContext(executionId)
      .then(setContext)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [executionId, apiClient]);

  if (loading) return <span className="text-[12px] text-outline">Loading context...</span>;
  if (!context) return <span className="text-[12px] text-outline">No context data available.</span>;

  return (
    <div className="rounded-lg border border-outline-variant bg-surface-container p-4">
      <pre className="text-[11px] font-code-sm text-on-surface overflow-auto">{JSON.stringify(context, null, 2)}</pre>
    </div>
  );
}

function AgentLoopPanel({ executionId }: { executionId: string }) {
  const apiClient = useApiClient();
  const [loop, setLoop] = useState<AgentLoopView | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiClient.getAgentLoop(executionId)
      .then(setLoop)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [executionId, apiClient]);

  if (loading) return <span className="text-[12px] text-outline">Loading agent loop...</span>;
  if (!loop) return <span className="text-[12px] text-outline">No agent loop data available.</span>;

  return (
    <div className="rounded-lg border border-outline-variant bg-surface-container p-4">
      <pre className="text-[11px] font-code-sm text-on-surface overflow-auto">{JSON.stringify(loop, null, 2)}</pre>
    </div>
  );
}

function ToolCallsPanel({ executionId }: { executionId: string }) {
  const apiClient = useApiClient();
  const [calls, setCalls] = useState<ToolCallDetail[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiClient.listToolCalls(executionId)
      .then(setCalls)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [executionId, apiClient]);

  if (loading) return <span className="text-[12px] text-outline">Loading tool calls...</span>;
  if (calls.length === 0) return <span className="text-[12px] text-outline">No tool calls recorded.</span>;

  return (
    <div className="rounded-lg border border-outline-variant bg-surface-container p-4">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="text-[10px]">Call ID</TableHead>
            <TableHead className="text-[10px]">Capability</TableHead>
            <TableHead className="text-[10px]">Input</TableHead>
            <TableHead className="text-[10px]">Output</TableHead>
            <TableHead className="text-[10px]">Latency</TableHead>
            <TableHead className="text-[10px]">Status</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {calls.map((call, i) => (
            <TableRow key={i}>
              <TableCell className="font-code-sm text-[11px]">{call.callId}</TableCell>
              <TableCell className="text-[11px]">{call.capabilityId}</TableCell>
              <TableCell><InlineCode className="text-[10px] max-w-48 truncate">{JSON.stringify(call.input)}</InlineCode></TableCell>
              <TableCell><InlineCode className="text-[10px] max-w-48 truncate">{call.output ? JSON.stringify(call.output) : "-"}</InlineCode></TableCell>
              <TableCell className="text-[11px]">{call.latencyMs}ms</TableCell>
              <TableCell><Badge variant="outline" className="text-[10px]">{call.policyDecision}</Badge></TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}

function ModelCallsPanel({ executionId }: { executionId: string }) {
  const apiClient = useApiClient();
  const [calls, setCalls] = useState<ModelCallDetail[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiClient.listModelCalls(executionId)
      .then(setCalls)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [executionId, apiClient]);

  if (loading) return <span className="text-[12px] text-outline">Loading model calls...</span>;
  if (calls.length === 0) return <span className="text-[12px] text-outline">No model calls recorded.</span>;

  return (
    <div className="rounded-lg border border-outline-variant bg-surface-container p-4">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="text-[10px]">Call ID</TableHead>
            <TableHead className="text-[10px]">Model</TableHead>
            <TableHead className="text-[10px]">Provider</TableHead>
            <TableHead className="text-[10px]">Tokens</TableHead>
            <TableHead className="text-[10px]">Latency</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {calls.map((call, i) => (
            <TableRow key={i}>
              <TableCell className="font-code-sm text-[11px]">{call.callId}</TableCell>
              <TableCell className="text-[11px]">{call.modelId}</TableCell>
              <TableCell className="text-[11px]">{call.provider}</TableCell>
              <TableCell className="text-[11px]">{call.tokenUsage?.totalTokens ?? "-"}</TableCell>
              <TableCell className="text-[11px]">{call.latencyMs}ms</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}

function EventsPanel({ executionId }: { executionId: string }) {
  const apiClient = useApiClient();
  const [events, setEvents] = useState<ExecutionEvent[]>([]);
  const [connected, setConnected] = useState(false);
  const eventSourceRef = useRef<ExecutionEventSource | null>(null);
  const listRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setEvents([]);
    const es = new ExecutionEventSource(getApiClientConfig(), executionId);
    eventSourceRef.current = es;

    es.on("*", (event) => {
      setEvents((prev) => [...prev, event]);
    });

    es.onError(() => setConnected(false));
    setConnected(false);
    es.onOpen(() => setConnected(true));
    es.connect();

    return () => { es.disconnect(); eventSourceRef.current = null; };
  }, [executionId, apiClient]);

  useEffect(() => {
    if (listRef.current) listRef.current.scrollTop = listRef.current.scrollHeight;
  }, [events]);

  return (
    <div className="rounded-lg border border-outline-variant bg-surface-container p-4 flex flex-col gap-2">
      <div className="flex items-center justify-between">
        <span className="text-[12px] font-label-md font-semibold text-on-surface uppercase tracking-wider">Events (SSE Stream)</span>
        <div className="flex items-center gap-2">
          <Badge className={connected ? "bg-success/20 text-success text-[10px]" : "bg-error/20 text-error text-[10px]"}>
            {connected ? "Connected" : "Disconnected"}
          </Badge>
          <span className="text-[10px] font-code-sm text-outline">
            Seq: {eventSourceRef.current?.getLastSequence() ?? 0}
          </span>
        </div>
      </div>
      <div ref={listRef} className="max-h-96 overflow-auto border border-outline-variant rounded bg-surface p-2 space-y-1">
        {events.length === 0 ? (
          <span className="text-[11px] text-outline">Waiting for events...</span>
        ) : (
          events.map((evt, i) => (
            <div key={i} className="flex gap-2 items-center text-[11px] font-code-sm py-0.5">
              <span className="text-outline w-20 shrink-0">
                {evt.timestamp ? new Date(evt.timestamp).toLocaleTimeString() : "--:--:--"}
              </span>
              <span className="text-outline w-10 shrink-0">#{evt.sequence}</span>
              <Badge variant="outline" className="text-[10px]">{evt.type}</Badge>
              <span className="text-outline">{evt.nodeId ?? "-"}</span>
            </div>
          ))
        )}
      </div>
    </div>
  );
}

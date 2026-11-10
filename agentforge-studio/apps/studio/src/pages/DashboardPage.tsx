import { useState, useEffect, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { useApiClient } from "../hooks/use-api-client.js";
import {
  MetricCard,
  StatusLight,
  SparkLine,
  ProgressBar,
  Button,
  Badge,
} from "@agentforge/ui";
import type { MetricCardProps } from "@agentforge/ui";
import {
  Play,
  Network,
  Server,
  Clock,
  Cpu,
  ArrowUpRight,
  AlertTriangle,
  CheckCircle2,
  RotateCcw,
  FileText,
} from "lucide-react";

// ─── Data types ────────────────────────────────────────────────────────────────

interface Counts {
  graphs: number;
  executions: number;
  agents: number;
  capabilities: number;
  skills: number;
  mcpServers: number;
  policies: number;
  evalDatasets: number;
}

interface WorkflowItem {
  name: string;
  status: "running" | "idle" | "completed" | "error";
  description: string;
  nodes: number;
  timeAgo: string;
}

interface AgentRow {
  id: string;
  role: string;
  status: "running" | "idle" | "paused" | "error";
  throughput: string;
}

interface HITLItem {
  title: string;
  risk: "high" | "medium" | "low";
  description: string;
  expires: string;
}

// ─── Mock data ─────────────────────────────────────────────────────────────────

const MOCK_WORKFLOWS: WorkflowItem[] = [
  {
    name: "threat_triage.graph",
    status: "running",
    description: "Autonomous ingestion of SIEM alerts, enrichment via VirusTotal MCP, and candidate...",
    nodes: 14,
    timeAgo: "4m ago",
  },
  {
    name: "customer_support.graph",
    status: "idle",
    description: "Multi-turn agent router with human-in-the-loop fallback for refund transactions exceeding $250.",
    nodes: 8,
    timeAgo: "18m ago",
  },
  {
    name: "incident_remediation.graph",
    status: "completed",
    description: "Automated database connection pool rebalancing and canary service failover with...",
    nodes: 22,
    timeAgo: "1h 12m ago",
  },
];

const MOCK_AGENTS: AgentRow[] = [
  { id: "supervisor_master_...", role: "supervisor_master", status: "running", throughput: "24.2 tok/s" },
  { id: "rag_context_retriev...", role: "rag_context_retriever", status: "idle", throughput: "0.0 tok/s" },
  { id: "code_gen_worker_v2", role: "code_gen_worker_v2", status: "running", throughput: "19.5 tok/s" },
  { id: "eval_judge_synth", role: "eval_judge_synth", status: "paused", throughput: "--" },
];

const MOCK_HITL: HITLItem[] = [
  {
    title: "IAM Role Escalation",
    risk: "high",
    description: "SecOps-Agent requesting temporary arn:aws:iam::root write session.",
    expires: "04m 12s",
  },
  {
    title: "Stripe Refund Dispa...",
    risk: "medium",
    description: "Customer-Agent triggered disbursement for customer invoice #INV-9921 ($340.00).",
    expires: "18m 40s",
  },
];

const SPARKLINE_TOKEN = [120, 135, 128, 142, 155, 148, 160, 152, 168, 175, 162, 170];
const SPARKLINE_SESSION = [30, 35, 42, 38, 45, 52, 48, 55, 60, 58, 64, 62];
const SPARKLINE_LATENCY = [5.2, 4.8, 5.1, 4.5, 4.9, 4.2, 4.6, 4.1, 4.3, 4.0, 4.2, 4.1];

// ─── Sub-components ────────────────────────────────────────────────────────────

function DashboardQuickStart({ navigate }: { navigate: (to: string) => void }) {
  const statusBadge: Record<string, { label: string; className: string }> = {
    running: { label: "Running", className: "bg-primary/20 text-primary border-primary/30" },
    idle: { label: "Idle / Ready", className: "bg-surface-container-high text-outline border-outline-variant" },
    completed: { label: "Completed", className: "bg-success/20 text-success border-success/30" },
    error: { label: "Error", className: "bg-error/20 text-error border-error/30" },
  };

  return (
    <div className="p-space-md bg-surface-container-low rounded-lg shadow-sm flex flex-col gap-space-md">
      {/* Section Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-space-xs">
          <Play className="h-4 w-4 text-primary" />
          <h2 className="font-label-sm text-label-sm uppercase tracking-wider font-semibold text-on-surface">
            Quick Start & Recent Workflows
          </h2>
        </div>
        <span className="px-space-xs py-0.5 rounded bg-surface-container font-code-sm text-code-sm text-outline">
          DAG: 3 Active
        </span>
      </div>

      {/* Workflow Cards Container */}
      <div className="flex flex-col gap-space-sm">
        {MOCK_WORKFLOWS.map((wf) => {
          const badge = statusBadge[wf.status];
          return (
            <div
              key={wf.name}
              className="group p-space-sm bg-surface-container rounded hover:bg-surface-container-high transition-all shadow-sm flex flex-col gap-space-xs cursor-pointer"
              onClick={() => navigate("/graphs")}
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-space-xs">
                  <span className="w-2 h-2 rounded-full bg-primary animate-pulse"></span>
                  <span className="font-body-md text-body-md font-semibold text-on-surface">{wf.name}</span>
                </div>
                <span className={`px-space-xs py-0.5 rounded font-label-sm text-label-sm font-semibold ${badge.className}`}>
                  {badge.label}
                </span>
              </div>
              <p className="font-body-sm text-body-sm text-on-surface-variant line-clamp-2">
                {wf.description}
              </p>
              <div className="flex items-center justify-between pt-1">
                <div className="flex items-center gap-space-sm font-code-sm text-code-sm text-outline">
                  <span className="flex items-center gap-1"><Network className="h-3 w-3" />{wf.nodes} Nodes</span>
                  <span>•</span>
                  <span className="flex items-center gap-1"><Clock className="h-3 w-3" />{wf.timeAgo}</span>
                </div>
                <button className="px-space-xs py-1 rounded bg-surface-container-highest hover:bg-primary hover:text-on-primary text-on-surface font-label-sm text-label-sm flex items-center gap-1 transition-colors">
                  <ArrowUpRight className="h-3 w-3" />
                  <span>Open in Editor</span>
                </button>
              </div>
            </div>
          );
        })}
      </div>

      <div
        className="rounded border border-dashed border-outline-variant bg-surface-container/50 p-space-sm flex items-center gap-3 cursor-pointer hover:border-primary/50 transition-colors"
        onClick={() => navigate("/graphs")}
      >
        <div className="h-8 w-8 rounded-full bg-accent-primary/20 flex items-center justify-center">
          <span className="text-accent-primary text-lg font-bold">+</span>
        </div>
        <div className="flex-1">
          <p className="font-body-md text-body-md font-medium text-on-surface">Create Blank Orchestrator</p>
          <p className="font-code-sm text-code-sm text-outline">Scaffold from MCP Agent Template</p>
        </div>
        <Button size="sm" className="h-6 text-[11px]">New DAG</Button>
      </div>
    </div>
  );
}

function DashboardFleetHealth() {
  return (
    <div className="p-space-md bg-surface-container-low rounded-lg shadow-sm flex flex-col gap-space-md">
      {/* Section Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-space-xs">
          <Cpu className="h-4 w-4 text-tertiary" />
          <h2 className="font-label-sm text-label-sm uppercase tracking-wider font-semibold text-on-surface">
            Runtime Fleet Health & Agent Nodes
          </h2>
        </div>
        <div className="flex items-center gap-3">
          <span className="text-[13px] font-code-sm text-primary font-medium">48.2 tok/s</span>
          <span className="text-[11px] font-code-sm text-outline">P99: 14ms</span>
        </div>
      </div>

      {/* Utilization bars */}
      <div className="grid grid-cols-2 gap-3">
        <div className="rounded border border-outline-variant bg-surface-container p-2.5">
          <div className="flex items-center justify-between mb-1.5">
            <span className="font-label-sm text-label-sm uppercase text-outline tracking-wider">Host Cluster Utilization</span>
            <span className="text-[11px] font-code-sm text-on-surface font-medium">42%</span>
          </div>
          <ProgressBar value={42} color="primary" size="sm" />
          <div className="flex items-center justify-between mt-1 text-[10px] font-code-sm text-outline">
            <span>CPU 8 / 16 vCPU</span>
            <span>Load: 0.84</span>
          </div>
        </div>
        <div className="rounded border border-outline-variant bg-surface-container p-2.5">
          <div className="flex items-center justify-between mb-1.5">
            <span className="font-label-sm text-label-sm uppercase text-outline tracking-wider">VRAM / Heap Memory</span>
            <span className="text-[11px] font-code-sm text-tertiary font-medium">58%</span>
          </div>
          <ProgressBar value={58} color="tertiary" size="sm" />
          <div className="flex items-center justify-between mt-1 text-[10px] font-code-sm text-outline">
            <span>18.4 GB / 32.0 GB</span>
            <span>Swap: 0.1%</span>
          </div>
        </div>
      </div>

      {/* Agent table */}
      <div className="rounded border border-outline-variant bg-surface-container overflow-hidden">
        <div className="grid grid-cols-[1fr_120px_100px_60px] px-3 py-1.5 bg-surface-container-low border-b border-outline-variant">
          <span className="font-label-sm text-label-sm uppercase text-outline tracking-wider">Agent ID / Role</span>
          <span className="font-label-sm text-label-sm uppercase text-outline tracking-wider">Status</span>
          <span className="font-label-sm text-label-sm uppercase text-outline tracking-wider">Throughput</span>
          <span className="font-label-sm text-label-sm uppercase text-outline tracking-wider">Actions</span>
        </div>
        {MOCK_AGENTS.map((agent) => (
          <div key={agent.id} className="grid grid-cols-[1fr_120px_100px_60px] px-3 py-2 border-b border-outline-variant/50 last:border-0 hover:bg-surface-container-high/50 transition-colors">
            <div className="flex items-center gap-1.5">
              <StatusLight status={agent.status === "running" ? "running" : agent.status === "error" ? "error" : "neutral"} size="sm" />
              <span className="text-[12px] font-code-sm text-on-surface truncate">{agent.role}</span>
            </div>
            <Badge
              variant="outline"
              className={
                agent.status === "running"
                  ? "bg-primary/20 text-primary border-primary/30 text-[10px]"
                  : agent.status === "paused"
                  ? "bg-amber-400/20 text-amber-400 border-amber-400/30 text-[10px]"
                  : "bg-surface-container-high text-outline border-outline-variant text-[10px]"
              }
            >
              {agent.status === "running" ? "Running" : agent.status === "paused" ? "Paused" : "Idle"}
            </Badge>
            <span className="text-[12px] font-code-sm text-on-surface">{agent.throughput}</span>
            <div className="flex items-center gap-1">
              <RotateCcw className="h-3 w-3 text-outline hover:text-on-surface cursor-pointer" />
              <FileText className="h-3 w-3 text-outline hover:text-on-surface cursor-pointer" />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function DashboardHITLQueue() {
  const riskBadge: Record<string, { label: string; className: string }> = {
    high: { label: "HIGH RISK", className: "bg-error text-white border-error" },
    medium: { label: "MED RISK", className: "bg-purple-500 text-white border-purple-500" },
    low: { label: "LOW RISK", className: "bg-success/20 text-success border-success/30" },
  };

  return (
    <div className="p-space-md bg-surface-container-low rounded-lg shadow-sm flex flex-col gap-space-md">
      {/* Section Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-space-xs">
          <AlertTriangle className="h-4 w-4 text-error" />
          <h2 className="font-label-sm text-label-sm uppercase tracking-wider font-semibold text-on-surface">
            HITL Gate Queue & Alerts
          </h2>
        </div>
        <Badge className="bg-error text-white text-[11px]">2 Approvals</Badge>
      </div>

      {MOCK_HITL.map((item) => {
        const badge = riskBadge[item.risk];
        return (
          <div key={item.title} className="rounded border border-outline-variant bg-surface-container p-space-sm flex flex-col gap-space-xs">
            <div className="flex items-center justify-between">
              <span className="font-body-md text-body-md font-medium text-on-surface truncate">{item.title}</span>
              <Badge variant="outline" className={badge.className}>{badge.label}</Badge>
            </div>
            <p className="font-body-sm text-body-sm text-on-surface-variant leading-tight">{item.description}</p>
            <div className="flex items-center justify-between">
              <span className="font-code-sm text-code-sm text-outline flex items-center gap-1">
                <Clock className="h-3 w-3" />Exp: {item.expires}
              </span>
              <div className="flex items-center gap-1.5">
                <Button variant="secondary" size="sm" className="h-5 text-[10px] px-2">Reject</Button>
                <Button size="sm" className="h-5 text-[10px] px-2">Authorize</Button>
              </div>
            </div>
          </div>
        );
      })}

      {/* Recent Audit Log */}
      <div className="mt-1">
        <span className="text-[10px] font-code-sm text-outline uppercase tracking-wider">Recent Audit Log</span>
        <div className="mt-1.5 flex flex-col gap-1">
          <p className="text-[11px] font-code-sm text-outline">
            <CheckCircle2 className="h-3 w-3 inline mr-1 text-success" />
            Policy #26 passed (OIDC...<span className="text-outline/60">12:04</span>
          </p>
          <p className="text-[11px] font-code-sm text-outline">
            <FileText className="h-3 w-3 inline mr-1 text-primary" />
            Dataset `bench_v4` re-i...<span className="text-outline/60">11:58</span>
          </p>
          <p className="text-[11px] font-code-sm text-outline">
            <Server className="h-3 w-3 inline mr-1 text-tertiary" />
            MCP Host: connection es...<span className="text-outline/60">11:42</span>
          </p>
        </div>
      </div>
    </div>
  );
}

function DashboardMetricsPanel() {
  return (
    <div className="p-space-md bg-surface-container-low rounded-lg shadow-sm">
      <div className="flex items-center justify-between mb-3">
        <span className="font-label-sm text-label-sm uppercase tracking-wider font-semibold text-on-surface">
          Metrics
        </span>
        <span className="font-code-sm text-code-sm text-outline">Sample Rate: 1000ms</span>
      </div>
      <div className="grid grid-cols-3 gap-3">
        {/* Token Throughput */}
        <div className="rounded border border-outline-variant bg-surface-container p-3 flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <span className="font-label-sm text-label-sm uppercase text-outline tracking-wider">Hourly Token Throughput</span>
            <span className="text-[13px] font-code-sm text-primary font-medium">142.8k tok/h</span>
          </div>
          <SparkLine data={SPARKLINE_TOKEN} width={200} height={40} color="var(--color-primary)" fillColor="var(--color-primary)" />
          <div className="flex items-center justify-between text-[10px] font-code-sm text-outline">
            <span>08:00</span>
            <span>10:00</span>
            <span>12:00</span>
            <span>14:00 (Current)</span>
          </div>
        </div>

        {/* Active Sessions */}
        <div className="rounded border border-outline-variant bg-surface-container p-3 flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <span className="font-label-sm text-label-sm uppercase text-outline tracking-wider">Active Sessions / Concurrency</span>
            <span className="text-[13px] font-code-sm text-on-surface font-medium">64 Active</span>
          </div>
          <SparkLine data={SPARKLINE_SESSION} width={200} height={40} color="var(--color-on-surface)" />
          <div className="flex items-center justify-between text-[10px] font-code-sm text-outline">
            <span>Min: 18</span>
            <span>Avg: 44</span>
            <span>Peak: 89</span>
          </div>
        </div>

        {/* Vector Latency */}
        <div className="rounded border border-outline-variant bg-surface-container p-3 flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <span className="font-label-sm text-label-sm uppercase text-outline tracking-wider">Vector Index Latency</span>
            <span className="text-[13px] font-code-sm text-tertiary font-medium">4.2ms avg</span>
          </div>
          <SparkLine data={SPARKLINE_LATENCY} width={200} height={40} color="var(--color-tertiary)" fillColor="var(--color-tertiary)" />
          <div className="flex items-center justify-between text-[10px] font-code-sm text-outline">
            <span>QPS: 840</span>
            <span>Cache Hit: 94.2%</span>
            <span>Err: 0.00%</span>
          </div>
        </div>
      </div>
    </div>
  );
}

// ─── Main Page ─────────────────────────────────────────────────────────────────

export function DashboardPage() {
  const api = useApiClient();
  const navigate = useNavigate();
  const [counts, setCounts] = useState<Counts | null>(null);

  useEffect(() => {
    Promise.all([
      api.listGraphs().catch(() => []),
      api.listExecutions().catch(() => []),
      api.request<unknown[]>("GET", "/api/v1/agents").catch(() => []),
      api.request<unknown[]>("GET", "/api/v1/capabilities").catch(() => []),
      api.request<unknown[]>("GET", "/api/v1/skills").catch(() => []),
      api.request<unknown[]>("GET", "/api/v1/mcp/servers").catch(() => []),
      api.request<unknown[]>("GET", "/api/v1/governance/policies").catch(() => []),
      api.request<unknown[]>("GET", "/api/v1/evaluations/datasets").catch(() => []),
    ]).then(([graphs, executions, agents, capabilities, skills, mcpServers, policies, evalDatasets]) => {
      setCounts({
        graphs: graphs.length,
        executions: executions.length,
        agents: agents.length,
        capabilities: capabilities.length,
        skills: skills.length,
        mcpServers: mcpServers.length,
        policies: policies.length,
        evalDatasets: evalDatasets.length,
      });
    });
  }, [api]);

  const metrics: MetricCardProps[] = useMemo(() => {
    const c = counts;
    return [
      { title: "GRAPHS", value: c?.graphs ?? "--", status: "neutral", sparklineData: [8, 10, 9, 11, 12], sparklineColor: "var(--color-primary)" },
      { title: "EXECUTIONS", value: c?.executions?.toLocaleString() ?? "--", status: "success", sparklineData: [800, 950, 1100, 1200, 1420], sparklineColor: "var(--color-success)" },
      { title: "AGENTS", value: c?.agents ?? "--", status: "running", sparklineData: [12, 14, 15, 16, 18], sparklineColor: "var(--color-secondary)" },
      { title: "CAPABILITIES", value: c?.capabilities ?? "--", status: "neutral", sparklineData: [30, 35, 40, 44, 48], sparklineColor: "var(--color-accent-tool)" },
      { title: "SKILLS", value: c?.skills ?? "--", status: "neutral", sparklineData: [20, 24, 26, 29, 32], sparklineColor: "var(--color-primary)" },
      { title: "MCP SERVERS", value: c?.mcpServers ?? "--", status: "running", sparklineData: [8, 10, 11, 13, 14], sparklineColor: "var(--color-primary)" },
      { title: "POLICIES", value: c?.policies ?? "--", status: "neutral", sparklineData: [18, 20, 22, 24, 26], sparklineColor: "var(--color-accent-tool)" },
      { title: "EVAL DATASETS", value: c?.evalDatasets ?? "--", status: "neutral", sparklineData: [30, 34, 36, 39, 42], sparklineColor: "var(--color-primary)" },
      { title: "CLUSTER SYNC", value: "99.98%", unit: "uptime", status: "success", sparklineData: [99.9, 99.95, 99.97, 99.98, 99.98], sparklineColor: "var(--color-success)" },
      { title: "SSE FEDERATION", value: "Node OK", status: "running", sparklineData: [1, 1, 1, 1, 1], sparklineColor: "var(--color-primary)" },
    ];
  }, [counts]);

  return (
    <div className="flex-1 overflow-auto bg-surface p-space-md flex flex-col gap-space-md">
      {/* Status bar info */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <StatusLight status="success" size="sm" pulse />
          <span className="font-code-sm text-code-sm text-primary font-medium">CLUSTER SYNC: ACTIVE</span>
        </div>
        <span className="font-code-sm text-code-sm text-outline">HOST: IP-10-244-12-88</span>
      </div>

      {/* Metric strip — 10 cards in a row (matches prototype grid-cols-10) */}
      <div className="grid grid-cols-10 gap-2 p-space-sm bg-surface-container-lowest my-2 rounded-lg shadow-sm">
        {metrics.map((m) => (
          <MetricCard key={m.title} {...m} />
        ))}
      </div>

      {/* Main Canvas Grid (Three Modular Columns for 1980px Layout) */}
      <div className="grid grid-cols-12 gap-space-md mb-4">
        {/* LEFT COLUMN (4 cols): Quick Start & Recent Workflows */}
        <div className="col-span-4">
          <DashboardQuickStart navigate={navigate} />
        </div>
        {/* MIDDLE COLUMN (5 cols): Runtime Fleet Health */}
        <div className="col-span-5">
          <DashboardFleetHealth />
        </div>
        {/* RIGHT COLUMN (3 cols): HITL Gate Queue */}
        <div className="col-span-3">
          <DashboardHITLQueue />
        </div>
      </div>

      {/* Bottom metrics panel */}
      <DashboardMetricsPanel />
    </div>
  );
}

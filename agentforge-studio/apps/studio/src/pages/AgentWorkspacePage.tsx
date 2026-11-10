import { useState, useEffect, useMemo } from "react";
import {
  Button,
  Badge,
  StatusLight,
  Tabs,
  TabsList,
  TabsTrigger,
} from "@agentforge/ui";
import { useApiClient } from "../hooks/use-api-client.js";
import {
  Search,
  Shield,
  Plus,
  Play,
} from "lucide-react";

// ── Mock data ───────────────────────────────────────────────────────────────

interface AgentPoolItem {
  id: string;
  name: string;
  archetype: string;
  status: "active" | "idle" | "standby" | "error";
  engine: string;
  tokenQuota: string;
  memoryBinding: string;
}

const MOCK_AGENT_POOL: AgentPoolItem[] = [
  {
    id: "secops-forensic-investigator",
    name: "SecOps-Forensic-Investigator",
    archetype: "autonomous-incident-response",
    status: "active",
    engine: "gpt-4o-2024-08-06",
    tokenQuota: "3.8M / 5.0M",
    memoryBinding: "Hybrid Vector+BM25",
  },
  {
    id: "code-gen-specialist",
    name: "Code-Gen-Specialist",
    archetype: "ast-synthesis-refactor",
    status: "idle",
    engine: "claude-3-5-sonnet",
    tokenQuota: "1.2M / 10.0M",
    memoryBinding: "Tree-Sitter Ephemeral Cache",
  },
  {
    id: "financial-risk-quant",
    name: "Financial-Risk-Quant",
    archetype: "stochastic-ledger-analyzer",
    status: "active",
    engine: "llama-3.1-70b-instruct",
    tokenQuota: "940K / 2.0M",
    memoryBinding: "TimescaleDB Vector Node #02",
  },
  {
    id: "compliance-auditor",
    name: "Compliance-Auditor",
    archetype: "soc2-governance",
    status: "standby",
    engine: "gpt-4o-mini",
    tokenQuota: "120K / 1.0M",
    memoryBinding: "PostgreSQL FTS",
  },
];

const MOCK_CODE = `import { defineAgent, ReActLoop, CapabilityManifest } from "@agentforge/agent";
import { HybridVectorBM25Retriever } from "@agentforge/memory/store";

/**
 * @agent SecOps-Forensic-Investigator
 * @version 2.4.1
 * @archetype Autonomous Incident Response Unit
 */
export const SecOpsForensicAgent = defineAgent({
  id: "secops-forensic-investigator",
  model: {
    primary: "gpt-4o-2024-08-06",
    fallback: "ollama/qwen2.5:72b",
    temperature: 0.20,
    maxContextTokens: 128000
  },

  // ReAct Orchestration Cycle Parameters
  loop: new ReActLoop({
    maxIterations: 8,
    earlyTerminationPolicy: "quorum-consensus",
    reflectionStepEnabled: true
  }),

  // Mounted Host Capabilities & Tool Calling Surface
  capabilities: [
    "cap-ebpf-telemetry",        // Kernel socket & process trace
    "cap-k8s-pod-quarantine",    // NetworkPolicy isolated container
    "cap-memory-heapsnoop"       // Volatile RAM dump parser
  ],

  // System Metaprompt (Jinja2 dynamic templating)
  promptTemplate: \`
    You are {{ agent.name }}, a tier-4 security intelligence system.
    {% if alert.severity == 'CRITICAL' %}
      CRITICAL ALERT TRIGGERED: Enforce non-destructive snapshot
    {% endif %}
    Follow the Action -> Observation -> Reflection protocol without deviation.
  \`
});`;

// ─── Sub-components ──────────────────────────────────────────────────────────

function AgentPoolPanel({
  agents,
  selectedId,
  onSelect,
  searchQuery,
  onSearchChange,
}: {
  agents: AgentPoolItem[];
  selectedId: string | null;
  onSelect: (id: string) => void;
  searchQuery: string;
  onSearchChange: (q: string) => void;
}) {
  const filtered = useMemo(() => {
    if (!searchQuery.trim()) return agents;
    const q = searchQuery.toLowerCase();
    return agents.filter(
      (a) => a.name.toLowerCase().includes(q) || a.archetype.toLowerCase().includes(q) || a.engine.toLowerCase().includes(q),
    );
  }, [agents, searchQuery]);

  const statusMap: Record<string, { label: string; className: string; lightStatus: "running" | "neutral" | "error" }> = {
    active: { label: "ACTIVE", className: "bg-primary/20 text-primary border-primary/30", lightStatus: "running" },
    idle: { label: "IDLE", className: "bg-surface-container-high text-outline border-outline-variant", lightStatus: "neutral" },
    standby: { label: "STANDBY", className: "bg-amber-400/20 text-amber-400 border-amber-400/30", lightStatus: "neutral" },
    error: { label: "ERROR", className: "bg-error/20 text-error border-error/30", lightStatus: "error" },
  };

  return (
    <div className="w-64 bg-surface-container-lowest border-r border-outline-variant flex flex-col overflow-hidden shrink-0">
      {/* Header */}
      <div className="flex items-center justify-between px-3 py-2 border-b border-outline-variant">
        <span className="text-[12px] font-label-sm font-semibold text-on-surface uppercase tracking-wider">
          Active Agent Pool
        </span>
        <div className="flex items-center gap-1">
          <Badge className="bg-accent-primary/20 text-accent-primary text-[10px]">{agents.length} nodes</Badge>
          <button className="p-1 rounded hover:bg-surface-container-high text-outline">
            <Plus className="h-3 w-3" />
          </button>
        </div>
      </div>

      {/* Search */}
      <div className="px-2 py-2 border-b border-outline-variant">
        <div className="flex items-center gap-1.5 px-2 py-1 rounded bg-surface border border-outline-variant">
          <Search className="h-3 w-3 text-outline" />
          <input
            className="flex-1 bg-transparent text-[11px] font-code-sm text-on-surface placeholder:text-outline outline-none"
            placeholder="Filter agents by role, model..."
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
          />
        </div>
      </div>

      {/* Agent list */}
      <div className="flex-1 overflow-auto p-1.5 space-y-1">
        {filtered.map((agent) => {
          const s = statusMap[agent.status];
          const isSelected = selectedId === agent.id;
          return (
            <button
              key={agent.id}
              className={`w-full text-left rounded border p-2.5 transition-colors ${
                isSelected
                  ? "border-accent-primary bg-surface-container-high"
                  : "border-outline-variant bg-surface hover:border-outline hover:bg-surface-container-high/50"
              }`}
              onClick={() => onSelect(agent.id)}
            >
              <div className="flex items-center justify-between mb-1">
                <div className="flex items-center gap-1.5">
                  <StatusLight status={s.lightStatus} size="sm" pulse={agent.status === "active"} />
                  <span className="text-[12px] font-body-md font-medium text-on-surface truncate">{agent.name}</span>
                </div>
                <Badge variant="outline" className={`text-[9px] shrink-0 ${s.className}`}>{s.label}</Badge>
              </div>
              <p className="text-[10px] font-code-sm text-outline mb-2 truncate">archetype://{agent.archetype}</p>
              <div className="grid grid-cols-2 gap-1 text-[10px]">
                <div>
                  <span className="text-outline">Engine</span>
                  <p className="text-on-surface font-code-sm truncate">{agent.engine}</p>
                </div>
                <div>
                  <span className="text-outline">Token Quota</span>
                  <p className="text-on-surface font-code-sm">{agent.tokenQuota}</p>
                </div>
              </div>
              <div className="mt-1.5">
                <span className="text-[10px] text-outline">Memory Binding</span>
                <p className="text-[10px] font-code-sm text-primary truncate">{agent.memoryBinding}</p>
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
}

function AgentCodeEditor() {
  return (
    <div className="flex-1 flex flex-col overflow-hidden min-w-0">
      {/* Editor header */}
      <div className="flex items-center justify-between px-3 py-1.5 border-b border-outline-variant bg-surface-container-lowest shrink-0">
        <div className="flex items-center gap-2">
          <span className="text-[12px] font-code-sm text-on-surface font-medium">SecOps-Forensic-Investigator</span>
          <Badge variant="outline" className="text-[10px]">v2.4.1-prod</Badge>
          <span className="text-[10px] font-code-sm text-outline">SHA: 7e2f18c</span>
        </div>
        <div className="flex items-center gap-1.5 text-[10px] font-code-sm text-outline">
          <span>Provider: OpenAI API (Priority) / Local Ollama Fallback (qwen2.5:72b)</span>
        </div>
      </div>

      {/* Toolbar */}
      <div className="flex items-center gap-2 px-3 py-1 border-b border-outline-variant bg-surface-container-low/50 shrink-0">
        <div className="flex items-center gap-1 text-[10px] font-code-sm text-outline">
          <span className="text-primary">TypeScript + Jinja2</span>
          <span>·</span>
          <span>UTF-8</span>
          <span>·</span>
          <span>LF</span>
        </div>
        <div className="ml-auto flex items-center gap-1">
          <Badge variant="outline" className="text-[10px]">ReadOnly: Find:</Badge>
          <Badge variant="outline" className="text-[10px]">Diff</Badge>
          <Badge variant="outline" className="text-[10px]">History</Badge>
        </div>
      </div>

      {/* Code content */}
      <div className="flex-1 overflow-auto bg-surface p-3">
        <pre className="text-[12px] font-code-sm leading-relaxed">
          {MOCK_CODE.split("\n").map((line, i) => (
            <div key={i} className="flex hover:bg-surface-container-high/30">
              <span className="text-outline/40 w-8 shrink-0 text-right mr-4 select-none text-[11px]">{String(i + 1).padStart(2, "0")}</span>
              <span
                className={
                  line.includes("import ") || line.includes("from ")
                    ? "text-secondary"
                    : line.includes("export const") || line.includes("defineAgent")
                    ? "text-primary"
                    : line.includes("//") || line.includes("/**") || line.includes("*")
                    ? "text-outline"
                    : line.includes('"') || line.includes("'") || line.includes("`")
                    ? "text-success"
                    : line.includes("@agent") || line.includes("@version") || line.includes("@archetype")
                    ? "text-tertiary"
                    : "text-on-surface"
                }
              >
                {line || " "}
              </span>
            </div>
          ))}
        </pre>
      </div>

      {/* Mounted capabilities bar */}
      <div className="flex items-center gap-2 px-3 py-1.5 border-t border-outline-variant bg-surface-container-lowest shrink-0">
        <span className="text-[10px] font-code-sm text-outline">Mounted:</span>
        {["cap-ebpf-telemetry", "cap-k8s-pod-quarantine", "cap-memory-heapsnoop", "Hybrid Vector+BM25"].map((cap) => (
          <Badge key={cap} variant="outline" className="text-[10px] bg-surface-container text-on-surface border-outline-variant">
            {cap}
          </Badge>
        ))}
      </div>
    </div>
  );
}

function AgentInspector() {
  return (
    <div className="w-72 bg-surface-container-lowest border-l border-outline-variant flex flex-col overflow-auto shrink-0">
      {/* Header */}
      <div className="flex items-center justify-between px-3 py-2 border-b border-outline-variant">
        <span className="text-[12px] font-label-sm font-semibold text-on-surface uppercase tracking-wider">
          Inspector & Runtime Metrics
        </span>
        <Badge className="bg-success/20 text-success text-[10px]">LIVE SYNC</Badge>
      </div>

      <div className="p-3 space-y-4">
        {/* Evaluation Score */}
        <div className="rounded border border-outline-variant bg-surface p-3">
          <div className="flex items-center justify-between mb-1">
            <span className="text-[10px] font-code-sm text-outline uppercase">Evaluation Score (EVAL-7)</span>
            <span className="text-[10px] font-code-sm text-success">+1.8% vs base</span>
          </div>
          <div className="flex items-baseline gap-1">
            <span className="text-[28px] font-headline-lg text-on-surface tabular-nums leading-none">94.6</span>
            <span className="text-[12px] font-code-sm text-outline">/ 100</span>
          </div>
          <div className="flex items-center gap-1 mt-1">
            {[60, 65, 70, 72, 75, 80, 82, 85, 88, 90, 92, 94.6].map((v, i) => (
              <div
                key={i}
                className="flex-1 rounded-sm bg-primary/60"
                style={{ height: `${(v / 100) * 24}px` }}
              />
            ))}
          </div>
          <div className="flex justify-between text-[9px] font-code-sm text-outline mt-0.5">
            <span>Benchmark Runs (T-7)</span>
            <span>Run #148: 94.6</span>
          </div>
        </div>

        {/* Security Clearance */}
        <div className="rounded border border-outline-variant bg-surface p-3">
          <div className="flex items-center justify-between mb-1">
            <span className="text-[10px] font-code-sm text-outline uppercase">Security Clearance</span>
            <Badge className="bg-error/20 text-error text-[10px]">RESTRICTED</Badge>
          </div>
          <div className="flex items-center gap-2 mt-1">
            <Shield className="h-5 w-5 text-accent-tool" />
            <div>
              <p className="text-[13px] font-body-md font-semibold text-on-surface">L4 Privileged</p>
              <p className="text-[10px] font-code-sm text-outline">Can invoke kernel eBPF probes & isolate K8s pods</p>
            </div>
          </div>
        </div>

        {/* Active Sessions + Median Latency */}
        <div className="grid grid-cols-2 gap-2">
          <div className="rounded border border-outline-variant bg-surface p-2.5">
            <span className="text-[10px] font-code-sm text-outline uppercase">Active Sessions</span>
            <div className="flex items-baseline gap-1 mt-1">
              <span className="text-[20px] font-headline-md text-on-surface tabular-nums leading-none">14</span>
              <span className="text-[10px] font-code-sm text-outline">concurrency</span>
            </div>
            <p className="text-[10px] font-code-sm text-outline mt-1">Peak: 28 threads</p>
          </div>
          <div className="rounded border border-outline-variant bg-surface p-2.5">
            <span className="text-[10px] font-code-sm text-outline uppercase">Median Latency</span>
            <div className="flex items-baseline gap-1 mt-1">
              <span className="text-[20px] font-headline-md text-on-surface tabular-nums leading-none">382</span>
              <span className="text-[10px] font-code-sm text-outline">ms/token</span>
            </div>
            <p className="text-[10px] font-code-sm text-outline mt-1">TTFT: 120ms</p>
          </div>
        </div>

        {/* Memory Partition */}
        <div className="rounded border border-outline-variant bg-surface p-3">
          <span className="text-[10px] font-code-sm text-outline uppercase">Memory Partition</span>
          <div className="mt-1.5 space-y-1.5">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-code-sm text-on-surface">Namespace</span>
              <span className="text-[11px] font-code-sm text-primary">secops-incidents-2025</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-code-sm text-on-surface">Indexed Records</span>
              <span className="text-[11px] font-code-sm text-on-surface">1,489,203 docs</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-code-sm text-on-surface">HNSW M/efSearch</span>
              <span className="text-[11px] font-code-sm text-on-surface">16 / 64</span>
            </div>
          </div>
        </div>

        {/* Action buttons */}
        <div className="flex items-center gap-2">
          <Button variant="secondary" size="sm" className="flex-1 text-[11px]">
            <Play className="h-3 w-3 mr-1" />Execute Mock Payload
          </Button>
        </div>
      </div>
    </div>
  );
}

// ── Main Page ───────────────────────────────────────────────────────────────

export function AgentWorkspacePage() {
  const apiClient = useApiClient();
  const [agents, setAgents] = useState<AgentPoolItem[]>(MOCK_AGENT_POOL);
  const [selectedId, setSelectedId] = useState<string | null>("secops-forensic-investigator");
  const [searchQuery, setSearchQuery] = useState("");
  const [activeTab, setActiveTab] = useState<"definition" | "overview" | "versions">("definition");

  // Fetch real agent data (fallback to mock)
  useEffect(() => {
    apiClient.listAgentSummaries()
      .then((realAgents) => {
        if (realAgents.length > 0) {
          setAgents(
            realAgents.map((a) => ({
              id: a.id,
              name: a.name,
              archetype: a.description ?? "unknown",
              status: a.status === "RELEASED" ? "active" : "idle",
              engine: "gpt-4o-2024-08-06",
              tokenQuota: "3.8M / 5.0M",
              memoryBinding: "Hybrid Vector+BM25",
            })),
          );
        }
      })
      .catch(() => {});
  }, [apiClient]);

  return (
    <div className="flex-1 flex flex-col overflow-hidden bg-surface">
      {/* Top tabs */}
      <div className="flex items-center gap-2 px-3 py-1.5 border-b border-outline-variant bg-surface-container-lowest shrink-0">
        <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as typeof activeTab)}>
          <TabsList className="h-7">
            <TabsTrigger value="definition" className="text-[11px]">Definition</TabsTrigger>
            <TabsTrigger value="overview" className="text-[11px]">Overview</TabsTrigger>
            <TabsTrigger value="versions" className="text-[11px]">Versions v2.4.1</TabsTrigger>
          </TabsList>
        </Tabs>
        <div className="ml-auto flex items-center gap-1.5 text-[10px] font-code-sm text-outline">
          <span>TS 5.4.2 Strict</span>
        </div>
      </div>

      {/* Three-column layout */}
      <div className="flex flex-1 overflow-hidden">
        <AgentPoolPanel
          agents={agents}
          selectedId={selectedId}
          onSelect={setSelectedId}
          searchQuery={searchQuery}
          onSearchChange={setSearchQuery}
        />

        <AgentCodeEditor />

        <AgentInspector />
      </div>
    </div>
  );
}

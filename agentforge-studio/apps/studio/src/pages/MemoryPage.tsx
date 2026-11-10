import { useState } from "react";
import {
  Badge,
  StatusLight,
  CodeEditor,
  LineChart,
} from "@agentforge/ui";
import type { LineChartDatum } from "@agentforge/ui";
import {
  Search,
  Database,
  Zap,
  Brain,
  Clock,
  Layers,
  TrendingDown,
  Verified,
  HardDrive,
  Timer,
  Radar,
  ChevronRight,
  RefreshCw,
} from "lucide-react";

// ── Mock data ───────────────────────────────────────────────────────────────

interface MemoryKPI {
  label: string;
  value: string;
  unit?: string;
  detail: string;
  icon: React.ElementType;
  iconColor: string;
  progress: number;
  progressColor: string;
  valueColor?: string;
}

const MOCK_KPIS: MemoryKPI[] = [
  { label: "Total Memories", value: "142.8k", detail: "+3.4%", icon: Layers, iconColor: "text-primary", progress: 82, progressColor: "bg-primary" },
  { label: "Hit Rate", value: "91.2%", detail: "L1/L2", icon: Zap, iconColor: "text-tertiary", progress: 91, progressColor: "bg-tertiary", valueColor: "text-tertiary" },
  { label: "Search Latency", value: "4.8", unit: "ms", detail: "p99: 11ms", icon: Clock, iconColor: "text-secondary", progress: 35, progressColor: "bg-secondary" },
  { label: "Vector Store", value: "Qdrant Cl.", detail: "6 Nodes", icon: Database, iconColor: "text-primary-container", progress: 100, progressColor: "bg-primary-container" },
  { label: "Decay Factor", value: "0.95", unit: "/d", detail: "Half: 14d", icon: TrendingDown, iconColor: "text-outline", progress: 95, progressColor: "bg-outline" },
  { label: "Epistemic Conf.", value: "96.4%", detail: "σ²: 0.02", icon: Verified, iconColor: "text-tertiary", progress: 96, progressColor: "bg-tertiary", valueColor: "text-tertiary" },
  { label: "Memory Size", value: "1.84", unit: " GB", detail: "RAM 38%", icon: HardDrive, iconColor: "text-primary", progress: 38, progressColor: "bg-primary" },
  { label: "Pruning Interval", value: "24", unit: "h", detail: "Next: 04:00Z", icon: Timer, iconColor: "text-secondary", progress: 65, progressColor: "bg-secondary-container" },
  { label: "Recall Precision", value: "94.8%", detail: "k@10", icon: Radar, iconColor: "text-tertiary", progress: 95, progressColor: "bg-tertiary", valueColor: "text-tertiary" },
];

interface MemoryResult {
  id: string;
  type: "Episodic" | "Semantic" | "Procedural" | "Working";
  score: number;
  content: string;
  agent: string;
  decay: string;
  vector: string;
  isActive?: boolean;
}

const MOCK_RESULTS: MemoryResult[] = [
  {
    id: "mem-094182f4",
    type: "Episodic",
    score: 0.972,
    content: "Incident INC-9021 diagnosis: Host kernel syn flood socket exhaustion mitigated using temporary eBPF packet drop bytecode directly at XDP ingress layer.",
    agent: "SecOps-Forensic",
    decay: "0.941 (3h ago)",
    vector: "#9042",
    isActive: true,
  },
  {
    id: "mem-88190c12",
    type: "Procedural",
    score: 0.934,
    content: "Compiled eBPF sockfilter bytecode payload reference for ingress mitigation: sys_enter_connect throttling heuristic with tail call to tracing hook.",
    agent: "SecOps-Forensic",
    decay: "0.880 (18h ago)",
    vector: "#6104",
  },
  {
    id: "mem-1204cf98",
    type: "Semantic",
    score: 0.891,
    content: "Incident root-cause summary: Network partition triggered abnormal syn loop between service mesh edge gateways and ingress ingress-envoy-ingress.",
    agent: "Financial-Risk",
    decay: "0.722 (2.1d ago)",
    vector: "#1998",
  },
];

const MOCK_PAYLOAD = `{
  "id": "mem-094182f4-7e82-411a-b8d9-29188190c12",
  "vector_dim": 1536,
  "embedding_head": [-0.03819, 0.09182, -0.00192, 0.1428, ...],
  "metadata": {
    "provenance_agent": "SecOps-Forensic-v4",
    "session_ref": "sess_9918274a",
    "incident_tag": "INC-9021",
    "triples": [
      {
        "s": "Incident:INC-9021",
        "p": "requires_mitigation",
        "o": "Method:eBPF_Socket_Drop"
      }
    ],
    "access_count": 42,
    "last_recalled_at": "2025-04-03T11:42:19.102Z",
    "decay_half_life_days": 14,
    "checksum_sha256": "e3b0c44298fc1c149afbf4c8996fb924..."
  }
}`;

// Ebbinghaus decay data points
const MOCK_DECAY_DATA: LineChartDatum[] = [
  { label: "D0", value: 100 },
  { label: "D1", value: 94 },
  { label: "D2", value: 88 },
  { label: "D3", value: 83 },
  { label: "D4", value: 78 },
  { label: "D5", value: 74 },
  { label: "D6", value: 70 },
  { label: "D7", value: 66 },
  { label: "D14", value: 50 },
];

const MOCK_AUDIT_LOGS = [
  { action: "READ_RECALL", time: "11:42:19Z", detail: 'Query: "mitigation socket syn"', caller: "AgentForge.Swarm.Planner", color: "text-tertiary" },
  { action: "DECAY_REINFORCE", time: "09:15:02Z", detail: "Boost factor: +0.081 retention", caller: "Positive user feedback signal", color: "text-primary" },
  { action: "VECTOR_SYNCHRONIZED", time: "04:00:11Z", detail: "Re-embedded via text-emb-3-large", caller: "Cluster consensus verified", color: "text-secondary" },
];

const MOCK_SSE_LOGS = [
  { time: "04:00:00.012", source: "GC_PRUNER", color: "text-tertiary", message: "Beginning daily memory garbage collection cycle (Half-life filter: 0.95^t)." },
  { time: "04:00:01.458", source: "GC_PRUNER", color: "text-primary", message: "Pruned 42 stale episodic nodes below threshold 0.15 at 04:00 UTC.", bold: true },
  { time: "04:00:01.890", source: "QDRANT_SHARD", color: "text-tertiary", message: "Payload vacuuming completed: 18.2 MB reclaimed from RocksDB block cache." },
  { time: "11:42:19.102", source: "RECALL_EVENT", color: "text-secondary", message: "Candidate mem-094182f4 retrieved with cos_sim=0.972 (query_hash: #8092ca)." },
];

// ── Sub-components ──────────────────────────────────────────────────────────

function KPIRibbon({ kpis }: { kpis: MemoryKPI[] }) {
  return (
    <div className="grid grid-cols-9 gap-1 px-3 py-2 bg-surface-container-lowest">
      {kpis.map((kpi) => {
        const Icon = kpi.icon;
        return (
          <div
            key={kpi.label}
            className="bg-surface-container p-2 rounded flex flex-col justify-between hover:bg-surface-container-high transition-colors"
          >
            <div className="flex items-center justify-between text-on-surface-variant">
              <span className="text-[10px] font-label-sm uppercase truncate">{kpi.label}</span>
              <Icon className={`h-3.5 w-3.5 ${kpi.iconColor}`} />
            </div>
            <div className="mt-1 flex items-baseline justify-between">
              <span className={`text-[16px] font-headline-lg text-on-surface font-semibold tracking-tight ${kpi.valueColor ?? ""}`}>
                {kpi.value}
                {kpi.unit && <span className="text-[11px] font-normal text-on-surface-variant">{kpi.unit}</span>}
              </span>
              <span className="text-[10px] font-code-sm text-on-surface-variant">{kpi.detail}</span>
            </div>
            <div className="w-full bg-surface-container-highest h-1 rounded mt-1.5 overflow-hidden">
              <div className={`${kpi.progressColor} h-full`} style={{ width: `${kpi.progress}%` }} />
            </div>
          </div>
        );
      })}
    </div>
  );
}

function SearchFilterPanel({
  results,
  selectedId,
  onSelect,
}: {
  results: MemoryResult[];
  selectedId: string | null;
  onSelect: (id: string) => void;
}) {
  return (
    <aside className="w-[340px] bg-surface-container-lowest/70 border-r border-outline-variant/30 flex flex-col overflow-hidden shrink-0">
      {/* Search Engine Box */}
      <div className="p-3 space-y-3 border-b border-outline-variant/30">
        <div className="flex items-center justify-between">
          <span className="text-[11px] font-label-sm text-on-surface font-semibold uppercase tracking-wider flex items-center gap-1">
            <Search className="h-3.5 w-3.5 text-primary" />
            Hybrid Neural Recall
          </span>
          <span className="text-[10px] font-code-sm text-tertiary">Cos Similarity</span>
        </div>

        {/* Search input */}
        <div className="relative flex items-center">
          <input
            className="w-full bg-surface-container-lowest text-on-surface text-[12px] font-body-sm px-3 py-1.5 rounded border border-outline-variant/30 focus:outline-none focus:ring-1 focus:ring-primary placeholder:text-on-surface-variant/40"
            placeholder="Type natural query, semantic keywords..."
            defaultValue="INC-9021 socket exhaustion eBPF mitigation"
          />
          <button className="absolute right-1 px-2 py-0.5 rounded bg-primary-container text-on-primary-container text-[10px] font-label-sm flex items-center gap-0.5 hover:brightness-110">
            <span>RUN</span>
            <ChevronRight className="h-3 w-3" />
          </button>
        </div>

        {/* Alpha Slider */}
        <div className="bg-surface-container-low p-2 rounded space-y-1.5">
          <div className="flex items-center justify-between text-[11px] font-code-sm">
            <span className="text-on-surface flex items-center gap-1 font-medium">
              <Brain className="h-3 w-3 text-primary" />
              Hybrid Ratio (α = 0.7)
            </span>
            <div className="flex gap-2">
              <span className="text-tertiary">0.7 Dense</span>
              <span className="text-outline">/</span>
              <span className="text-secondary">0.3 Sparse</span>
            </div>
          </div>
          <div className="relative w-full h-2 bg-surface-container-highest rounded flex items-center overflow-hidden">
            <div className="h-full bg-primary" style={{ width: "70%" }} />
            <div className="h-full bg-secondary" style={{ width: "30%" }} />
          </div>
          <div className="flex justify-between text-[9px] font-code-sm text-on-surface-variant">
            <span>text-embedding-3-large (1536d)</span>
            <span>Splade-v2 Lexical BM25</span>
          </div>
        </div>

        {/* Filters */}
        <div className="grid grid-cols-2 gap-2">
          <div>
            <label className="block text-[10px] font-label-sm text-on-surface-variant mb-1 uppercase">Filter Agent</label>
            <select className="w-full bg-surface-container-lowest text-on-surface text-[11px] px-2 py-1 rounded border border-outline-variant/30 focus:outline-none focus:ring-1 focus:ring-primary">
              <option>SecOps-Forensic (Active)</option>
              <option>Code-Gen Orchestrator</option>
              <option>All Swarm Agents</option>
            </select>
          </div>
          <div>
            <label className="block text-[10px] font-label-sm text-on-surface-variant mb-1 uppercase">Time Decay</label>
            <select className="w-full bg-surface-container-lowest text-on-surface text-[11px] px-2 py-1 rounded border border-outline-variant/30 focus:outline-none focus:ring-1 focus:ring-primary">
              <option>Past 7 Days (e^-0.95t)</option>
              <option>Past 24 Hours</option>
              <option>All Historical Epochs</option>
            </select>
          </div>
        </div>

        {/* Type tags */}
        <div className="flex items-center gap-1 flex-wrap">
          <span className="text-[10px] font-label-sm text-on-surface-variant mr-1">TYPE:</span>
          <Badge className="bg-primary text-on-primary text-[10px]">Episodic ✓</Badge>
          <Badge variant="outline" className="text-[10px] text-on-surface-variant">Semantic</Badge>
          <Badge variant="outline" className="text-[10px] text-on-surface-variant">Procedural</Badge>
          <Badge variant="outline" className="text-[10px] text-on-surface-variant">Working</Badge>
        </div>
      </div>

      {/* Result Cards */}
      <div className="flex-1 overflow-auto p-2 space-y-1.5">
        <div className="flex items-center justify-between px-1 mb-1">
          <span className="text-[10px] font-label-sm text-on-surface font-semibold uppercase tracking-wider flex items-center gap-1">
            <Database className="h-3 w-3 text-tertiary" />
            Retrieved Clusters ({results.length})
          </span>
          <span className="text-[10px] font-code-sm text-on-surface-variant">Top-k: 3 | Temp: 0.1</span>
        </div>

        {results.map((r) => (
          <button
            key={r.id}
            className={`w-full text-left p-2.5 rounded cursor-pointer transition-all ${
              r.isActive || r.id === selectedId
                ? "bg-surface-container-high shadow-md"
                : "bg-surface-container hover:bg-surface-container-high"
            }`}
            onClick={() => onSelect(r.id)}
          >
            <div className="flex items-start justify-between gap-2 mb-1">
              <div className="flex items-center gap-1.5">
                {r.isActive && <span className="w-2 h-2 rounded-full bg-primary animate-pulse" />}
                {!r.isActive && <span className="w-1.5 h-1.5 rounded-full bg-outline" />}
                <span className={`text-[11px] font-code-sm font-medium ${r.isActive ? "text-primary" : "text-on-surface"}`}>
                  {r.id}
                </span>
                <Badge variant="outline" className="text-[9px]">{r.type}</Badge>
              </div>
              <div className="flex items-center gap-1 text-[11px] font-code-sm text-tertiary font-semibold">
                <span>{r.score.toFixed(3)}</span>
              </div>
            </div>
            <p className="text-[11px] text-on-surface-variant line-clamp-2 leading-relaxed">{r.content}</p>
            <div className="mt-2 flex items-center justify-between text-[10px] font-code-sm text-on-surface-variant">
              <span className="flex items-center gap-1">
                <Brain className="h-3 w-3" />
                {r.agent}
              </span>
              <span className="flex items-center gap-1">
                <TrendingDown className="h-3 w-3" />
                {r.decay}
              </span>
            </div>
          </button>
        ))}
      </div>

      {/* Quick Index Diagnostic */}
      <div className="p-2 bg-surface-container-low border-t border-outline-variant/30 flex items-center justify-between text-[10px] font-code-sm text-on-surface-variant">
        <span className="flex items-center gap-1">
          <StatusLight status="success" size="sm" />
          1,489 Vectors in RAM
        </span>
        <span className="text-primary hover:underline cursor-pointer">Export Query Plan</span>
      </div>
    </aside>
  );
}

function SemanticTriplePanel() {
  return (
    <div className="bg-surface-container rounded p-3 flex flex-col">
      <div className="flex items-center justify-between pb-2 mb-2 border-b border-outline-variant/20">
        <span className="text-[11px] font-label-sm text-on-surface font-semibold flex items-center gap-1">
          <Brain className="h-3.5 w-3.5 text-tertiary" />
          Semantic Triple Extraction
        </span>
        <Badge variant="outline" className="text-[9px] text-tertiary">Knowledge Graph</Badge>
      </div>

      {/* Triple Diagram */}
      <div className="bg-surface-container-lowest rounded p-3 flex flex-col items-center justify-center">
        <div className="flex items-center justify-between w-full gap-2">
          {/* Subject */}
          <div className="px-2.5 py-1.5 rounded bg-surface-container-high shadow text-center max-w-[120px]">
            <span className="block text-[9px] font-code-sm text-primary uppercase">Subject</span>
            <span className="block text-[12px] font-headline-md text-on-surface font-semibold truncate">Incident:INC-9021</span>
            <span className="block text-[9px] font-code-sm text-on-surface-variant">Type: SecurityAlert</span>
          </div>

          {/* Edge */}
          <div className="flex-1 flex flex-col items-center px-1">
            <span className="text-[9px] font-code-sm text-tertiary font-medium bg-surface-container px-1.5 py-0.5 rounded">
              requires_mitigation
            </span>
            <div className="w-full h-0.5 bg-gradient-to-r from-primary via-tertiary to-secondary my-1 relative">
              <div className="absolute right-0 top-1/2 -translate-y-1/2 w-1.5 h-1.5 border-t-2 border-r-2 border-secondary rotate-45" />
            </div>
            <span className="text-[9px] font-code-sm text-on-surface-variant">Weight: 0.982</span>
          </div>

          {/* Object */}
          <div className="px-2.5 py-1.5 rounded bg-surface-container-high shadow text-center max-w-[130px]">
            <span className="block text-[9px] font-code-sm text-secondary uppercase">Object</span>
            <span className="block text-[12px] font-headline-md text-on-surface font-semibold truncate">eBPF_Socket_Drop</span>
            <span className="block text-[9px] font-code-sm text-on-surface-variant">Type: ActionRoutine</span>
          </div>
        </div>

        {/* Secondary entailment */}
        <div className="mt-3 pt-2 border-t border-outline-variant/20 flex items-center gap-1.5 w-full text-[10px] font-code-sm text-on-surface-variant">
          <span className="text-tertiary">Secondary:</span>
          <span className="text-on-surface bg-surface-container px-1.5 py-0.5 rounded">
            (eBPF_Socket_Drop) → [targets] → (Interface:eth0_xdp)
          </span>
        </div>
      </div>

      <div className="mt-2 flex items-center justify-between text-[10px] font-code-sm text-on-surface-variant">
        <span>Extractor: GLiNER-Large-v2.1</span>
        <span className="text-tertiary">Entropy Loss: 0.0124</span>
      </div>
    </div>
  );
}

function DecayCurvePanel() {
  return (
    <div className="bg-surface-container rounded p-3 flex flex-col">
      <div className="flex items-center justify-between pb-2 mb-2 border-b border-outline-variant/20">
        <span className="text-[11px] font-label-sm text-on-surface font-semibold flex items-center gap-1">
          <TrendingDown className="h-3.5 w-3.5 text-primary" />
          Ebbinghaus Retention & Decay
        </span>
        <span className="text-[10px] font-code-sm text-primary">λ = 0.051/day</span>
      </div>

      <div className="bg-surface-container-lowest rounded p-2">
        <LineChart
          data={MOCK_DECAY_DATA}
          height={100}
          colorScheme="primary"
          showArea
          dataKey="value"
          labelKey="label"
        />
      </div>

      <div className="mt-2 flex items-center justify-between text-[10px] font-code-sm text-on-surface-variant">
        <span>Boost: Reinforcement on 4 queries</span>
        <span className="text-tertiary font-medium">+12d Memory Ext. Granted</span>
      </div>
    </div>
  );
}

function PayloadViewer() {
  return (
    <div className="bg-surface-container rounded p-3 flex flex-col">
      <div className="flex items-center justify-between pb-2 mb-2 border-b border-outline-variant/20">
        <span className="text-[11px] font-label-sm text-on-surface font-semibold flex items-center gap-1">
          <Database className="h-3.5 w-3.5 text-tertiary" />
          Qdrant Point Payload
        </span>
        <span className="text-[10px] font-code-sm text-outline">payload_format: json.v2</span>
      </div>
      <CodeEditor value={MOCK_PAYLOAD} language="json" readOnly minHeight="140px" maxHeight="200px" />
    </div>
  );
}

function EpisodicDiffPanel() {
  return (
    <div className="bg-surface-container rounded p-3 flex flex-col">
      <div className="flex items-center justify-between pb-2 mb-2 border-b border-outline-variant/20">
        <span className="text-[11px] font-label-sm text-on-surface font-semibold flex items-center gap-1">
          <Brain className="h-3.5 w-3.5 text-secondary" />
          Episodic Context Diff
        </span>
        <span className="text-[10px] font-code-sm text-secondary">Turn #14</span>
      </div>

      <div className="bg-surface-container-lowest rounded p-2 space-y-2 max-h-40 overflow-y-auto">
        {/* User Turn */}
        <div className="bg-surface-container p-2 rounded">
          <div className="flex items-center justify-between text-[10px] font-code-sm text-on-surface-variant mb-1">
            <span className="text-tertiary font-semibold">AGENT_ORCHESTRATOR</span>
            <span>T+00:12:01</span>
          </div>
          <p className="text-[11px] text-on-surface leading-relaxed">
            &quot;Memory store query returned syn drop recommendation. Is this safely deployable on production ingress nodes?&quot;
          </p>
        </div>
        {/* Assistant Turn */}
        <div className="bg-surface-container p-2 rounded">
          <div className="flex items-center justify-between text-[10px] font-code-sm text-on-surface-variant mb-1">
            <span className="text-primary font-semibold">SECOPS_FORENSIC</span>
            <span>T+00:12:04</span>
          </div>
          <p className="text-[11px] text-on-surface leading-relaxed">
            &quot;Affirmative. eBPF bytecode attaches via XDP generic driver hook in kernel space. No socket reboot required.&quot;
          </p>
        </div>
      </div>

      <div className="mt-2 flex items-center justify-between text-[10px] font-code-sm text-on-surface-variant">
        <span>Context Tokens: 412 tok</span>
        <span className="text-primary hover:underline cursor-pointer flex items-center gap-0.5">
          View Full Trace <ChevronRight className="h-3 w-3" />
        </span>
      </div>
    </div>
  );
}

function AuditLogSection({ logs }: { logs: typeof MOCK_AUDIT_LOGS }) {
  return (
    <div className="bg-surface-container rounded p-3">
      <div className="flex items-center justify-between pb-2 mb-2 border-b border-outline-variant/20">
        <span className="text-[11px] font-label-sm text-on-surface font-semibold flex items-center gap-1">
          <Verified className="h-3.5 w-3.5 text-primary" />
          Immutable Memory Access Audit Log
        </span>
        <span className="text-[10px] font-code-sm text-on-surface-variant">Last {logs.length} Events</span>
      </div>
      <div className="grid grid-cols-3 gap-2">
        {logs.map((log) => (
          <div key={log.action} className="bg-surface-container-lowest p-2 rounded">
            <div className="flex items-center justify-between mb-1">
              <span className={`text-[10px] font-code-sm font-medium ${log.color}`}>{log.action}</span>
              <span className="text-[9px] font-code-sm text-on-surface-variant">{log.time}</span>
            </div>
            <div className="text-[11px] text-on-surface">{log.detail}</div>
            <div className="text-[10px] text-on-surface-variant mt-1">{log.caller}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

function BottomTelemetryPanel() {
  return (
    <div className="border-t border-outline-variant bg-surface-container-lowest p-3 shrink-0">
      {/* Console Tab Header */}
      <div className="flex items-center justify-between pb-2">
        <div className="flex items-center gap-2">
          <Badge className="bg-primary/20 text-primary text-[10px] flex items-center gap-1">
            <span className="w-1.5 h-1.5 rounded-full bg-primary animate-pulse" />
            SSE Telemetry Stream
          </Badge>
          <span className="text-[10px] font-code-sm text-on-surface-variant hover:text-on-surface cursor-pointer">
            Vector Distance Inspector
          </span>
          <span className="text-[10px] font-code-sm text-on-surface-variant hover:text-on-surface cursor-pointer">
            GC Pruning Audit
          </span>
          <span className="text-[10px] font-code-sm text-on-surface-variant hover:text-on-surface cursor-pointer">
            Qdrant Shell
          </span>
        </div>
        <div className="flex items-center gap-3 text-[10px] font-code-sm text-on-surface-variant">
          <span className="flex items-center gap-1">
            <span className="w-1.5 h-1.5 rounded-full bg-tertiary" />
            SSE 200 Connected (58.2 events/sec)
          </span>
        </div>
      </div>

      {/* Log Grid */}
      <div className="grid grid-cols-[2fr_1fr] gap-2 text-[11px] font-code-sm">
        {/* Live Stream Logs */}
        <div className="bg-surface-container p-2 rounded max-h-24 overflow-y-auto space-y-1">
          {MOCK_SSE_LOGS.map((log, i) => (
            <div key={i} className="flex items-start gap-2">
              <span className="text-on-surface-variant shrink-0">[{log.time} UTC]</span>
              <span className={`${log.color} shrink-0`}>[{log.source}]</span>
              <span className={`${log.bold ? "text-on-surface font-semibold" : "text-on-surface-variant"}`}>
                {log.message}
              </span>
            </div>
          ))}
        </div>

        {/* Real-time Cosine Projection */}
        <div className="bg-surface-container p-2 rounded flex flex-col justify-between">
          <div className="flex items-center justify-between text-[10px] font-label-sm text-on-surface-variant uppercase">
            <span>Cosine Projection</span>
            <span className="text-tertiary">k=3 Neighbors</span>
          </div>
          <div className="space-y-1.5 my-1">
            <div className="flex items-center justify-between text-[10px]">
              <span className="text-on-surface font-medium truncate">mem-094182f4 (Top 1)</span>
              <span className="text-tertiary font-semibold">0.972</span>
            </div>
            <div className="w-full bg-surface-container-highest h-1 rounded overflow-hidden">
              <div className="bg-primary h-full" style={{ width: "97.2%" }} />
            </div>
            <div className="flex items-center justify-between text-[10px]">
              <span className="text-on-surface-variant font-medium truncate">mem-88190c12 (Top 2)</span>
              <span className="text-on-surface-variant">0.934</span>
            </div>
            <div className="w-full bg-surface-container-highest h-1 rounded overflow-hidden">
              <div className="bg-tertiary h-full" style={{ width: "93.4%" }} />
            </div>
          </div>
          <div className="flex items-center justify-between text-[10px] text-on-surface-variant">
            <span>Quantization: SQ 8-bit</span>
            <span className="text-primary">Loss: -0.0031</span>
          </div>
        </div>
      </div>
    </div>
  );
}

// ── Main Page ───────────────────────────────────────────────────────────────

export function MemoryPage() {
  const [selectedMemoryId, setSelectedMemoryId] = useState<string | null>("mem-094182f4");

  return (
    <div className="flex-1 flex flex-col overflow-hidden bg-surface">
      {/* KPI Ribbon */}
      <KPIRibbon kpis={MOCK_KPIS} />

      {/* Main split: Left Search + Center-Right Inspector */}
      <div className="flex flex-1 overflow-hidden min-h-0">
        {/* Left Panel: Search & Filter */}
        <SearchFilterPanel
          results={MOCK_RESULTS}
          selectedId={selectedMemoryId}
          onSelect={setSelectedMemoryId}
        />

        {/* Center-Right Panel: Inspector */}
        <main className="flex-1 flex flex-col overflow-hidden bg-surface-container-low/50">
          {/* Inspector Tab Bar */}
          <div className="h-9 bg-surface-container-lowest px-3 flex items-center justify-between border-b border-outline-variant/30 shrink-0">
            <div className="flex items-center gap-2 text-[11px] font-code-sm">
              <div className="flex items-center gap-1.5 px-2 py-1 rounded bg-surface-container-high text-primary font-medium">
                <Brain className="h-3.5 w-3.5" />
                <span>{selectedMemoryId ?? "mem-094182f4"}.entity.json</span>
                <span className="w-1.5 h-1.5 rounded-full bg-tertiary" />
              </div>
              <span className="text-on-surface-variant">TTL: 2025-04-12T04:00:00Z</span>
              <span className="text-outline-variant">|</span>
              <span className="text-tertiary font-semibold">Decay State: HEALTHY (0.941)</span>
            </div>
            <div className="flex items-center gap-2">
              <button className="text-[10px] font-code-sm text-on-surface-variant hover:text-on-surface flex items-center gap-1">
                <RefreshCw className="h-3 w-3" />Revert
              </button>
              <button className="px-2 py-1 bg-primary text-on-primary rounded text-[10px] font-label-sm font-medium flex items-center gap-1 hover:brightness-110">
                <Database className="h-3 w-3" />Save & Propagate
              </button>
            </div>
          </div>

          {/* Inspector Content */}
          <div className="flex-1 overflow-auto p-3 space-y-3">
            {/* Row 1: Triple + Decay */}
            <div className="grid grid-cols-2 gap-3">
              <SemanticTriplePanel />
              <DecayCurvePanel />
            </div>

            {/* Row 2: Payload + Episodic */}
            <div className="grid grid-cols-[7fr_5fr] gap-3">
              <PayloadViewer />
              <EpisodicDiffPanel />
            </div>

            {/* Row 3: Audit Log */}
            <AuditLogSection logs={MOCK_AUDIT_LOGS} />
          </div>
        </main>
      </div>

      {/* Bottom Telemetry Panel */}
      <BottomTelemetryPanel />
    </div>
  );
}

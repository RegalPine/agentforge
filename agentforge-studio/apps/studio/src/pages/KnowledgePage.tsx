import { useState } from "react";
import {
  Badge,
  CodeEditor,
  WaterfallChart,
} from "@agentforge/ui";
import type { WaterfallSpan } from "@agentforge/ui";
import {
  Search,
  Database,
  Layers,
  Zap,
  Clock,
  Play,
  ArrowDown,
  Merge,
  CheckCircle2,
} from "lucide-react";

// ── Mock data ───────────────────────────────────────────────────────────────

interface KnowledgeMetric {
  label: string;
  value: string;
  detail: string;
  icon: React.ElementType;
  iconColor: string;
  detailColor?: string;
}

const MOCK_METRICS: KnowledgeMetric[] = [
  { label: "Total Docs", value: "24,890", detail: "+128 today", icon: Layers, iconColor: "text-primary", detailColor: "text-tertiary" },
  { label: "Chunks Parsed", value: "1,421,804", detail: "(512 avg tok)", icon: Database, iconColor: "text-tertiary" },
  { label: "Vector Space", value: "1536-dim", detail: "text-embedding-3-large", icon: Zap, iconColor: "text-secondary", detailColor: "text-secondary" },
  { label: "Hybrid Recall@20", value: "98.42%", detail: "RRF k=60", icon: Search, iconColor: "text-tertiary", detailColor: "text-tertiary" },
  { label: "Avg Pipeline Latency", value: "38.2 ms", detail: "p95: 54ms", icon: Clock, iconColor: "text-primary-container", detailColor: "text-primary" },
];

interface PipelineStageData {
  index: number;
  name: string;
  latencyMs: number;
  description: string;
  borderColor: string;
  metric: string;
  metricLabel: string;
  metricColor: string;
  codeDetail?: string;
  codeIcon?: string;
  isHighlighted?: boolean;
  isWide?: boolean;
}

const MOCK_STAGES: PipelineStageData[] = [
  {
    index: 1, name: "Scope Filter", latencyMs: 0.8,
    description: "Tenant & Domain context isolation boundary enforcement.",
    borderColor: "border-l-tertiary", metric: "PASSED", metricLabel: "tenant_id == 'org-cloudsec-prod'", metricColor: "text-tertiary",
    codeDetail: "check_circle",
  },
  {
    index: 2, name: "Permission ACL Gate", latencyMs: 1.1,
    description: "RBAC security evaluation on chunk classification level.",
    borderColor: "border-l-primary", metric: "ALLOW", metricLabel: "roles ∈ ['sec-ops', 'sre-lead']", metricColor: "text-primary",
    codeDetail: "lock_open",
  },
  {
    index: 3, name: "BM25 Sparse Search", latencyMs: 3.9,
    description: "Exact match on tokens & technical primitives.",
    borderColor: "border-l-secondary", metric: "42 chunks", metricLabel: "Elastic Lucene BM25 (k1=1.2, b=0.75)", metricColor: "text-secondary",
  },
  {
    index: 4, name: "Dense Vector HNSW", latencyMs: 11.6,
    description: "HNSW Approximate Nearest Neighbors (Cosine 0.88+).",
    borderColor: "border-l-tertiary-container", metric: "50 chunks", metricLabel: "ef_search=64, m=16, metric=cosine", metricColor: "text-tertiary",
  },
  {
    index: 5, name: "RRF Merge", latencyMs: 1.4,
    description: "Combines 92 candidates from BM25 and HNSW into unified rank.",
    borderColor: "border-l-primary-container", metric: "64 unique", metricLabel: "RRF_Score = Σ 1 / (60 + Rank_i)", metricColor: "text-primary-container",
  },
  {
    index: 6, name: "Deduplication", latencyMs: 2.1,
    description: "SimHash & semantic cosine adjacency prune redundant chunks.",
    borderColor: "border-l-outline", metric: "-14 drop", metricLabel: "64 → 50 chunks", metricColor: "text-error",
  },
  {
    index: 7, name: "BGE Re-ranking", latencyMs: 18.2,
    description: "Deep full-attention scoring of [Query, Chunk] token interactions.",
    borderColor: "border-l-secondary-container", metric: "Top-20", metricLabel: "BAAI/bge-reranker-large", metricColor: "text-secondary",
  },
  {
    index: 8, name: "Compliance Guardrails", latencyMs: 1.8,
    description: "PII scrubber, secrets detector, zero-trust leakage verification.",
    borderColor: "border-l-error", metric: "0 detected", metricLabel: "PII/Secrets clean", metricColor: "text-tertiary",
    codeDetail: "verified_user",
  },
  {
    index: 9, name: "Top-K Context Formatter", latencyMs: 0.9,
    description: "Final 5 Chunks injected into LLM System Prompt with schema headers.",
    borderColor: "border-l-primary", metric: "2,140 tokens", metricLabel: "Ready for Prompt Synthesis", metricColor: "text-primary",
    isHighlighted: true,
  },
];

interface ChunkItem {
  rank: number;
  filename: string;
  content: string;
  tokens: number;
  docId: string;
  cosine: number;
  score: number;
  isTop?: boolean;
}

const MOCK_CHUNKS: ChunkItem[] = [
  { rank: 1, filename: "ebpf_kernel_probe_rules.md", content: "Tracee / Tetragon container runtime hook. SecOps agents must attach eBPF probes specifically to sys_enter_execve and commit_creds inside cgroup namespaces...", tokens: 412, docId: "doc_0994_secops", cosine: 0.923, score: 0.941, isTop: true },
  { rank: 2, filename: "k8s_containerd_sandbox.pdf", content: "Hardening containerd CRI runtime via gVisor and eBPF network filtering (Cilium L7). When isolating untrusted multi-tenant workloads...", tokens: 480, docId: "doc_1842_k8s", cosine: 0.899, score: 0.918 },
  { rank: 3, filename: "cilium_network_bpf.rst", content: "By offloading iptables packet inspection directly to eBPF tc filters, pod inter-isolation achieves near-zero latency degradation...", tokens: 395, docId: "doc_0411_cilium", cosine: 0.885, score: 0.887 },
  { rank: 4, filename: "falco_detection_baseline.yaml", content: "Macro rule: container and not container_entrypoint with eBPF driver. Alert on shell spawned in root cgroup hierarchy...", tokens: 322, docId: "doc_7712_falco", cosine: 0.871, score: 0.865 },
  { rank: 5, filename: "apparmor_seccomp_bpf.md", content: "Complementing seccomp-bpf system call filters with LSM BPF programs provides programmable MAC without recompiling host kernels...", tokens: 531, docId: "doc_3108_lsm", cosine: 0.860, score: 0.849 },
];

const MOCK_WATERFALL: WaterfallSpan[] = [
  { name: "S1 Scope Filter", startMs: 0, durationMs: 0.8, color: "tertiary", status: "success" },
  { name: "S2 Permission ACL", startMs: 0.8, durationMs: 1.1, color: "primary", status: "success" },
  { name: "S3 BM25 Sparse", startMs: 1.9, durationMs: 3.9, color: "secondary" },
  { name: "S4 Dense HNSW", startMs: 1.9, durationMs: 11.6, color: "tertiary" },
  { name: "S5 RRF Merge", startMs: 13.5, durationMs: 1.4, color: "primary" },
  { name: "S6 Deduplication", startMs: 14.9, durationMs: 2.1, color: "outline" },
  { name: "S7 BGE Re-rank", startMs: 17.0, durationMs: 18.2, color: "secondary" },
  { name: "S8 Compliance", startMs: 35.2, durationMs: 1.8, color: "error" },
  { name: "S9 Top-K Format", startMs: 37.0, durationMs: 0.9, color: "primary", status: "success" },
];

const MOCK_TRACE_JSON = `{
  "trace_id": "trc_9f81ca7b82e",
  "query": "SecOps eBPF container isolation...",
  "stages_executed": 9,
  "total_duration_ms": 38.2,
  "sparse_candidates": 42,
  "dense_candidates": 50,
  "rerank_model": "bge-reranker-large",
  "injected_chunks": 5,
  "injected_tokens": 2140,
  "guardrail_status": "pass"
}`;

// ── Sub-components ──────────────────────────────────────────────────────────

function MetricsBar({ metrics }: { metrics: KnowledgeMetric[] }) {
  return (
    <div className="grid grid-cols-5 gap-0 bg-surface-container-lowest/60 border-b border-outline-variant/20 py-2.5 px-3">
      {metrics.map((m) => {
        const Icon = m.icon;
        return (
          <div key={m.label} className="flex items-center gap-3 px-2">
            <div className="w-8 h-8 rounded bg-surface-container-high flex items-center justify-center shrink-0">
              <Icon className={`h-4 w-4 ${m.iconColor}`} />
            </div>
            <div>
              <div className="text-[10px] font-label-sm uppercase tracking-wider text-outline">{m.label}</div>
              <div className="text-[14px] font-headline-md font-semibold text-on-surface">
                {m.value}{" "}
                <span className={`text-[11px] font-normal ${m.detailColor ?? "text-on-surface-variant"}`}>{m.detail}</span>
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}

function QueryInspectorBar() {
  return (
    <div className="px-3 py-2 bg-surface-container-low border-b border-outline-variant/20">
      <div className="flex items-center gap-3">
        <div className="flex items-center gap-1.5 px-2 py-1 bg-surface-container rounded text-on-surface-variant text-[11px] font-code-sm shrink-0">
          <span className="text-tertiary">POST</span>
          <span>/v2/rag/query</span>
        </div>
        <div className="relative flex-1">
          <input
            className="w-full bg-surface-container-lowest border border-outline-variant/40 rounded px-3 py-1.5 text-[12px] font-code-sm text-on-surface focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary"
            placeholder="Enter evaluation query prompt..."
            defaultValue="SecOps eBPF container isolation best practices"
          />
        </div>
        <button className="flex items-center gap-1.5 px-3 py-1.5 bg-primary-container text-on-primary-container text-[11px] font-label-sm rounded font-semibold hover:brightness-110 active:scale-95 transition-all">
          <Play className="h-3.5 w-3.5" />
          Inspect Flow
        </button>
        <div className="flex items-center gap-2 shrink-0 text-[10px] font-code-sm">
          <span className="text-on-surface-variant">
            Tenant: <Badge variant="outline" className="text-[9px] text-tertiary">org-cloudsec-prod</Badge>
          </span>
          <span className="text-on-surface-variant">
            Top-K: <Badge variant="outline" className="text-[9px] text-primary font-bold">5</Badge>
          </span>
        </div>
      </div>
    </div>
  );
}

function StageCard({ stage }: { stage: PipelineStageData }) {
  return (
    <div
      className={`bg-surface-container rounded border-l-[3px] border border-outline-variant/20 p-3 shadow-sm hover:border-outline-variant/40 transition-all ${stage.borderColor} ${stage.isHighlighted ? "ring-1 ring-primary/40" : ""}`}
    >
      <div className="flex items-center justify-between mb-1.5">
        <div className="flex items-center gap-1.5">
          <span className={`w-5 h-5 rounded flex items-center justify-center text-[10px] font-code-sm font-bold ${stage.borderColor.replace("border-l-", "bg-")}/15`}>
            {String(stage.index).padStart(2, "0")}
          </span>
          <span className={`text-[13px] font-headline-md text-on-surface ${stage.isHighlighted ? "text-primary font-bold" : ""}`}>
            {stage.name}
          </span>
        </div>
        <span className={`text-[11px] font-code-sm px-1.5 py-0.5 rounded bg-surface-container-high ${stage.metricColor}`}>
          {stage.latencyMs} ms
        </span>
      </div>
      <p className="text-[11px] text-on-surface-variant mb-2 leading-relaxed">{stage.description}</p>
      <div className="bg-surface-container-lowest p-2 rounded text-[10px] font-code-sm flex items-center justify-between">
        <span className="text-on-surface-variant">
          {stage.metricLabel && <span className="text-outline">{stage.metricLabel.split(" ")[0]}: </span>}
          <strong className={stage.metricColor}>{stage.metric}</strong>
        </span>
        {stage.codeDetail && (
          <span className={`${stage.metricColor} material-symbols-outlined text-[13px]`}>{stage.codeDetail}</span>
        )}
      </div>
    </div>
  );
}

function FlowConnector({ label, icon }: { label: string; icon?: "arrow" | "merge" }) {
  const Icon = icon === "merge" ? Merge : ArrowDown;
  return (
    <div className="flex justify-center my-0.5">
      <div className="flex items-center gap-1.5 text-outline text-[10px] font-code-sm">
        <Icon className="h-3.5 w-3.5 text-outline-variant" />
        <span>{label}</span>
        {icon !== "merge" && <Icon className="h-3.5 w-3.5 text-outline-variant" />}
      </div>
    </div>
  );
}

function PipelineCanvas({ stages }: { stages: PipelineStageData[] }) {
  return (
    <div className="flex-1 p-3 overflow-auto bg-surface-container-lowest/30 relative">
      {/* Canvas Header */}
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-primary animate-pulse" />
          <span className="text-[14px] font-headline-md font-semibold text-on-surface">9-Stage RAG Pipeline</span>
          <span className="text-outline text-[10px] font-code-sm">// end-to-end execution path</span>
        </div>
        <div className="flex items-center gap-1 bg-surface-container px-2 py-1 rounded border border-outline-variant/20 text-[10px] text-on-surface-variant font-code-sm">
          <span>Active Flow:</span>
          <span className="text-primary">DAG_HYBRID_RRF_BGE_v9</span>
        </div>
      </div>

      {/* Stages */}
      <div className="space-y-2">
        {/* Phase A: Stages 1 & 2 (Pre-Retrieval) */}
        <div className="grid grid-cols-2 gap-2">
          <StageCard stage={stages[0]} />
          <StageCard stage={stages[1]} />
        </div>

        <FlowConnector label="Parallel Branching to Dual Search Indices" icon="arrow" />

        {/* Phase B: Stages 3 & 4 (Dual Retrieval) */}
        <div className="grid grid-cols-2 gap-2">
          <StageCard stage={stages[2]} />
          <StageCard stage={stages[3]} />
        </div>

        <FlowConnector label="Reciprocal Rank Fusion Ingestion" icon="merge" />

        {/* Stage 5: RRF Merge */}
        <StageCard stage={stages[4]} />

        <FlowConnector label="" icon="arrow" />

        {/* Stage 6: Deduplication */}
        <StageCard stage={stages[5]} />

        <FlowConnector label="" icon="arrow" />

        {/* Stage 7: Re-ranking */}
        <StageCard stage={stages[6]} />

        <FlowConnector label="" icon="arrow" />

        {/* Phase C: Stages 8 & 9 (Output) */}
        <div className="grid grid-cols-2 gap-2">
          <StageCard stage={stages[7]} />
          <StageCard stage={stages[8]} />
        </div>
      </div>
    </div>
  );
}

function ChunkInspector({ chunks }: { chunks: ChunkItem[] }) {
  return (
    <div className="w-[380px] bg-surface-container-lowest/80 border-l border-outline-variant/20 flex flex-col overflow-hidden shrink-0">
      <div className="flex items-center justify-between px-3 py-2 border-b border-outline-variant/20">
        <span className="text-[12px] font-headline-md text-on-surface flex items-center gap-1.5">
          <Database className="h-4 w-4 text-primary" />
          Top-5 Injected Chunks
        </span>
        <span className="text-[10px] font-code-sm text-outline">Sort: Cross-Attention Desc</span>
      </div>

      <div className="flex-1 overflow-auto p-3 space-y-2.5">
        {chunks.map((chunk) => (
          <div
            key={chunk.rank}
            className={`bg-surface-container p-3 rounded border ${chunk.isTop ? "border-primary/40" : "border-outline-variant/20 hover:border-outline-variant/40"} transition-all`}
          >
            <div className="flex items-center justify-between mb-1.5">
              <div className="flex items-center gap-2">
                <Badge className={`text-[9px] font-bold ${chunk.isTop ? "bg-primary text-on-primary" : "bg-surface-container-high text-on-surface"}`}>
                  #{chunk.rank}{chunk.isTop ? " RELEVANCE" : ""}
                </Badge>
                <span className="text-[11px] font-code-sm text-on-surface font-semibold truncate max-w-[180px]">
                  {chunk.filename}
                </span>
              </div>
              <span className="text-[11px] font-code-sm text-tertiary font-bold">
                {chunk.score.toFixed(3)}
              </span>
            </div>
            <div className="bg-surface-container-lowest p-2 rounded text-[10px] font-code-sm text-on-surface-variant border border-outline-variant/15 leading-relaxed mb-2 line-clamp-3">
              {chunk.content}
            </div>
            <div className="flex items-center justify-between text-[10px] font-code-sm text-outline">
              <span>Tokens: <strong className="text-on-surface">{chunk.tokens}</strong></span>
              <span className="text-tertiary hover:underline cursor-pointer">{chunk.docId}</span>
              <span className="text-primary">Cos: {chunk.cosine.toFixed(3)}</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function TraceConsole({ waterfall, traceJson }: { waterfall: WaterfallSpan[]; traceJson: string }) {
  const [activeTab, setActiveTab] = useState<"waterfall" | "json">("waterfall");

  return (
    <div className="border-t border-outline-variant bg-surface-container-lowest flex flex-col shrink-0" style={{ height: 220 }}>
      {/* Tab Bar */}
      <div className="h-8 px-3 flex items-center justify-between bg-surface-container-low border-b border-outline-variant/20">
        <div className="flex items-center h-full">
          <button
            className={`px-3 h-full text-[10px] font-code-sm flex items-center gap-1.5 transition-colors ${activeTab === "waterfall" ? "text-primary border-b-2 border-primary bg-surface font-semibold" : "text-on-surface-variant hover:text-on-surface"}`}
            onClick={() => setActiveTab("waterfall")}
          >
            <span className="w-1.5 h-1.5 rounded-full bg-tertiary animate-pulse" />
            RAG TRACE CONSOLE
            <Badge variant="outline" className="text-[9px] text-primary">38.2ms total</Badge>
          </button>
          <button
            className={`px-3 h-full text-[10px] font-code-sm flex items-center gap-1.5 transition-colors ${activeTab === "json" ? "text-primary border-b-2 border-primary bg-surface font-semibold" : "text-on-surface-variant hover:text-on-surface"}`}
            onClick={() => setActiveTab("json")}
          >
            TRACE TELEMETRY JSON
          </button>
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-auto p-3">
        {activeTab === "waterfall" ? (
          <WaterfallChart data={waterfall} />
        ) : (
          <div className="flex gap-3 h-full">
            <div className="flex-1">
              <CodeEditor value={traceJson} language="json" readOnly minHeight="140px" maxHeight="180px" />
            </div>
            <div className="w-48 flex flex-col justify-between">
              <div className="flex items-center gap-1 text-[10px] font-code-sm text-outline">
                <CheckCircle2 className="h-3.5 w-3.5 text-tertiary" />
                All 9 assertions passed
              </div>
              <span className="text-[10px] font-code-sm text-outline">
                Engine: <span className="text-on-surface">AgentForge v2.4.1</span>
              </span>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

// ── Main Page ───────────────────────────────────────────────────────────────

export function KnowledgePage() {
  return (
    <div className="flex-1 flex flex-col overflow-hidden bg-surface">
      {/* Metrics Bar */}
      <MetricsBar metrics={MOCK_METRICS} />

      {/* Query Inspector Bar */}
      <QueryInspectorBar />

      {/* Main area: Pipeline + Chunk Inspector */}
      <div className="flex flex-1 overflow-hidden min-h-0">
        <PipelineCanvas stages={MOCK_STAGES} />
        <ChunkInspector chunks={MOCK_CHUNKS} />
      </div>

      {/* Bottom Trace Console */}
      <TraceConsole waterfall={MOCK_WATERFALL} traceJson={MOCK_TRACE_JSON} />
    </div>
  );
}

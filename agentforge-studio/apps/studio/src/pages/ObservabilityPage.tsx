import { useState } from "react";
import {
  Badge,
  LineChart,
  DonutChart,
  WaterfallChart,
} from "@agentforge/ui";
import type { LineChartDatum } from "@agentforge/ui";
import {
  Zap,
  Timer,
  MemoryStick,
  ShieldCheck,
  DollarSign,
  Server,
  TrendingUp,
  Activity,
  BarChart3,
  PieChart,
  Network,
  Layers,
  Terminal,
  CloudCheck,
  RefreshCw,
  ChevronDown,
  CheckCircle2,
} from "lucide-react";

// ── Mock data ───────────────────────────────────────────────────────────────

const MOCK_THROUGHPUT_DATA: LineChartDatum[] = [
  { label: "14:00", value: 42000 },
  { label: "14:01", value: 48000 },
  { label: "14:02", value: 55000 },
  { label: "14:03", value: 51000 },
  { label: "14:04", value: 62000 },
  { label: "14:05", value: 58000 },
  { label: "14:06", value: 71000 },
  { label: "14:07", value: 68000 },
  { label: "14:08", value: 82000 },
  { label: "14:09", value: 76000 },
  { label: "14:10", value: 91000 },
  { label: "14:11", value: 85000 },
  { label: "14:12", value: 98000 },
  { label: "14:13", value: 105000 },
  { label: "14:14", value: 112000 },
  { label: "14:15", value: 184500 },
];

const MOCK_COST_DATA = [
  { label: "GPT-4o (OpenAI)", value: 58, color: "#aac7ff" },
  { label: "Claude 3.5 Sonnet", value: 34, color: "#ddb7ff" },
  { label: "Local Ollama (Llama-3.3)", value: 8, color: "#4cd7f6" },
];

interface ModelFleetRow {
  name: string;
  provider: string;
  quotaPct: number;
  tpot: string;
  errRate: string;
  errColor: string;
  breaker: "CLOSED" | "HALF-OPEN" | "OPEN";
}

const MOCK_MODEL_FLEET: ModelFleetRow[] = [
  { name: "claude-3-7-sonnet-20250219", provider: "Anthropic (Direct API)", quotaPct: 64, tpot: "18.4 ms/tok", errRate: "0.001%", errColor: "text-[#4cd7f6]", breaker: "CLOSED" },
  { name: "gpt-4o-2024-11-20", provider: "Azure OpenAI (East US 2)", quotaPct: 82, tpot: "14.2 ms/tok", errRate: "0.003%", errColor: "text-[#4cd7f6]", breaker: "CLOSED" },
  { name: "deepseek-r1-distill-qwen-32b", provider: "Self-Hosted vLLM Pod #03", quotaPct: 45, tpot: "11.8 ms/tok", errRate: "0.000%", errColor: "text-[#4cd7f6]", breaker: "CLOSED" },
  { name: "mistral-large-2411", provider: "Mistral La Plateforme", quotaPct: 94, tpot: "42.1 ms/tok", errRate: "1.48%", errColor: "text-[#ffb4ab]", breaker: "HALF-OPEN" },
  { name: "text-embedding-3-small", provider: "OpenAI Embeddings Cluster", quotaPct: 28, tpot: "4.1 ms/tok", errRate: "0.000%", errColor: "text-[#4cd7f6]", breaker: "CLOSED" },
];

interface CapabilityHealth {
  name: string;
  status: "healthy" | "degraded" | "warning";
  details: string[];
  highlight: string;
  borderColor: string;
  dotColor: string;
  textColor: string;
}

const MOCK_CAPABILITY_HEALTH: CapabilityHealth[] = [
  { name: "eBPF Kernel Probe", status: "healthy", details: ["Ring Buffer: 1.2% Drop", "Kernel Hook: v6.5-bpf"], highlight: "Latency: 0.12ms", borderColor: "border-l-[#4cd7f6]", dotColor: "bg-[#4cd7f6]", textColor: "text-[#4cd7f6]" },
  { name: "Vector Memory (HNSW)", status: "healthy", details: ["Segments: 12 Shards", "Recall Rate: 99.4%"], highlight: "Search: 14ms (p95)", borderColor: "border-l-[#4cd7f6]", dotColor: "bg-[#4cd7f6]", textColor: "text-[#4cd7f6]" },
  { name: "K8s Pod Containment", status: "healthy", details: ["OOMKills: 0 (24h)", "CPU Throttling: 0.08%"], highlight: "148 / 148 Replicas", borderColor: "border-l-[#4cd7f6]", dotColor: "bg-[#4cd7f6]", textColor: "text-[#4cd7f6]" },
  { name: "LLM MCP Gateway", status: "warning", details: ["Envoy Router: v1.30", "Tool Calling: 5 MCPs"], highlight: "Cache Hit: 38.2%", borderColor: "border-l-[#3e90ff]", dotColor: "bg-[#3e90ff]", textColor: "text-[#3e90ff]" },
];

interface MeshNode {
  id: string;
  label: string;
  subtitle: string;
  meta1: string;
  meta2: string;
  borderColor: string;
  dotColor: string;
  textColor: string;
  left: string;
  top: string;
}

const MOCK_MESH_NODES: MeshNode[] = [
  { id: "orch", label: "Orchestrator", subtitle: "ORCH-01", meta1: "QPS: 1.2k", meta2: "12ms", borderColor: "border-l-[#aac7ff]", dotColor: "bg-[#aac7ff]", textColor: "text-[#aac7ff]", left: "left-4", top: "top-1/2 -translate-y-1/2" },
  { id: "plan", label: "Planner (Claude 3.7)", subtitle: "AGENT-PLAN", meta1: "Ctx: 14.2k", meta2: "182ms", borderColor: "border-l-[#ddb7ff]", dotColor: "bg-[#ddb7ff]", textColor: "text-[#ddb7ff]", left: "left-[240px]", top: "top-6" },
  { id: "rag", label: "Memory Synthesizer", subtitle: "AGENT-RAG", meta1: "HNSW TopK: 8", meta2: "22ms", borderColor: "border-l-[#4cd7f6]", dotColor: "bg-[#4cd7f6]", textColor: "text-[#4cd7f6]", left: "left-[240px]", top: "bottom-6" },
  { id: "exec", label: "Tool Sandbox", subtitle: "EXEC-MCP", meta1: "gRPC Pool: 24", meta2: "64ms", borderColor: "border-l-[#3e90ff]", dotColor: "bg-[#3e90ff]", textColor: "text-[#3e90ff]", left: "right-4", top: "top-1/2 -translate-y-1/2" },
];

interface WaterfallTraceSpan {
  name: string;
  startMs: number;
  durationMs: number;
  color: "primary" | "secondary" | "tertiary" | "error" | "outline";
  status: "success" | "warning" | "error";
}

const MOCK_WATERFALL_SPANS: WaterfallTraceSpan[] = [
  { name: "HTTP /v1/agent/invoke", startMs: 0, durationMs: 284, color: "primary", status: "success" },
  { name: "orchestrator.jinja_render", startMs: 5, durationMs: 18, color: "secondary", status: "success" },
  { name: "qdrant.vector_similarity_search", startMs: 23, durationMs: 32, color: "tertiary", status: "success" },
  { name: "llm.claude-3-7-sonnet.stream", startMs: 55, durationMs: 164, color: "primary", status: "success" },
  { name: "mcp.execute_shell_sandbox", startMs: 214, durationMs: 70, color: "tertiary", status: "success" },
];

interface OtelLogLine {
  time: string;
  level: "INFO" | "DEBUG" | "METRIC" | "WARN";
  source: string;
  message: string;
  levelColor: string;
  sourceColor: string;
}

const MOCK_OTEL_LOGS: OtelLogLine[] = [
  { time: "14:15:32.102", level: "INFO", source: "[otel.collector.grpc]", message: "Batch exported 148 spans to ClickHouse cluster backend. Latency: 3.4ms.", levelColor: "bg-[#aac7ff]/20 text-[#aac7ff]", sourceColor: "text-[#4cd7f6]" },
  { time: "14:15:32.418", level: "DEBUG", source: "[agent.mesh.router]", message: "Route resolved for span `orch_eval_04` -> target node `vllm-deepseek-qwen-worker-02`.", levelColor: "bg-[#ddb7ff]/20 text-[#ddb7ff]", sourceColor: "text-[#ddb7ff]" },
  { time: "14:15:32.880", level: "INFO", source: "[kafka.consumer.telemetry]", message: "Partition 03 committed offset #1092842. Ingestion lag: 42ms | Velocity: 184,520 tok/sec.", levelColor: "bg-[#aac7ff]/20 text-[#aac7ff]", sourceColor: "text-[#4cd7f6]" },
  { time: "14:15:33.012", level: "METRIC", source: "[ebpf.kernel.socket]", message: "kprobe/tcp_v4_connect captured trace for agent-pod-48 -> openai-gw.us-east.azure.com:443. RTT 18ms.", levelColor: "bg-[#252a32] text-[#4cd7f6]", sourceColor: "text-[#009eb9]" },
  { time: "14:15:33.245", level: "WARN", source: "[breaker.circuit.mistral]", message: "High latency threshold breach (42.1ms/tok). Switched from CLOSED to HALF-OPEN state.", levelColor: "bg-[#93000a]/30 text-[#ffb4ab]", sourceColor: "text-[#ffb4ab]" },
  { time: "14:15:33.420", level: "INFO", source: "[mcp.registry]", message: "Tool execution completed: `cluster_inspect_fs` exit code 0. Result serialized to context memory.", levelColor: "bg-[#aac7ff]/20 text-[#aac7ff]", sourceColor: "text-[#aac7ff]" },
];

// ── Component ───────────────────────────────────────────────────────────────

export function ObservabilityPage() {
  const [activeTab, setActiveTab] = useState("overview");

  return (
    <div className="flex flex-col w-full h-full bg-[#0f141b] text-[#dee2ec]">
      {/* Sub-header */}
      <div className="flex items-center justify-between px-3 py-1.5 bg-[#090f15]/80 backdrop-blur-md shrink-0">
        <div className="flex items-center gap-1.5 text-[11px] font-['JetBrains_Mono'] text-[#8b91a0]">
          <span className="hover:text-[#dee2ec] cursor-pointer">workspace</span>
          <span className="text-[#8b91a0]">/</span>
          <span className="hover:text-[#dee2ec] cursor-pointer">quality</span>
          <span className="text-[#8b91a0]">/</span>
          <span className="hover:text-[#dee2ec] cursor-pointer">observability</span>
          <span className="text-[#8b91a0]">/</span>
          <div className="flex items-center gap-1 text-[#aac7ff] bg-[#252a32]/60 px-1 py-0.5 rounded">
            <Activity className="w-3 h-3 text-[#4cd7f6]" />
            <span className="font-medium text-[#dee2ec]">cluster_telemetry.dashboard</span>
            <span className="w-1.5 h-1.5 rounded-full bg-[#4cd7f6] animate-pulse ml-1" />
          </div>
        </div>
        <div className="flex items-center gap-3 text-[11px] font-['JetBrains_Mono']">
          <div className="flex items-center gap-1.5 bg-[#171c23] px-2 py-1 rounded text-[#8b91a0]">
            <CloudCheck className="w-3 h-3 text-[#aac7ff]" />
            <span>us-east-prod-cluster-04</span>
          </div>
          <div className="flex items-center gap-1 bg-[#171c23] px-2 py-1 rounded text-[#8b91a0]">
            <Timer className="w-3 h-3" />
            <span className="text-[#dee2ec]">Last 15 min</span>
            <ChevronDown className="w-3 h-3 text-[#8b91a0]" />
          </div>
          <button className="flex items-center gap-1 px-2 py-1 bg-[#252a32] hover:bg-[#343941] text-[#dee2ec] rounded transition-colors">
            <RefreshCw className="w-3 h-3 text-[#4cd7f6]" />
            <span>Auto-Refresh (1s)</span>
          </button>
        </div>
      </div>

      {/* Sub-tabs */}
      <div className="flex items-center justify-between px-3 bg-[#171c23] shrink-0">
        <div className="flex items-center gap-1 overflow-x-auto">
          {[
            { id: "overview", label: "Overview Metrics", icon: <BarChart3 className="w-3.5 h-3.5" />, active: true },
            { id: "filters", label: "Filters & Tags", icon: <Zap className="w-3.5 h-3.5" /> },
            { id: "model", label: "Model Dashboard", icon: <Activity className="w-3.5 h-3.5" /> },
            { id: "capability", label: "Capability Health", icon: <ShieldCheck className="w-3.5 h-3.5" /> },
            { id: "mesh", label: "MultiAgent Tree", icon: <Network className="w-3.5 h-3.5" /> },
          ].map((tab) => (
            <button
              key={tab.id}
              className={`flex items-center gap-1.5 px-3 py-2 text-xs transition-colors ${
                activeTab === tab.id
                  ? "bg-[#0f141b] text-[#aac7ff] rounded-t shadow-sm font-medium"
                  : "text-[#8b91a0] hover:text-[#dee2ec]"
              }`}
              onClick={() => setActiveTab(tab.id)}
            >
              {tab.icon}
              <span>{tab.label}</span>
              {tab.active && <span className="px-1 py-0.5 rounded bg-[#aac7ff]/20 text-[#aac7ff] text-[9px]">LIVE</span>}
            </button>
          ))}
        </div>
        <div className="flex items-center gap-3 text-[11px] font-['JetBrains_Mono'] text-[#8b91a0]">
          <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-[#aac7ff] animate-ping" /> eBPF Flow Engine: Active</span>
          <span className="text-[#414754]">|</span>
          <span>Sampling Rate: 100%</span>
          <span className="text-[#414754]">|</span>
          <span className="text-[#ddb7ff] font-medium">Trace ID: af-7f89b-9021</span>
        </div>
      </div>

      {/* Scrollable content */}
      <div className="flex-1 overflow-y-auto p-3 space-y-3">
        {/* 6 Metric Cards */}
        <div className="grid grid-cols-6 gap-2">
          <MetricCard label="Global QPS" value="4,820" unit="req/s" icon={<Zap className="w-4 h-4 text-[#aac7ff]" />} trend={<>+14.2%</>} trendUp meta={<>Peak: 5.1k</>} />
          <MetricCard label="Avg Turn Latency" value="284" unit="ms" icon={<Timer className="w-4 h-4 text-[#4cd7f6]" />} trend={<>TTFT: 82ms</>} trendColor="text-[#4cd7f6]" meta={<>P95: 412ms</>} />
          <MetricCard label="Token Velocity" value="184.5k" unit="tok/s" icon={<MemoryStick className="w-4 h-4 text-[#ddb7ff]" />} trend={<>In: 62k / Out: 122.5k</>} trendColor="text-[#ddb7ff]" />
          <MetricCard label="5XX Fault Rate" value="0.004%" unit="" icon={<ShieldCheck className="w-4 h-4 text-[#009eb9]" />} trend={<>SLO: 99.99%</>} meta={<>Errors: 2 / 100k</>} metaHighlight="text-[#4cd7f6]" metaRight="Healthy" />
          <MetricCard label="Cloud Cost / hr" value="$14.28" unit="/ hr" icon={<DollarSign className="w-4 h-4 text-[#009eb9]" />} trend={<>Est: $342.70 / day</>} meta={<>-4.1% MoM</>} metaHighlight="text-[#aac7ff]" />
          <MetricCard label="Active Agent Instances" value="148" unit="Pods" icon={<Server className="w-4 h-4 text-[#3e90ff]" />} trend={<>HPA: 12 Nodes</>} meta={<>100% Ready</>} metaHighlight="text-[#4cd7f6]" />
        </div>

        {/* Row 1: 3 Charts */}
        <div className="grid grid-cols-3 gap-3">
          {/* Token Throughput */}
          <div className="bg-[#171c23] p-3 rounded-lg shadow-sm flex flex-col">
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-1.5">
                <Activity className="w-4 h-4 text-[#4cd7f6]" />
                <span className="text-xs font-medium text-[#dee2ec]">Hourly Token Throughput</span>
              </div>
              <div className="flex items-center gap-2 text-[10px]">
                <span className="flex items-center gap-1 text-[#aac7ff]"><span className="w-2 h-0.5 bg-[#aac7ff]" /> Input</span>
                <span className="flex items-center gap-1 text-[#4cd7f6]"><span className="w-2 h-0.5 bg-[#4cd7f6]" /> Output</span>
              </div>
            </div>
            <div className="text-[10px] font-['JetBrains_Mono'] text-[#8b91a0] mb-2">Throughput: 184.5k tok/s aggregate peak (15s rolling)</div>
            <div className="flex-1 min-h-[120px]">
              <LineChart data={MOCK_THROUGHPUT_DATA} height={120} colorScheme="primary" showArea dataKey="value" labelKey="label" />
            </div>
            <div className="flex items-center justify-between text-[10px] font-['JetBrains_Mono'] text-[#8b91a0] mt-2">
              <span>14:00</span><span>14:05</span><span>14:10</span><span>Now (14:15)</span>
            </div>
          </div>

          {/* Latency Distribution */}
          <div className="bg-[#171c23] p-3 rounded-lg shadow-sm flex flex-col">
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-1.5">
                <BarChart3 className="w-4 h-4 text-[#aac7ff]" />
                <span className="text-xs font-medium text-[#dee2ec]">End-to-End Latency Distribution</span>
              </div>
              <div className="flex items-center gap-1.5 text-[10px] font-['JetBrains_Mono']">
                <span className="px-1.5 py-0.5 rounded bg-[#252a32] text-[#aac7ff]">P50: 195ms</span>
                <span className="px-1.5 py-0.5 rounded bg-[#252a32] text-[#4cd7f6]">P90: 340ms</span>
                <span className="px-1.5 py-0.5 rounded bg-[#252a32] text-[#ddb7ff]">P99: 680ms</span>
              </div>
            </div>
            <div className="text-[10px] font-['JetBrains_Mono'] text-[#8b91a0] mb-2">Quantile histogram with 99.8% SLO threshold line</div>
            <div className="flex-1 min-h-[120px]">
              <LineChart data={MOCK_THROUGHPUT_DATA.map((d, i) => ({ ...d, value: 85 + i * 3 }))} height={120} colorScheme="primary" dataKey="value" labelKey="label" />
            </div>
            <div className="flex items-center justify-between text-[10px] font-['JetBrains_Mono'] text-[#8b91a0] mt-2">
              <span>0ms</span><span>150ms</span><span>300ms</span><span>600ms+</span>
            </div>
          </div>

          {/* Model Cost Donut */}
          <div className="bg-[#171c23] p-3 rounded-lg shadow-sm flex flex-col">
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-1.5">
                <PieChart className="w-4 h-4 text-[#ddb7ff]" />
                <span className="text-xs font-medium text-[#dee2ec]">Model Cost & Distribution</span>
              </div>
              <span className="px-1.5 py-0.5 rounded bg-[#252a32] text-[#dee2ec] text-[10px] font-['JetBrains_Mono']">$14.28/hr</span>
            </div>
            <div className="flex items-center gap-4 my-auto flex-1">
              <div className="w-24 h-24 shrink-0">
                <DonutChart data={MOCK_COST_DATA} width={96} height={96} innerRadiusRatio={0.6} centerLabel="3 Models" />
              </div>
              <div className="flex-1 space-y-1.5 text-[11px] font-['JetBrains_Mono']">
                {MOCK_COST_DATA.map((d) => (
                  <div key={d.label} className="flex items-center justify-between">
                    <div className="flex items-center gap-1.5">
                      <span className="w-2.5 h-2.5 rounded-sm" style={{ backgroundColor: d.color }} />
                      <span className="text-[#dee2ec]">{d.label}</span>
                    </div>
                    <span className="text-[#8b91a0] font-medium">${(14.28 * d.value / 100).toFixed(2)} <span className="text-[#8b91a0] text-[9px]">({d.value}%)</span></span>
                  </div>
                ))}
              </div>
            </div>
            <div className="mt-2 pt-1 text-[10px] font-['JetBrains_Mono'] text-[#8b91a0] flex items-center justify-between border-t border-[#252a32]">
              <span>Cost Optimization Target: -$2.10/hr with caching</span>
              <span className="text-[#aac7ff] cursor-pointer hover:underline">Inspect Allocations</span>
            </div>
          </div>
        </div>

        {/* Row 2: Model Fleet + Capability Health */}
        <div className="grid grid-cols-12 gap-3">
          {/* Model Fleet Table */}
          <div className="col-span-8 bg-[#171c23] rounded-lg p-3 shadow-sm flex flex-col">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <Activity className="w-4.5 h-4.5 text-[#aac7ff]" />
                <span className="text-base font-semibold text-[#dee2ec]">Active Foundation Model Fleet</span>
                <Badge variant="secondary" className="text-[10px]">{MOCK_MODEL_FLEET.length} Active Handlers</Badge>
              </div>
              <div className="flex items-center gap-2 text-[10px] font-['JetBrains_Mono']">
                <span className="text-[#8b91a0]">Circuit Breakers:</span>
                <span className="px-1.5 py-0.5 rounded bg-[#009eb9]/30 text-[#4cd7f6] font-medium">4 Closed</span>
                <span className="px-1.5 py-0.5 rounded bg-[#93000a]/30 text-[#ffb4ab] font-medium">1 Half-Open</span>
              </div>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-[11px] font-['JetBrains_Mono']">
                <thead>
                  <tr className="text-[#8b91a0] text-[10px] uppercase tracking-wider bg-[#252a32]/40">
                    <th className="py-1.5 px-2">Model Handler</th>
                    <th className="py-1.5 px-2">Provider Endpoint</th>
                    <th className="py-1.5 px-2">Quota Allocated</th>
                    <th className="py-1.5 px-2">TPOT (Out)</th>
                    <th className="py-1.5 px-2">Err Rate</th>
                    <th className="py-1.5 px-2">Circuit Breaker</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-transparent">
                  {MOCK_MODEL_FLEET.map((m) => (
                    <tr key={m.name} className="hover:bg-[#1b2027] transition-colors">
                      <td className="py-2 px-2 flex items-center gap-2">
                        <span className={`w-1.5 h-1.5 rounded-full ${m.breaker === "HALF-OPEN" ? "bg-[#ffb4ab] animate-pulse" : "bg-[#4cd7f6]"}`} />
                        <span className="font-medium text-[#dee2ec]">{m.name}</span>
                      </td>
                      <td className="py-2 px-2 text-[#8b91a0]">{m.provider}</td>
                      <td className="py-2 px-2">
                        <div className="flex items-center gap-1.5">
                          <div className="w-16 h-1.5 bg-[#30353d] rounded-full overflow-hidden">
                            <div className={`h-full rounded-full ${m.breaker === "HALF-OPEN" ? "bg-[#ffb4ab]" : "bg-[#aac7ff]"}`} style={{ width: `${m.quotaPct}%` }} />
                          </div>
                          <span className="text-[#8b91a0] text-[10px]">{m.quotaPct}%</span>
                        </div>
                      </td>
                      <td className={`py-2 px-2 ${m.breaker === "HALF-OPEN" ? "text-[#ffb4ab]" : "text-[#dee2ec]"}`}>{m.tpot}</td>
                      <td className={`py-2 px-2 font-medium ${m.errColor}`}>{m.errRate}</td>
                      <td className="py-2 px-2">
                        <span className={`inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] ${
                          m.breaker === "CLOSED"
                            ? "bg-[#252a32] text-[#4cd7f6]"
                            : "bg-[#93000a]/40 text-[#ffb4ab] font-semibold"
                        }`}>
                          <span className={`w-1.5 h-1.5 rounded-full ${m.breaker === "CLOSED" ? "bg-[#4cd7f6]" : "bg-[#ffb4ab]"}`} />
                          {m.breaker}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Capability Health Matrix */}
          <div className="col-span-4 bg-[#171c23] rounded-lg p-3 shadow-sm flex flex-col">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <ShieldCheck className="w-4.5 h-4.5 text-[#4cd7f6]" />
                <span className="text-base font-semibold text-[#dee2ec]">Capability Health</span>
              </div>
              <span className="text-[10px] font-['JetBrains_Mono'] text-[#4cd7f6]">All Systems Nominal</span>
            </div>
            <div className="grid grid-cols-2 gap-2">
              {MOCK_CAPABILITY_HEALTH.map((c) => (
                <div key={c.name} className={`bg-[#1b2027] p-2 rounded border-l-2 ${c.borderColor}`}>
                  <div className="flex items-center justify-between text-xs mb-1">
                    <span className="text-[#dee2ec]">{c.name}</span>
                    <span className={`w-2 h-2 rounded-full ${c.dotColor}`} />
                  </div>
                  <div className="text-[10px] font-['JetBrains_Mono'] text-[#8b91a0] space-y-0.5">
                    {c.details.map((d, i) => <div key={i}>{d}</div>)}
                    <div className={`${c.textColor} font-medium`}>{c.highlight}</div>
                  </div>
                </div>
              ))}
            </div>
            <div className="mt-3 pt-2 bg-[#252a32]/40 p-1.5 rounded flex items-center justify-between text-[11px] font-['JetBrains_Mono'] text-[#8b91a0]">
              <span>Security Context: SELinux Enforcing</span>
              <span className="flex items-center gap-1 text-[#4cd7f6]">
                <CheckCircle2 className="w-3 h-3" /> Zero CVEs Active
              </span>
            </div>
          </div>
        </div>

        {/* Row 3: Mesh Topology + Waterfall */}
        <div className="grid grid-cols-12 gap-3">
          {/* Multi-Agent Mesh */}
          <div className="col-span-6 bg-[#171c23] rounded-lg p-3 shadow-sm flex flex-col">
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-2">
                <Network className="w-4.5 h-4.5 text-[#aac7ff]" />
                <span className="text-base font-semibold text-[#dee2ec]">Multi-Agent Inter-Service Mesh</span>
              </div>
              <div className="flex items-center gap-1 text-[10px] font-['JetBrains_Mono'] text-[#8b91a0]">
                <span className="w-1.5 h-1.5 rounded-full bg-[#aac7ff] animate-pulse" />
                <span>Live Mesh Topology</span>
              </div>
            </div>
            {/* Mesh Visualization */}
            <div className="w-full h-56 bg-[#0f141b] rounded-lg relative overflow-hidden flex items-center justify-center p-2">
              {/* Grid pattern */}
              <div className="absolute inset-0 opacity-10 bg-[radial-gradient(#dee2ec_1px,transparent_1px)] [background-size:16px_16px]" />
              {/* SVG connections */}
              <svg className="absolute inset-0 w-full h-full pointer-events-none" viewBox="0 0 600 220">
                <path d="M 120 110 C 180 110, 190 60, 260 60" fill="none" stroke="#aac7ff" strokeOpacity="0.7" strokeDasharray="4 2" strokeWidth="2" />
                <path d="M 120 110 C 180 110, 190 160, 260 160" fill="none" stroke="#4cd7f6" strokeOpacity="0.7" strokeWidth="2" />
                <path d="M 380 60 C 420 60, 440 90, 470 100" fill="none" stroke="#aac7ff" strokeOpacity="0.7" strokeWidth="2" />
                <path d="M 380 160 C 420 160, 440 130, 470 120" fill="none" stroke="#4cd7f6" strokeOpacity="0.7" strokeDasharray="3 3" strokeWidth="2" />
                <circle cx="200" cy="80" r="3" fill="#aac7ff" opacity="0.6" />
                <circle cx="430" cy="75" r="2.5" fill="#4cd7f6" opacity="0.6" />
              </svg>
              {/* Nodes */}
              {MOCK_MESH_NODES.map((node) => (
                <div key={node.id} className={`absolute ${node.left} ${node.top} w-36 bg-[#252a32] ${node.borderColor} border-l-2 p-2 rounded shadow-md text-[11px] font-['JetBrains_Mono']`}>
                  <div className={`flex items-center justify-between text-[10px] ${node.textColor} mb-1`}>
                    <span>{node.subtitle}</span>
                    <span className={`w-1.5 h-1.5 rounded-full ${node.dotColor}`} />
                  </div>
                  <div className="font-medium text-[#dee2ec] truncate">{node.label}</div>
                  <div className="text-[9px] text-[#8b91a0] mt-1 flex justify-between">
                    <span>{node.meta1}</span>
                    <span className={node.textColor}>{node.meta2}</span>
                  </div>
                </div>
              ))}
            </div>
            <div className="flex items-center justify-between text-[10px] font-['JetBrains_Mono'] text-[#8b91a0] mt-2">
              <span>Active In-Flight Graph Traversal: 84 instances</span>
              <span className="text-[#4cd7f6]">Zero deadlocks detected</span>
            </div>
          </div>

          {/* Waterfall Trace */}
          <div className="col-span-6 bg-[#171c23] rounded-lg p-3 shadow-sm flex flex-col">
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-2">
                <Layers className="w-4.5 h-4.5 text-[#4cd7f6]" />
                <span className="text-base font-semibold text-[#dee2ec]">Span Waterfall Trace (af-7f89b)</span>
              </div>
              <span className="px-2 py-0.5 rounded bg-[#252a32] text-[#aac7ff] text-[10px] font-['JetBrains_Mono']">Duration: 284ms</span>
            </div>
            <div className="flex-1">
              <WaterfallChart data={MOCK_WATERFALL_SPANS} height={180} />
            </div>
            <div className="flex items-center justify-between text-[10px] font-['JetBrains_Mono'] text-[#8b91a0] mt-1">
              <span>Spans Captured: 14</span>
              <span className="hover:text-[#aac7ff] cursor-pointer">Export OpenTelemetry JSON</span>
            </div>
          </div>
        </div>
      </div>

      {/* Bottom Panel: OTel Stream */}
      <div className="bg-[#171c23] rounded-lg shadow-lg overflow-hidden flex flex-col shrink-0 mx-3 mb-3">
        {/* Terminal Header */}
        <div className="h-9 bg-[#252a32] px-3 flex items-center justify-between">
          <div className="flex items-center gap-3 h-full text-[11px] font-['JetBrains_Mono']">
            <button className="flex items-center gap-1.5 px-2 h-full bg-[#171c23] text-[#dee2ec] font-medium border-t-2 border-[#aac7ff]">
              <Terminal className="w-3.5 h-3.5 text-[#4cd7f6]" />
              <span>Real-time OTel Distributed Tracing Stream</span>
              <span className="w-2 h-2 rounded-full bg-[#4cd7f6] animate-pulse" />
            </button>
            <button className="flex items-center gap-1.5 px-2 h-full text-[#8b91a0] hover:text-[#dee2ec] transition-colors">
              <span>Kafka Pipeline (Lag: 42ms)</span>
            </button>
            <button className="flex items-center gap-1.5 px-2 h-full text-[#8b91a0] hover:text-[#dee2ec] transition-colors">
              <span>Trace Errors (0)</span>
            </button>
          </div>
          <div className="flex items-center gap-3 text-[11px] font-['JetBrains_Mono'] text-[#8b91a0]">
            <span className="flex items-center gap-1 text-[#dee2ec]"><span className="w-1.5 h-1.5 rounded-full bg-[#aac7ff]" /> OTel Collector: 1.28.0</span>
            <span>Buffer: 99.8% Healthy</span>
          </div>
        </div>
        {/* Log Stream */}
        <div className="p-2 bg-[#090f15] text-[11px] font-['JetBrains_Mono'] space-y-1 overflow-x-auto max-h-48 overflow-y-auto">
          {MOCK_OTEL_LOGS.map((log, i) => (
            <div key={i} className="flex items-center gap-2 hover:bg-[#1b2027]/40 px-1 py-0.5 rounded text-[#8b91a0]">
              <span className="text-[#8b91a0]">{log.time}</span>
              <span className={`px-1 rounded text-[10px] ${log.levelColor}`}>{log.level}</span>
              <span className={log.sourceColor}>{log.source}</span>
              <span className="text-[#dee2ec]">{log.message}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// ── Sub-components ──────────────────────────────────────────────────────────

function MetricCard({ label, value, unit, icon, trend, trendUp, trendColor, meta, metaHighlight, metaRight }: {
  label: string;
  value: string;
  unit: string;
  icon: React.ReactNode;
  trend?: React.ReactNode;
  trendUp?: boolean;
  trendColor?: string;
  meta?: React.ReactNode;
  metaHighlight?: string;
  metaRight?: string;
}) {
  return (
    <div className="bg-[#171c23] p-2.5 rounded-lg shadow-sm hover:bg-[#1b2027] transition-all">
      <div className="flex items-center justify-between text-[#8b91a0] text-[10px] uppercase tracking-wider mb-1">
        <span>{label}</span>
        {icon}
      </div>
      <div className="flex items-baseline gap-1.5">
        <span className="text-[28px] leading-[34px] font-semibold tracking-tight text-[#dee2ec]">{value}</span>
        {unit && <span className="text-[10px] text-[#8b91a0]">{unit}</span>}
      </div>
      {trend && (
        <div className="flex items-center justify-between mt-2 pt-1.5 text-[10px]">
          <span className={`font-['JetBrains_Mono'] ${trendColor ?? "text-[#8b91a0]"}`}>
            {trendUp && <TrendingUp className="w-3 h-3 inline mr-0.5" />}
            {trend}
          </span>
          {meta && <span className={`font-['JetBrains_Mono'] ${metaHighlight ?? "text-[#8b91a0]"}`}>{meta}</span>}
        </div>
      )}
      {metaRight && (
        <div className="flex items-center justify-end mt-1">
          <span className={`font-['JetBrains_Mono'] text-[10px] ${metaHighlight ?? "text-[#8b91a0]"}`}>{metaRight}</span>
        </div>
      )}
    </div>
  );
}

import { useState, useEffect, useMemo } from "react";
import { useApiClient } from "../hooks/use-api-client.js";
import {
  Badge,
  MetricCard,
} from "@agentforge/ui";
import type { MetricCardProps } from "@agentforge/ui";
import {
  Search,
  Copy,
} from "lucide-react";

// ── Mock data ───────────────────────────────────────────────────────────────

interface CapabilityItem {
  id: string;
  name: string;
  type: "eBPF" | "Docker" | "LLM" | "Memory" | "HTTP" | "MCP";
  status: "active" | "healthy" | "degraded" | "inactive";
  category: "KERNEL" | "SANDBOX" | "SECURITY" | "INTEL";
  tier: string;
  latency: string;
  description: string;
}

const MOCK_CAPABILITIES: CapabilityItem[] = [
  { id: "cap-ebpf-quarantine.v1", name: "cap.ebpf.quarantine.v1", type: "eBPF", status: "active", category: "KERNEL", tier: "TIER_4_CRITICAL", latency: "0.18ms", description: "Isolate malicious network sockets via XDP/TC filters & cgroup ringbuffers" },
  { id: "cap-container-snapshot.v2", name: "cap.container.snapshot.v2", type: "Docker", status: "healthy", category: "SANDBOX", tier: "TIER_2_STRICT", latency: "4.80ms", description: "Copy-on-Write memory state dump and checkpointing for fork operations" },
  { id: "cap-vault-secret-rotate.v1", name: "cap.vault.secret_rotate.v1", type: "HTTP", status: "healthy", category: "SECURITY", tier: "TIER_3_RESTRICTED", latency: "18.2ms", description: "Ephemeral token generation and zero-knowledge KMS cryptographic rotation" },
  { id: "cap-vector-dense-ann.v3", name: "cap.vector.dense_ann.v3", type: "Memory", status: "healthy", category: "INTEL", tier: "TIER_1_STANDARD", latency: "2.15ms", description: "HNSW vector index search with SIMD-accelerated distance dot calculations" },
  { id: "cap-llm-inference.v2", name: "cap.llm.inference.v2", type: "LLM", status: "active", category: "INTEL", tier: "TIER_2_STRICT", latency: "382ms", description: "Multi-model LLM inference with fallback chain and token budget enforcement" },
  { id: "cap-mcp-registry.v1", name: "cap.mcp.registry.v1", type: "MCP", status: "healthy", category: "INTEL", tier: "TIER_1_STANDARD", latency: "12ms", description: "MCP server registry with tool discovery, health probing, and schema validation" },
];

const MOCK_CODE = `import { CapabilitySpec, SecurityClearance } from '@agentforge/core-capabilities';
import { XdpSocketHook, KernelRingBuffer } from '@agentforge/ebpf-runtime';

// §7.2 Runtime Binding for Kernel Socket Isolation
export const EbpfQuarantineCapability: CapabilitySpec = {
  urn: 'urn:forge:cap:kernel:ebpf:quarantine:v1',
  version: '1.4.2-ebpf',
  provider: {
    type: 'KERNEL_EBPF_FILTER',
    kernelAttachPoint: 'xdp_ingress_prog_socket_isolate',
    driverInterface: 'tc_bpf_redirect_quarantine'
  },
  securityClearance: SecurityClearance.TIER_4_CRITICAL,
  schema: {
    inputs: {
      socket_fd: { type: 'number', required: true },
      pid: { type: 'number', required: true },
      protocol: { type: 'enum', values: ['TCP', 'UDP', 'RAW_IP'] },
      rule_id: { type: 'string', format: 'uuid-v4' }
    },
    outputs: {
      isolation_status: { type: 'enum', values: ['APPLIED', 'BYPASS_REJECTED'] },
      dropped_packets: { type: 'counter_u64' }
    }
  },
  policy: { timeoutMs: 250, maxRetries: 3, backoff: 'exponential' },
};`;

// ── Sub-components ──────────────────────────────────────────────────────────

function CapabilityCatalog({
  items,
  selectedId,
  onSelect,
  searchQuery,
  onSearchChange,
  activeFilter,
  onFilterChange,
}: {
  items: CapabilityItem[];
  selectedId: string | null;
  onSelect: (id: string) => void;
  searchQuery: string;
  onSearchChange: (q: string) => void;
  activeFilter: string;
  onFilterChange: (f: string) => void;
}) {
  const filters = ["ALL", "eBPF", "Docker", "LLM", "Memory", "MCP"];
  const typeCounts: Record<string, number> = useMemo(() => {
    const counts: Record<string, number> = { ALL: items.length };
    items.forEach((item) => { counts[item.type] = (counts[item.type] ?? 0) + 1; });
    return counts;
  }, [items]);

  const filtered = useMemo(() => {
    let result = items;
    if (activeFilter !== "ALL") result = result.filter((i) => i.type === activeFilter);
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      result = result.filter((i) => i.name.toLowerCase().includes(q) || i.description.toLowerCase().includes(q));
    }
    return result;
  }, [items, activeFilter, searchQuery]);

  const statusBadge: Record<string, { label: string; className: string }> = {
    active: { label: "ACTIVE", className: "bg-primary/20 text-primary border-primary/30" },
    healthy: { label: "HEALTHY", className: "bg-success/20 text-success border-success/30" },
    degraded: { label: "DEGRADED", className: "bg-amber-400/20 text-amber-400 border-amber-400/30" },
    inactive: { label: "INACTIVE", className: "bg-surface-container-high text-outline border-outline-variant" },
  };

  const categoryColor: Record<string, string> = {
    KERNEL: "text-error",
    SANDBOX: "text-tertiary",
    SECURITY: "text-accent-tool",
    INTEL: "text-primary",
  };

  return (
    <div className="w-72 bg-surface-container-lowest border-r border-outline-variant flex flex-col overflow-hidden shrink-0">
      {/* Header */}
      <div className="flex items-center justify-between px-3 py-2 border-b border-outline-variant">
        <span className="text-[12px] font-label-sm font-semibold text-on-surface uppercase tracking-wider">
          Registered Catalog ({items.length})
        </span>
        <Badge variant="outline" className="text-[10px]">v2.4-eBPF</Badge>
      </div>

      {/* Search */}
      <div className="px-2 py-2 border-b border-outline-variant">
        <div className="flex items-center gap-1.5 px-2 py-1 rounded bg-surface border border-outline-variant">
          <Search className="h-3 w-3 text-outline" />
          <input
            className="flex-1 bg-transparent text-[11px] font-code-sm text-on-surface placeholder:text-outline outline-none"
            placeholder="quarantine"
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
          />
        </div>
      </div>

      {/* Filter tabs */}
      <div className="flex items-center gap-1 px-2 py-1.5 border-b border-outline-variant overflow-x-auto">
        {filters.map((f) => (
          <button
            key={f}
            className={`px-2 py-0.5 rounded text-[10px] font-code-sm whitespace-nowrap transition-colors ${
              activeFilter === f
                ? "bg-accent-primary text-on-primary"
                : "bg-surface-container text-outline hover:bg-surface-container-high"
            }`}
            onClick={() => onFilterChange(f)}
          >
            {f} {f !== "ALL" && `(${typeCounts[f] ?? 0})`}
          </button>
        ))}
      </div>

      {/* Capability list */}
      <div className="flex-1 overflow-auto p-1.5 space-y-1">
        {filtered.map((cap) => {
          const badge = statusBadge[cap.status];
          const isSelected = selectedId === cap.id;
          return (
            <button
              key={cap.id}
              className={`w-full text-left rounded border p-2.5 transition-colors ${
                isSelected
                  ? "border-accent-primary bg-surface-container-high"
                  : "border-outline-variant bg-surface hover:border-outline hover:bg-surface-container-high/50"
              }`}
              onClick={() => onSelect(cap.id)}
            >
              <div className="flex items-center justify-between mb-1">
                <span className="text-[11px] font-code-sm font-medium text-on-surface truncate">{cap.name}</span>
                <div className="flex items-center gap-1 shrink-0">
                  <span className={`text-[9px] font-code-sm ${categoryColor[cap.category]}`}>{cap.category}</span>
                  <Badge variant="outline" className={`text-[9px] ${badge.className}`}>{badge.label}</Badge>
                </div>
              </div>
              <p className="text-[10px] font-body-md text-outline leading-tight line-clamp-2 mb-1.5">{cap.description}</p>
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-code-sm text-outline">LATENCY: {cap.latency}</span>
                <span className="text-[10px] font-code-sm text-outline">{cap.tier}</span>
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
}

function CapabilityCodeViewer({ capabilityId }: { capabilityId: string | null }) {
  if (!capabilityId) {
    return (
      <div className="flex-1 flex items-center justify-center bg-surface min-w-0">
        <span className="text-[12px] text-outline">Select a capability to view code</span>
      </div>
    );
  }

  return (
    <div className="flex-1 flex flex-col overflow-hidden min-w-0">
      {/* Header */}
      <div className="flex items-center justify-between px-3 py-1.5 border-b border-outline-variant bg-surface-container-lowest shrink-0">
        <div className="flex items-center gap-2">
          <span className="text-[12px] font-code-sm text-on-surface font-medium">{capabilityId}</span>
          <span className="text-[10px] font-code-sm text-outline">TypeScript Strict AST</span>
        </div>
        <div className="flex items-center gap-1.5">
          <Badge variant="outline" className="text-[10px]">ReadOnly Runtime Binding</Badge>
          <button className="p-1 rounded hover:bg-surface-container-high text-outline">
            <Copy className="h-3 w-3" />
          </button>
        </div>
      </div>

      {/* Code */}
      <div className="flex-1 overflow-auto bg-surface p-3">
        <pre className="text-[12px] font-code-sm leading-relaxed">
          {MOCK_CODE.split("\n").map((line, i) => (
            <div key={i} className="flex hover:bg-surface-container-high/30">
              <span className="text-outline/40 w-6 shrink-0 text-right mr-3 select-none text-[11px]">{String(i + 1).padStart(2, "0")}</span>
              <span
                className={
                  line.includes("import ") || line.includes("from ")
                    ? "text-secondary"
                    : line.includes("export const") || line.includes("CapabilitySpec")
                    ? "text-primary"
                    : line.includes("//")
                    ? "text-outline"
                    : line.includes("'") || line.includes('"')
                    ? "text-success"
                    : line.includes("TIER_4_CRITICAL") || line.includes("TIER_")
                    ? "text-error"
                    : "text-on-surface"
                }
              >
                {line || " "}
              </span>
            </div>
          ))}
        </pre>
      </div>
    </div>
  );
}

function CapabilityMetrics() {
  const metrics: MetricCardProps[] = [
    { title: "ACTIVE CAPABILITIES", value: "24", status: "success", trend: "up", trendValue: "100% Registered" },
    { title: "24H INVOCATIONS", value: "1.84M", unit: "calls logged", status: "neutral", sparklineData: [120, 135, 128, 142, 155, 148, 160, 175, 162, 184], sparklineColor: "var(--color-primary)" },
    { title: "SECURITY POLICY CLEARANCE", value: "Zero-Trust", status: "success", trend: "flat", trendValue: "Tier-4 Jail" },
    { title: "HEALTH & SLA MATRIX", value: "99.99%", unit: "Operational", status: "success", sparklineData: [99.9, 99.95, 99.97, 99.98, 99.99, 99.99, 99.99, 99.99, 99.99, 99.99], sparklineColor: "var(--color-success)" },
  ];

  return (
    <div className="grid grid-cols-4 gap-2 shrink-0">
      {metrics.map((m) => (
        <MetricCard key={m.title} {...m} />
      ))}
    </div>
  );
}

// ── Main Page ───────────────────────────────────────────────────────────────

export function CapabilityWorkspacePage() {
  const api = useApiClient();
  const [caps, setCaps] = useState<CapabilityItem[]>(MOCK_CAPABILITIES);
  const [selectedId, setSelectedId] = useState<string | null>("cap-ebpf-quarantine.v1");
  const [searchQuery, setSearchQuery] = useState("");
  const [activeFilter, setActiveFilter] = useState("ALL");

  // Fetch real data (fallback to mock)
  useEffect(() => {
    api.request<any[]>("GET", "/api/v1/capabilities")
      .then((data) => {
        if (data.length > 0) {
          setCaps(
            data.map((cap: any) => ({
              id: cap.id,
              name: cap.name ?? cap.id,
              type: cap.type ?? "HTTP",
              status: cap.status === "ACTIVE" ? "active" : "healthy",
              category: "INTEL",
              tier: "TIER_1_STANDARD",
              latency: "-",
              description: cap.description ?? "",
            })),
          );
        }
      })
      .catch(() => {});
  }, [api]);

  return (
    <div className="flex-1 flex flex-col overflow-hidden bg-surface p-3 gap-3">
      {/* Metric cards */}
      <CapabilityMetrics />

      {/* Two-column layout */}
      <div className="flex flex-1 overflow-hidden min-h-0">
        <CapabilityCatalog
          items={caps}
          selectedId={selectedId}
          onSelect={setSelectedId}
          searchQuery={searchQuery}
          onSearchChange={setSearchQuery}
          activeFilter={activeFilter}
          onFilterChange={setActiveFilter}
        />
        <CapabilityCodeViewer capabilityId={selectedId} />
      </div>
    </div>
  );
}

import { useState } from "react";
import {
  Badge,
  StatusLight,
  ProgressBar,
  Tabs,
  TabsList,
  TabsTrigger,
  Separator,
} from "@agentforge/ui";
import {
  Search,
  Shield,
  Lock,
  CheckCircle2,
  ChevronDown,
  ChevronRight,
  AlertCircle,
  Zap,
} from "lucide-react";

// ── Mock data ───────────────────────────────────────────────────────────────

interface SkillGroup {
  id: string;
  label: string;
  count: number;
  children: { id: string; name: string; version: string; status: "active" | "live" | "draft" }[];
}

const MOCK_SKILL_GROUPS: SkillGroup[] = [
  {
    id: "devops",
    label: "DEVOPS & INFRA",
    count: 7,
    children: [
      { id: "redis-cache", name: "redis-cache", version: "v1.2", status: "active" },
      { id: "ingress-cev", name: "ingress-cev", version: "v3.0", status: "active" },
    ],
  },
  {
    id: "security",
    label: "SECURITY & REMEDIATION",
    count: 6,
    children: [
      { id: "k8s-isolate-guard", name: "k8s-container-isolate-guard", version: "v2.4.1", status: "live" },
      { id: "ebpf-netw", name: "ebpf-netw", version: "v2.1", status: "active" },
      { id: "iam-priv", name: "iam-priv", version: "v0.9", status: "draft" },
    ],
  },
  {
    id: "data",
    label: "DATA & ANALYTICS",
    count: 5,
    children: [
      { id: "vector-ann", name: "vector-ann", version: "v3.0", status: "active" },
    ],
  },
];

const MOCK_CONTRACT = `{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "skillId": "k8s-container-isolate-guard",
  "version": "2.4.1",
  "executionPolicy": {
    "timeoutMs": 15000,
    "retryBackoff": { "strategy": "exponential", "maxRetries": 3 }
  },
  "parameters": {
    "type": "object",
    "required": ["targetPod", "namespace", "isolationMethod"],
    "properties": {
      "targetPod": { "type": "string", "pattern": "^[a-z0-9-]+$" },
      "namespace": { "type": "string", "default": "production-critical" },
      "isolationMethod": {
        "type": "string",
        "enum": ["NETWORK_POLICY_DENY_ALL", "CRI_FREEZE", "TAINT_DRAIN"]
      }
    }
  },
  "response": {
    "type": "object",
    "properties": {
      "quarantinePolicyApplied": { "type": "boolean" },
      "executionProof": { "type": "string" }
    }
  }
}`;

const MOCK_TEST_RESULTS = [
  { id: "TEST-01..09", label: "Parameter schema adherence check", status: "pass" as const },
  { id: "TEST-10", label: "Enclave secret inject KUBE_CONFIG_TOKEN: Mask verified", status: "pass" as const },
  { id: "TEST-11..13", label: "Mock CRI Network Isolation Policy applied: Status HTTP/2 201 Created", status: "pass" as const },
  { id: "TEST-14", label: "Assertion: output.quarantinePolicyApplied === true (Time: 24.18ms)", status: "check" as const },
];

// ── Sub-components ──────────────────────────────────────────────────────────

function SkillRegistryTree({
  groups,
  selectedId,
  onSelect,
  searchQuery,
  onSearchChange,
}: {
  groups: SkillGroup[];
  selectedId: string | null;
  onSelect: (id: string) => void;
  searchQuery: string;
  onSearchChange: (q: string) => void;
}) {
  const [expanded, setExpanded] = useState<Record<string, boolean>>({ devops: true, security: true });
  const totalSkills = groups.reduce((sum, g) => sum + g.count, 0);

  const toggleGroup = (id: string) => setExpanded((prev) => ({ ...prev, [id]: !prev[id] }));

  const statusBadge: Record<string, { label: string; className: string }> = {
    live: { label: "live", className: "bg-accent-primary text-on-primary text-[9px]" },
    active: { label: "active", className: "bg-success/20 text-success text-[9px]" },
    draft: { label: "draft", className: "bg-surface-container-high text-outline text-[9px]" },
  };

  return (
    <div className="w-64 bg-surface-container-lowest border-r border-outline-variant flex flex-col overflow-hidden shrink-0">
      {/* Header */}
      <div className="px-3 py-2 border-b border-outline-variant">
        <div className="flex items-center justify-between mb-2">
          <span className="text-[11px] font-label-sm font-semibold text-on-surface uppercase tracking-wider">
            Skill Registry
          </span>
          <Badge className="bg-accent-primary/20 text-accent-primary text-[10px]">{totalSkills} total</Badge>
        </div>
        <div className="flex items-center gap-1.5 px-2 py-1 rounded bg-surface border border-outline-variant">
          <Search className="h-3 w-3 text-outline" />
          <input
            className="flex-1 bg-transparent text-[11px] font-code-sm text-on-surface placeholder:text-outline outline-none"
            placeholder="Filter skills"
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
          />
        </div>
      </div>

      {/* Tree */}
      <div className="flex-1 overflow-auto p-1.5 space-y-1">
        {groups.map((group) => (
          <div key={group.id}>
            <button
              className="flex items-center gap-1.5 w-full px-1 py-1 text-[10px] font-code-sm text-outline uppercase tracking-wider hover:text-on-surface transition-colors"
              onClick={() => toggleGroup(group.id)}
            >
              {expanded[group.id] ? <ChevronDown className="h-3 w-3" /> : <ChevronRight className="h-3 w-3" />}
              {group.label} ({group.count})
            </button>
            {expanded[group.id] && (
              <div className="ml-2 space-y-0.5">
                {group.children.map((skill) => {
                  const badge = statusBadge[skill.status];
                  const isSelected = selectedId === skill.id;
                  return (
                    <button
                      key={skill.id}
                      className={`flex items-center gap-1.5 w-full px-2 py-1.5 rounded text-[11px] font-body-md transition-colors ${
                        isSelected
                          ? "bg-accent-primary text-on-primary"
                          : "text-on-surface hover:bg-surface-container-high"
                      }`}
                      onClick={() => onSelect(skill.id)}
                    >
                      <Shield className="h-3 w-3 shrink-0" />
                      <span className="truncate">{skill.name}</span>
                      <Badge variant="outline" className={`ml-auto shrink-0 ${badge.className}`}>{badge.label}</Badge>
                    </button>
                  );
                })}
              </div>
            )}
          </div>
        ))}
      </div>

      {/* Schema sync + Execution target */}
      <div className="border-t border-outline-variant p-2.5 space-y-2">
        <div className="flex items-center justify-between">
          <span className="text-[10px] font-code-sm text-outline">Schema Sync</span>
          <span className="text-[10px] font-code-sm text-success flex items-center gap-1">
            <StatusLight status="success" size="sm" />Up-to-date
          </span>
        </div>
        <div className="space-y-1">
          <div>
            <span className="text-[10px] font-code-sm text-outline">Execution Target</span>
            <p className="text-[11px] font-code-sm text-on-surface">Clusterprod-us-east-1</p>
          </div>
          <div>
            <span className="text-[10px] font-code-sm text-outline">Namespace</span>
            <p className="text-[11px] font-code-sm text-on-surface">sec-runtime</p>
          </div>
          <div>
            <span className="text-[10px] font-code-sm text-outline">Engine Runtime</span>
            <p className="text-[11px] font-code-sm text-primary">MCP / v1.4-beta</p>
          </div>
        </div>
      </div>
    </div>
  );
}

function ContractEditor() {
  return (
    <div className="flex-1 flex flex-col overflow-hidden min-w-0">
      {/* Header */}
      <div className="flex items-center justify-between px-3 py-1.5 border-b border-outline-variant bg-surface-container-lowest shrink-0">
        <div className="flex items-center gap-2">
          <span className="text-[12px] font-code-sm text-on-surface font-medium">k8s-isolate-guard.contract.json</span>
          <Badge variant="outline" className="text-[10px]">OpenAPI 3.1.0</Badge>
          <span className="text-[10px] font-code-sm text-outline">Strict</span>
        </div>
        <div className="flex items-center gap-2 text-[10px] font-code-sm">
          <span className="text-outline">TIMEOUT:</span>
          <span className="text-primary">15,000ms</span>
          <span className="text-outline">RETRY:</span>
          <span className="text-on-surface">exp(max:3)</span>
        </div>
      </div>

      {/* Code */}
      <div className="flex-1 overflow-auto bg-surface p-3">
        <pre className="text-[12px] font-code-sm leading-relaxed">
          {MOCK_CONTRACT.split("\n").map((line, i) => (
            <div key={i} className="flex hover:bg-surface-container-high/30">
              <span className="text-outline/40 w-6 shrink-0 text-right mr-3 select-none text-[11px]">{String(i + 1).padStart(2, "0")}</span>
              <span
                className={
                  line.includes('"$schema"') || line.includes('"skillId"') || line.includes('"version"')
                    ? "text-primary"
                    : line.includes('"type"') || line.includes('"required"') || line.includes('"properties"') || line.includes('"enum"')
                    ? "text-secondary"
                    : line.includes('"string"') || line.includes('"boolean"') || line.includes('"object"') || line.includes('"number"')
                    ? "text-tertiary"
                    : line.includes('"NETWORK_POLICY') || line.includes('"CRI_FREEZE') || line.includes('"TAINT_DRAIN') || line.includes('"production-critical"')
                    ? "text-success"
                    : line.includes('"exponential"') || line.includes('15000') || line.includes('3')
                    ? "text-on-surface"
                    : "text-outline/70"
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

function SecurityGovernancePanel() {
  return (
    <div className="w-72 bg-surface-container-lowest border-l border-outline-variant flex flex-col overflow-auto shrink-0">
      {/* Header */}
      <div className="px-3 py-2 border-b border-outline-variant">
        <span className="text-[11px] font-label-sm font-semibold text-on-surface uppercase tracking-wider">
          Security & Governance Enclave
        </span>
      </div>

      <div className="p-3 space-y-4">
        {/* Compliance */}
        <div className="rounded border border-success/30 bg-success/10 p-2.5">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-code-sm text-success font-semibold">SOX / ISO 27001</span>
            <Badge className="bg-success text-white text-[10px]">Passed</Badge>
          </div>
        </div>

        {/* Privileged Capability */}
        <div className="space-y-1.5">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-code-sm text-outline uppercase">Privileged Capability</span>
            <span className="text-[11px] font-code-sm text-error font-medium">TRUE (L4 K8s HOOK)</span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-code-sm text-outline uppercase">Required RBAC Role</span>
            <Badge variant="outline" className="text-[10px] text-accent-tool border-accent-tool/30">ROLE_SECURITY_ADMIN</Badge>
          </div>
        </div>

        <Separator />

        {/* Secret Injection */}
        <div>
          <div className="flex items-center justify-between mb-2">
            <span className="text-[10px] font-code-sm text-outline uppercase">Secret Injection & Vault Bindings</span>
            <span className="text-[10px] font-code-sm text-tertiary flex items-center gap-1">
              <Lock className="h-3 w-3" />Enclave Sealed
            </span>
          </div>

          <div className="space-y-2">
            <div className="rounded border border-outline-variant bg-surface p-2">
              <div className="flex items-center justify-between mb-0.5">
                <span className="text-[11px] font-code-sm font-semibold text-primary">KUBE_CONFIG_TOKEN</span>
                <span className="text-[9px] font-code-sm text-outline">Masked</span>
              </div>
              <p className="text-[10px] font-code-sm text-outline">Vault Mount: /v1/secret/creds/k8s-secops</p>
              <p className="text-[10px] font-code-sm text-on-surface mt-0.5">••••••••••••••••••••</p>
            </div>

            <div className="rounded border border-outline-variant bg-surface p-2">
              <div className="flex items-center justify-between mb-0.5">
                <span className="text-[11px] font-code-sm font-semibold text-primary">CRI_SOCKET_CERT_PEM</span>
                <span className="text-[9px] font-code-sm text-outline">Masked</span>
              </div>
              <p className="text-[10px] font-code-sm text-outline">Vault Mount: /v1/secret/certs/cri-runtime</p>
              <p className="text-[10px] font-code-sm text-on-surface mt-0.5">••••••••••••••••••••</p>
            </div>
          </div>

          <div className="flex items-start gap-1.5 mt-2 p-1.5 rounded bg-surface-container-high/50">
            <AlertCircle className="h-3 w-3 text-outline shrink-0 mt-0.5" />
            <p className="text-[10px] font-code-sm text-outline leading-tight">
              Tokens strictly injected at sub-process invocation. Plaintext keys never touch browser DOM.
            </p>
          </div>
        </div>

        <Separator />

        {/* Governance Ledger */}
        <div className="space-y-1">
          <span className="text-[10px] font-code-sm text-outline uppercase">Governance Ledger Signed</span>
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-code-sm text-on-surface">SHA256: 3a7f804</span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-code-sm text-outline">Last Authorized By:</span>
            <span className="text-[11px] font-code-sm text-primary">sec-lead@infra-corp.internal</span>
          </div>
        </div>

        <Separator />

        {/* SLO Profile */}
        <div>
          <span className="text-[10px] font-code-sm text-outline uppercase">SLO & Resiliency Profile</span>
          <div className="flex items-center justify-between mt-1">
            <span className="text-[11px] font-code-sm text-on-surface">Max Allowed Runtime</span>
            <span className="text-[13px] font-code-sm text-on-surface font-medium">15,000ms</span>
          </div>
          <ProgressBar value={35} color="primary" size="sm" className="mt-1" />

          <div className="grid grid-cols-3 gap-2 mt-2">
            <div className="rounded border border-outline-variant bg-surface p-1.5 text-center">
              <p className="text-[9px] font-code-sm text-outline">p50 Latency</p>
              <p className="text-[12px] font-code-sm text-on-surface font-medium">18ms</p>
            </div>
            <div className="rounded border border-outline-variant bg-surface p-1.5 text-center">
              <p className="text-[9px] font-code-sm text-outline">p95 Latency</p>
              <p className="text-[12px] font-code-sm text-on-surface font-medium">24ms</p>
            </div>
            <div className="rounded border border-outline-variant bg-surface p-1.5 text-center">
              <p className="text-[9px] font-code-sm text-outline">Circuit Breaker</p>
              <p className="text-[12px] font-code-sm text-error font-medium">Auto-trip</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function TestRunnerPanel() {
  const coverage = 98.4;

  return (
    <div className="border-t border-outline-variant bg-surface-container-lowest p-3 shrink-0">
      <div className="grid grid-cols-[1fr_1.5fr] gap-4">
        {/* Mock payload */}
        <div className="rounded border border-outline-variant bg-surface p-3">
          <div className="flex items-center justify-between mb-2">
            <span className="text-[10px] font-code-sm text-outline uppercase">Active Test Fixture</span>
            <div className="flex items-center gap-1.5">
              <button className="text-[10px] font-code-sm text-primary hover:underline">EDIT</button>
              <button className="text-[10px] font-code-sm text-primary hover:underline">PAYLOAD</button>
            </div>
          </div>
          <span className="text-[10px] font-code-sm text-on-surface">MOCK_CONTAINMENT_01.JSON</span>
          <pre className="text-[10px] font-code-sm text-on-surface mt-1.5 leading-relaxed overflow-auto max-h-24">
{`{
  "targetPod": "payment-auth-daemon-89bc9f-z",
  "namespace": "production-critical",
  "isolationMethod": "NETWORK_POLICY_DENY_ALL",
  "dryRun": false
}`}
          </pre>
        </div>

        {/* Test results */}
        <div className="rounded border border-outline-variant bg-surface p-3">
          <div className="space-y-1.5">
            {MOCK_TEST_RESULTS.map((result) => (
              <div key={result.id} className="flex items-center gap-1.5">
                {result.status === "check" ? (
                  <CheckCircle2 className="h-3 w-3 text-success shrink-0" />
                ) : (
                  <CheckCircle2 className="h-3 w-3 text-success shrink-0" />
                )}
                <span className="text-[10px] font-code-sm text-outline shrink-0">[{result.id}]</span>
                <span className="text-[11px] font-code-sm text-on-surface">{result.label}</span>
              </div>
            ))}
          </div>
          <div className="flex items-center justify-between mt-2 pt-2 border-t border-outline-variant">
            <span className="text-[10px] font-code-sm text-outline flex items-center gap-1">
              <Zap className="h-3 w-3" />Warm Mock Runner (Zero external network egress)
            </span>
            <span className="text-[11px] font-code-sm text-success font-medium">Suite Coverage: {coverage}%</span>
          </div>
        </div>
      </div>
    </div>
  );
}

// ── Main Page ───────────────────────────────────────────────────────────────

export function SkillWorkspacePage() {
  const [selectedId, setSelectedId] = useState<string | null>("k8s-isolate-guard");
  const [searchQuery, setSearchQuery] = useState("");
  const [activeTab, setActiveTab] = useState<"contract" | "security" | "evaluation">("contract");

  return (
    <div className="flex-1 flex flex-col overflow-hidden bg-surface">
      {/* Top tabs */}
      <div className="flex items-center gap-2 px-3 py-1.5 border-b border-outline-variant bg-surface-container-lowest shrink-0">
        <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as typeof activeTab)}>
          <TabsList className="h-7">
            <TabsTrigger value="contract" className="text-[11px]">Contract (OpenAPI 3.1)</TabsTrigger>
            <TabsTrigger value="security" className="text-[11px]">Security</TabsTrigger>
            <TabsTrigger value="evaluation" className="text-[11px]">Evaluation</TabsTrigger>
          </TabsList>
        </Tabs>
        <div className="ml-auto flex items-center gap-2 text-[10px] font-code-sm text-outline">
          <span>Spec Hash: c69f..9e20</span>
          <Badge variant="outline" className="text-[10px]">Autosave ON</Badge>
        </div>
      </div>

      {/* Three-column layout */}
      <div className="flex flex-1 overflow-hidden min-h-0">
        <SkillRegistryTree
          groups={MOCK_SKILL_GROUPS}
          selectedId={selectedId}
          onSelect={setSelectedId}
          searchQuery={searchQuery}
          onSearchChange={setSearchQuery}
        />

        <ContractEditor />

        <SecurityGovernancePanel />
      </div>

      {/* Test runner */}
      <TestRunnerPanel />
    </div>
  );
}

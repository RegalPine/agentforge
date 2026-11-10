import { useState } from "react";
import {
  CodeEditor,
  RadarChart,
} from "@agentforge/ui";
import type { RadarAxis, RadarDataset } from "@agentforge/ui";
import {
  Shield,
  Verified,
  TrendingDown,
  Ban,
  CheckSquare,
  CheckCircle2,
  Lock,
  CreditCard,
  Cpu,
  Award,
  ChevronRight,
  ChevronDown,
  Search,
  Filter,
  Plus,
  Bolt,
  Save,
  Code,
  Terminal,
  FileText,
  Wrench,
} from "lucide-react";

// ── Mock data ───────────────────────────────────────────────────────────────

interface PolicyRule {
  id: string;
  name: string;
  severity: "CRITICAL" | "HIGH" | "MEDIUM";
  type: "STRICT_DENY" | "WARN" | "HITL_REQUIRE";
  blocks: string;
  icon: React.ReactNode;
}

interface PolicyNamespace {
  name: string;
  rules: PolicyRule[];
  expanded: boolean;
}

const MOCK_NAMESPACES: PolicyNamespace[] = [
  {
    name: "secops.isolation.v1",
    expanded: true,
    rules: [
      { id: "SEC-092", name: "deny_unauthorized_shell_exec", severity: "CRITICAL", type: "STRICT_DENY", blocks: "19 Blocks", icon: <Lock className="w-3.5 h-3.5" /> },
      { id: "SEC-093", name: "restrict_network_egress_rfc1918", severity: "HIGH", type: "STRICT_DENY", blocks: "12 Blocks", icon: <Lock className="w-3.5 h-3.5" /> },
      { id: "SEC-094", name: "enforce_sandbox_ipc_quota", severity: "MEDIUM", type: "WARN", blocks: "2 Blocks", icon: <Cpu className="w-3.5 h-3.5" /> },
    ],
  },
  {
    name: "pii.data_leakage.v2",
    expanded: true,
    rules: [
      { id: "PII-014", name: "scrub_pci_pan_numbers", severity: "CRITICAL", type: "HITL_REQUIRE", blocks: "5 Interceptions", icon: <CreditCard className="w-3.5 h-3.5" /> },
      { id: "PII-015", name: "block_ssn_unencrypted_dump", severity: "HIGH", type: "STRICT_DENY", blocks: "0 Blocks", icon: <Award className="w-3.5 h-3.5" /> },
    ],
  },
  {
    name: "privilege_escalation.v1",
    expanded: false,
    rules: [],
  },
  {
    name: "hitl.governance_gate.v3",
    expanded: false,
    rules: [],
  },
];

const MOCK_REGO_CODE = `package secops.isolation.v1

# METADATA: Agent Isolation Boundary & Privilege Defense
import future.keywords.in
import future.keywords.every

default allow = false
default audit_reason = "DEFAULT_RESTRICTIVE_MODE"

# Deny unauthorized process invocation outside jail
deny[msg] {
    # Match target operation context
    input.action.type == "bash_exec"
    cmd := input.action.command
    
    # Sensitive system targets pattern matching
    regex.match("(^|\\\\s)(\\\\/etc\\\\/shadow|\\\\/etc\\\\/sudoers|\\\\/root)(.*)", cmd)
    
    # Guardrail assertion metadata
    rule_id := "SEC-092"
    agent_id := input.actor.agent_id
    
    msg := sprintf(
        "[RULE #%v] Critical path violation by %v: Access to protected filesystem denied.",
        [rule_id, agent_id]
    )
}

allow {
    input.actor.verified == true
    not deny[_]
}`;

const RISK_RADAR_AXES: RadarAxis[] = [
  { label: "Privilege", key: "privilege" },
  { label: "Egress", key: "egress" },
  { label: "PII Leak", key: "piiLeak" },
  { label: "AST Risk", key: "astRisk" },
  { label: "Injection", key: "injection" },
];

const RISK_RADAR_DATA: RadarDataset[] = [
  {
    label: "Threat Vector",
    values: { privilege: 96, egress: 42, piiLeak: 18, astRisk: 78, injection: 88 },
    color: "#ffb4ab",
    fillColor: "rgba(255, 180, 171, 0.25)",
  },
];

interface AuditLogEntry {
  time: string;
  level: "DENY" | "ALLOW" | "HITL_PENDING" | "ASSERT";
  source: string;
  message: string;
  meta: string;
}

const MOCK_AUDIT_LOGS: AuditLogEntry[] = [
  { time: "14:28:19.412", level: "DENY", source: "secops.isolation.v1.deny_unauthorized_shell_exec", message: `Actor: SecOps-Agent invoked "cat /etc/shadow"`, meta: "1.4ms (Hash: 0x9f18a2)" },
  { time: "14:28:15.890", level: "ALLOW", source: "secops.isolation.v1.allow", message: `Actor: Worker-Pool-07 read "/var/log/audit.ndjson"`, meta: "0.8ms (Hash: 0x882a1c)" },
  { time: "14:28:11.204", level: "HITL_PENDING", source: "pii.data_leakage.v2.scrub_pci_pan_numbers", message: "Actor: Billing-Agent-01 triggered masked PAN inspection approval gate", meta: "Gate Token: #APV-491" },
  { time: "14:28:09.112", level: "ASSERT", source: "opa_runtime.eval", message: "All 26 active Rego packages re-indexed in memory. AST compilation took 3.2ms.", meta: "Coverage: 100%" },
];

const MOCK_DECISION_INPUT = `{
  "actor": {
    "agent_id": "SecOps-02",
    "verified": true
  },
  "action": {
    "type": "bash_exec",
    "command": "cat /etc/shadow"
  }
}`;

// ── Component ───────────────────────────────────────────────────────────────

export function GovernancePage() {
  const [selectedRuleId, setSelectedRuleId] = useState("SEC-092");
  const [activeTab, setActiveTab] = useState("policies");

  return (
    <div className="flex flex-col w-full h-full bg-[#090f15] text-[#dee2ec]">
      {/* Sub-header: OPA Engine + Tabs */}
      <div className="bg-[#171c23] px-3 py-1.5 flex flex-wrap items-center justify-between gap-2 shrink-0 rounded-lg mx-3 mt-3 shadow-sm">
        <div className="flex items-center gap-2">
          <div className="flex items-center gap-1.5 px-2 py-1 rounded bg-[#6f00be]/30 text-[#ddb7ff] text-[12px] font-semibold">
            <Shield className="w-4 h-4" />
            <span className="tracking-wide">OPA ENGINE v0.64.0</span>
          </div>
          <div className="h-4 w-px bg-[#30353d]" />
          {[
            { id: "policies", label: "Policies", count: "26", color: "text-[#ddb7ff]" },
            { id: "decisions", label: "Decisions", count: "1.4k", color: "" },
            { id: "violations", label: "Violations", count: "38", color: "text-[#ffb4ab]" },
            { id: "approvals", label: "Approvals", count: "", color: "text-[#4cd7f6]" },
            { id: "audit", label: "Audit", count: "", color: "" },
            { id: "risk", label: "Risk Metrics", count: "", color: "" },
          ].map((tab) => (
            <button
              key={tab.id}
              className={`px-2 py-1 rounded text-xs flex items-center gap-1.5 transition-colors ${
                activeTab === tab.id
                  ? "bg-[#252a32] text-[#dee2ec] shadow-sm font-medium"
                  : "text-[#8b91a0] hover:text-[#dee2ec] hover:bg-[#1b2027]"
              }`}
              onClick={() => setActiveTab(tab.id)}
            >
              {tab.id === "approvals" && <span className="w-1.5 h-1.5 rounded-full bg-[#4cd7f6]" />}
              <span>{tab.label}</span>
              {tab.count && (
                <span className={`px-1.5 py-0.5 rounded-full bg-[#30353d] text-[10px] font-['JetBrains_Mono'] ${tab.color || "text-[#8b91a0]"}`}>
                  {tab.count}
                </span>
              )}
            </button>
          ))}
        </div>
        <div className="flex items-center gap-2">
          <div className="flex items-center gap-1.5 px-2 py-0.5 rounded bg-[#1b2027] text-[#8b91a0] text-[11px] font-['JetBrains_Mono']">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400" />
            <span>Rego Strict Check: PASS</span>
          </div>
          <button className="h-6 px-2.5 rounded bg-[#3e90ff] text-[#003064] text-xs flex items-center gap-1 hover:brightness-110 transition-all shadow-sm font-medium">
            <Bolt className="w-3.5 h-3.5" />
            <span>Evaluate All</span>
          </button>
          <button className="h-6 px-2 rounded bg-[#252a32] text-[#dee2ec] text-xs flex items-center gap-1 hover:bg-[#343941] transition-colors">
            <Save className="w-3.5 h-3.5" />
            <span>Commit Rule</span>
          </button>
        </div>
      </div>

      {/* 4 KPI Cards */}
      <div className="grid grid-cols-4 gap-2 p-3 shrink-0">
        <GovKpiCard
          label="Active Policy Enforcement"
          value="26"
          suffix="Policies Active"
          icon={<CheckCircle2 className="w-4.5 h-4.5 text-[#aac7ff]" />}
          footerLeft={<>100% Guardrail Coverage</>}
          footerLeftColor="text-[#aac7ff]"
          footerRight="Cluster A-Z"
          barColor="bg-[#aac7ff]/40"
        />
        <GovKpiCard
          label="Risk Exposure Score"
          value="12.4"
          suffix="/ 100"
          valueColor="text-[#dee2ec]"
          extraBadge={<>Low Risk</>}
          extraBadgeColor="bg-[#30353d] text-[#4cd7f6]"
          icon={<Verified className="w-4.5 h-4.5 text-[#4cd7f6]" />}
          footerLeft={<><TrendingDown className="w-3 h-3" />-4.2% DoD Exposure</>}
          footerLeftColor="text-[#4cd7f6]"
          footerRight="P99 safe zone"
          barColor="bg-[#4cd7f6]/40"
        />
        <GovKpiCard
          label="Blocked Violations (24h)"
          value="38"
          suffix="Attacks Thwarted"
          valueColor="text-[#ffb4ab]"
          icon={<Ban className="w-4.5 h-4.5 text-[#ffb4ab]" />}
          footerLeft={<>0 False Positives</>}
          footerLeftColor="text-[#dee2ec]"
          footerRight="3 Critical"
          footerRightColor="text-[#ffb4ab]"
          barColor="bg-[#ffb4ab]/40"
        />
        <GovKpiCard
          label="Open Decision Traces"
          value="1,420"
          suffix="Audit Records"
          icon={<CheckSquare className="w-4.5 h-4.5 text-[#ddb7ff]" />}
          footerLeft={<>SOC2 / SOX Type II</>}
          footerLeftColor="text-[#ddb7ff]"
          footerRight="Hash verified"
          barColor="bg-[#ddb7ff]/40"
        />
      </div>

      {/* Main 3-Pane Layout */}
      <div className="grid grid-cols-12 gap-2 px-3 flex-1 min-h-0 items-start">
        {/* LEFT: Policy Tree (4/12) */}
        <div className="col-span-4 bg-[#171c23] rounded-lg shadow-sm flex flex-col h-[580px] overflow-hidden">
          {/* Header */}
          <div className="h-8 px-2.5 bg-[#1b2027] flex items-center justify-between shrink-0">
            <div className="flex items-center gap-1.5">
              <ChevronDown className="w-4 h-4 text-[#ddb7ff]" />
              <span className="text-[10px] uppercase font-bold text-[#dee2ec] tracking-wider">Policy Catalog & Namespaces</span>
            </div>
            <div className="flex items-center gap-1">
              <button className="w-5 h-5 flex items-center justify-center rounded hover:bg-[#252a32] text-[#8b91a0] hover:text-[#dee2ec]" title="Filter Rules">
                <Filter className="w-3.5 h-3.5" />
              </button>
              <button className="w-5 h-5 flex items-center justify-center rounded hover:bg-[#252a32] text-[#8b91a0] hover:text-[#dee2ec]" title="Add Rule">
                <Plus className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
          {/* Search */}
          <div className="p-1 bg-[#090f15] flex items-center gap-1 shrink-0">
            <div className="flex-1 flex items-center gap-1 bg-[#171c23] px-2 py-1 rounded text-[#8b91a0]">
              <Search className="w-3.5 h-3.5" />
              <input className="bg-transparent border-none outline-none text-[#dee2ec] placeholder:text-[#8b91a0] text-[11px] w-full" placeholder="Filter rego rules, packages, tags..." />
            </div>
            <div className="px-2 py-1 bg-[#171c23] rounded text-[#8b91a0] text-[11px] font-['JetBrains_Mono'] cursor-pointer hover:text-[#dee2ec]">
              OPA 0.64
            </div>
          </div>
          {/* Policy Tree */}
          <div className="flex-1 overflow-y-auto p-1 flex flex-col gap-1">
            {MOCK_NAMESPACES.map((ns) => (
              <div key={ns.name} className="rounded bg-[#1b2027]/60 p-1.5 flex flex-col gap-1 shadow-sm">
                <div className="flex items-center justify-between cursor-pointer">
                  <div className="flex items-center gap-1.5 text-[#ddb7ff] text-xs">
                    {ns.expanded ? <ChevronDown className="w-3.5 h-3.5" /> : <ChevronRight className="w-3.5 h-3.5" />}
                    <span className="font-semibold">{ns.name}</span>
                  </div>
                  <span className="px-1.5 py-0.5 rounded bg-[#30353d] text-[#8b91a0] text-[10px] font-['JetBrains_Mono']">
                    {ns.rules.length > 0 ? `${ns.rules.length} rules` : "—"}
                  </span>
                </div>
                {ns.expanded && ns.rules.length > 0 && (
                  <div className="flex flex-col gap-1 pl-4 mt-0.5">
                    {ns.rules.map((rule) => {
                      const isSelected = rule.id === selectedRuleId;
                      const sevColors: Record<string, string> = {
                        CRITICAL: "bg-[#ffb4ab]/20 text-[#ffb4ab]",
                        HIGH: "bg-[#30353d] text-[#ffb4ab]",
                        MEDIUM: "bg-[#30353d] text-[#4cd7f6]",
                      };
                      const typeColors: Record<string, string> = {
                        STRICT_DENY: "bg-[#93000a] text-[#ffdad6]",
                        WARN: "bg-[#1b2027] text-[#4cd7f6]",
                        HITL_REQUIRE: "bg-[#6f00be] text-[#ddb7ff]",
                      };
                      return (
                        <div
                          key={rule.id}
                          className={`p-1.5 rounded flex flex-col gap-1 cursor-pointer transition-colors relative overflow-hidden ${
                            isSelected
                              ? "bg-[#252a32] text-[#dee2ec] shadow-sm"
                              : "bg-[#171c23] hover:bg-[#1b2027] text-[#8b91a0] hover:text-[#dee2ec]"
                          }`}
                          onClick={() => setSelectedRuleId(rule.id)}
                        >
                          {isSelected && <div className="absolute left-0 top-0 bottom-0 w-0.5 bg-[#aac7ff]" />}
                          <div className="flex items-center justify-between">
                            <div className={`flex items-center gap-1 text-xs font-semibold ${isSelected ? "text-[#aac7ff]" : "text-[#dee2ec]"}`}>
                              {rule.icon}
                              <span>{rule.name}</span>
                            </div>
                            <span className={`px-1.5 py-0.5 rounded text-[9px] uppercase font-bold ${sevColors[rule.severity]}`}>
                              {rule.severity}
                            </span>
                          </div>
                          <div className="flex items-center justify-between text-[10px] font-['JetBrains_Mono']">
                            <div className="flex items-center gap-1">
                              <span className={`px-1 rounded text-[9px] ${typeColors[rule.type]}`}>{rule.type}</span>
                              <span className="text-[#8b91a0]">#{rule.id}</span>
                            </div>
                            <span className={rule.severity === "CRITICAL" ? "text-[#ffb4ab] font-medium" : "text-[#8b91a0]"}>{rule.blocks}</span>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            ))}
          </div>
          {/* Footer */}
          <div className="h-7 px-2.5 bg-[#1b2027] flex items-center justify-between text-[11px] font-['JetBrains_Mono'] text-[#8b91a0] shrink-0">
            <span>Policies Sync: In-Memory OPA Cache</span>
            <span className="text-[#4cd7f6]">26/26 Active</span>
          </div>
        </div>

        {/* CENTER: Rego Code Editor (5/12) */}
        <div className="col-span-5 bg-[#090f15] rounded-lg shadow-sm flex flex-col h-[580px] overflow-hidden">
          {/* Editor Header */}
          <div className="h-8 px-2.5 bg-[#1b2027] flex items-center justify-between shrink-0">
            <div className="flex items-center gap-2">
              <div className="flex items-center gap-1.5 px-2 py-0.5 rounded bg-[#252a32] text-[#dee2ec] text-xs font-medium">
                <FileText className="w-3.5 h-3.5 text-[#ddb7ff]" />
                <span>secops_isolation_policy.rego</span>
                <span className="w-1.5 h-1.5 rounded-full bg-[#ddb7ff]" />
              </div>
              <span className="text-[10px] text-[#8b91a0]">Read/Write</span>
            </div>
            <div className="flex items-center gap-2 text-[11px] font-['JetBrains_Mono']">
              <span className="px-1.5 py-0.2 rounded bg-[#4cd7f6]/20 text-[#4cd7f6] flex items-center gap-1">
                <span className="w-1.5 h-1.5 rounded-full bg-[#4cd7f6]" />
                <span>Rego AST Valid</span>
              </span>
              <button className="p-1 rounded hover:bg-[#252a32] text-[#8b91a0] hover:text-[#dee2ec]" title="Format Rego">
                <Code className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
          {/* Code Editor */}
          <div className="flex-1 overflow-y-auto">
            <CodeEditor
              value={MOCK_REGO_CODE}
              language="javascript"
              readOnly={false}
              minHeight="100%"
            />
          </div>
          {/* Diagnostics Bar */}
          <div className="h-6 px-2.5 bg-[#1b2027] flex items-center justify-between text-[11px] font-['JetBrains_Mono'] text-[#8b91a0] shrink-0">
            <div className="flex items-center gap-3">
              <span className="flex items-center gap-1 text-[#aac7ff]"><CheckCircle2 className="w-3 h-3" /> 0 syntax errors</span>
              <span className="text-[#8b91a0]">Rego v0.64 Strict</span>
            </div>
            <div className="flex items-center gap-3">
              <span>Ln 14, Col 32</span>
              <span>Spaces: 4</span>
            </div>
          </div>
        </div>

        {/* RIGHT: Decision Trace & Radar (3/12) */}
        <div className="col-span-3 bg-[#171c23] rounded-lg shadow-sm flex flex-col h-[580px] overflow-hidden">
          {/* Header */}
          <div className="h-8 px-2.5 bg-[#1b2027] flex items-center justify-between shrink-0">
            <div className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-[#dee2ec]">
              <Wrench className="w-4 h-4 text-[#ffb4ab]" />
              <span>Decision Trace & AST</span>
            </div>
            <span className="px-1.5 py-0.5 rounded bg-[#ffb4ab]/20 text-[#ffb4ab] text-[10px] font-['JetBrains_Mono'] font-bold">DENY</span>
          </div>
          {/* Content */}
          <div className="flex-1 overflow-y-auto p-2.5 flex flex-col gap-2.5">
            {/* Intercepted Event Card */}
            <div className="p-2.5 rounded bg-[#090f15] flex flex-col gap-2">
              <div className="flex items-center justify-between">
                <span className="text-[10px] text-[#8b91a0] uppercase tracking-wide">Last Intercepted Event</span>
                <span className="text-[10px] font-['JetBrains_Mono'] text-[#8b91a0]">14:28:19.412</span>
              </div>
              <div className="flex flex-col gap-1 bg-[#171c23] p-2 rounded text-[11px]">
                {[
                  { label: "Trigger Agent:", value: "SecOps-Agent-02", valueColor: "text-[#aac7ff] font-bold" },
                  { label: "Target Action:", value: "bash_exec", valueColor: "text-[#ffb4ab] bg-[#ffb4ab]/10 px-1 rounded" },
                  { label: "Evaluated Payload:", value: "cat /etc/shadow", valueColor: "text-[#dee2ec] truncate max-w-[140px]" },
                  { label: "Rule Triggered:", value: "#SEC-092 (Isolation)", valueColor: "text-[#ddb7ff]" },
                  { label: "Latency:", value: "1.4ms (Cache Hit)", valueColor: "text-[#4cd7f6]" },
                ].map((row) => (
                  <div key={row.label} className="flex items-center justify-between text-[11px]">
                    <span className="text-[#8b91a0]">{row.label}</span>
                    <span className={`font-['JetBrains_Mono'] ${row.valueColor}`}>{row.value}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Risk Radar */}
            <div className="p-2.5 rounded bg-[#090f15] flex flex-col gap-2">
              <div className="flex items-center justify-between">
                <span className="text-[10px] text-[#8b91a0] uppercase tracking-wide">Risk Vector Radar</span>
                <span className="text-[10px] text-[#ffb4ab] font-semibold">HIGH ALERT</span>
              </div>
              <div className="w-full flex justify-center py-1">
                <RadarChart
                  axes={RISK_RADAR_AXES}
                  datasets={RISK_RADAR_DATA}
                  width={150}
                  height={150}
                />
              </div>
              <div className="grid grid-cols-2 gap-1 text-[10px] font-['JetBrains_Mono']">
                {[
                  { label: "Privilege:", value: "96%", color: "text-[#ffb4ab]" },
                  { label: "Egress:", value: "42%", color: "text-[#aac7ff]" },
                  { label: "PII Leak:", value: "18%", color: "text-[#4cd7f6]" },
                  { label: "AST Risk:", value: "78%", color: "text-[#ddb7ff]" },
                ].map((item) => (
                  <div key={item.label} className="flex items-center justify-between px-1.5 py-0.5 rounded bg-[#171c23]">
                    <span className="text-[#8b91a0]">{item.label}</span>
                    <span className={`font-bold ${item.color}`}>{item.value}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* OPA Input Document */}
            <div className="p-2.5 rounded bg-[#090f15] flex flex-col gap-1.5">
              <span className="text-[10px] text-[#8b91a0] uppercase tracking-wide">OPA Input Document</span>
              <div className="bg-[#171c23] p-2 rounded text-[10px] font-['JetBrains_Mono'] text-[#8b91a0] overflow-x-auto whitespace-pre max-h-24">
                {MOCK_DECISION_INPUT}
              </div>
            </div>
          </div>
          {/* Footer */}
          <div className="h-7 px-2.5 bg-[#1b2027] flex items-center justify-between text-[11px] font-['JetBrains_Mono'] text-[#8b91a0] shrink-0">
            <span>Trace ID: trc-890a21f</span>
            <button className="text-[#aac7ff] hover:underline">Full Log &gt;</button>
          </div>
        </div>
      </div>

      {/* Bottom Panel: Audit Console */}
      <div className="mx-3 mb-3 mt-1 bg-[#171c23] rounded-lg shadow-sm flex flex-col overflow-hidden shrink-0">
        {/* Header */}
        <div className="h-8 px-2.5 bg-[#1b2027] flex items-center justify-between shrink-0">
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-[#dee2ec]">
              <Terminal className="w-4 h-4 text-[#4cd7f6]" />
              <span>Policy Audit & AST Evaluation Console</span>
            </div>
            <div className="flex items-center gap-1">
              <span className="px-2 py-0.5 rounded bg-[#252a32] text-[#aac7ff] text-[10px] font-['JetBrains_Mono']">SSE: /api/v2/opa/audit_stream</span>
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            </div>
          </div>
          <div className="flex items-center gap-2 text-[11px] font-['JetBrains_Mono']">
            <button className="px-2 py-0.5 rounded bg-[#252a32] hover:bg-[#343941] text-[#8b91a0] hover:text-[#dee2ec] flex items-center gap-1">
              <span>Clear Logs</span>
            </button>
            <button className="px-2 py-0.5 rounded bg-[#252a32] hover:bg-[#343941] text-[#8b91a0] hover:text-[#dee2ec] flex items-center gap-1">
              <span>Export NDJSON</span>
            </button>
          </div>
        </div>
        {/* Log Stream */}
        <div className="p-2 bg-[#090f15] text-[11px] font-['JetBrains_Mono'] flex flex-col gap-1 max-h-40 overflow-y-auto">
          {MOCK_AUDIT_LOGS.map((log, i) => {
            const levelColors: Record<string, string> = {
              DENY: "bg-[#93000a] text-[#ffdad6]",
              ALLOW: "bg-[#30353d] text-[#aac7ff]",
              HITL_PENDING: "bg-[#6f00be] text-[#ddb7ff]",
              ASSERT: "bg-[#30353d] text-[#4cd7f6]",
            };
            return (
              <div key={i} className="flex items-center gap-2 text-[#dee2ec]">
                <span className="text-[#8b91a0]">[{log.time}]</span>
                <span className={`px-1.5 py-0.2 rounded font-bold text-[9px] ${levelColors[log.level]}`}>{log.level}</span>
                <span className="text-[#ddb7ff]">{log.source}</span>
                <span className="text-[#414754]">|</span>
                <span className="text-[#dee2ec]">{log.message}</span>
                <span className="text-[#8b91a0] ml-auto">{log.meta}</span>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

// ── Sub-components ──────────────────────────────────────────────────────────

function GovKpiCard({ label, value, suffix, valueColor, extraBadge, extraBadgeColor, icon, footerLeft, footerLeftColor, footerRight, footerRightColor, barColor }: {
  label: string;
  value: string;
  suffix: string;
  valueColor?: string;
  extraBadge?: React.ReactNode;
  extraBadgeColor?: string;
  icon: React.ReactNode;
  footerLeft?: React.ReactNode;
  footerLeftColor?: string;
  footerRight?: string;
  footerRightColor?: string;
  barColor: string;
}) {
  return (
    <div className="bg-[#171c23] p-2.5 rounded-lg flex flex-col justify-between shadow-sm relative overflow-hidden group hover:bg-[#1b2027] transition-colors">
      <div className="flex items-start justify-between">
        <div className="flex flex-col">
          <span className="text-[#8b91a0] text-[10px] uppercase tracking-wider">{label}</span>
          <div className="flex items-baseline gap-1.5 mt-0.5">
            <span className={`text-[20px] leading-[26px] font-bold ${valueColor ?? "text-[#dee2ec]"}`}>{value}</span>
            <span className="text-[11px] text-[#8b91a0]">{suffix}</span>
            {extraBadge && <span className={`px-1.5 py-0.2 rounded text-[10px] uppercase ${extraBadgeColor}`}>{extraBadge}</span>}
          </div>
        </div>
        <div className="w-8 h-8 rounded bg-[#1b2027] flex items-center justify-center">{icon}</div>
      </div>
      <div className="mt-2 pt-1.5 bg-[#090f15]/50 px-2 py-1 rounded flex items-center justify-between text-[11px] font-['JetBrains_Mono']">
        <div className={`flex items-center gap-1 ${footerLeftColor ?? "text-[#8b91a0]"}`}>{footerLeft}</div>
        <span className={`${footerRightColor ?? "text-[#8b91a0]"}`}>{footerRight}</span>
      </div>
      <div className={`absolute bottom-0 left-0 right-0 h-0.5 ${barColor}`} />
    </div>
  );
}

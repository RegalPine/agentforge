import { useState } from "react";
import {
  Hourglass,
  Timer,
  Verified,
  Gavel,
  Frown,
  Bell,
  Calendar,
  Shield,
  Brain,
  GitCompare,
  LockOpen,
  X,
  MessageCircle,
  Fingerprint,
  Receipt,
  RefreshCw,
  Filter,
  ChevronRight,
} from "lucide-react";

// ── Mock data ───────────────────────────────────────────────────────────────

interface ApprovalTicket {
  id: string;
  number: number;
  title: string;
  description: string;
  agent: string;
  target: string;
  riskLevel: "CRITICAL" | "ELEVATED" | "MEDIUM";
  riskScore: number;
  slaRemaining: string;
  slaSeconds: number;
  incidentId: string;
  workflow?: string;
  leaseDuration?: string;
  borderColor: string;
  riskColor: string;
  badgeBg: string;
  badgeText: string;
  levelLabel: string;
}

const MOCK_TICKETS: ApprovalTicket[] = [
  {
    id: "APPR-8921", number: 8921,
    title: "Root IAM Permission Elevation Request",
    description: "Agent: SecOps-Agent • Target: arn:aws:iam::root rw",
    agent: "SecOps-Agent-v3.4", target: "arn:aws:iam::6281920:role/breakglass-admin",
    riskLevel: "CRITICAL", riskScore: 94,
    slaRemaining: "04m 12s", slaSeconds: 252,
    incidentId: "INC-4402", workflow: "SecOps-Incident-Response #401",
    leaseDuration: "15 Minutes",
    borderColor: "border-l-[#ffb4ab]", riskColor: "text-[#ffb4ab]",
    badgeBg: "bg-[#93000a]", badgeText: "text-[#ffdad6]", levelLabel: "CRITICAL LVL 4",
  },
  {
    id: "APPR-8920", number: 8920,
    title: "High-Tier User Refund Override ($340.00)",
    description: "Agent: Customer-Support-Agent • Ceiling: $250.00",
    agent: "Customer-Support-Agent", target: "stripe-refund-api",
    riskLevel: "MEDIUM", riskScore: 58,
    slaRemaining: "18m 40s", slaSeconds: 1120,
    incidentId: "INC-4401",
    borderColor: "border-l-[#4cd7f6]", riskColor: "text-[#4cd7f6]",
    badgeBg: "bg-[#009eb9]/30", badgeText: "text-[#4cd7f6]", levelLabel: "MEDIUM LVL 2",
  },
  {
    id: "APPR-8918", number: 8918,
    title: "Production Network Policy Isolation",
    description: "Agent: K8s-AutoHealer • Ingress: Deny Port 443",
    agent: "K8s-AutoHealer", target: "prod-k8s-east:ingress-443",
    riskLevel: "ELEVATED", riskScore: 79,
    slaRemaining: "35m 10s", slaSeconds: 2110,
    incidentId: "INC-4399",
    borderColor: "border-l-[#ddb7ff]", riskColor: "text-[#ddb7ff]",
    badgeBg: "bg-[#6f00be]/40", badgeText: "text-[#ddb7ff]", levelLabel: "ELEVATED LVL 3",
  },
];

const MOCK_REACT_CHAIN = [
  {
    label: "THOUGHT",
    color: "text-[#8b91a0]",
    text: "Active zero-day exfiltration signature detected in Pod auth-token-daemon-9f2b. Pod is evading SIGKILL signals due to zombie process locks.",
  },
  {
    label: "ACTION",
    color: "text-[#4cd7f6]",
    text: "Request emergency AWS KMS key rotation and IAM isolation token to detach VPC peering router before outbound payload syn-ack completes.",
  },
  {
    label: "OBSERVE",
    color: "text-[#ddb7ff]",
    text: "Default agent credentials lack iam:AttachRolePolicy and kms:DisableKey. Invoking Human-in-the-Loop Gateway §7.5.",
  },
];

const MOCK_DIFF_LINES = [
  { num: 1, type: "ctx", text: "{" },
  { num: 2, type: "ctx", text: '  "Version": "2012-10-17",' },
  { num: 3, type: "ctx", text: '  "Statement": [' },
  { num: 4, type: "del", text: '    "Action": ["iam:Get*", "iam:List*"],' },
  { num: 4, type: "add", text: '    "Action": [' },
  { num: 5, type: "add", text: '      "iam:AttachRolePolicy",' },
  { num: 6, type: "add", text: '      "kms:DisableKey",' },
  { num: 7, type: "add", text: '      "ec2:RevokeSecurityGroupIngress"' },
  { num: 8, type: "add", text: "    ]," },
  { num: 9, type: "ctx", text: '  "Resource": "*",' },
  { num: 10, type: "ctx", text: '  "Condition": { "NumericLessThan": { "aws:TokenIssueTime": 900 } }' },
  { num: 11, type: "ctx", text: "  }" },
  { num: 12, type: "ctx", text: "]" },
  { num: 13, type: "ctx", text: "}" },
];

interface AuditStreamEntry {
  time: string;
  level: "DISPATCH" | "OPA_EVAL" | "WEBSOCKET" | "INSPECT" | "DECISION";
  levelColor: string;
  message: string;
}

const MOCK_AUDIT_STREAM: AuditStreamEntry[] = [
  { time: "14:32:01", level: "DISPATCH", levelColor: "text-[#aac7ff] font-bold", message: "Agent SecOps-Agent generated request APPR-8921. Pausing execution pipeline." },
  { time: "14:32:02", level: "OPA_EVAL", levelColor: "text-[#ddb7ff] font-bold", message: "Policy governance_rules.rego:allow_execution evaluated FALSE. Escalate to HITL supervisor." },
  { time: "14:32:04", level: "WEBSOCKET", levelColor: "text-[#4cd7f6] font-bold", message: "Notified active operators (3 authenticated human supervisors active in cluster)." },
  { time: "14:32:08", level: "INSPECT", levelColor: "text-[#dee2ec] font-bold", message: "Active human operator opened inspector for APPR-8921. Countdown timer ticking." },
];

// ── Component ───────────────────────────────────────────────────────────────

export function HumanInTheLoopPage() {
  const [selectedTicketId, setSelectedTicketId] = useState("APPR-8921");
  const [activeView, setActiveView] = useState("pending");
  const [decisionReason, setDecisionReason] = useState("");

  const selectedTicket = MOCK_TICKETS.find((t) => t.id === selectedTicketId) ?? MOCK_TICKETS[0];

  return (
    <div className="flex flex-col w-full h-full bg-[#090f15] text-[#dee2ec]">
      {/* Sub-tab Navigation */}
      <div className="flex items-center justify-between pb-2.5 mb-2.5 border-b border-[#252a32]/40 px-3 pt-3 shrink-0">
        <div className="flex items-center gap-1 bg-[#171c23] p-1 rounded-lg">
          {[
            { id: "pending", label: "Pending Tasks", icon: <Hourglass className="w-3.5 h-3.5 text-[#4cd7f6]" />, count: "3", countBg: "bg-[#93000a] text-[#ffdad6]" },
            { id: "history", label: "Approval History", icon: <ChevronRight className="w-3.5 h-3.5" />, count: "78", countBg: "bg-[#30353d] text-[#8b91a0]" },
            { id: "summary", label: "SLA & Risk Summary", icon: <Timer className="w-3.5 h-3.5" />, count: "", countBg: "" },
          ].map((tab) => (
            <button
              key={tab.id}
              className={`flex items-center gap-2 px-3 py-1 rounded text-xs transition-all ${
                activeView === tab.id
                  ? "bg-[#252a32] text-[#dee2ec] shadow-sm font-medium"
                  : "text-[#8b91a0] hover:text-[#dee2ec] hover:bg-[#1b2027]"
              }`}
              onClick={() => setActiveView(tab.id)}
            >
              {tab.icon}
              <span>{tab.label}</span>
              {tab.count && (
                <span className={`flex items-center justify-center px-1.5 py-0.5 rounded-full text-[10px] font-['JetBrains_Mono'] font-bold ${tab.countBg}`}>
                  {tab.count}
                </span>
              )}
            </button>
          ))}
        </div>
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2 px-2.5 py-1 rounded bg-[#171c23] text-[11px] font-['JetBrains_Mono'] text-[#dee2ec]">
            <span className="w-2 h-2 rounded-full bg-[#4cd7f6] animate-pulse" />
            <span className="text-[#8b91a0] uppercase tracking-wider text-[10px]">OPA Engine:</span>
            <span className="text-[#4cd7f6] font-bold">§7.5 Strict Enforce</span>
          </div>
          <div className="flex items-center gap-1">
            <button className="px-2 py-1 rounded bg-[#171c23] hover:bg-[#1b2027] text-[#8b91a0] hover:text-[#dee2ec] text-[12px] flex items-center gap-1">
              <Filter className="w-3.5 h-3.5" />Filters
            </button>
            <button className="px-2 py-1 rounded bg-[#171c23] hover:bg-[#1b2027] text-[#8b91a0] hover:text-[#dee2ec] text-[12px] flex items-center gap-1">
              <RefreshCw className="w-3.5 h-3.5" />Auto-sync (3s)
            </button>
          </div>
        </div>
      </div>

      {/* 4 KPI Cards */}
      <div className="grid grid-cols-4 gap-2 px-3 mb-3 shrink-0">
        <HitlKpiCard
          label="Pending Critical Tasks"
          value="3 Tasks"
          valueColor="text-[#ffb4ab]"
          sla="SLA 04m 12s"
          slaColor="text-[#ffb4ab] bg-[#93000a]/40"
          description="Root privilege elevation requested"
          icon={<Frown className="w-5 h-5 text-[#ffb4ab]" />}
          iconBg="bg-[#93000a]/30"
          barColor="bg-[#ffb4ab]"
        />
        <HitlKpiCard
          label="Avg Decision Time"
          value="1m 45s"
          valueColor="text-[#aac7ff]"
          sla="Target < 5m"
          slaColor="text-[#aac7ff] bg-[#30353d]"
          description=""
          icon={<Timer className="w-5 h-5 text-[#aac7ff]" />}
          iconBg="bg-[#1b2027]"
          barColor="bg-[#aac7ff]"
          progressPct={35}
        />
        <HitlKpiCard
          label="Approval Pass Rate"
          value="84.6%"
          valueColor="text-[#4cd7f6]"
          sla="66 APPR / 12 REJ"
          slaColor="text-[#8b91a0]"
          description="Rolling 30-day decision metrics"
          icon={<Verified className="w-5 h-5 text-[#4cd7f6]" />}
          iconBg="bg-[#1b2027]"
          barColor="bg-[#4cd7f6]"
        />
        <HitlKpiCard
          label="Risk Gate Status"
          value="ZERO BYPASS ENFORCED"
          valueColor="text-[#ddb7ff]"
          valueSmall
          sla="SOX 404 & SOC2 Type II Certified"
          slaColor="text-[#8b91a0]"
          description=""
          icon={<Gavel className="w-5 h-5 text-[#ddb7ff]" />}
          iconBg="bg-[#1b2027]"
          barColor="bg-[#ddb7ff]"
        />
      </div>

      {/* Main Two-Column Layout */}
      <div className="grid grid-cols-12 gap-3 px-3 flex-1 min-h-0">
        {/* LEFT: Pending Queue (4/12) */}
        <div className="col-span-4 flex flex-col gap-2 overflow-y-auto">
          <div className="flex items-center justify-between px-1">
            <div className="flex items-center gap-1.5">
              <span className="text-xs font-bold text-[#dee2ec]">Execution Queue</span>
              <span className="px-1.5 py-0.5 rounded bg-[#30353d] text-[#dee2ec] text-[10px] font-['JetBrains_Mono']">3 Active</span>
            </div>
            <span className="text-[10px] font-['JetBrains_Mono'] text-[#8b91a0]">Sorted by: Urgency</span>
          </div>

          {MOCK_TICKETS.map((ticket) => {
            const isSelected = ticket.id === selectedTicketId;
            return (
              <div
                key={ticket.id}
                className={`cursor-pointer p-2.5 rounded-lg transition-all relative overflow-hidden ${
                  isSelected ? "bg-[#1b2027] shadow-md" : "bg-[#171c23] hover:bg-[#1b2027]"
                } ${ticket.borderColor} border-l-2`}
                onClick={() => setSelectedTicketId(ticket.id)}
              >
                <div className="flex items-center justify-between mb-1 pl-1">
                  <div className="flex items-center gap-2">
                    <span className={`text-[11px] font-['JetBrains_Mono'] font-bold ${ticket.riskColor}`}>{ticket.id}</span>
                    <span className={`px-1.5 py-0.5 rounded ${ticket.badgeBg} ${ticket.badgeText} text-[9px] uppercase font-bold tracking-wider`}>
                      {ticket.levelLabel}
                    </span>
                  </div>
                  <div className={`flex items-center gap-1 text-[11px] font-['JetBrains_Mono'] font-semibold px-1.5 py-0.5 rounded ${
                    ticket.slaSeconds < 300 ? "text-[#ffb4ab] bg-[#93000a]/30" : "text-[#8b91a0] bg-[#30353d]"
                  }`}>
                    {ticket.slaSeconds < 300 ? <Bell className="w-3 h-3 animate-spin" /> : <Calendar className="w-3 h-3" />}
                    <span>{ticket.slaRemaining}</span>
                  </div>
                </div>
                <div className="pl-1 mb-1.5">
                  <h4 className="text-[13px] text-[#dee2ec] font-semibold leading-tight">{ticket.title}</h4>
                  <p className="text-[11px] text-[#8b91a0] mt-0.5 truncate">{ticket.description}</p>
                </div>
                <div className="flex items-center justify-between pl-1 pt-1.5 border-t border-[#252a32]/60 text-[10px] font-['JetBrains_Mono'] text-[#8b91a0]">
                  <div className="flex items-center gap-1">
                    <Shield className={`w-3 h-3 ${ticket.riskColor}`} />
                    <span>Risk Score: <strong className={ticket.riskColor}>{ticket.riskScore}/100</strong></span>
                  </div>
                  <span>{ticket.incidentId}</span>
                </div>
              </div>
            );
          })}

          {/* HITL Guardrail Invariant */}
          <div className="p-2.5 rounded-lg bg-[#090f15] mt-1">
            <div className="flex items-center gap-2 mb-1 text-[#dee2ec]">
              <Gavel className="w-3.5 h-3.5 text-[#aac7ff]" />
              <span className="text-[10px] font-bold uppercase tracking-wider">HITL Guardrail Invariant</span>
            </div>
            <p className="text-[11px] text-[#8b91a0] leading-relaxed">
              Autonomous agents cannot self-approve Tier-3 or Tier-4 operations. In accordance with Architecture §7.5, timed-out actions default to{" "}
              <span className="text-[#ffb4ab] font-semibold font-['JetBrains_Mono']">FAIL_CLOSED (REJECT)</span>.
            </p>
          </div>
        </div>

        {/* RIGHT: Approval Detail (8/12) */}
        <div className="col-span-8 flex flex-col gap-2 overflow-y-auto">
          {/* Ticket Header */}
          <div className="bg-[#171c23] rounded-lg p-3 shadow-sm">
            <div className="flex items-start justify-between pb-2.5 border-b border-[#252a32]/50">
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <span className={`px-2 py-0.5 rounded ${selectedTicket.badgeBg} ${selectedTicket.badgeText} text-[11px] font-['JetBrains_Mono'] font-bold`}>
                    {selectedTicket.id}
                  </span>
                  <span className="text-[20px] leading-[26px] font-bold text-[#dee2ec]">{selectedTicket.title}</span>
                  {selectedTicket.workflow && (
                    <span className="px-2 py-0.5 rounded bg-[#1b2027] text-[#8b91a0] text-[11px]">
                      Workflow: {selectedTicket.workflow}
                    </span>
                  )}
                </div>
                <div className="flex items-center gap-4 text-[11px] text-[#8b91a0]">
                  <span>Initiated by: <strong className="text-[#aac7ff] font-['JetBrains_Mono']">{selectedTicket.agent}</strong></span>
                  <span>•</span>
                  <span>Target: <code className="text-[#dee2ec] font-['JetBrains_Mono']">{selectedTicket.target}</code></span>
                  {selectedTicket.leaseDuration && (
                    <><span>•</span><span>Lease: <strong className="text-[#dee2ec]">{selectedTicket.leaseDuration}</strong></span></>
                  )}
                </div>
              </div>
              {/* Risk Gauge */}
              <div className="flex items-center gap-3 bg-[#1b2027] px-3 py-1.5 rounded-lg">
                <div className="flex flex-col text-right">
                  <span className="text-[9px] uppercase tracking-wider text-[#8b91a0]">Risk Vector</span>
                  <span className={`text-[16px] leading-[22px] font-bold ${selectedTicket.riskColor}`}>{selectedTicket.riskScore} / 100</span>
                </div>
                <div className={`w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs ${
                  selectedTicket.riskScore >= 80 ? "bg-[#93000a] text-[#ffdad6]" : selectedTicket.riskScore >= 50 ? "bg-[#009eb9]/30 text-[#4cd7f6]" : "bg-[#1b2027] text-[#8b91a0]"
                }`}>
                  !
                </div>
              </div>
            </div>

            {/* ReAct Chain */}
            <div className="mt-2.5 bg-[#090f15] p-2.5 rounded-lg">
              <div className="flex items-center justify-between mb-1.5">
                <div className="flex items-center gap-2">
                  <Brain className="w-4 h-4 text-[#4cd7f6]" />
                  <span className="text-[10px] font-bold text-[#dee2ec] uppercase tracking-wider">Agent ReAct Chain-of-Thought (Rationale)</span>
                </div>
                <span className="text-[11px] font-['JetBrains_Mono'] text-[#4cd7f6]">Confidence: 98.7%</span>
              </div>
              <div className="space-y-1.5 text-[11px] font-['JetBrains_Mono']">
                {MOCK_REACT_CHAIN.map((step) => (
                  <div key={step.label} className="flex items-start gap-2">
                    <span className={`${step.color} font-bold shrink-0`}>{step.label}:</span>
                    <span className={step.label === "ACTION" ? "text-[#dee2ec]" : "text-[#8b91a0]"}>{step.text}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Payload Diff */}
          <div className="bg-[#171c23] rounded-lg p-2.5 shadow-sm flex flex-col">
            <div className="flex items-center justify-between pb-1.5 border-b border-[#252a32]/50 mb-1">
              <div className="flex items-center gap-2">
                <GitCompare className="w-4 h-4 text-[#aac7ff]" />
                <span className="text-xs font-bold text-[#dee2ec]">Execution Payload Preview: Proposed IAM Policy Diff</span>
                <span className="px-1.5 py-0.5 rounded bg-[#1b2027] text-[10px] font-['JetBrains_Mono'] text-[#8b91a0]">inline_policy.json</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="flex items-center gap-1 text-[11px] font-['JetBrains_Mono'] text-[#ffb4ab]">
                  <span className="w-2 h-2 rounded bg-[#ffb4ab]" /> -2 deletions
                </span>
                <span className="flex items-center gap-1 text-[11px] font-['JetBrains_Mono'] text-[#27c93f]">
                  <span className="w-2 h-2 rounded bg-[#27c93f]" /> +7 additions
                </span>
              </div>
            </div>
            <div className="bg-[#090f15] rounded p-2.5 text-[11px] font-['JetBrains_Mono'] overflow-x-auto">
              {MOCK_DIFF_LINES.map((line, i) => (
                <div key={i} className={`flex items-center gap-2 ${
                  line.type === "del" ? "bg-[#93000a]/20" :
                  line.type === "add" ? "bg-[#27c93f]/10" : ""
                }`}>
                  <span className={`w-6 text-right pr-2 select-none shrink-0 ${
                    line.type === "del" ? "text-[#ffb4ab]/60" :
                    line.type === "add" ? "text-[#27c93f]/60" : "text-[#8b91a0]"
                  }`}>{line.num}</span>
                  <span className={`${
                    line.type === "del" ? "text-[#ffb4ab]" :
                    line.type === "add" ? "text-[#dee2ec]" : "text-[#dee2ec]"
                  }`}>
                    {line.type === "del" && <span className="text-[#ffb4ab]">-</span>}
                    {line.type === "add" && <span className="text-[#27c93f]">+</span>}
                    {line.type === "ctx" && <span className="text-[#8b91a0]"> </span>}
                    {line.text}
                  </span>
                </div>
              ))}
            </div>
          </div>

          {/* Decision Controls */}
          <div className="bg-[#171c23] rounded-lg p-3 shadow-sm">
            <div className="flex items-center justify-between mb-2.5">
              <div className="flex items-center gap-2">
                <Gavel className="w-4 h-4 text-[#dee2ec]" />
                <span className="text-base font-semibold text-[#dee2ec]">Human Decision Authority</span>
              </div>
              <div className="flex items-center gap-1 text-[#8b91a0] text-[11px] font-['JetBrains_Mono']">
                <Fingerprint className="w-3.5 h-3.5 text-[#4cd7f6]" />
                <span>2FA Hardware Token Required</span>
              </div>
            </div>
            {/* Decision Notes */}
            <div className="flex flex-col gap-1.5 mb-3">
              <label className="text-[10px] uppercase tracking-wider text-[#8b91a0]">
                Decision Notes & Audit Rationale (Mandatory for SOX 404)
              </label>
              <textarea
                className="w-full bg-[#090f15] text-[#dee2ec] text-[11px] p-2 rounded focus:outline-none focus:ring-1 focus:ring-[#aac7ff] placeholder:text-[#8b91a0]/40 resize-none font-['JetBrains_Mono']"
                placeholder="Explain the verification criteria, threat context, or root cause justification..."
                rows={2}
                value={decisionReason}
                onChange={(e) => setDecisionReason(e.target.value)}
              />
            </div>
            {/* Action Buttons */}
            <div className="flex items-center justify-between pt-1">
              <div className="flex items-center gap-2">
                <button className="px-3 py-1.5 rounded bg-[#93000a] text-[#ffdad6] hover:bg-[#ffb4ab]/30 text-xs font-bold flex items-center gap-1.5 transition-colors">
                  <X className="w-4 h-4" />
                  <span>Reject Execution</span>
                </button>
                <button className="px-3 py-1.5 rounded bg-[#1b2027] text-[#8b91a0] hover:text-[#dee2ec] hover:bg-[#252a32] text-xs flex items-center gap-1.5 transition-colors">
                  <MessageCircle className="w-4 h-4" />
                  <span>Ask Agent for More Data</span>
                </button>
              </div>
              <div className="flex items-center gap-3">
                <div className="flex items-center gap-1 text-[11px] font-['JetBrains_Mono'] text-[#8b91a0]">
                  <span>Token:</span>
                  <input
                    className="w-16 bg-[#090f15] text-[#4cd7f6] px-1.5 py-0.5 rounded text-center tracking-widest font-mono text-xs"
                    readOnly
                    type="password"
                    value="881920"
                  />
                </div>
                <button className="px-4 py-1.5 rounded bg-[#aac7ff] text-[#003064] hover:bg-[#d6e3ff] text-xs font-bold flex items-center gap-2 shadow-sm transition-all">
                  <LockOpen className="w-4.5 h-4.5" />
                  <span>Authorize & Execute (2FA)</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Bottom: HITL Audit Stream */}
      <div className="mx-3 mt-2 mb-3 bg-[#171c23] rounded-lg p-2.5 shadow-sm shrink-0">
        <div className="flex items-center justify-between pb-1.5 border-b border-[#252a32]/50 mb-1">
          <div className="flex items-center gap-2">
            <Receipt className="w-3.5 h-3.5 text-[#4cd7f6] animate-pulse" />
            <span className="text-xs font-bold text-[#dee2ec] uppercase tracking-wider">HITL Audit Stream & Observability (Live SSE)</span>
          </div>
          <div className="flex items-center gap-3 text-[11px] font-['JetBrains_Mono'] text-[#8b91a0]">
            <span>Connected to <code className="text-[#4cd7f6]">hitl-daemon:9092</code></span>
            <span>Filter: Strict OPA §42</span>
          </div>
        </div>
        <div className="h-20 bg-[#090f15] rounded p-1.5 text-[11px] font-['JetBrains_Mono'] overflow-y-auto space-y-1">
          {MOCK_AUDIT_STREAM.map((entry, i) => (
            <div key={i} className="text-[#8b91a0]">
              <span className="text-[#8b91a0]">[{entry.time}]</span>{" "}
              <span className={entry.levelColor}>{entry.level}</span>{" "}
              <span className="text-[#dee2ec]">{entry.message}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// ── Sub-components ──────────────────────────────────────────────────────────

function HitlKpiCard({ label, value, valueColor, valueSmall, sla, slaColor, description, icon, iconBg, barColor, progressPct }: {
  label: string;
  value: string;
  valueColor: string;
  valueSmall?: boolean;
  sla: string;
  slaColor: string;
  description: string;
  icon: React.ReactNode;
  iconBg: string;
  barColor: string;
  progressPct?: number;
}) {
  return (
    <div className="bg-[#171c23] p-2.5 rounded-lg flex items-center justify-between shadow-sm relative overflow-hidden">
      <div className="flex flex-col gap-0.5">
        <span className="text-[#8b91a0] text-[10px] uppercase tracking-wider">{label}</span>
        <div className="flex items-baseline gap-2">
          <span className={`${valueSmall ? "text-[12px] font-bold tracking-tight" : "text-[20px] leading-[26px] font-bold"} ${valueColor}`}>{value}</span>
          <span className={`text-[10px] font-['JetBrains_Mono'] px-1.5 py-0.5 rounded ${slaColor}`}>{sla}</span>
        </div>
        {description && <span className="text-[11px] text-[#8b91a0] mt-0.5">{description}</span>}
        {progressPct !== undefined && (
          <div className="w-32 bg-[#30353d] h-1 rounded-full overflow-hidden mt-1">
            <div className={`${barColor} h-full rounded-full`} style={{ width: `${progressPct}%` }} />
          </div>
        )}
      </div>
      <div className={`w-10 h-10 rounded-lg ${iconBg} flex items-center justify-center`}>{icon}</div>
      <div className={`absolute bottom-0 left-0 right-0 h-[2px] ${barColor}`} />
    </div>
  );
}

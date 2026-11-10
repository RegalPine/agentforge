// ── Skill Workspace (AF-STUDIO-01 §50-58) ──
// Maps to backend Skill Registry metadata.
// Studio must NOT create parallel semantic types.

/** Skill lifecycle state from backend registry. */
export type SkillLifecycleState =
  | "DRAFT"
  | "VALIDATED"
  | "TESTED"
  | "EVALUATED"
  | "APPROVED"
  | "PUBLISHED"
  | "ACTIVE"
  | "DEPRECATED"
  | "RETIRED"
  | "REVOKED";

/** Skill risk level. */
export type SkillRiskLevel = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

/** Skill execution strategy. */
export type SkillExecutionStrategy =
  | "DIRECT_CAPABILITY"
  | "GRAPH"
  | "AGENT"
  | "COMPOSITE_SKILL";

/** Skill summary for list view (Overview tab). */
export interface SkillSummaryView {
  readonly skillId: string;
  readonly name: string;
  readonly version: string;
  readonly owner: string;
  readonly domain: string;
  readonly riskLevel: SkillRiskLevel;
  readonly lifecycleState: SkillLifecycleState;
  readonly strategy: SkillExecutionStrategy;
  readonly tags: readonly string[];
}

/** Skill overview with identity and status (T-S5.16). */
export interface SkillOverviewView {
  readonly skillId: string;
  readonly name: string;
  readonly version: string;
  readonly goal: string;
  readonly owner: string;
  readonly domain: string;
  readonly riskLevel: SkillRiskLevel;
  readonly lifecycleState: SkillLifecycleState;
  readonly strategy: SkillExecutionStrategy;
  readonly tags: readonly string[];
  readonly createdAt: string;
  readonly updatedAt: string;
  readonly artifactDigest: string;
}

/** Skill contract view (T-S5.17). */
export interface SkillContractView {
  readonly skillId: string;
  readonly version: string;
  readonly goal: string;
  readonly inputSchema: string;
  readonly outputSchema: string;
  readonly strategy: SkillExecutionStrategy;
  readonly requiredCapabilities: readonly string[];
  readonly requiredKnowledge: readonly string[];
  readonly requiredSkills: readonly string[];
}

/** Skill dependency graph view (T-S5.18). */
export interface SkillDependencyView {
  readonly skillId: string;
  readonly version: string;
  readonly capabilities: readonly SkillDependencyNode[];
  readonly knowledge: readonly SkillDependencyNode[];
  readonly skills: readonly SkillDependencyNode[];
}

export interface SkillDependencyNode {
  readonly id: string;
  readonly name: string;
  readonly version: string;
  readonly type: "capability" | "knowledge" | "skill";
  readonly status: "available" | "missing" | "deprecated";
}

/** Skill security view (T-S5.19). */
export interface SkillSecurityView {
  readonly skillId: string;
  readonly version: string;
  readonly riskLevel: SkillRiskLevel;
  readonly classification: string;
  readonly dataClassification: string;
  readonly allowedEgressHosts: readonly string[];
  readonly egressAllowed: boolean;
  readonly requiresApproval: boolean;
  readonly approvalRoles: readonly string[];
  readonly requiredPermissions: readonly string[];
  readonly policyReference: string;
  readonly dataBoundaryEgress: boolean;
  readonly dataBoundaryIngress: boolean;
  readonly sideEffectsAllowed: boolean;
}

/** Skill evaluation view (T-S5.20). */
export interface SkillEvaluationView {
  readonly skillId: string;
  readonly version: string;
  readonly datasetReferences: readonly string[];
  readonly criteriaReferences: readonly string[];
  readonly requiredEvaluationModes: readonly string[];
  readonly gateThreshold: number;
  readonly requiresEvaluation: boolean;
  readonly lastEvaluationResult: SkillEvaluationResultView | null;
}

export interface SkillEvaluationResultView {
  readonly executionId: string;
  readonly result: "PASS" | "FAIL" | "INCONCLUSIVE";
  readonly score: number;
  readonly evaluatedAt: string;
  readonly findingsCount: number;
}

/** Skill version entry view (T-S5.21). */
export interface SkillVersionView {
  readonly skillId: string;
  readonly version: string;
  readonly lifecycleState: SkillLifecycleState;
  readonly artifactDigest: string;
  readonly createdAt: string;
  readonly publishedAt: string | null;
  readonly deprecatedAt: string | null;
  readonly releaseGateStatus: SkillReleaseGateView;
}

export interface SkillReleaseGateView {
  readonly status: "PASS" | "FAIL" | "PENDING" | "SKIPPED";
  readonly checks: readonly SkillGateCheckView[];
}

export interface SkillGateCheckView {
  readonly name: string;
  readonly status: "PASS" | "FAIL" | "WARN" | "SKIPPED";
  readonly message: string | null;
}

/** Skill governance view (T-S5.22). */
export interface SkillGovernanceView {
  readonly skillId: string;
  readonly version: string;
  readonly owner: string;
  readonly domain: string;
  readonly riskLevel: SkillRiskLevel;
  readonly requiredRoles: readonly string[];
  readonly approvalRoles: readonly string[];
  readonly policyReference: string;
  readonly auditEntries: readonly SkillAuditEntryView[];
}

export interface SkillAuditEntryView {
  readonly timestamp: string;
  readonly action: string;
  readonly actor: string;
  readonly tenantId: string;
  readonly detail: string;
}

/** Skill usage metrics view (T-S5.22). */
export interface SkillUsageView {
  readonly skillId: string;
  readonly totalInvocations: number;
  readonly successCount: number;
  readonly failureCount: number;
  readonly avgLatencyMs: number;
  readonly lastInvokedAt: string | null;
  readonly activeAgents: readonly string[];
  readonly activeGraphs: readonly string[];
}

/** Skill execution history view (T-S5.22). */
export interface SkillExecutionView {
  readonly executionId: string;
  readonly skillId: string;
  readonly version: string;
  readonly callerId: string;
  readonly tenantId: string;
  readonly status: "SUCCESS" | "FAILED" | "DENIED" | "TIMEOUT";
  readonly startedAt: string;
  readonly completedAt: string | null;
  readonly latencyMs: number;
  readonly errorCode: string | null;
}

/** Skill invocation request from Studio (T-S5.23). */
export interface SkillInvocationRequest {
  readonly skillId: string;
  readonly version: string;
  readonly input: Record<string, unknown>;
  readonly tenantId: string;
  readonly callerId: string;
}

/** Skill invocation result (T-S5.23). */
export interface SkillInvocationResult {
  readonly executionId: string;
  readonly skillId: string;
  readonly version: string;
  readonly status: "SUCCESS" | "FAILED" | "DENIED" | "TIMEOUT";
  readonly output: Record<string, unknown> | null;
  readonly errorCode: string | null;
  readonly message: string | null;
  readonly latencyMs: number;
  readonly policyDecision: string;
  readonly auditRecorded: boolean;
}

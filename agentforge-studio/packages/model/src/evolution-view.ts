/**
 * Evolution viewer models.
 * AF-ENHANCE-12 §4: EvolutionCandidate / Validation / Lineage.
 *
 * Maps to backend agentforge-evolution types.
 */

// ── Candidate ──

/**
 * EvolutionCandidateView - evolution candidate overview.
 */
export interface EvolutionCandidateView {
  readonly candidateId: string;
  readonly agentId: string;
  readonly name: string;
  readonly status: CandidateStatusView;
  readonly changeCount: number;
  readonly evidenceCount: number;
  readonly score: number;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export type CandidateStatusView =
  | "PROPOSED"
  | "VALIDATING"
  | "EVIDENCE_GATHERING"
  | "REVIEW"
  | "APPROVED"
  | "REJECTED"
  | "DEPLOYING"
  | "DEPLOYED"
  | "ROLLED_BACK"
  | "RETIRED";

// ── Change ──

/**
 * EvolutionChangeView - a single change within a candidate.
 */
export interface EvolutionChangeView {
  readonly changeId: string;
  readonly candidateId: string;
  readonly scope: ChangeScopeView;
  readonly targetPath: string;
  readonly beforeValue?: string;
  readonly afterValue: string;
  readonly description: string;
}

export type ChangeScopeView =
  | "AGENT_DEFINITION"
  | "GRAPH_DEFINITION"
  | "CAPABILITY_BINDING"
  | "MODEL_CONFIG"
  | "POLICY_RULE"
  | "SKILL_VERSION"
  | "PROMPT_TEMPLATE";

// ── Evidence ──

/**
 * EvolutionEvidenceView - evidence supporting a candidate.
 */
export interface EvolutionEvidenceView {
  readonly evidenceId: string;
  readonly candidateId: string;
  readonly type: EvidenceTypeView;
  readonly source: string;
  readonly value: string;
  readonly collectedAt: string;
}

export type EvidenceTypeView =
  | "EVALUATION_SCORE"
  | "REGRESSION_TEST"
  | "PERFORMANCE_BENCHMARK"
  | "USER_FEEDBACK"
  | "A_B_COMPARISON"
  | "SAFETY_AUDIT"
  | "COST_ANALYSIS"
  | "COVERAGE_REPORT";

// ── Problem ──

/**
 * EvolutionProblemView - problem detected in evolution.
 */
export interface EvolutionProblemView {
  readonly problemId: string;
  readonly candidateId: string;
  readonly category: ProblemCategoryView;
  readonly severity: ProblemSeverityView;
  readonly description: string;
  readonly detectedAt: string;
  readonly resolved: boolean;
}

export type ProblemCategoryView =
  | "REGRESSION"
  | "PERFORMANCE"
  | "SAFETY"
  | "CORRECTNESS"
  | "COMPATIBILITY"
  | "RESOURCE"
  | "GOVERNANCE"
  | "OTHER";

export type ProblemSeverityView =
  | "CRITICAL"
  | "HIGH"
  | "MEDIUM"
  | "LOW"
  | "INFO";

// ── Validation ──

/**
 * ValidationResultView - validation gate result.
 */
export interface ValidationResultView {
  readonly resultId: string;
  readonly candidateId: string;
  readonly gateName: string;
  readonly passed: boolean;
  readonly score: number;
  readonly details: string;
  readonly validatedAt: string;
}

// ── Lineage ──

/**
 * CandidateLineageView - evolution lineage tree.
 */
export interface CandidateLineageView {
  readonly rootCandidateId: string;
  readonly rootAgentId: string;
  readonly generations: LineageGenerationView[];
}

export interface LineageGenerationView {
  readonly generation: number;
  readonly candidates: LineageNodeView[];
}

export interface LineageNodeView {
  readonly candidateId: string;
  readonly parentCandidateId?: string;
  readonly status: CandidateStatusView;
  readonly score: number;
  readonly changeCount: number;
}

// ── Policy ──

/**
 * EvolutionPolicyView - evolution policy config.
 */
export interface EvolutionPolicyView {
  readonly policyId: string;
  readonly name: string;
  readonly maxCandidates: number;
  readonly minScore: number;
  readonly requireEvidence: boolean;
  readonly autoApprove: boolean;
  readonly enabled: boolean;
}

/**
 * Evaluation domain models.
 * AF-STUDIO-01 §37-39: Evaluation Workspace.
 *
 * Maps to backend AE-12 Evaluation types.
 * Studio must NOT create parallel semantic types.
 */

// ── Evaluation Dataset ──

/**
 * EvaluationDataset - a collection of test cases
 * used for evaluation runs.
 */
export interface EvaluationDataset {
  readonly datasetId: string;
  readonly name: string;
  readonly description: string;
  readonly version: string;
  readonly caseCount: number;
  readonly tags: string[];
  readonly createdAt: string;
  readonly updatedAt: string;
  readonly metadata: Record<string, unknown>;
}

// ── Test Case ──

/**
 * TestCaseStatus - lifecycle of a test case.
 */
export type TestCaseStatus =
  | "DRAFT"
  | "ACTIVE"
  | "DISABLED"
  | "ARCHIVED";

/**
 * TestCase - a single evaluation case.
 */
export interface EvaluationTestCase {
  readonly caseId: string;
  readonly datasetId: string;
  readonly name: string;
  readonly description: string;
  readonly status: TestCaseStatus;
  readonly input: Record<string, unknown>;
  readonly expectedOutput: Record<string, unknown>;
  readonly criteria: EvaluationCriteria[];
  readonly tags: string[];
  readonly metadata: Record<string, unknown>;
}

// ── Evaluation Criteria ──

/**
 * CriteriaType - the kind of evaluation check.
 */
export type CriteriaType =
  | "SCHEMA_VALIDATION"
  | "POLICY_COMPLIANCE"
  | "GROUNDING"
  | "CITATION"
  | "LATENCY"
  | "TOKEN_BUDGET"
  | "COST"
  | "REGRESSION"
  | "CUSTOM";

/**
 * EvaluationCriteria - a single check within a test case.
 */
export interface EvaluationCriteria {
  readonly criteriaId: string;
  readonly type: CriteriaType;
  readonly name: string;
  readonly description: string;
  readonly threshold: number;
  readonly weight: number;
  readonly config: Record<string, unknown>;
}

// ── Evaluation Run ──

/**
 * EvaluationRunStatus - lifecycle of a run.
 */
export type EvaluationRunStatus =
  | "PENDING"
  | "RUNNING"
  | "COMPLETED"
  | "FAILED"
  | "CANCELLED";

/**
 * EvaluationRun - a single evaluation execution.
 */
export interface EvaluationRun {
  readonly runId: string;
  readonly datasetId: string;
  readonly datasetVersion: string;
  readonly agentId?: string;
  readonly graphId?: string;
  readonly status: EvaluationRunStatus;
  readonly totalCases: number;
  readonly completedCases: number;
  readonly passedCases: number;
  readonly failedCases: number;
  readonly startedAt: string;
  readonly completedAt?: string;
  readonly environmentFingerprint: EnvironmentFingerprint;
  readonly metadata: Record<string, unknown>;
}

// ── Evaluation Result ──

/**
 * CaseResultStatus - outcome of a single case.
 */
export type CaseResultStatus =
  | "PASSED"
  | "FAILED"
  | "ERROR"
  | "SKIPPED";

/**
 * EvaluationCaseResult - result for one test case.
 */
export interface EvaluationCaseResult {
  readonly caseId: string;
  readonly caseName: string;
  readonly status: CaseResultStatus;
  readonly criteriaResults: CriteriaResult[];
  readonly actualOutput: Record<string, unknown>;
  readonly latencyMs: number;
  readonly tokenUsage: number;
  readonly cost: number;
  readonly errors: string[];
}

/**
 * CriteriaResultStatus - outcome of a criteria check.
 */
export type CriteriaResultStatus = "PASS" | "FAIL" | "WARN" | "SKIP";

/**
 * CriteriaResult - result of a single criteria evaluation.
 */
export interface CriteriaResult {
  readonly criteriaId: string;
  readonly criteriaName: string;
  readonly type: CriteriaType;
  readonly status: CriteriaResultStatus;
  readonly score: number;
  readonly threshold: number;
  readonly message: string;
}

// ── Evaluation Report ──

/**
 * EvaluationReport - aggregated report for a run.
 * AF-STUDIO-01 §38: Evaluation Report.
 */
export interface EvaluationReport {
  readonly runId: string;
  readonly datasetId: string;
  readonly totalCases: number;
  readonly passedCases: number;
  readonly failedCases: number;
  readonly errorCases: number;
  readonly skippedCases: number;
  readonly policyViolations: number;
  readonly schemaViolations: number;
  readonly avgLatencyMs: number;
  readonly p95LatencyMs: number;
  readonly totalTokenUsage: number;
  readonly totalCost: number;
  readonly passRate: number;
  readonly environmentFingerprint: EnvironmentFingerprint;
  readonly caseResults: EvaluationCaseResult[];
  readonly findings: EvaluationFinding[];
}

// ── Environment Fingerprint ──

/**
 * EnvironmentFingerprint - versions of all components
 * at the time of evaluation.
 * AF-STUDIO-01 §38: Environment Fingerprint.
 */
export interface EnvironmentFingerprint {
  readonly agentVersion?: string;
  readonly graphVersion?: string;
  readonly modelVersion?: string;
  readonly capabilityVersion?: string;
  readonly promptVersion?: string;
  readonly memoryVersion?: string;
  readonly knowledgeVersion?: string;
  readonly policyVersion?: string;
  readonly runtimeVersion: string;
}

// ── Evaluation Finding ──

/**
 * FindingSeverity - how serious a finding is.
 */
export type FindingSeverity = "INFO" | "WARNING" | "ERROR" | "CRITICAL";

/**
 * EvaluationFinding - an observation from the evaluation.
 */
export interface EvaluationFinding {
  readonly findingId: string;
  readonly severity: FindingSeverity;
  readonly category: string;
  readonly title: string;
  readonly description: string;
  readonly affectedCaseIds: string[];
  readonly recommendation: string;
}

// ── Regression ──

/**
 * RegressionComparison - diff between two evaluation runs.
 * AF-STUDIO-01 §39: Regression.
 */
export interface RegressionComparison {
  readonly currentRunId: string;
  readonly baselineRunId: string;
  readonly behaviorChanges: RegressionChange[];
  readonly schemaChanges: RegressionChange[];
  readonly policyChanges: RegressionChange[];
  readonly latencyChanges: NumericRegressionChange[];
  readonly costChanges: NumericRegressionChange[];
  readonly groundingChanges: NumericRegressionChange[];
}

/**
 * RegressionChange - a single regression diff.
 */
export interface RegressionChange {
  readonly type: "ADDED" | "REMOVED" | "CHANGED";
  readonly description: string;
  readonly affectedCaseId?: string;
  readonly details: Record<string, unknown>;
}

/**
 * NumericRegressionChange - regression with numeric delta.
 */
export interface NumericRegressionChange {
  readonly metric: string;
  readonly previousValue: number;
  readonly currentValue: number;
  readonly delta: number;
  readonly deltaPercent: number;
  readonly improved: boolean;
}

/**
 * Knowledge / RAG viewer models.
 * AF-STUDIO-01 §32-33: Knowledge / RAG Viewer, Citation Viewer.
 *
 * Maps to backend AE-09 Knowledge types.
 */

// ── Knowledge Source ──

export type KnowledgeSourceStatus =
  | "ACTIVE"
  | "PROCESSING"
  | "ERROR"
  | "DISABLED";

export interface KnowledgeSourceView {
  readonly sourceId: string;
  readonly name: string;
  readonly description: string;
  readonly type: "FILE" | "URL" | "DATABASE" | "API";
  readonly status: KnowledgeSourceStatus;
  readonly documentCount: number;
  readonly chunkCount: number;
  readonly createdAt: string;
  readonly updatedAt: string;
  readonly metadata: Record<string, unknown>;
}

// ── Document ──

export interface KnowledgeDocumentView {
  readonly documentId: string;
  readonly sourceId: string;
  readonly title: string;
  readonly version: number;
  readonly chunkCount: number;
  readonly status: "INDEXED" | "PROCESSING" | "ERROR";
  readonly createdAt: string;
  readonly metadata: Record<string, unknown>;
}

// ── Chunk ──

export interface KnowledgeChunkView {
  readonly chunkId: string;
  readonly documentId: string;
  readonly content: string;
  readonly index: number;
  readonly tokenCount: number;
  readonly embeddingModel?: string;
}

// ── Retrieval ──

export interface RetrievalRequestView {
  readonly query: string;
  readonly topK: number;
  readonly strategy: "KEYWORD" | "VECTOR" | "HYBRID";
  readonly filters: Record<string, unknown>;
}

export interface RetrievalResultView {
  readonly chunkId: string;
  readonly documentId: string;
  readonly documentTitle: string;
  readonly content: string;
  readonly score: number;
  readonly rank: number;
}

// ── Retrieval Trace ──

/**
 * RetrievalTraceView - full retrieval pipeline trace.
 * AF-STUDIO-01 §32: Retrieval Trace.
 */
export interface RetrievalTraceView {
  readonly traceId: string;
  readonly query: string;
  readonly stages: RetrievalStageView[];
  readonly totalDurationMs: number;
  readonly resultCount: number;
}

export interface RetrievalStageView {
  readonly stage: "SCOPE_FILTER" | "PERMISSION" | "KEYWORD" | "VECTOR" | "MERGE" | "DEDUP" | "RERANK" | "POLICY_FILTER" | "TOP_K";
  readonly durationMs: number;
  readonly inputCount: number;
  readonly outputCount: number;
}

// ── Citation ──

/**
 * CitationView - answer-to-source traceability.
 * AF-STUDIO-01 §33: Citation Viewer.
 */
export interface CitationView {
  readonly citationId: string;
  readonly answerSegment: string;
  readonly chunkId: string;
  readonly documentId: string;
  readonly documentTitle: string;
  readonly sourceUrl?: string;
  readonly score: number;
}

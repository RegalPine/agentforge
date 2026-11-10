/**
 * Search Viewer Models (AF-STUDIO-01 §49)
 *
 * Global Search across:
 *   Agent / Capability / Graph / Execution / Tool / Model /
 *   Memory / Knowledge / MCP / Policy / Evaluation
 */

export type SearchResultType =
  | "AGENT"
  | "CAPABILITY"
  | "GRAPH"
  | "EXECUTION"
  | "TOOL"
  | "MODEL"
  | "MEMORY"
  | "KNOWLEDGE"
  | "MCP"
  | "POLICY"
  | "EVALUATION";

export interface SearchRequest {
  readonly query: string;
  readonly types?: SearchResultType[];
  readonly tenantId?: string;
  readonly limit?: number;
}

export interface SearchResult {
  readonly type: SearchResultType;
  readonly id: string;
  readonly name: string;
  readonly description: string;
  readonly version?: string;
  readonly status?: string;
  readonly score: number;
  readonly link: string;
  readonly highlights: string[];
}

export interface SearchResponse {
  readonly query: string;
  readonly totalResults: number;
  readonly results: SearchResult[];
  readonly groupedResults: Record<SearchResultType, SearchResult[]>;
}

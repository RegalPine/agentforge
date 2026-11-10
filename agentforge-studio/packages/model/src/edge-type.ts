/**
 * EdgeType - must match backend frozen definition exactly.
 */
export type EdgeType =
  | "NORMAL"
  | "CONDITIONAL"
  | "ERROR"
  | "LOOP_BACK";

export const ALL_EDGE_TYPES: readonly EdgeType[] = [
  "NORMAL", "CONDITIONAL", "ERROR", "LOOP_BACK",
] as const;

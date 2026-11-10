/**
 * NodeType - must match backend frozen definition exactly.
 * Studio must NOT add custom node types.
 */
export type NodeType =
  | "START"
  | "END"
  | "ACTION"
  | "AGENT"
  | "CAPABILITY"
  | "TOOL"
  | "LLM"
  | "CONDITION"
  | "SWITCH"
  | "PARALLEL"
  | "JOIN"
  | "LOOP"
  | "ITERATOR"
  | "WAIT"
  | "HUMAN"
  | "SUBGRAPH";

export const ALL_NODE_TYPES: readonly NodeType[] = [
  "START", "END", "ACTION", "AGENT", "CAPABILITY", "TOOL",
  "LLM", "CONDITION", "SWITCH", "PARALLEL", "JOIN",
  "LOOP", "ITERATOR", "WAIT", "HUMAN", "SUBGRAPH",
] as const;

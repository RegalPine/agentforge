import type { EdgeType } from "./edge-type.js";

/**
 * EdgeDefinition - maps to backend GraphDefinition.edge entries.
 */
export interface EdgeDefinition {
  id: string;
  source: string;
  target: string;
  type: EdgeType;
  condition?: string;
}

export function createEdge(
  id: string,
  source: string,
  target: string,
  type: EdgeType = "NORMAL",
  condition?: string
): EdgeDefinition {
  return { id, source, target, type, condition };
}

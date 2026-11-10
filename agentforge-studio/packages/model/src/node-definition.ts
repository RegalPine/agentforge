import type { NodeType } from "./node-type.js";
import type { Reference } from "./reference.js";

/**
 * NodeDefinition - maps to backend GraphDefinition.node entries.
 * Studio must NOT add fields that alter graph semantics.
 */
export interface NodeDefinition {
  id: string;
  type: NodeType;
  name?: string;
  agent?: Reference;
  capability?: Reference;
  config: Record<string, unknown>;
  metadata?: Record<string, unknown>;
}

export function createNode(
  id: string,
  type: NodeType,
  options?: {
    name?: string;
    agent?: Reference;
    capability?: Reference;
    config?: Record<string, unknown>;
    metadata?: Record<string, unknown>;
  }
): NodeDefinition {
  return {
    id,
    type,
    name: options?.name,
    agent: options?.agent,
    capability: options?.capability,
    config: options?.config ?? {},
    metadata: options?.metadata,
  };
}

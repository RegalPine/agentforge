import type { NodeDefinition } from "./node-definition.js";
import type { EdgeDefinition } from "./edge-definition.js";
import type { DataBinding, Expression } from "./binding.js";

/**
 * GraphDefinition - the canonical graph model.
 * Maps directly to backend io.github.regalpine.agentforge.graph.GraphDefinition.
 *
 * Studio must NOT create a parallel "StudioGraph" type.
 * This is the single source of truth for graph semantics.
 */
export interface GraphDefinition {
  id: string;
  version: string;
  name: string;
  description?: string;

  inputs: Record<string, unknown>;
  outputs: Record<string, unknown>;

  nodes: NodeDefinition[];
  edges: EdgeDefinition[];

  bindings: DataBinding[];
  expressions: Expression[];

  policies: Record<string, unknown>;
  metadata: Record<string, unknown>;
}

/**
 * Create a minimal valid GraphDefinition.
 */
export function createGraphDefinition(
  id: string,
  version: string,
  name: string,
  options?: {
    description?: string;
    nodes?: NodeDefinition[];
    edges?: EdgeDefinition[];
    bindings?: DataBinding[];
    expressions?: Expression[];
    policies?: Record<string, unknown>;
    metadata?: Record<string, unknown>;
  }
): GraphDefinition {
  return {
    id,
    version,
    name,
    description: options?.description,
    inputs: {},
    outputs: {},
    nodes: options?.nodes ?? [],
    edges: options?.edges ?? [],
    bindings: options?.bindings ?? [],
    expressions: options?.expressions ?? [],
    policies: options?.policies ?? {},
    metadata: options?.metadata ?? {},
  };
}

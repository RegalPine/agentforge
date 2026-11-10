import type { NodeDefinition, NodeType } from "@agentforge/model";

/**
 * NodeData - data payload for each React Flow node.
 * Contains the AgentForge NodeDefinition plus UI-only display properties.
 *
 * React Flow handles rendering; NodeDefinition carries graph semantics.
 */
export interface NodeData {
  /** The canonical AgentForge node definition */
  definition: NodeDefinition;
  /** Display label (derived from definition.name or definition.type) */
  label: string;
  /** Whether this node is currently selected in the editor */
  selected?: boolean;
}

/**
 * Node type configuration for palette and rendering.
 */
export interface NodeTypeConfig {
  type: NodeType;
  label: string;
  description: string;
  category: "flow" | "action" | "control" | "integration";
  icon: string;
  color: string;
}

/**
 * Registry of all supported node types with display metadata.
 * Palette reads from this; Studio must NOT add custom types.
 */
export const NODE_TYPE_CONFIGS: readonly NodeTypeConfig[] = [
  { type: "START", label: "Start", description: "Graph entry point", category: "flow", icon: "▶", color: "#22c55e" },
  { type: "END", label: "End", description: "Graph exit point", category: "flow", icon: "⏹", color: "#ef4444" },
  { type: "ACTION", label: "Action", description: "Generic action step", category: "action", icon: "⚡", color: "#3b82f6" },
  { type: "AGENT", label: "Agent", description: "Agent invocation", category: "integration", icon: "🤖", color: "#8b5cf6" },
  { type: "CAPABILITY", label: "Capability", description: "Capability invocation", category: "integration", icon: "🔧", color: "#f59e0b" },
  { type: "TOOL", label: "Tool", description: "Tool invocation", category: "integration", icon: "🛠", color: "#06b6d4" },
  { type: "LLM", label: "LLM", description: "LLM completion", category: "integration", icon: "🧠", color: "#ec4899" },
  { type: "CONDITION", label: "Condition", description: "Conditional branch", category: "control", icon: "◇", color: "#6366f1" },
  { type: "SWITCH", label: "Switch", description: "Multi-way branch", category: "control", icon: "⊞", color: "#6366f1" },
  { type: "PARALLEL", label: "Parallel", description: "Parallel execution fork", category: "control", icon: "⫸", color: "#14b8a6" },
  { type: "JOIN", label: "Join", description: "Parallel execution join", category: "control", icon: "⫷", color: "#14b8a6" },
  { type: "LOOP", label: "Loop", description: "Loop construct", category: "control", icon: "↻", color: "#f97316" },
  { type: "ITERATOR", label: "Iterator", description: "Collection iteration", category: "control", icon: "⇄", color: "#f97316" },
  { type: "WAIT", label: "Wait", description: "Wait for external signal", category: "control", icon: "⏳", color: "#a855f7" },
  { type: "HUMAN", label: "Human", description: "Human-in-the-loop", category: "integration", icon: "👤", color: "#64748b" },
  { type: "SUBGRAPH", label: "Subgraph", description: "Nested graph invocation", category: "control", icon: "▣", color: "#78716c" },
];

export function getNodeConfig(type: NodeType): NodeTypeConfig | undefined {
  return NODE_TYPE_CONFIGS.find((c) => c.type === type);
}

/**
 * Map an AgentForge NodeType to the React Flow registered node type name.
 *
 * Specialized renderers are registered for START/END, AGENT, CAPABILITY, SUBGRAPH.
 * All other types fall back to the universal "agentForgeNode" renderer.
 */
export function getReactFlowNodeType(nodeType: NodeType): string {
  switch (nodeType) {
    case "START":
    case "END":
      return "startEndNode";
    case "AGENT":
      return "agentNode";
    case "CAPABILITY":
      return "capabilityNode";
    case "SUBGRAPH":
      return "subgraphNode";
    default:
      return "agentForgeNode";
  }
}

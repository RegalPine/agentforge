import type {
  GraphDefinition,
  NodeDefinition,
  EdgeDefinition,
  CanvasState,
} from "@agentforge/model";
import { createGraphDefinition, createDefaultCanvasState } from "@agentforge/model";
import type { NodeData } from "../nodes/node-data.js";
import { getReactFlowNodeType } from "../nodes/node-data.js";
import type { Edge, Node } from "reactflow";

/**
 * Serialization module - converts between GraphDefinition ↔ React Flow state.
 *
 * Key principle: GraphDefinition is the canonical model.
 * React Flow nodes/edges are a VIEW PROJECTION.
 *
 * serialize:   React Flow → GraphDefinition
 * deserialize: GraphDefinition → React Flow
 */

/**
 * EditorState combines graph semantics with canvas view state.
 */
export interface EditorState {
  graph: GraphDefinition;
  canvas: CanvasState;
}

/**
 * Deserialize a GraphDefinition into React Flow nodes and edges.
 * Positions come from CanvasState (not from GraphDefinition).
 */
export function deserialize(
  graph: GraphDefinition,
  canvas: CanvasState,
): { nodes: Node<NodeData>[]; edges: Edge[] } {
  const rfNodes: Node<NodeData>[] = graph.nodes.map((def, index) => {
    const pos = canvas.positions[def.id] ?? {
      x: 250 + (index % 4) * 250,
      y: 100 + Math.floor(index / 4) * 150,
    };

    return {
      id: def.id,
      type: getReactFlowNodeType(def.type),
      position: pos,
      data: {
        definition: def,
        label: def.name ?? def.type,
      } as NodeData,
    };
  });

  const rfEdges: Edge[] = graph.edges.map((def) => ({
    id: def.id,
    source: def.source,
    target: def.target,
    label: def.condition,
    // G3-7: Store edge type in data for
    // round-trip preservation
    data: { edgeType: def.type },
    type: def.type === "ERROR" ? "default" : "default",
    animated: def.type === "LOOP_BACK",
    style: def.type === "ERROR" ? { stroke: "#ef4444" } : undefined,
  }));

  return { nodes: rfNodes, edges: rfEdges };
}

/**
 * Serialize React Flow state back into a GraphDefinition.
 * Extracts NodeDefinition from each React Flow node's data.
 */
export function serialize(
  rfNodes: Node<NodeData>[],
  rfEdges: Edge[],
): GraphDefinition {
  const nodes: NodeDefinition[] = rfNodes.map((rfNode) => ({
    ...rfNode.data.definition,
    // Update name from data if changed
    name: rfNode.data.label !== rfNode.data.definition.type
      ? rfNode.data.label
      : rfNode.data.definition.name,
  }));

  const edges: EdgeDefinition[] = rfEdges.map((rfEdge) => ({
    id: rfEdge.id,
    source: rfEdge.source,
    target: rfEdge.target,
    type: (rfEdge.data?.edgeType as EdgeDefinition["type"]) ?? "NORMAL",
    condition: typeof rfEdge.label === "string" ? rfEdge.label : undefined,
  }));

  // Derive graph ID/version from first node metadata or use defaults
  const graphId = nodes[0]?.metadata?.graphId as string ?? "untitled";
  const graphVersion = nodes[0]?.metadata?.graphVersion as string ?? "0.0.1";
  const graphName = nodes[0]?.metadata?.graphName as string ?? "Untitled Graph";

  return createGraphDefinition(graphId, graphVersion, graphName, {
    nodes,
    edges,
  });
}

/**
 * Extract canvas state from React Flow nodes (positions) and viewport.
 */
export function extractCanvasState(
  rfNodes: Node<NodeData>[],
  viewport?: { x: number; y: number; zoom: number },
): CanvasState {
  const positions: Record<string, { x: number; y: number }> = {};
  for (const node of rfNodes) {
    positions[node.id] = { x: node.position.x, y: node.position.y };
  }

  return {
    positions,
    viewport: viewport ?? createDefaultCanvasState().viewport,
  };
}

/**
 * Round-trip test: GraphDefinition → deserialize → serialize → compare.
 * Must produce a canonically equivalent GraphDefinition.
 */
export function roundTripEquals(
  original: GraphDefinition,
  roundTripped: GraphDefinition,
): boolean {
  // Compare node IDs, types, references
  if (original.nodes.length !== roundTripped.nodes.length) return false;
  if (original.edges.length !== roundTripped.edges.length) return false;

  const origNodeIds = new Set(original.nodes.map((n) => n.id));
  for (const node of roundTripped.nodes) {
    if (!origNodeIds.has(node.id)) return false;
    const orig = original.nodes.find((n) => n.id === node.id);
    if (!orig) return false;
    if (orig.type !== node.type) return false;
    if (orig.capability?.id !== node.capability?.id) return false;
    if (orig.capability?.version !== node.capability?.version) return false;
    if (orig.agent?.id !== node.agent?.id) return false;
    if (orig.agent?.version !== node.agent?.version) return false;
  }

  const origEdgeIds = new Set(original.edges.map((e) => e.id));
  for (const edge of roundTripped.edges) {
    if (!origEdgeIds.has(edge.id)) return false;
    const orig = original.edges.find((e) => e.id === edge.id);
    if (!orig) return false;
    if (orig.source !== edge.source) return false;
    if (orig.target !== edge.target) return false;
    if (orig.type !== edge.type) return false;
  }

  return true;
}

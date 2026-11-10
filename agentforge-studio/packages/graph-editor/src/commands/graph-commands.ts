import type { GraphDefinition, NodeDefinition, EdgeDefinition } from "@agentforge/model";

/**
 * GraphCommand - command pattern for undo/redo.
 *
 * All graph mutations go through commands.
 * Commands operate on the GraphDefinition model, NOT on React Flow DOM.
 */
export interface GraphCommand {
  readonly type: string;
  execute(graph: GraphDefinition): GraphDefinition;
  undo(graph: GraphDefinition): GraphDefinition;
}

/**
 * AddNodeCommand - adds a node to the graph.
 */
export class AddNodeCommand implements GraphCommand {
  readonly type = "ADD_NODE";

  constructor(private readonly node: NodeDefinition) {}

  execute(graph: GraphDefinition): GraphDefinition {
    return {
      ...graph,
      nodes: [...graph.nodes, this.node],
    };
  }

  undo(graph: GraphDefinition): GraphDefinition {
    return {
      ...graph,
      nodes: graph.nodes.filter((n) => n.id !== this.node.id),
    };
  }
}

/**
 * DeleteNodeCommand - removes a node and its connected edges.
 */
export class DeleteNodeCommand implements GraphCommand {
  readonly type = "DELETE_NODE";
  private removedEdges: EdgeDefinition[] = [];

  constructor(private readonly nodeId: string) {}

  execute(graph: GraphDefinition): GraphDefinition {
    this.removedEdges = graph.edges.filter(
      (e) => e.source === this.nodeId || e.target === this.nodeId,
    );
    return {
      ...graph,
      nodes: graph.nodes.filter((n) => n.id !== this.nodeId),
      edges: graph.edges.filter(
        (e) => e.source !== this.nodeId && e.target !== this.nodeId,
      ),
    };
  }

  undo(graph: GraphDefinition): GraphDefinition {
    return {
      ...graph,
      nodes: [...graph.nodes, graph.nodes.find((n) => n.id === this.nodeId)!].filter(Boolean),
      edges: [...graph.edges, ...this.removedEdges],
    };
  }
}

/**
 * UpdateNodeCommand - updates a node's properties.
 */
export class UpdateNodeCommand implements GraphCommand {
  readonly type = "UPDATE_NODE";
  private previousNode: NodeDefinition | undefined;

  constructor(
    private readonly nodeId: string,
    private readonly updates: Partial<NodeDefinition>,
  ) {}

  execute(graph: GraphDefinition): GraphDefinition {
    this.previousNode = graph.nodes.find((n) => n.id === this.nodeId);
    if (!this.previousNode) return graph;

    return {
      ...graph,
      nodes: graph.nodes.map((n) =>
        n.id === this.nodeId ? { ...n, ...this.updates } : n,
      ),
    };
  }

  undo(graph: GraphDefinition): GraphDefinition {
    if (!this.previousNode) return graph;
    return {
      ...graph,
      nodes: graph.nodes.map((n) =>
        n.id === this.nodeId ? this.previousNode! : n,
      ),
    };
  }
}

/**
 * AddEdgeCommand - adds an edge between two nodes.
 */
export class AddEdgeCommand implements GraphCommand {
  readonly type = "ADD_EDGE";

  constructor(private readonly edge: EdgeDefinition) {}

  execute(graph: GraphDefinition): GraphDefinition {
    return {
      ...graph,
      edges: [...graph.edges, this.edge],
    };
  }

  undo(graph: GraphDefinition): GraphDefinition {
    return {
      ...graph,
      edges: graph.edges.filter((e) => e.id !== this.edge.id),
    };
  }
}

/**
 * DeleteEdgeCommand - removes an edge.
 */
export class DeleteEdgeCommand implements GraphCommand {
  readonly type = "DELETE_EDGE";
  private removedEdge: EdgeDefinition | undefined;

  constructor(private readonly edgeId: string) {}

  execute(graph: GraphDefinition): GraphDefinition {
    this.removedEdge = graph.edges.find((e) => e.id === this.edgeId);
    return {
      ...graph,
      edges: graph.edges.filter((e) => e.id !== this.edgeId),
    };
  }

  undo(graph: GraphDefinition): GraphDefinition {
    if (!this.removedEdge) return graph;
    return {
      ...graph,
      edges: [...graph.edges, this.removedEdge],
    };
  }
}

/**
 * MoveNodeCommand - moves a node to a new position.
 *
 * Positions are stored in graph.metadata._canvas.positions
 * (canvas state is kept alongside the graph for portability).
 */
export class MoveNodeCommand implements GraphCommand {
  readonly type = "MOVE_NODE";
  private previousPosition: { x: number; y: number } | undefined;

  constructor(
    private readonly nodeId: string,
    private readonly newPosition: { x: number; y: number },
  ) {}

  execute(graph: GraphDefinition): GraphDefinition {
    const positions = this.getPositions(graph);
    this.previousPosition = positions[this.nodeId] ?? { x: 0, y: 0 };
    positions[this.nodeId] = this.newPosition;
    return this.setPositions(graph, positions);
  }

  undo(graph: GraphDefinition): GraphDefinition {
    if (!this.previousPosition) return graph;
    const positions = this.getPositions(graph);
    positions[this.nodeId] = this.previousPosition;
    return this.setPositions(graph, positions);
  }

  private getPositions(graph: GraphDefinition): Record<string, { x: number; y: number }> {
    const canvas = (graph.metadata as Record<string, unknown>)?._canvas as Record<string, unknown> | undefined;
    return (canvas?.positions as Record<string, { x: number; y: number }>) ?? {};
  }

  private setPositions(
    graph: GraphDefinition,
    positions: Record<string, { x: number; y: number }>,
  ): GraphDefinition {
    const metadata = { ...graph.metadata };
    const canvas = { ...(metadata._canvas as Record<string, unknown> ?? {}) };
    canvas.positions = positions;
    metadata._canvas = canvas;
    return { ...graph, metadata };
  }
}

/**
 * BatchMoveCommand - moves multiple nodes at once (e.g., drag selection).
 */
export class BatchMoveCommand implements GraphCommand {
  readonly type = "BATCH_MOVE";
  private previousPositions: Record<string, { x: number; y: number }> = {};

  constructor(
    private readonly moves: Record<string, { x: number; y: number }>,
  ) {}

  execute(graph: GraphDefinition): GraphDefinition {
    const positions = this.getPositions(graph);
    for (const [nodeId, newPos] of Object.entries(this.moves)) {
      this.previousPositions[nodeId] = positions[nodeId] ?? { x: 0, y: 0 };
      positions[nodeId] = newPos;
    }
    return this.setPositions(graph, positions);
  }

  undo(graph: GraphDefinition): GraphDefinition {
    const positions = this.getPositions(graph);
    for (const [nodeId, prevPos] of Object.entries(this.previousPositions)) {
      positions[nodeId] = prevPos;
    }
    return this.setPositions(graph, positions);
  }

  private getPositions(graph: GraphDefinition): Record<string, { x: number; y: number }> {
    const canvas = (graph.metadata as Record<string, unknown>)?._canvas as Record<string, unknown> | undefined;
    return (canvas?.positions as Record<string, { x: number; y: number }>) ?? {};
  }

  private setPositions(
    graph: GraphDefinition,
    positions: Record<string, { x: number; y: number }>,
  ): GraphDefinition {
    const metadata = { ...graph.metadata };
    const canvas = { ...(metadata._canvas as Record<string, unknown> ?? {}) };
    canvas.positions = positions;
    metadata._canvas = canvas;
    return { ...graph, metadata };
  }
}

/**
 * UpdateEdgeCommand - updates an edge's properties (type, condition).
 */
export class UpdateEdgeCommand implements GraphCommand {
  readonly type = "UPDATE_EDGE";
  private previousEdge: EdgeDefinition | undefined;

  constructor(
    private readonly edgeId: string,
    private readonly updates: Partial<EdgeDefinition>,
  ) {}

  execute(graph: GraphDefinition): GraphDefinition {
    this.previousEdge = graph.edges.find((e) => e.id === this.edgeId);
    if (!this.previousEdge) return graph;

    return {
      ...graph,
      edges: graph.edges.map((e) =>
        e.id === this.edgeId ? { ...e, ...this.updates } : e,
      ),
    };
  }

  undo(graph: GraphDefinition): GraphDefinition {
    if (!this.previousEdge) return graph;
    return {
      ...graph,
      edges: graph.edges.map((e) =>
        e.id === this.edgeId ? this.previousEdge! : e,
      ),
    };
  }
}

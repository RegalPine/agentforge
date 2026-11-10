// @agentforge/graph-editor - Visual graph editor based on React Flow
//
// React Flow handles: Canvas Rendering, Interaction, Dragging, Connection, Zoom
// This package does NOT: Graph Semantic Validation, Compilation, Execution

export {
  AgentForgeNode,
  StartEndNode,
  AgentNode,
  CapabilityNode,
  SubgraphNode,
  NODE_TYPE_CONFIGS,
  getNodeConfig,
  getReactFlowNodeType,
} from "./nodes/index.js";
export type { NodeData, NodeTypeConfig } from "./nodes/index.js";

export type { GraphCommand } from "./commands/index.js";
export {
  AddNodeCommand,
  DeleteNodeCommand,
  UpdateNodeCommand,
  AddEdgeCommand,
  DeleteEdgeCommand,
  MoveNodeCommand,
  BatchMoveCommand,
  UpdateEdgeCommand,
  CommandHistory,
} from "./commands/index.js";

export type { EditorState } from "./serialization/index.js";
export {
  deserialize,
  serialize,
  extractCanvasState,
  roundTripEquals,
} from "./serialization/index.js";

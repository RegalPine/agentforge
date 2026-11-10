import { Handle, Position, type NodeProps } from "reactflow";
import type { NodeData } from "./node-data.js";
import { getNodeConfig } from "./node-data.js";

/**
 * AgentForgeNode - the universal node renderer for all NodeType values.
 *
 * Renders within React Flow canvas. Does NOT call providers or execute anything.
 * Displays node type, name, and reference information.
 */
export function AgentForgeNode({ data, selected }: NodeProps<NodeData>) {
  const config = getNodeConfig(data.definition.type);
  const bgColor = config?.color ?? "#6b7280";
  const icon = config?.icon ?? "•";

  return (
    <div
      className={`af-node ${selected ? "af-node--selected" : ""}`}
      style={{ borderColor: bgColor }}
    >
      <Handle type="target" position={Position.Top} className="af-handle" />

      <div className="af-node__header" style={{ backgroundColor: bgColor }}>
        <span className="af-node__icon">{icon}</span>
        <span className="af-node__type">{data.definition.type}</span>
      </div>

      <div className="af-node__body">
        <div className="af-node__name">{data.label}</div>

        {data.definition.capability && (
          <div className="af-node__ref">
            <span className="af-node__ref-label">Capability:</span>
            <span className="af-node__ref-value">
              {data.definition.capability.id}:{data.definition.capability.version}
            </span>
          </div>
        )}

        {data.definition.agent && (
          <div className="af-node__ref">
            <span className="af-node__ref-label">Agent:</span>
            <span className="af-node__ref-value">
              {data.definition.agent.id}:{data.definition.agent.version}
            </span>
          </div>
        )}
      </div>

      <Handle type="source" position={Position.Bottom} className="af-handle" />
    </div>
  );
}

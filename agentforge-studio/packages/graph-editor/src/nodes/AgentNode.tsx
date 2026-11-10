import { Handle, Position, type NodeProps } from "reactflow";
import type { NodeData } from "./node-data.js";
import { getNodeConfig } from "./node-data.js";

/**
 * AgentNode - specialized renderer for AGENT nodes.
 *
 * Visual: accent border, agent reference badge, and status indicator.
 * Shows agent ID:version when a reference is present.
 */
export function AgentNode({ data, selected }: NodeProps<NodeData>) {
  const config = getNodeConfig(data.definition.type);
  const bgColor = config?.color ?? "#8b5cf6";
  const icon = config?.icon ?? "🤖";
  const agentRef = data.definition.agent;

  return (
    <div
      className={`af-node af-node--agent ${selected ? "af-node--selected" : ""}`}
      style={{ borderColor: bgColor }}
    >
      <Handle type="target" position={Position.Top} className="af-handle" />

      <div className="af-node__header" style={{ backgroundColor: bgColor }}>
        <span className="af-node__icon">{icon}</span>
        <span className="af-node__type">AGENT</span>
      </div>

      <div className="af-node__body">
        <div className="af-node__name">{data.label}</div>

        {agentRef && (
          <div className="af-node__ref">
            <span className="af-node__ref-label">Agent:</span>
            <span className="af-node__ref-value af-node__badge">
              {agentRef.id}
              <span className="af-node__version">:{agentRef.version}</span>
            </span>
          </div>
        )}

        {data.definition.config && Object.keys(data.definition.config).length > 0 && (
          <div className="af-node__config-hint">
            {Object.keys(data.definition.config).length} config field(s)
          </div>
        )}
      </div>

      <Handle type="source" position={Position.Bottom} className="af-handle" />
    </div>
  );
}

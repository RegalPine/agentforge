import { Handle, Position, type NodeProps } from "reactflow";
import type { NodeData } from "./node-data.js";
import { getNodeConfig } from "./node-data.js";

/**
 * CapabilityNode - specialized renderer for CAPABILITY nodes.
 *
 * Visual: amber accent with capability reference badge.
 * Shows capability ID:version and provider hint.
 */
export function CapabilityNode({ data, selected }: NodeProps<NodeData>) {
  const config = getNodeConfig(data.definition.type);
  const bgColor = config?.color ?? "#f59e0b";
  const icon = config?.icon ?? "🔧";
  const capRef = data.definition.capability;

  return (
    <div
      className={`af-node af-node--capability ${selected ? "af-node--selected" : ""}`}
      style={{ borderColor: bgColor }}
    >
      <Handle type="target" position={Position.Top} className="af-handle" />

      <div className="af-node__header" style={{ backgroundColor: bgColor }}>
        <span className="af-node__icon">{icon}</span>
        <span className="af-node__type">CAPABILITY</span>
      </div>

      <div className="af-node__body">
        <div className="af-node__name">{data.label}</div>

        {capRef && (
          <div className="af-node__ref">
            <span className="af-node__ref-label">Capability:</span>
            <span className="af-node__ref-value af-node__badge">
              {capRef.id}
              <span className="af-node__version">:{capRef.version}</span>
            </span>
          </div>
        )}

        {data.definition.config?.provider != null && (
          <div className="af-node__provider">
            Provider: {String(data.definition.config.provider)}
          </div>
        )}
      </div>

      <Handle type="source" position={Position.Bottom} className="af-handle" />
    </div>
  );
}

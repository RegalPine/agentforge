import { Handle, Position, type NodeProps } from "reactflow";
import type { NodeData } from "./node-data.js";
import { getNodeConfig } from "./node-data.js";

/**
 * StartEndNode - specialized renderer for START and END nodes.
 *
 * Visual: rounded pill shape with distinct entry/exit styling.
 * START has only a source handle (bottom); END has only a target handle (top).
 */
export function StartEndNode({ data, selected }: NodeProps<NodeData>) {
  const config = getNodeConfig(data.definition.type);
  const bgColor = config?.color ?? "#6b7280";
  const icon = config?.icon ?? "•";
  const isStart = data.definition.type === "START";

  return (
    <div
      className={`af-node af-node--terminal ${selected ? "af-node--selected" : ""}`}
      style={{
        borderColor: bgColor,
        borderRadius: "9999px",
        minWidth: "100px",
        textAlign: "center",
      }}
    >
      {isStart ? (
        <Handle type="source" position={Position.Bottom} className="af-handle" />
      ) : (
        <Handle type="target" position={Position.Top} className="af-handle" />
      )}

      <div
        className="af-node__header af-node__header--terminal"
        style={{
          backgroundColor: bgColor,
          borderRadius: "9999px",
          justifyContent: "center",
        }}
      >
        <span className="af-node__icon">{icon}</span>
        <span className="af-node__type">{data.definition.type}</span>
      </div>

      <div className="af-node__body" style={{ textAlign: "center" }}>
        <div className="af-node__name">{data.label}</div>
      </div>
    </div>
  );
}

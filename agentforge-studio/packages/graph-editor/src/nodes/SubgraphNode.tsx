import { Handle, Position, type NodeProps } from "reactflow";
import type { NodeData } from "./node-data.js";
import { getNodeConfig } from "./node-data.js";

/**
 * SubgraphNode - specialized renderer for SUBGRAPH nodes.
 *
 * Visual: dashed border with nested graph indicator.
 * Shows the subgraph reference and a visual cue that it contains a nested graph.
 */
export function SubgraphNode({ data, selected }: NodeProps<NodeData>) {
  const config = getNodeConfig(data.definition.type);
  const bgColor = config?.color ?? "#78716c";
  const icon = config?.icon ?? "▣";
  const subgraphId = data.definition.config?.graphId;

  return (
    <div
      className={`af-node af-node--subgraph ${selected ? "af-node--selected" : ""}`}
      style={{
        borderColor: bgColor,
        borderStyle: "dashed",
        borderWidth: "2px",
        minWidth: "180px",
      }}
    >
      <Handle type="target" position={Position.Top} className="af-handle" />

      <div className="af-node__header" style={{ backgroundColor: bgColor }}>
        <span className="af-node__icon">{icon}</span>
        <span className="af-node__type">SUBGRAPH</span>
      </div>

      <div className="af-node__body">
        <div className="af-node__name">{data.label}</div>

        {subgraphId != null && (
          <div className="af-node__ref">
            <span className="af-node__ref-label">Graph:</span>
            <span className="af-node__ref-value">{String(subgraphId)}</span>
          </div>
        )}

        <div className="af-node__subgraph-indicator">
          <span className="af-node__nested-label">▣ Nested Graph</span>
        </div>
      </div>

      <Handle type="source" position={Position.Bottom} className="af-handle" />
    </div>
  );
}

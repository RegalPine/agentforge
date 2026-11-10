import { cn } from "../../lib/utils";

/**
 * RadarChart - SVG-based radar/spider chart component.
 * Used for Defense Radar, Risk Vector, multi-dimensional evaluation.
 */

export interface RadarAxis {
  /** Axis label */
  label: string;
  /** Axis key (matches data keys) */
  key: string;
  /** Max value for this axis (default 100) */
  max?: number;
}

export interface RadarDataset {
  /** Dataset label */
  label: string;
  /** Data values keyed by axis key */
  values: Record<string, number>;
  /** Stroke color */
  color?: string;
  /** Fill color (defaults to color with opacity) */
  fillColor?: string;
}

export interface RadarChartProps {
  axes: RadarAxis[];
  datasets: RadarDataset[];
  width?: number;
  height?: number;
  className?: string;
}

const defaultColors = ["#aac7ff", "#ddb7ff", "#4cd7f6"];

export function RadarChart({
  axes,
  datasets,
  width = 240,
  height = 240,
  className,
}: RadarChartProps) {
  const cx = width / 2;
  const cy = height / 2;
  const radius = Math.min(cx, cy) - 32;
  const levels = 4; // concentric rings
  const angleStep = (2 * Math.PI) / axes.length;

  // Helper: get point on radar for a given axis index and value ratio
  const getPoint = (axisIndex: number, ratio: number) => {
    const angle = angleStep * axisIndex - Math.PI / 2; // start from top
    return {
      x: cx + radius * ratio * Math.cos(angle),
      y: cy + radius * ratio * Math.sin(angle),
    };
  };

  // Build polygon path for a dataset
  const buildPolygonPath = (dataset: RadarDataset) => {
    return axes
      .map((axis, i) => {
        const max = axis.max ?? 100;
        const value = dataset.values[axis.key] ?? 0;
        const ratio = Math.min(value / max, 1);
        const point = getPoint(i, ratio);
        return `${i === 0 ? "M" : "L"}${point.x},${point.y}`;
      })
      .join(" ") + " Z";
  };

  return (
    <div className={cn("inline-flex flex-col items-center", className)}>
      <svg width={width} height={height} viewBox={`0 0 ${width} ${height}`} className="select-none">
        {/* Concentric level rings */}
        {Array.from({ length: levels }, (_, level) => {
          const ratio = (level + 1) / levels;
          const points = axes.map((_, i) => {
            const p = getPoint(i, ratio);
            return `${p.x},${p.y}`;
          });
          return (
            <polygon
              key={`level-${level}`}
              points={points.join(" ")}
              fill="none"
              stroke="rgba(139,145,160,0.15)"
              strokeWidth={1}
            />
          );
        })}

        {/* Axis lines */}
        {axes.map((_, i) => {
          const p = getPoint(i, 1);
          return (
            <line
              key={`axis-${i}`}
              x1={cx}
              y1={cy}
              x2={p.x}
              y2={p.y}
              stroke="rgba(139,145,160,0.2)"
              strokeWidth={1}
            />
          );
        })}

        {/* Dataset polygons */}
        {datasets.map((dataset, di) => {
          const color = dataset.color ?? defaultColors[di % defaultColors.length];
          const fill = dataset.fillColor ?? color;
          return (
            <g key={dataset.label}>
              <path
                d={buildPolygonPath(dataset)}
                fill={fill}
                fillOpacity={0.12}
                stroke={color}
                strokeWidth={1.5}
              />
              {/* Data points */}
              {axes.map((axis, i) => {
                const max = axis.max ?? 100;
                const value = dataset.values[axis.key] ?? 0;
                const ratio = Math.min(value / max, 1);
                const p = getPoint(i, ratio);
                return (
                  <circle
                    key={`${dataset.label}-${axis.key}`}
                    cx={p.x}
                    cy={p.y}
                    r={2.5}
                    fill={color}
                    stroke="#0f141b"
                    strokeWidth={1}
                  />
                );
              })}
            </g>
          );
        })}

        {/* Axis labels */}
        {axes.map((axis, i) => {
          const p = getPoint(i, 1.18);
          return (
            <text
              key={`label-${axis.key}`}
              x={p.x}
              y={p.y}
              textAnchor="middle"
              dominantBaseline="central"
              className="fill-on-surface-variant"
              fontSize="10"
              fontFamily="Inter, sans-serif"
            >
              {axis.label}
            </text>
          );
        })}
      </svg>

      {/* Legend */}
      {datasets.length > 1 && (
        <div className="flex items-center gap-3 mt-1">
          {datasets.map((ds, i) => (
            <div key={ds.label} className="flex items-center gap-1">
              <span
                className="w-2 h-2 rounded-full"
                style={{ backgroundColor: ds.color ?? defaultColors[i % defaultColors.length] }}
              />
              <span className="text-[10px] font-label-sm text-on-surface-variant">{ds.label}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

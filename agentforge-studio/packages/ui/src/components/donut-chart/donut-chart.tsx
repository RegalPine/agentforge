import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from "recharts";

/**
 * DonutChart - Recharts-based donut/ring chart component.
 * Used for distribution visualizations (e.g., memory type breakdown).
 */

export interface DonutChartDatum {
  label: string;
  value: number;
  color?: string;
}

export interface DonutChartProps {
  data: DonutChartDatum[];
  width?: number | string;
  height?: number;
  colorScheme?: "primary" | "secondary" | "tertiary" | "multi";
  /** Inner radius ratio (0-1), default 0.6 */
  innerRadiusRatio?: number;
  /** Center label */
  centerLabel?: string;
  centerValue?: string | number;
  className?: string;
}

const defaultColors = ["#aac7ff", "#ddb7ff", "#4cd7f6", "#3e90ff", "#009eb9", "#ffb4ab", "#6f00be", "#acedff"];

export function DonutChart({
  data,
  width = "100%",
  height = 160,
  innerRadiusRatio = 0.6,
  centerLabel,
  centerValue,
  className,
}: DonutChartProps) {
  const radius = Math.min(height, 160) / 2 - 8;
  const innerRadius = radius * innerRadiusRatio;

  return (
    <div className={className} style={{ width, height }}>
      <ResponsiveContainer width="100%" height={height}>
        <PieChart>
          <Pie
            data={data}
            dataKey="value"
            nameKey="label"
            cx="50%"
            cy="50%"
            innerRadius={innerRadius}
            outerRadius={radius}
            strokeWidth={0}
            paddingAngle={2}
          >
            {data.map((entry, index) => (
              <Cell key={entry.label} fill={entry.color ?? defaultColors[index % defaultColors.length]} />
            ))}
          </Pie>
          <Tooltip
            contentStyle={{
              backgroundColor: "#1b2027",
              border: "1px solid #414754",
              borderRadius: "6px",
              fontSize: "11px",
              color: "#dee2ec",
            }}
          />
          {/* Center text rendered as custom label */}
          {(centerLabel || centerValue !== undefined) && (
            <text
              x="50%"
              y="50%"
              textAnchor="middle"
              dominantBaseline="central"
              className="fill-on-surface"
            >
              {centerValue !== undefined && (
                <tspan x="50%" dy="-0.5em" className="fill-on-surface" fontSize="16" fontWeight="600">
                  {centerValue}
                </tspan>
              )}
              {centerLabel && (
                <tspan x="50%" dy="1.4em" fontSize="10" className="fill-on-surface-variant">
                  {centerLabel}
                </tspan>
              )}
            </text>
          )}
        </PieChart>
      </ResponsiveContainer>
    </div>
  );
}

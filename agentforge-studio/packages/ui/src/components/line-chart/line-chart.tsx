import {
  LineChart as RechartsLineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Area,
  AreaChart,
} from "recharts";

/**
 * LineChart - Recharts-based line chart component.
 * Used for Ebbinghaus Decay curves, throughput trends, etc.
 */

export interface LineChartDatum {
  label: string;
  value: number;
  [key: string]: string | number;
}

export interface LineChartProps {
  data: LineChartDatum[];
  width?: number | string;
  height?: number;
  colorScheme?: "primary" | "secondary" | "tertiary" | "multi";
  /** Show area fill under the line */
  showArea?: boolean;
  /** Data key for the value */
  dataKey?: string;
  /** Label key for X axis */
  labelKey?: string;
  className?: string;
}

const colorMap = {
  primary: { stroke: "#aac7ff", fill: "#aac7ff" },
  secondary: { stroke: "#ddb7ff", fill: "#ddb7ff" },
  tertiary: { stroke: "#4cd7f6", fill: "#4cd7f6" },
  multi: { stroke: "#aac7ff", fill: "#aac7ff" },
};

export function LineChart({
  data,
  width = "100%",
  height = 160,
  colorScheme = "primary",
  showArea = true,
  dataKey = "value",
  labelKey = "label",
  className,
}: LineChartProps) {
  const colors = colorMap[colorScheme];

  const chart = (
    <ResponsiveContainer width="100%" height={height}>
      {showArea ? (
        <AreaChart data={data} margin={{ top: 4, right: 4, bottom: 0, left: 0 }}>
          <defs>
            <linearGradient id={`gradient-${colorScheme}`} x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor={colors.fill} stopOpacity={0.2} />
              <stop offset="95%" stopColor={colors.fill} stopOpacity={0} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" stroke="rgba(139,145,160,0.15)" />
          <XAxis
            dataKey={labelKey}
            tick={{ fontSize: 10, fill: "#8b91a0" }}
            axisLine={{ stroke: "rgba(139,145,160,0.3)" }}
            tickLine={false}
          />
          <YAxis
            tick={{ fontSize: 10, fill: "#8b91a0" }}
            axisLine={false}
            tickLine={false}
            width={32}
          />
          <Tooltip
            contentStyle={{
              backgroundColor: "#1b2027",
              border: "1px solid #414754",
              borderRadius: "6px",
              fontSize: "11px",
              color: "#dee2ec",
            }}
          />
          <Area
            type="monotone"
            dataKey={dataKey}
            stroke={colors.stroke}
            strokeWidth={1.5}
            fill={`url(#gradient-${colorScheme})`}
            dot={false}
            activeDot={{ r: 3, fill: colors.stroke, stroke: "#0f141b", strokeWidth: 2 }}
          />
        </AreaChart>
      ) : (
        <RechartsLineChart data={data} margin={{ top: 4, right: 4, bottom: 0, left: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="rgba(139,145,160,0.15)" />
          <XAxis
            dataKey={labelKey}
            tick={{ fontSize: 10, fill: "#8b91a0" }}
            axisLine={{ stroke: "rgba(139,145,160,0.3)" }}
            tickLine={false}
          />
          <YAxis
            tick={{ fontSize: 10, fill: "#8b91a0" }}
            axisLine={false}
            tickLine={false}
            width={32}
          />
          <Tooltip
            contentStyle={{
              backgroundColor: "#1b2027",
              border: "1px solid #414754",
              borderRadius: "6px",
              fontSize: "11px",
              color: "#dee2ec",
            }}
          />
          <Line
            type="monotone"
            dataKey={dataKey}
            stroke={colors.stroke}
            strokeWidth={1.5}
            dot={false}
            activeDot={{ r: 3, fill: colors.stroke, stroke: "#0f141b", strokeWidth: 2 }}
          />
        </RechartsLineChart>
      )}
    </ResponsiveContainer>
  );

  return (
    <div className={className} style={{ width }}>
      {chart}
    </div>
  );
}

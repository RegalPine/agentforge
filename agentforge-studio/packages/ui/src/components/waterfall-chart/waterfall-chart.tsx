import { cn } from "../../lib/utils";

/**
 * WaterfallChart - Span Trace waterfall visualization.
 * Displays horizontal bars representing stage execution times,
 * arranged in a waterfall/sequence layout.
 */

export interface WaterfallSpan {
  /** Stage name */
  name: string;
  /** Start time offset in ms from the beginning */
  startMs: number;
  /** Duration in ms */
  durationMs: number;
  /** Color theme */
  color?: "primary" | "secondary" | "tertiary" | "error" | "outline";
  /** Optional status indicator */
  status?: "success" | "warning" | "error";
}

export interface WaterfallChartProps {
  data: WaterfallSpan[];
  width?: number | string;
  height?: number;
  className?: string;
}

const barColors = {
  primary: "bg-primary/60",
  secondary: "bg-secondary/60",
  tertiary: "bg-tertiary/60",
  error: "bg-error/60",
  outline: "bg-outline/40",
};

const statusDotColors = {
  success: "bg-success",
  warning: "bg-warning",
  error: "bg-error",
};

export function WaterfallChart({ data, width = "100%", className }: WaterfallChartProps) {
  if (data.length === 0) return null;

  // Calculate total time span
  const totalMs = Math.max(...data.map((s) => s.startMs + s.durationMs));
  const rowHeight = 24;
  const labelWidth = 120;
  const timeWidth = 56;

  return (
    <div
      className={cn("rounded-lg border border-outline-variant/40 bg-surface-container-low overflow-hidden", className)}
      style={{ width }}
    >
      {/* Header */}
      <div className="flex items-center px-3 py-1.5 border-b border-outline-variant/30 text-[10px] font-label-sm text-on-surface-variant uppercase tracking-wider">
        <span style={{ width: labelWidth }}>Stage</span>
        <span className="flex-1">Timeline</span>
        <span style={{ width: timeWidth, textAlign: "right" }}>Duration</span>
      </div>

      {/* Rows */}
      <div className="divide-y divide-outline-variant/20">
        {data.map((span, i) => {
          const barLeft = totalMs > 0 ? (span.startMs / totalMs) * 100 : 0;
          const barWidth = totalMs > 0 ? (span.durationMs / totalMs) * 100 : 0;
          const colorClass = barColors[span.color ?? "primary"];
          const statusColor = span.status ? statusDotColors[span.status] : null;

          return (
            <div
              key={`${span.name}-${i}`}
              className="flex items-center px-3 hover:bg-surface-container/50 transition-colors"
              style={{ height: rowHeight }}
            >
              {/* Label */}
              <div className="flex items-center gap-1.5" style={{ width: labelWidth }}>
                {statusColor && (
                  <span className={cn("w-1.5 h-1.5 rounded-full shrink-0", statusColor)} />
                )}
                <span className="text-[11px] font-code-sm text-on-surface-variant truncate">
                  {span.name}
                </span>
              </div>

              {/* Bar area */}
              <div className="flex-1 relative h-3 mx-2">
                <div
                  className={cn("absolute top-0 h-full rounded-sm transition-all", colorClass)}
                  style={{
                    left: `${barLeft}%`,
                    width: `${Math.max(barWidth, 0.5)}%`,
                  }}
                />
              </div>

              {/* Duration */}
              <div style={{ width: timeWidth }} className="text-right">
                <span className="text-[10px] font-code-sm text-on-surface-variant tabular-nums">
                  {span.durationMs.toFixed(1)}ms
                </span>
              </div>
            </div>
          );
        })}
      </div>

      {/* Footer: total time */}
      <div className="flex items-center justify-end px-3 py-1 border-t border-outline-variant/30">
        <span className="text-[10px] font-code-sm text-outline tabular-nums">
          Total: {totalMs.toFixed(1)}ms
        </span>
      </div>
    </div>
  );
}

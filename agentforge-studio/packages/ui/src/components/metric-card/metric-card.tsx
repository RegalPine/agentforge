import { TrendingUp, TrendingDown, Minus } from "lucide-react";
import { cn } from "../../lib/utils";
import { StatusLight } from "../status-light/status-light.js";
import { SparkLine } from "../sparkline/sparkline.js";
import type { StatusLightProps } from "../status-light/status-light.js";

export interface MetricCardProps {
  title: string;
  value: string | number;
  unit?: string;
  trend?: "up" | "down" | "flat";
  trendValue?: string;
  status?: StatusLightProps["status"];
  sparklineData?: number[];
  sparklineColor?: string;
  className?: string;
}

const trendIcons = {
  up: TrendingUp,
  down: TrendingDown,
  flat: Minus,
};

const trendColors = {
  up: "text-success",
  down: "text-error",
  flat: "text-outline",
};

export function MetricCard({
  title,
  value,
  unit,
  trend,
  trendValue,
  status = "neutral",
  sparklineData,
  sparklineColor,
  className,
}: MetricCardProps) {
  const TrendIcon = trend ? trendIcons[trend] : null;

  return (
    <div
      className={cn(
        "flex items-center justify-between p-space-xs px-space-sm bg-surface-container-low rounded hover:bg-surface-container transition-colors",
        className,
      )}
    >
      {/* Left: title + value */}
      <div className="flex flex-col min-w-0">
        <span className="font-label-sm text-label-sm uppercase text-outline tracking-wider truncate">{title}</span>
        <span className="font-headline-md text-headline-md text-on-surface font-semibold leading-tight">
          {value}
          {unit && <span className="ml-1 text-[11px] font-label-sm text-outline">{unit}</span>}
        </span>
      </div>

      {/* Right: trend or sparkline */}
      {TrendIcon && trendValue ? (
        <span className={cn("flex items-center gap-0.5 text-[11px] font-label-sm shrink-0", trendColors[trend!])}>
          <TrendIcon className="h-3.5 w-3.5" />
          {trendValue}
        </span>
      ) : sparklineData && sparklineData.length > 1 ? (
        <SparkLine
          data={sparklineData}
          width={60}
          height={20}
          color={sparklineColor ?? "var(--color-primary)"}
          fillColor={sparklineColor ?? "var(--color-primary)"}
        />
      ) : (
        <StatusLight status={status} size="sm" />
      )}
    </div>
  );
}

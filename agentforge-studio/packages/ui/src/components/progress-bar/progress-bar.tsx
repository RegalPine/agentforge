import { cn } from "../../lib/utils";

export interface ProgressBarProps {
  value: number; // 0-100
  size?: "xs" | "sm" | "md";
  color?: "primary" | "success" | "error" | "warning" | "tertiary";
  showLabel?: boolean;
  className?: string;
}

const sizeMap = { xs: "h-1", sm: "h-1.5", md: "h-2" };
const colorMap = {
  primary: "bg-primary",
  success: "bg-success",
  error: "bg-error",
  warning: "bg-amber-400",
  tertiary: "bg-tertiary",
};

export function ProgressBar({
  value,
  size = "sm",
  color = "primary",
  showLabel = false,
  className,
}: ProgressBarProps) {
  const pct = Math.min(100, Math.max(0, value));
  return (
    <div className={cn("flex items-center gap-2", className)}>
      <div className={cn("flex-1 rounded-full bg-surface-container-high overflow-hidden", sizeMap[size])}>
        <div
          className={cn("h-full rounded-full transition-all duration-300", colorMap[color])}
          style={{ width: `${pct}%` }}
        />
      </div>
      {showLabel && (
        <span className="text-[10px] font-code-sm text-outline tabular-nums min-w-[2.5ch] text-right">
          {Math.round(pct)}%
        </span>
      )}
    </div>
  );
}

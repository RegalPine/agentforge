import { cn } from "../../lib/utils";

export interface StatusLightProps {
  status: "success" | "error" | "warning" | "neutral" | "running";
  size?: "sm" | "md";
  pulse?: boolean;
  className?: string;
}

const statusColors: Record<string, string> = {
  success: "bg-success",
  error: "bg-error",
  warning: "bg-amber-400",
  neutral: "bg-outline",
  running: "bg-primary",
};

export function StatusLight({ status, size = "md", pulse = false, className }: StatusLightProps) {
  const dim = size === "sm" ? "h-1.5 w-1.5" : "h-2 w-2";
  return (
    <span
      className={cn(
        "inline-block rounded-full shrink-0",
        dim,
        statusColors[status] ?? statusColors.neutral,
        pulse && "animate-ping-slow",
        className,
      )}
    />
  );
}

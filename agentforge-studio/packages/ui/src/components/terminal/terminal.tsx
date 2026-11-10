import * as React from "react";
import { cn } from "../../lib/utils";

export interface TerminalLine {
  text: string;
  type?: "info" | "success" | "error" | "warning" | "sse" | "runtime";
  timestamp?: string;
}

export interface TerminalProps extends React.HTMLAttributes<HTMLDivElement> {
  lines: TerminalLine[];
  autoScroll?: boolean;
  maxLines?: number;
}

const typeColorMap: Record<string, string> = {
  info: "text-on-surface-variant",
  success: "text-success",
  error: "text-error",
  warning: "text-accent-tool",
  sse: "text-tertiary",
  runtime: "text-primary",
};

export function Terminal({
  lines,
  autoScroll = true,
  maxLines = 1000,
  className,
  ...props
}: TerminalProps) {
  const scrollRef = React.useRef<HTMLDivElement>(null);

  React.useEffect(() => {
    if (autoScroll && scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [lines.length, autoScroll]);

  const visible = lines.slice(-maxLines);

  return (
    <div
      ref={scrollRef}
      className={cn(
        "bg-surface p-2 font-code-sm text-code-sm leading-tight overflow-auto",
        className
      )}
      {...props}
    >
      {visible.map((line, i) => (
        <div key={i} className={cn("flex gap-2", typeColorMap[line.type ?? "info"] ?? "text-on-surface-variant")}>
          {line.timestamp && (
            <span className="text-outline shrink-0 select-none">{line.timestamp}</span>
          )}
          <span className="whitespace-pre-wrap break-all">{line.text}</span>
        </div>
      ))}
      {visible.length === 0 && (
        <div className="text-outline italic">No output</div>
      )}
    </div>
  );
}

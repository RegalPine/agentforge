import * as React from "react";
import { cn } from "../../lib/utils";

export interface BreadcrumbSegment {
  label: string;
  path?: string;
}

export interface BreadcrumbIDEProps extends React.HTMLAttributes<HTMLDivElement> {
  segments: BreadcrumbSegment[];
  onSegmentClick?: (path: string) => void;
}

export function BreadcrumbIDE({
  segments,
  onSegmentClick,
  className,
  ...props
}: BreadcrumbIDEProps) {
  return (
    <div
      className={cn(
        "flex h-6 items-center gap-1 bg-surface-container px-3 font-code-sm text-code-sm text-outline overflow-x-auto",
        className
      )}
      {...props}
    >
      {segments.map((seg, i) => {
        const isLast = i === segments.length - 1;
        return (
          <React.Fragment key={i}>
            {i > 0 && <span className="text-outline/50 select-none">&gt;</span>}
            {seg.path && !isLast ? (
              <button
                onClick={() => onSegmentClick?.(seg.path!)}
                className="hover:text-on-surface cursor-pointer transition-colors"
              >
                {seg.label}
              </button>
            ) : (
              <span className={isLast ? "text-on-surface font-medium" : ""}>
                {seg.label}
              </span>
            )}
          </React.Fragment>
        );
      })}
    </div>
  );
}

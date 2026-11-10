import { cn } from "../../lib/utils";

/**
 * PipelineStage - RAG Pipeline stage visualization component.
 * Displays a single stage in a multi-stage processing pipeline
 * with colored border, timing, and count metrics.
 */

export interface PipelineStageData {
  /** Stage number (1-based) */
  index: number;
  /** Stage display name */
  name: string;
  /** Processing latency in ms */
  latencyMs: number;
  /** Items count after this stage */
  count?: number;
  /** Optional count label (e.g. "42 chunks", "Top-20") */
  countLabel?: string;
  /** Color theme for the left border */
  borderColor: "primary" | "secondary" | "tertiary" | "tertiary-container" | "primary-container" | "outline" | "secondary-container" | "error";
  /** Whether this stage has a ring/highlight effect */
  highlighted?: boolean;
  /** Subtitle/description */
  subtitle?: string;
}

export interface PipelineStageProps {
  stage: PipelineStageData;
  className?: string;
}

const borderColors: Record<PipelineStageData["borderColor"], string> = {
  primary: "border-l-primary",
  secondary: "border-l-secondary",
  tertiary: "border-l-tertiary",
  "tertiary-container": "border-l-tertiary-container",
  "primary-container": "border-l-primary-container",
  outline: "border-l-outline",
  "secondary-container": "border-l-secondary-container",
  error: "border-l-error",
};

export function PipelineStage({ stage, className }: PipelineStageProps) {
  return (
    <div
      className={cn(
        "rounded-lg border border-outline-variant/40 border-l-[3px] bg-surface-container-low p-3 flex flex-col gap-1.5 transition-colors hover:bg-surface-container",
        borderColors[stage.borderColor],
        stage.highlighted && "ring-1 ring-primary/40",
        className,
      )}
    >
      {/* Header: stage number + name */}
      <div className="flex items-center gap-2">
        <span className="text-[10px] font-label-sm text-outline bg-surface-container-high rounded px-1.5 py-0.5 tabular-nums">
          #{stage.index}
        </span>
        <span className="text-[12px] font-label-md text-on-surface font-medium truncate">
          {stage.name}
        </span>
      </div>

      {/* Metrics row */}
      <div className="flex items-center gap-3 mt-auto">
        <span className="text-[11px] font-code-sm text-tertiary tabular-nums">
          {stage.latencyMs.toFixed(1)}ms
        </span>
        {stage.count !== undefined && (
          <span className="text-[11px] font-code-sm text-on-surface-variant tabular-nums">
            {stage.countLabel ?? stage.count}
          </span>
        )}
        {stage.subtitle && (
          <span className="text-[10px] font-code-sm text-outline truncate ml-auto">
            {stage.subtitle}
          </span>
        )}
      </div>
    </div>
  );
}

/**
 * PipelineChain - renders a sequence of connected PipelineStage cards
 * in a grid layout with flow connectors.
 */
export interface PipelineChainProps {
  stages: PipelineStageData[];
  columns?: number;
  className?: string;
}

export function PipelineChain({ stages, columns = 5, className }: PipelineChainProps) {
  return (
    <div
      className={cn("grid gap-2", className)}
      style={{ gridTemplateColumns: `repeat(${columns}, 1fr)` }}
    >
      {stages.map((stage, i) => (
        <div key={stage.index} className="relative">
          <PipelineStage stage={stage} />
          {/* Flow connector arrow (except last) */}
          {i < stages.length - 1 && (
            <div className="absolute -right-1.5 top-1/2 -translate-y-1/2 z-10 text-outline-variant">
              <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
                <path d="M4 2L8 6L4 10" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </div>
          )}
        </div>
      ))}
    </div>
  );
}

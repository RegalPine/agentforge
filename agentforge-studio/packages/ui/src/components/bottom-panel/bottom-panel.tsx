import * as React from "react";
import { cn } from "../../lib/utils";

export interface BottomPanelTab {
  id: string;
  label: string;
  badge?: number;
  indicator?: "live" | "error";
}

export interface BottomPanelProps extends React.HTMLAttributes<HTMLDivElement> {
  tabs: BottomPanelTab[];
  activeTab: string;
  onTabSelect: (id: string) => void;
  height: number;
  onHeightChange: (h: number) => void;
  minHeight?: number;
  maxHeight?: number;
  collapsible?: boolean;
  onCollapse?: () => void;
}

export function BottomPanel({
  tabs,
  activeTab,
  onTabSelect,
  height,
  onHeightChange,
  minHeight = 100,
  maxHeight = 500,
  collapsible = true,
  onCollapse,
  className,
  children,
  ...props
}: BottomPanelProps) {
  const dragRef = React.useRef<{ startY: number; startH: number } | null>(null);

  const handleMouseDown = React.useCallback(
    (e: React.MouseEvent) => {
      e.preventDefault();
      dragRef.current = { startY: e.clientY, startH: height };
      const handleMove = (ev: MouseEvent) => {
        if (!dragRef.current) return;
        const delta = dragRef.current.startY - ev.clientY;
        onHeightChange(Math.max(minHeight, Math.min(maxHeight, dragRef.current.startH + delta)));
      };
      const handleUp = () => {
        dragRef.current = null;
        document.removeEventListener("mousemove", handleMove);
        document.removeEventListener("mouseup", handleUp);
      };
      document.addEventListener("mousemove", handleMove);
      document.addEventListener("mouseup", handleUp);
    },
    [height, onHeightChange, minHeight, maxHeight]
  );

  return (
    <div
      className={cn("flex flex-col border-t border-outline-variant", className)}
      style={{ height }}
      {...props}
    >
      {/* Drag handle */}
      <div
        className="h-1 cursor-row-resize hover:bg-accent-primary/30 transition-colors"
        onMouseDown={handleMouseDown}
      />
      {/* Tab bar */}
      <div className="flex h-8 items-center bg-surface-container-lowest px-2 gap-0.5">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => onTabSelect(tab.id)}
            className={cn(
              "flex items-center gap-1.5 px-2 py-1 text-label-sm uppercase transition-colors",
              activeTab === tab.id
                ? "text-primary border-b border-primary font-semibold"
                : "text-outline hover:text-on-surface"
            )}
          >
            {tab.indicator === "live" && (
              <span className="h-1.5 w-1.5 rounded-full bg-success animate-pulse" />
            )}
            {tab.indicator === "error" && (
              <span className="h-1.5 w-1.5 rounded-full bg-error" />
            )}
            {tab.label}
            {tab.badge != null && tab.badge > 0 && (
              <span className="flex h-4 min-w-4 items-center justify-center rounded bg-surface-container-highest px-1 text-[9px] text-outline">
                {tab.badge}
              </span>
            )}
          </button>
        ))}
        {collapsible && (
          <button
            onClick={onCollapse}
            className="ml-auto text-outline hover:text-on-surface p-1"
            aria-label="Collapse panel"
          >
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="1.5">
              <path d="M2 8L6 4L10 8" />
            </svg>
          </button>
        )}
      </div>
      {/* Content */}
      <div className="flex-1 overflow-auto bg-surface-container font-code-sm text-code-sm">
        {children}
      </div>
    </div>
  );
}

import * as React from "react";
import { cn } from "../../lib/utils";

export interface StatusBarItem {
  id: string;
  icon?: React.ReactNode;
  label: string;
  color?: string;
  pulse?: boolean;
  onClick?: () => void;
}

export interface StatusBarProps extends React.HTMLAttributes<HTMLDivElement> {
  leftItems: StatusBarItem[];
  rightItems: StatusBarItem[];
}

export function StatusBar({
  leftItems,
  rightItems,
  className,
  ...props
}: StatusBarProps) {
  return (
    <div
      className={cn(
        "flex h-6 items-center justify-between bg-surface-container-lowest border-t border-outline-variant px-2 text-label-sm text-outline",
        className
      )}
      {...props}
    >
      <div className="flex items-center gap-3">
        {leftItems.map((item) => (
          <StatusBarItem key={item.id} item={item} />
        ))}
      </div>
      <div className="flex items-center gap-3">
        {rightItems.map((item) => (
          <StatusBarItem key={item.id} item={item} />
        ))}
      </div>
    </div>
  );
}

function StatusBarItem({ item }: { item: StatusBarItem }) {
  const Comp = item.onClick ? "button" : "span";
  return (
    <Comp
      onClick={item.onClick}
      className={cn(
        "flex items-center gap-1 hover:text-on-surface transition-colors",
        item.color,
        item.onClick && "cursor-pointer"
      )}
    >
      {item.pulse && <span className="h-1.5 w-1.5 rounded-full bg-success animate-pulse" />}
      {item.icon}
      <span>{item.label}</span>
    </Comp>
  );
}

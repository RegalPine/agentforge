import * as React from "react";
import { cn } from "../../lib/utils";

export interface TopBarProps extends React.HTMLAttributes<HTMLDivElement> {
  children?: React.ReactNode;
}

export function TopBar({ className, children, ...props }: TopBarProps) {
  return (
    <div
      className={cn(
        "flex flex-col bg-surface-container-lowest/90 backdrop-blur-xl border-b border-outline-variant",
        className
      )}
      {...props}
    >
      {children}
    </div>
  );
}

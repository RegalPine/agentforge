import * as React from "react";
import { cn } from "../../lib/utils";
import { X } from "lucide-react";

export interface TabBarItem {
  id: string;
  title: string;
  icon?: React.ReactNode;
  dirty?: boolean;
  badge?: number;
  closeable?: boolean;
}

export interface TabBarProps extends React.HTMLAttributes<HTMLDivElement> {
  tabs: TabBarItem[];
  activeTab: string | null;
  onTabSelect: (id: string) => void;
  onTabClose: (id: string) => void;
}

export function TabBar({
  tabs,
  activeTab,
  onTabSelect,
  onTabClose,
  className,
  ...props
}: TabBarProps) {
  return (
    <div
      className={cn(
        "flex h-10 items-center bg-surface-container-lowest border-b border-outline-variant overflow-x-auto",
        className
      )}
      role="tablist"
      {...props}
    >
      {tabs.map((tab) => (
        <TabBarItem
          key={tab.id}
          tab={tab}
          active={activeTab === tab.id}
          onSelect={() => onTabSelect(tab.id)}
          onClose={() => onTabClose(tab.id)}
        />
      ))}
    </div>
  );
}

function TabBarItem({
  tab,
  active,
  onSelect,
  onClose,
}: {
  tab: TabBarItem;
  active: boolean;
  onSelect: () => void;
  onClose: () => void;
}) {
  return (
    <div
      role="tab"
      aria-selected={active}
      onClick={onSelect}
      className={cn(
        "group relative flex h-full items-center gap-1.5 border-r border-outline-variant px-3 text-label-md cursor-pointer select-none transition-colors",
        active
          ? "bg-surface-container text-on-surface border-t-2 border-t-primary"
          : "text-on-surface-variant hover:text-on-surface hover:bg-surface-container"
      )}
    >
      {tab.icon && <span className="shrink-0">{tab.icon}</span>}
      {tab.dirty && !active && (
        <span className="h-2 w-2 rounded-full bg-outline" />
      )}
      <span className="truncate max-w-[120px]">{tab.title}</span>
      {tab.badge != null && tab.badge > 0 && (
        <span className="flex h-4 min-w-4 items-center justify-center rounded-full bg-surface-container-highest px-1 text-[9px] font-semibold text-outline">
          {tab.badge}
        </span>
      )}
      {tab.closeable !== false && (
        <button
          onClick={(e) => {
            e.stopPropagation();
            onClose();
          }}
          className="ml-0.5 flex h-4 w-4 items-center justify-center rounded opacity-0 hover:bg-surface-container-highest group-hover:opacity-100 transition-opacity"
          aria-label={`Close ${tab.title}`}
        >
          <X className="h-3 w-3" />
        </button>
      )}
    </div>
  );
}

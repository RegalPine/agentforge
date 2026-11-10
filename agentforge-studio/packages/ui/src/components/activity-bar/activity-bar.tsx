import * as React from "react";
import { cn } from "../../lib/utils";

export interface ActivityBarItem {
  id: string;
  icon: React.ReactNode;
  label: string;
  badge?: number;
}

export interface ActivityBarProps extends React.HTMLAttributes<HTMLDivElement> {
  items: ActivityBarItem[];
  activeItem: string;
  onItemClick: (id: string) => void;
  bottomItems?: ActivityBarItem[];
}

export function ActivityBar({
  items,
  activeItem,
  onItemClick,
  bottomItems,
  className,
  ...props
}: ActivityBarProps) {
  return (
    <div
      className={cn(
        "flex w-12 flex-col items-center justify-between bg-surface-container-lowest border-r border-outline-variant py-2",
        className
      )}
      role="tablist"
      aria-label="Activity Bar"
      {...props}
    >
      <div className="flex flex-col items-center gap-1">
        {items.map((item) => (
          <ActivityBarButton
            key={item.id}
            item={item}
            active={activeItem === item.id}
            onClick={() => onItemClick(item.id)}
          />
        ))}
      </div>
      {bottomItems && (
        <div className="flex flex-col items-center gap-1">
          {bottomItems.map((item) => (
            <ActivityBarButton
              key={item.id}
              item={item}
              active={activeItem === item.id}
              onClick={() => onItemClick(item.id)}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function ActivityBarButton({
  item,
  active,
  onClick,
}: {
  item: ActivityBarItem;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button
      role="tab"
      aria-selected={active}
      aria-label={item.label}
      title={item.label}
      onClick={onClick}
      className={cn(
        "relative flex h-10 w-10 items-center justify-center rounded transition-colors duration-fast",
        active
          ? "bg-surface-container-high text-primary"
          : "text-outline hover:text-on-surface hover:bg-surface-container"
      )}
    >
      {item.icon}
      {item.badge != null && item.badge > 0 && (
        <span className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-accent-primary px-1 text-[9px] font-semibold text-on-primary">
          {item.badge}
        </span>
      )}
    </button>
  );
}

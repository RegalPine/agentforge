import * as React from "react";
import { cn } from "../../lib/utils";
import { ChevronRight, ChevronDown } from "lucide-react";

export interface FileExplorerItem {
  path: string;
  name: string;
  icon: React.ReactNode;
  iconColor?: string;
  children?: FileExplorerItem[];
}

export interface FileExplorerGroup {
  id: string;
  title: string;
  icon?: React.ReactNode;
  children: FileExplorerItem[];
  defaultExpanded?: boolean;
}

export interface FileExplorerProps extends React.HTMLAttributes<HTMLDivElement> {
  groups: FileExplorerGroup[];
  activePath?: string;
  onFileSelect: (path: string) => void;
  expandedGroups?: Set<string>;
  onToggleGroup?: (id: string) => void;
  width?: number;
}

export function FileExplorer({
  groups,
  activePath,
  onFileSelect,
  expandedGroups,
  onToggleGroup,
  width = 256,
  className,
  ...props
}: FileExplorerProps) {
  const [internalExpanded, setInternalExpanded] = React.useState<Set<string>>(
    () => new Set(groups.filter((g) => g.defaultExpanded !== false).map((g) => g.id))
  );

  const expanded = expandedGroups ?? internalExpanded;
  const toggle = onToggleGroup ?? ((id: string) => {
    setInternalExpanded((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  });

  return (
    <div
      className={cn(
        "flex flex-1 flex-col border-r border-outline-variant bg-surface-container-low overflow-y-auto",
        className
      )}
      style={{ width }}
      {...props}
    >
      {groups.map((group) => (
        <div key={group.id}>
          <button
            onClick={() => toggle(group.id)}
            className="flex w-full items-center gap-1.5 px-3 py-1.5 text-label-sm text-outline uppercase tracking-wider hover:text-on-surface hover:bg-surface-container transition-colors"
          >
            {expanded.has(group.id) ? (
              <ChevronDown className="h-3 w-3 shrink-0" />
            ) : (
              <ChevronRight className="h-3 w-3 shrink-0" />
            )}
            {group.icon && <span className="shrink-0">{group.icon}</span>}
            <span className="truncate">{group.title}</span>
          </button>
          {expanded.has(group.id) && (
            <div className="pb-1">
              {group.children.map((item) => (
                <ExplorerItem
                  key={item.path}
                  item={item}
                  activePath={activePath}
                  onSelect={onFileSelect}
                  depth={0}
                />
              ))}
            </div>
          )}
        </div>
      ))}
    </div>
  );
}

function ExplorerItem({
  item,
  activePath,
  onSelect,
  depth,
}: {
  item: FileExplorerItem;
  activePath?: string;
  onSelect: (path: string) => void;
  depth: number;
}) {
  const [expanded, setExpanded] = React.useState(false);
  const isActive = activePath === item.path;
  const hasChildren = item.children && item.children.length > 0;

  return (
    <>
      <button
        onClick={() => {
          if (hasChildren) setExpanded(!expanded);
          onSelect(item.path);
        }}
        className={cn(
          "flex w-full items-center gap-1.5 py-0.5 text-body-sm transition-colors",
          isActive
            ? "bg-surface-container-high text-primary font-medium"
            : "text-on-surface-variant hover:text-on-surface hover:bg-surface-container",
        )}
        style={{ paddingLeft: `${12 + depth * 12}px`, paddingRight: "8px" }}
      >
        {hasChildren && (
          expanded ? <ChevronDown className="h-3 w-3 shrink-0" /> : <ChevronRight className="h-3 w-3 shrink-0" />
        )}
        <span className={cn("shrink-0", item.iconColor)}>{item.icon}</span>
        <span className="truncate">{item.name}</span>
      </button>
      {hasChildren && expanded && (
        <div>
          {item.children!.map((child) => (
            <ExplorerItem
              key={child.path}
              item={child}
              activePath={activePath}
              onSelect={onSelect}
              depth={depth + 1}
            />
          ))}
        </div>
      )}
    </>
  );
}

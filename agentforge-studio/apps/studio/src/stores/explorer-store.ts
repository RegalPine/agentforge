import { create } from "zustand";

/**
 * ExplorerStore - Activity Bar + File Explorer state.
 *
 * Manages which activity is active in the left icon rail,
 * whether the explorer panel is collapsed, and which file/group is selected.
 */
interface ExplorerState {
  activeActivity: string;
  explorerCollapsed: boolean;
  explorerWidth: number;
  expandedGroups: Set<string>;
  activeFilePath: string | null;

  setActiveActivity: (id: string) => void;
  toggleExplorer: () => void;
  setExplorerCollapsed: (collapsed: boolean) => void;
  setExplorerWidth: (w: number) => void;
  toggleGroup: (id: string) => void;
  setActiveFile: (path: string | null) => void;
}

export const useExplorerStore = create<ExplorerState>((set) => ({
  activeActivity: "explorer",
  explorerCollapsed: false,
  explorerWidth: 256,
  expandedGroups: new Set(["core", "intelligence"]),
  activeFilePath: null,

  setActiveActivity: (id) => set({ activeActivity: id }),
  toggleExplorer: () => set((s) => ({ explorerCollapsed: !s.explorerCollapsed })),
  setExplorerCollapsed: (collapsed) => set({ explorerCollapsed: collapsed }),
  setExplorerWidth: (w) => set({ explorerWidth: Math.max(180, Math.min(480, w)) }),
  toggleGroup: (id) =>
    set((s) => {
      const next = new Set(s.expandedGroups);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return { expandedGroups: next };
    }),
  setActiveFile: (path) => set({ activeFilePath: path }),
}));

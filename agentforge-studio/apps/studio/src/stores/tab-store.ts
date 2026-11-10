import { create } from "zustand";

/**
 * TabStore - multi-tab editor state.
 *
 * Each open page/file is a tab. Tabs can be reordered, closed, and marked dirty.
 */
export interface TabItem {
  id: string;
  title: string;
  icon?: string;
  dirty?: boolean;
  badge?: number;
  closeable?: boolean;
  routePath?: string;
}

interface TabState {
  openTabs: TabItem[];
  activeTabId: string | null;
  tabHistory: string[];

  openTab: (tab: TabItem) => void;
  closeTab: (id: string) => void;
  setActiveTab: (id: string) => void;
  setDirty: (id: string, dirty: boolean) => void;
  closeAllTabs: () => void;
  closeOtherTabs: (keepId: string) => void;
  closeTabsToTheRight: (id: string) => void;
}

export const useTabStore = create<TabState>((set) => ({
  openTabs: [],
  activeTabId: null,
  tabHistory: [],

  openTab: (tab) =>
    set((s) => {
      const exists = s.openTabs.find((t) => t.id === tab.id);
      if (exists) {
        return {
          activeTabId: tab.id,
          tabHistory: [tab.id, ...s.tabHistory.filter((h) => h !== tab.id)].slice(0, 20),
        };
      }
      return {
        openTabs: [...s.openTabs, { closeable: true, ...tab }],
        activeTabId: tab.id,
        tabHistory: [tab.id, ...s.tabHistory.filter((h) => h !== tab.id)].slice(0, 20),
      };
    }),

  closeTab: (id) =>
    set((s) => {
      const idx = s.openTabs.findIndex((t) => t.id === id);
      if (idx === -1) return s;
      const tab = s.openTabs[idx];
      if (tab.closeable === false) return s;
      const next = s.openTabs.filter((t) => t.id !== id);
      let nextActive = s.activeTabId;
      if (s.activeTabId === id) {
        nextActive = next[Math.min(idx, next.length - 1)]?.id ?? null;
      }
      return {
        openTabs: next,
        activeTabId: nextActive,
        tabHistory: s.tabHistory.filter((h) => h !== id),
      };
    }),

  setActiveTab: (id) =>
    set((s) => ({
      activeTabId: id,
      tabHistory: [id, ...s.tabHistory.filter((h) => h !== id)].slice(0, 20),
    })),

  setDirty: (id, dirty) =>
    set((s) => ({
      openTabs: s.openTabs.map((t) => (t.id === id ? { ...t, dirty } : t)),
    })),

  closeAllTabs: () =>
    set((s) => ({
      openTabs: s.openTabs.filter((t) => t.closeable === false),
      activeTabId: null,
      tabHistory: [],
    })),

  closeOtherTabs: (keepId) =>
    set((s) => ({
      openTabs: s.openTabs.filter((t) => t.id === keepId || t.closeable === false),
      activeTabId: keepId,
      tabHistory: [keepId],
    })),

  closeTabsToTheRight: (id) =>
    set((s) => {
      const idx = s.openTabs.findIndex((t) => t.id === id);
      if (idx === -1) return s;
      const keep = s.openTabs.slice(0, idx + 1);
      return {
        openTabs: keep.filter((t) => t.closeable !== false || t.id === id),
        activeTabId: keep.find((t) => t.id === s.activeTabId) ? s.activeTabId : id,
      };
    }),
}));

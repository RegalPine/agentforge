import { create } from "zustand";

/**
 * PanelStore - bottom panel (terminal/output/problems/SSE) state.
 */
interface PanelState {
  bottomPanelOpen: boolean;
  bottomPanelHeight: number;
  bottomPanelActiveTab: string;

  toggleBottomPanel: () => void;
  setBottomPanelOpen: (open: boolean) => void;
  setBottomPanelHeight: (h: number) => void;
  setBottomPanelTab: (id: string) => void;
}

export const usePanelStore = create<PanelState>((set) => ({
  bottomPanelOpen: true,
  bottomPanelHeight: 200,
  bottomPanelActiveTab: "terminal",

  toggleBottomPanel: () => set((s) => ({ bottomPanelOpen: !s.bottomPanelOpen })),
  setBottomPanelOpen: (open) => set({ bottomPanelOpen: open }),
  setBottomPanelHeight: (h) => set({ bottomPanelHeight: Math.max(100, Math.min(500, h)) }),
  setBottomPanelTab: (id) => set({ bottomPanelActiveTab: id, bottomPanelOpen: true }),
}));

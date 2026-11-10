import { create } from "zustand";

/**
 * EditorStore - local UI state for the graph editor.
 *
 * Uses Zustand for local UI state only.
 * Server state (graphs, executions) uses TanStack Query.
 */
interface EditorState {
  selectedNodeId: string | null;
  selectedEdgeId: string | null;
  panelOpen: boolean;
  editorMode: "select" | "connect" | "pan";

  setSelectedNodeId: (id: string | null) => void;
  setSelectedEdgeId: (id: string | null) => void;
  setPanelOpen: (open: boolean) => void;
  setEditorMode: (mode: "select" | "connect" | "pan") => void;
}

export const useEditorStore = create<EditorState>((set) => ({
  selectedNodeId: null,
  selectedEdgeId: null,
  panelOpen: true,
  editorMode: "select",

  setSelectedNodeId: (id) => set({ selectedNodeId: id, selectedEdgeId: null }),
  setSelectedEdgeId: (id) => set({ selectedEdgeId: id, selectedNodeId: null }),
  setPanelOpen: (open) => set({ panelOpen: open }),
  setEditorMode: (mode) => set({ editorMode: mode }),
}));
